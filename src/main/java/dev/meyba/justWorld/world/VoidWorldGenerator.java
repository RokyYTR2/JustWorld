package dev.meyba.justWorld.world;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

public class VoidWorldGenerator extends ChunkGenerator {

    @Override
    public void generateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {
<<<<<<< HEAD
        // Generate single bedrock block at spawn (0, 64, 0)
        if (chunkX == 0 && chunkZ == 0) {
            chunkData.setBlock(0, 64, 0, Material.BEDROCK);
=======
        if (chunkX == 0 && chunkZ == 0) {
            for (int x = 0; x < 5; x++) {
                for (int z = 0; z < 5; z++) {
                    chunkData.setBlock(x, 64, z, Material.BEDROCK);
                }
            }
>>>>>>> f323cb6245fa5c1077ee077af97d9e46ea4ea616
        }
    }

    @Override
    public void generateSurface(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {}

    @Override
    public void generateBedrock(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {}

    @Override
    public void generateCaves(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {}

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
<<<<<<< HEAD
        // Spawn on single bedrock block
        return new Location(world, 0.5, 65, 0.5);
=======
        return new Location(world, 2.5, 65, 2.5);
>>>>>>> f323cb6245fa5c1077ee077af97d9e46ea4ea616
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