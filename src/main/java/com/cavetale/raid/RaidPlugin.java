package com.cavetale.raid;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import lombok.NonNull;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class RaidPlugin extends JavaPlugin {
    HashMap<String, Raid> raids = new HashMap<>();
    HashMap<String, Instance> instances = new HashMap<>();
    RaidCommand raidCommand = new RaidCommand(this);
    EventListener eventListener = new EventListener(this);
    Gson gson = new Gson();
    Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
    File raidFolder;
    Random random = new Random();
    Json json = new Json(this);
    Yaml yaml = new Yaml(this);

    @Override
    public void onEnable() {
        raidFolder = new File(getDataFolder(), "raids");
        raidFolder.mkdirs();
        loadRaids();
        getCommand("raid").setExecutor(raidCommand);
        getServer().getScheduler().runTaskTimer(this, this::onTick, 1L, 1L);
        getServer().getPluginManager().registerEvents(eventListener, this);
        // Raid instances
        for (World world : getServer().getWorlds()) {
            Raid raid = raids.get(world.getName());
            if (raid != null) raidInstance(raid);
        }
    }

    @Override
    public void onDisable() {
        for (Instance instance : instances.values()) {
            instance.clear();
        }
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
            World world = getServer().getWorld(raid.worldName);
            if (world == null) return null;
            inst = new Instance(this, raid, world);
            instances.put(raid.worldName, inst);
        }
        return inst;
    }

    Instance raidInstance(@NonNull World world) {
        Raid raid = raids.get(world.getName());
        if (raid == null) return null;
        return raidInstance(raid);
    }

    void onTick() {
        for (Instance instance : instances.values()) {
            instance.onTick();
        }
    }

    double rnd() {
        return random.nextBoolean()
            ? random.nextDouble()
            : -random.nextDouble();
    }
}
