# JustWorld

⚡ **Ultra-fast async world management plugin for Minecraft**

A highly optimized alternative to Multiverse-Core with full asynchronous operations and modern Java 21 features.

## Features

- **⚡ Blazing Fast** - All world operations run asynchronously with optimized spawn chunk handling
- **📊 Performance Metrics** - See exactly how long world creation takes
- **🔧 Modern Java 21** - Utilizes latest features like Records and CompletableFuture
- **🚀 Optimized** - Minimal overhead, ConcurrentHashMap for thread-safe operations
- **💾 Persistent** - Automatic world configuration saving
- **🎯 Simple API** - Easy-to-use commands and developer API

## Commands

All commands have aliases `/w` and `/jw`

- `/world create <name> [normal|nether|end] [seed]` - Create a new world (shows creation time!)
- `/world delete <name> [confirm]` - Delete a world including all files
- `/world load <name>` - Load an existing world
- `/world unload <name>` - Unload a world from server (without deleting)
- `/world tp <name>` - Teleport to a world
- `/world list` - List all loaded worlds
- `/world info <name>` - View detailed world information

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

// Asynchronous world creation with timing
WorldData data = WorldData.builder("myworld")
    .environment(World.Environment.NORMAL)
    .seed(12345L)
    .pvpEnabled(true)
    .build();

worldManager.createWorldAsync(data).thenAccept(result -> {
    if (result.isSuccess()) {
        World world = result.world();
        System.out.println("World created in " + result.getFormattedTime());
    }
});

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
- **Normal world**: ~500-1500ms (vs 3-5s with spawn chunks)
- **Nether**: ~300-800ms
- **End**: ~200-600ms

*Results may vary based on hardware and seed complexity*

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
