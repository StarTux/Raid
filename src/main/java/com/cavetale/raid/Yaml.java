package com.cavetale.raid;

import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

@RequiredArgsConstructor
public final class Yaml {
    private final JavaPlugin plugin;
    Gson gson = new Gson();
    org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();

    /**
     * Load a config file from the config folder, using the resource
     * that comes with the package jar as default.  Save the resource
     * to disk if it does not already exist.
     */
    public ConfigurationSection load(@NonNull final File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        return config;
    }

    <E> E load(File file, Class<E> clazz) {
        Object o;
        try (FileReader in = new FileReader(file)) {
            o = yaml.load(in);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return null;
        }
        String tmp = gson.toJson(o);
        return gson.fromJson(tmp, clazz);
    }
}
