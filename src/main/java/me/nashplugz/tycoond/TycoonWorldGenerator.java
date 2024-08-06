package me.nashplugz.tycoond;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

public class TycoonWorldGenerator extends ChunkGenerator {
    private static final int PLOT_SIZE = 35;
    private static final int BORDER_WIDTH = 1;
    private static final int PATH_WIDTH = 3;
    private static final int TOTAL_SIZE = PLOT_SIZE + 2 * BORDER_WIDTH + PATH_WIDTH;

    @Override
    public void generateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;

                if (isPlotArea(worldX, worldZ)) {
                    chunkData.setBlock(x, 64, z, Material.GRASS_BLOCK);
                } else if (isBorder(worldX, worldZ)) {
                    chunkData.setBlock(x, 64, z, Material.STONE);
                    chunkData.setBlock(x, 65, z, Material.SMOOTH_STONE_SLAB);
                } else {
                    chunkData.setBlock(x, 64, z, Material.BIRCH_PLANKS);
                }

                for (int y = 0; y < 64; y++) {
                    chunkData.setBlock(x, y, z, Material.STONE);
                }
            }
        }
    }

    private boolean isPlotArea(int x, int z) {
        int plotX = Math.floorMod(x, TOTAL_SIZE);
        int plotZ = Math.floorMod(z, TOTAL_SIZE);
        return plotX > BORDER_WIDTH && plotX <= BORDER_WIDTH + PLOT_SIZE &&
                plotZ > BORDER_WIDTH && plotZ <= BORDER_WIDTH + PLOT_SIZE;
    }

    private boolean isBorder(int x, int z) {
        int plotX = Math.floorMod(x, TOTAL_SIZE);
        int plotZ = Math.floorMod(z, TOTAL_SIZE);
        return (plotX == BORDER_WIDTH || plotX == BORDER_WIDTH + PLOT_SIZE + 1) &&
                (plotZ > BORDER_WIDTH && plotZ <= BORDER_WIDTH + PLOT_SIZE) ||
                (plotZ == BORDER_WIDTH || plotZ == BORDER_WIDTH + PLOT_SIZE + 1) &&
                        (plotX > BORDER_WIDTH && plotX <= BORDER_WIDTH + PLOT_SIZE);
    }

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateBedrock() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }
}
