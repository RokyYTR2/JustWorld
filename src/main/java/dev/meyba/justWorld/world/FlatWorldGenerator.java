package dev.meyba.justWorld.world;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * Ultra-fast flat world generator
 * Creates simple flat worlds with minimal generation time
 */
public class FlatWorldGenerator extends ChunkGenerator {

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random, int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        // Generate only essential layers for maximum speed
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                // Bedrock at Y=0
                chunkData.setBlock(x, 0, z, Material.BEDROCK);
                // Dirt layers (2 blocks)
                chunkData.setBlock(x, 1, z, Material.DIRT);
                chunkData.setBlock(x, 2, z, Material.DIRT);
                // Grass on top
                chunkData.setBlock(x, 3, z, Material.GRASS_BLOCK);
            }
        }
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
