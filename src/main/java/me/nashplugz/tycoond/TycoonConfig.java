package me.nashplugz.tycoond;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class TycoonConfig {
    private final TycoonDroppers plugin;
    private FileConfiguration config;
    private File configFile;

    private double initialClaimCost;
    private double secondPlotCost;
    private int maxPlotsPerPlayer;

    public TycoonConfig(TycoonDroppers plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "config.yml");
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // Load values from config or set defaults
        initialClaimCost = config.getDouble("initialClaimCost", 1000.0);
        secondPlotCost = config.getDouble("secondPlotCost", 400000.0);
        maxPlotsPerPlayer = config.getInt("maxPlotsPerPlayer", 5);

        // Save config to add any missing values
        saveConfig();
    }

    private void saveConfig() {
        if (config == null || configFile == null) {
            return;
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config to " + configFile);
        }
    }

    public double getClaimCost(int ownedPlots) {
        if (ownedPlots == 0) {
            return initialClaimCost;
        } else if (ownedPlots == 1) {
            return secondPlotCost;
        } else {
            return secondPlotCost * (2 * ownedPlots);
        }
    }

    public int getMaxPlotsPerPlayer() {
        return maxPlotsPerPlayer;
    }

    public double getInitialClaimCost() {
        return initialClaimCost;
    }

    public double getSecondPlotCost() {
        return secondPlotCost;
    }

    public void setInitialClaimCost(double cost) {
        initialClaimCost = cost;
        config.set("initialClaimCost", cost);
        saveConfig();
    }

    public void setSecondPlotCost(double cost) {
        secondPlotCost = cost;
        config.set("secondPlotCost", cost);
        saveConfig();
    }

    public void setMaxPlotsPerPlayer(int max) {
        maxPlotsPerPlayer = max;
        config.set("maxPlotsPerPlayer", max);
        saveConfig();
    }
}
