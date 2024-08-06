package me.nashplugz.tycoond;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class PlotManager {
    public static final int PLOT_SIZE = 35;
    public static final int BORDER_WIDTH = 1;
    public static final int PATH_WIDTH = 3;
    public static final int TOTAL_SIZE = PLOT_SIZE + BORDER_WIDTH + PATH_WIDTH;
    private final Map<String, Plot> plots = new HashMap<>();
    private final TycoonDroppers plugin;
    private final LoadingCache<UUID, List<Plot>> playerPlotsCache;

    public PlotManager(TycoonDroppers plugin) {
        this.plugin = plugin;
        this.playerPlotsCache = CacheBuilder.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build(new CacheLoader<UUID, List<Plot>>() {
                    @Override
                    public List<Plot> load(UUID playerUUID) {
                        return plots.values().stream()
                                .filter(plot -> plot.isOwner(playerUUID))
                                .collect(Collectors.toList());
                    }
                });
    }

    public Plot getPlot(int x, int z) {
        int plotX = Math.floorDiv(x - BORDER_WIDTH, TOTAL_SIZE);
        int plotZ = Math.floorDiv(z - BORDER_WIDTH, TOTAL_SIZE);
        String key = plotX + "," + plotZ;
        Plot plot = plots.get(key);
        if (plot == null) {
            plot = new Plot(plotX, plotZ);
            plots.put(key, plot);
        }
        return plot;
    }

    public boolean isWithinBuildableArea(Location location, Plot plot) {
        int plotStartX = plot.getX() * TOTAL_SIZE + BORDER_WIDTH;
        int plotStartZ = plot.getZ() * TOTAL_SIZE + BORDER_WIDTH;

        int relativeX = location.getBlockX() - plotStartX;
        int relativeZ = location.getBlockZ() - plotStartZ;

        return relativeX >= 0 && relativeX < PLOT_SIZE &&
                relativeZ >= 0 && relativeZ < PLOT_SIZE;
    }

    public boolean canBuild(Player player, Location location) {
        Plot plot = getPlot(location.getBlockX(), location.getBlockZ());
        if (plot == null || !plot.canEdit(player.getUniqueId())) {
            return false;
        }

        return isWithinBuildableArea(location, plot);
    }

    public void claimPlot(Player player, int x, int z) {
        int plotX = Math.floorDiv(x - BORDER_WIDTH, TOTAL_SIZE);
        int plotZ = Math.floorDiv(z - BORDER_WIDTH, TOTAL_SIZE);
        String key = plotX + "," + plotZ;
        Plot plot = plots.get(key);
        if (plot == null) {
            plot = new Plot(plotX, plotZ);
            plots.put(key, plot);
        }
        plot.setOwner(player.getUniqueId());
        playerPlotsCache.invalidate(player.getUniqueId());
        savePlayerDataAsync(player);
    }

    public Plot findNearestUnclaimedPlot(Player player) {
        for (int x = 0; x < 100; x++) {
            for (int z = 0; z < 100; z++) {
                String key = x + "," + z;
                Plot plot = plots.get(key);
                if (plot == null || plot.getOwner() == null) {
                    return new Plot(x, z);
                }
            }
        }
        return null;
    }

    public void teleportToPlot(Player player, Plot plot) {
        if (plot != null) {
            World world = plugin.getServer().getWorld("CoinCrazeTycoon");
            if (world != null) {
                int x = plot.getX() * TOTAL_SIZE + BORDER_WIDTH + PLOT_SIZE / 2;
                int z = plot.getZ() * TOTAL_SIZE + BORDER_WIDTH + PLOT_SIZE / 2;
                Location location = new Location(world, x + 0.5, 65, z + 0.5);
                player.teleport(location);
            }
        }
    }

    public int getPlayerPlotCount(UUID playerUUID) {
        return getPlayerPlots(playerUUID).size();
    }

    public List<Plot> getPlayerPlots(UUID playerUUID) {
        try {
            return playerPlotsCache.get(playerUUID);
        } catch (ExecutionException e) {
            plugin.getLogger().log(Level.WARNING, "Error loading player plots", e);
            return Collections.emptyList();
        }
    }

    public boolean tryClaimNewPlot(Player player) {
        if (getPlayerPlotCount(player.getUniqueId()) >= plugin.getTycoonConfig().getMaxPlotsPerPlayer()) {
            player.sendMessage(ChatColor.RED + "You have reached the maximum number of plots.");
            return false;
        }

        double cost = getNextPlotCost(player);
        if (!canAffordNextPlot(player)) {
            player.sendMessage(ChatColor.RED + "You cannot afford a new plot. Cost: $" + String.format("%.2f", cost));
            return false;
        }

        Plot plot = findNearestUnclaimedPlot(player);
        if (plot == null) {
            plot = createNewPlot();
        }

        plugin.getEconomy().withdrawPlayer(player, cost);
        plot.setOwner(player.getUniqueId());
        playerPlotsCache.invalidate(player.getUniqueId());
        teleportToPlot(player, plot);
        player.sendMessage(ChatColor.GREEN + "You have claimed a new plot for $" + String.format("%.2f", cost));
        savePlayerDataAsync(player);
        return true;
    }

    public boolean createInitialPlot(Player player) {
        if (getPlayerPlotCount(player.getUniqueId()) > 0) {
            player.sendMessage(ChatColor.RED + "You already have an initial plot.");
            return false;
        }

        Plot plot = createNewPlot();
        plot.setOwner(player.getUniqueId());
        playerPlotsCache.invalidate(player.getUniqueId());
        teleportToPlot(player, plot);
        player.sendMessage(ChatColor.GREEN + "You have created your initial plot!");
        savePlayerDataAsync(player);
        return true;
    }

    public boolean claimPlot(Player player) {
        Location playerLocation = player.getLocation();
        Plot currentPlot = getPlot(playerLocation.getBlockX(), playerLocation.getBlockZ());

        if (currentPlot == null || !isWithinBuildableArea(playerLocation, currentPlot)) {
            player.sendMessage(ChatColor.RED + "You must be standing within the 35x35 grass area of an unclaimed plot to claim it.");
            return false;
        }

        if (currentPlot.getOwner() != null) {
            player.sendMessage(ChatColor.RED + "This plot is already claimed. Use /plotnear to find an unclaimed plot.");
            return false;
        }

        if (getPlayerPlotCount(player.getUniqueId()) >= plugin.getTycoonConfig().getMaxPlotsPerPlayer()) {
            player.sendMessage(ChatColor.RED + "You have reached the maximum number of plots.");
            return false;
        }

        double cost = getNextPlotCost(player);
        if (!canAffordNextPlot(player)) {
            player.sendMessage(ChatColor.RED + "You cannot afford this plot. Cost: $" + String.format("%.2f", cost));
            return false;
        }

        plugin.getEconomy().withdrawPlayer(player, cost);
        currentPlot.setOwner(player.getUniqueId());
        playerPlotsCache.invalidate(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "You have claimed this plot for $" + String.format("%.2f", cost));
        savePlayerDataAsync(player);
        return true;
    }

    public boolean teleportToNearestUnclaimedPlot(Player player) {
        Plot plot = findNearestUnclaimedPlot(player);
        if (plot == null) {
            player.sendMessage(ChatColor.RED + "No unclaimed plots found.");
            return false;
        }

        teleportToPlot(player, plot);
        player.sendMessage(ChatColor.GREEN + "Teleported to the nearest unclaimed plot.");
        return true;
    }

    private Plot createNewPlot() {
        int newPlotX = 0;
        int newPlotZ = 0;
        String key = newPlotX + "," + newPlotZ;
        while (plots.containsKey(key)) {
            newPlotZ++;
            if (newPlotZ > 100) {
                newPlotZ = 0;
                newPlotX++;
            }
            key = newPlotX + "," + newPlotZ;
        }
        Plot plot = new Plot(newPlotX, newPlotZ);
        plots.put(key, plot);
        return plot;
    }

    public void savePlayerDataAsync(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Implement async data saving
            plugin.getLogger().info("Saving data for player: " + player.getName());
        });
    }

    public void loadPlayerDataAsync(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Implement async data loading
            plugin.getLogger().info("Loading data for player: " + player.getName());
        });
    }

    public void unloadPlayerChunks(Player player) {
        List<Plot> playerPlots = getPlayerPlots(player.getUniqueId());
        World world = plugin.getServer().getWorld("CoinCrazeTycoon");
        if (world != null) {
            for (Plot plot : playerPlots) {
                int startX = plot.getX() * TOTAL_SIZE;
                int startZ = plot.getZ() * TOTAL_SIZE;
                for (int x = startX; x < startX + PLOT_SIZE; x += 16) {
                    for (int z = startZ; z < startZ + PLOT_SIZE; z += 16) {
                        Chunk chunk = world.getChunkAt(x >> 4, z >> 4);
                        if (chunk.isLoaded() && !chunk.isForceLoaded()) {
                            chunk.unload();
                        }
                    }
                }
            }
        }
    }

    public void loadPlayerChunks(Player player) {
        List<Plot> playerPlots = getPlayerPlots(player.getUniqueId());
        World world = plugin.getServer().getWorld("CoinCrazeTycoon");
        if (world != null) {
            for (Plot plot : playerPlots) {
                int startX = plot.getX() * TOTAL_SIZE;
                int startZ = plot.getZ() * TOTAL_SIZE;
                for (int x = startX; x < startX + PLOT_SIZE; x += 16) {
                    for (int z = startZ; z < startZ + PLOT_SIZE; z += 16) {
                        world.getChunkAt(x >> 4, z >> 4).load();
                    }
                }
            }
        }
    }

    public double getNextPlotCost(Player player) {
        int ownedPlots = getPlayerPlotCount(player.getUniqueId());
        return plugin.getTycoonConfig().getClaimCost(ownedPlots);
    }

    public boolean canAffordNextPlot(Player player) {
        double cost = getNextPlotCost(player);
        return plugin.getEconomy().has(player, cost);
    }
}
