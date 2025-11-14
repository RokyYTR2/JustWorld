package dev.meyba.justWorld.world;

import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import java.util.UUID;

public record WorldData(
        String name,
        UUID uuid,
        World.Environment environment,
        WorldType worldType,
        boolean generateStructures,
        long seed,
        boolean pvpEnabled,
        boolean keepSpawnInMemory,
        boolean autoLoad,
        GeneratorType generatorType
) {

    public enum GeneratorType {
        DEFAULT,  // Standard Minecraft generation
        VOID,     // Empty void world (fastest)
        FLAT      // Simple flat world (very fast)
    }

    public static WorldData fromWorld(World world) {
        return new WorldData(
                world.getName(),
                world.getUID(),
                world.getEnvironment(),
                world.getWorldType(),
                world.canGenerateStructures(),
                world.getSeed(),
                world.getPVP(),
                world.getKeepSpawnInMemory(),
                true,
                GeneratorType.DEFAULT
        );
    }

    public WorldCreator toWorldCreator() {
        WorldCreator creator = new WorldCreator(name)
                .environment(environment)
                .type(worldType)
                .generateStructures(generateStructures)
                .seed(seed);

        // Apply custom generator for maximum speed
        switch (generatorType) {
            case VOID -> creator.generator(new VoidWorldGenerator());
            case FLAT -> creator.generator(new FlatWorldGenerator());
            // DEFAULT uses vanilla generation
        }

        return creator;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static class Builder {
        private final String name;
        private World.Environment environment = World.Environment.NORMAL;
        private WorldType worldType = WorldType.NORMAL;
        private boolean generateStructures = true;
        private long seed = 0;
        private boolean pvpEnabled = true;
        private boolean keepSpawnInMemory = false;
        private boolean autoLoad = true;
        private GeneratorType generatorType = GeneratorType.DEFAULT;

        private Builder(String name) {
            this.name = name;
        }

        public Builder environment(World.Environment environment) {
            this.environment = environment;
            return this;
        }

        public Builder worldType(WorldType worldType) {
            this.worldType = worldType;
            return this;
        }

        public Builder generateStructures(boolean generateStructures) {
            this.generateStructures = generateStructures;
            return this;
        }

        public Builder seed(long seed) {
            this.seed = seed;
            return this;
        }

        public Builder pvpEnabled(boolean pvpEnabled) {
            this.pvpEnabled = pvpEnabled;
            return this;
        }

        public Builder keepSpawnInMemory(boolean keepSpawnInMemory) {
            this.keepSpawnInMemory = keepSpawnInMemory;
            return this;
        }

        public Builder autoLoad(boolean autoLoad) {
            this.autoLoad = autoLoad;
            return this;
        }

        public Builder generatorType(GeneratorType generatorType) {
            this.generatorType = generatorType;
            return this;
        }

        public WorldData build() {
            return new WorldData(
                    name,
                    UUID.randomUUID(),
                    environment,
                    worldType,
                    generateStructures,
                    seed,
                    pvpEnabled,
                    keepSpawnInMemory,
                    autoLoad,
                    generatorType
            );
        }
    }
}