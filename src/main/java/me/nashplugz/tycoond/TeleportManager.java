package me.nashplugz.tycoond;

import org.bukkit.entity.Player;

public class TeleportManager {
    private final TycoonDroppers plugin;
    private final PlotManager plotManager;

    public TeleportManager(TycoonDroppers plugin, PlotManager plotManager) {
        this.plugin = plugin;
        this.plotManager = plotManager;
    }

    public void teleportToNearestUnclaimedPlot(Player player) {
        Plot plot = plotManager.findNearestUnclaimedPlot(player);
        if (plot != null) {
            plotManager.teleportToPlot(player, plot);
        }
    }

    public void warpToPlot(Player player, int plotNumber) {
        // Implement logic to teleport player to their specified plot
    }

    // Other teleportation methods...
}
