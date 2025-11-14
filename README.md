# JustWorld

⚡ **Ultra-fast async world management plugin for Minecraft**

A highly optimized alternative to Multiverse-Core with full asynchronous operations and modern Java 21 features.

## Features

- **⚡ Blazing Fast** - All world operations run asynchronously with optimized spawn chunk handling
- **🚀 Ultra-Fast Generators** - Void worlds create in ~50ms, flat worlds in ~100ms!
- **📊 Performance Metrics** - See exactly how long world creation takes
- **⚡ Smart Teleportation** - Instant teleports to loaded worlds, async loading for unloaded ones
- **🔧 Modern Java 21** - Utilizes latest features like Records and CompletableFuture
- **🚀 Optimized** - Minimal overhead, ConcurrentHashMap for thread-safe operations
- **💾 Persistent** - Automatic world configuration saving
- **📈 Statistics Dashboard** - `/justworld` shows plugin stats, TPS, memory usage
- **🎯 Simple API** - Easy-to-use commands and developer API

## Commands

### World Management (`/world`, `/w`, `/jw`)

- `/world create <name> [normal|nether|end|void|flat] [seed]` - Create a new world
  - **normal/nether/end** - Standard Minecraft generation
  - **void** - Empty void world (**Ultra Fast!** ~50-200ms)
  - **flat** - Simple flat world (**Very Fast!** ~100-400ms)
  - Shows creation time after completion!
- `/world delete <name> [confirm]` - Delete a world including all files
- `/world load <name>` - Load an existing world
- `/world unload <name>` - Unload a world from server (without deleting)
- `/world tp <name>` - Teleport to a world (instant if already loaded!)
- `/world list` - List all loaded worlds
- `/world info <name>` - View detailed world information

### Plugin Info (`/justworld`, `/jworld`)

- `/justworld` - Display plugin info, statistics, and performance metrics
- `/justworld reload` - Reload plugin configuration

## Permissions

- `justworld.admin` - Access to all commands
- `justworld.create` - Create worlds
- `justworld.delete` - Delete worlds
- `justworld.load` - Load worlds
- `justworld.unload` - Unload worlds
- `justworld.teleport` - Teleport between worlds
- `justworld.list` - List worlds
- `justworld.info` - View world info

## Installation

1. Download the latest release
2. Place the `.jar` file in your `plugins/` folder
3. Restart your server

## Build

```bash
./gradlew build
```

## Developer API

```java
WorldManager worldManager = JustWorld.getInstance().getWorldManager();

// Create ultra-fast void world
WorldData voidWorld = WorldData.builder("lobby")
    .generatorType(WorldData.GeneratorType.VOID)  // ~50ms creation!
    .pvpEnabled(false)
    .build();

worldManager.createWorldAsync(voidWorld).thenAccept(result -> {
    if (result.isSuccess()) {
        System.out.println("World created in " + result.getFormattedTime());
    }
});

// Create fast flat world
WorldData flatWorld = WorldData.builder("buildworld")
    .generatorType(WorldData.GeneratorType.FLAT)  // ~100ms creation!
    .build();

// Standard world with custom settings
WorldData normalWorld = WorldData.builder("survival")
    .environment(World.Environment.NORMAL)
    .seed(12345L)
    .pvpEnabled(true)
    .build();

// Asynchronous world loading
worldManager.loadWorldAsync("myworld").thenAccept(world -> {
    // World loaded
});
```

## Performance Optimizations

### Default Settings
- **`keep-spawn-in-memory: false`** - Spawn chunks aren't loaded during creation (massive speed boost!)
- **`keepSpawnLoaded: false`** - WorldCreator optimization for faster world generation

### Architecture
- **ConcurrentHashMap** - Lock-free thread-safe data storage
- **CompletableFuture** - Modern async programming patterns
- **Records** - Immutable data classes with low memory footprint
- **Lazy Loading** - Worlds only load when needed
- **Async File I/O** - Non-blocking configuration saves

### Benchmarks
Typical world creation times:
- **Void world**: ~50-200ms (**Fastest!** Perfect for lobbies/arenas)
- **Flat world**: ~100-400ms (**Very Fast!** Great for building/minigames)
- **End**: ~200-600ms
- **Nether**: ~300-800ms
- **Normal world**: ~500-1500ms (vs 3-5s with spawn chunks loaded)

*Results may vary based on hardware and seed complexity*

### Teleportation Speed
- **Already loaded world**: Instant! (no async overhead)
- **Unloaded world**: Loads asynchronously, then teleports

The plugin intelligently checks if a world is already loaded before attempting async operations, resulting in instant teleports for loaded worlds!

## Configuration

```yaml
defaults:
  keep-spawn-in-memory: false  # FAST: Spawn chunks load when player joins

performance:
  show-creation-time: true     # Display creation time in chat
  pre-generate-radius: 0       # 0 = fastest creation
```

## Requirements

- Java 21+
- Spigot/Paper 1.21+

## Authors

- Meyba._.
- Jezevcik20

## License

MIT License
