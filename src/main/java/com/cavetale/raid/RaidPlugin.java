package com.cavetale.raid;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class RaidPlugin extends JavaPlugin {
    @Getter private static RaidPlugin instance;
    HashMap<String, Raid> raids = new HashMap<>();
    HashMap<String, Instance> instances = new HashMap<>();
    RaidCommand raidCommand = new RaidCommand(this);
    RaidEditCommand raidEditCommand = new RaidEditCommand(this);
    EventListener eventListener = new EventListener(this);
    Gson gson = new Gson();
    Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
    File raidFolder;
    Json json = new Json(this);
    Yaml yaml = new Yaml(this);
    Sessions sessions;
    private final Map<Integer, EscortMarker.Handle> idEscortMap = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadConfig();
        raidFolder = new File(getDataFolder(), "raids");
        raidFolder.mkdirs();
        loadRaids();
        getCommand("raid").setExecutor(raidCommand);
        getCommand("raidedit").setExecutor(raidEditCommand);
        getServer().getScheduler().runTaskTimer(this, this::tick, 1L, 1L);
        getServer().getPluginManager().registerEvents(eventListener, this);
        sessions = new Sessions(this).enable();
        // Raid instances
        for (World world : getServer().getWorlds()) {
            Raid raid = raids.get(world.getName());
            if (raid != null) raidInstance(raid);
        }
    }

    @Override
    public void onDisable() {
        for (Instance it : instances.values()) {
            it.clear();
        }
        instances.clear();
        raids.clear();
        sessions.disable();
    }

    void loadRaids() {
        for (File file : raidFolder.listFiles()) {
            String name = file.getName();
            if (!name.endsWith(".json")) continue;
            name = name.substring(0, name.length() - 5);
            try (FileReader reader = new FileReader(file)) {
                Raid raid = gson.fromJson(reader, Raid.class);
                raid.worldName = name;
                raids.put(name, raid);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    boolean saveRaid(@NonNull Raid raid) {
        raid.onSave();
        File file = new File(raidFolder, raid.worldName + ".json");
        try (FileWriter writer = new FileWriter(file)) {
            prettyGson.toJson(raid, writer);
            return true;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        }
    }

    Instance raidInstance(@NonNull Raid raid) {
        Instance inst = instances.get(raid.worldName);
        if (inst == null) {
            inst = new Instance(this, raid);
            instances.put(raid.worldName, inst);
            World world = raid.getWorld();
            if (world != null) {
                inst.onWorldLoaded(world);
            }
        }
        return inst;
    }

    Instance raidInstance(@NonNull World world) {
        Raid raid = raids.get(world.getName());
        if (raid == null) return null;
        return raidInstance(raid);
    }

    void tick() {
        for (Instance it : instances.values()) {
            it.tick();
        }
        sessions.tick();
    }
}
