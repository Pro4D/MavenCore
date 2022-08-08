package com.pro4d.mavencore;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MConfig {

    private final FileConfiguration config;
    private File f;
    public MConfig(String name, MavenCore plugin) {
        config = createConfig(name, plugin);
    }

    public FileConfiguration getConfig() {return config;}

//    public void saveConfig() {
//        try {
//            config.save(f);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

    private FileConfiguration createConfig(String name, MavenCore plugin) {
        String configName = name + ".yml";
        String pathName = plugin.getDataFolder().getAbsolutePath() + "/" + configName;

        Path path = Paths.get(pathName);
        if (!path.toFile().exists()) {
            try {
                if (path.toFile().createNewFile()) {
                    plugin.getLogger().info("Generated config!");
                }

            } catch (IOException ignored) {
            }
            plugin.saveResource(configName, true);
        }
        f = path.toFile();

        return YamlConfiguration.loadConfiguration(path.toFile());
    }

}
