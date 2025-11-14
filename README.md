# JustWorld

Vysoce optimalizovaný Minecraft plugin pro správu světů s plnou podporou asynchronních operací.

## Vlastnosti

- **Plně asynchronní** - Všechny operace se světy běží asynchronně pro maximální výkon
- **Moderní Java 21** - Využívá nejnovější funkce jako Records a CompletableFuture
- **Optimalizovaný** - Minimální overhead, ConcurrentHashMap pro thread-safe operace
- **Snadné použití** - Jednoduché příkazy a intuitivní API
- **Perzistence** - Automatické ukládání konfigurace světů

## Příkazy

Všechny příkazy mají aliasy `/w` a `/jw`

- `/world create <název> [normal|nether|end] [seed]` - Vytvoří nový svět
- `/world delete <název> [confirm]` - Smaže svět včetně všech souborů
- `/world load <název>` - Načte existující svět
- `/world unload <název>` - Odebere svět ze serveru (bez smazání)
- `/world tp <název>` - Teleportuje hráče do světa
- `/world list` - Zobrazí seznam všech načtených světů
- `/world info <název>` - Zobrazí detailní informace o světě

## Oprávnění

- `justworld.admin` - Přístup ke všem příkazům
- `justworld.create` - Vytváření světů
- `justworld.delete` - Mazání světů
- `justworld.load` - Načítání světů
- `justworld.unload` - Odebírání světů
- `justworld.teleport` - Teleportace mezi světy
- `justworld.list` - Seznam světů
- `justworld.info` - Informace o světech

## Instalace

1. Stáhněte nejnovější verzi z Releases
2. Nahrajte `.jar` soubor do složky `plugins/`
3. Restartujte server

## Build

```bash
./gradlew build
```

## Použití v kódu

```java
WorldManager worldManager = JustWorld.getInstance().getWorldManager();

// Asynchronní vytvoření světa
WorldData data = WorldData.builder("myworld")
    .environment(World.Environment.NORMAL)
    .seed(12345L)
    .pvpEnabled(true)
    .build();

worldManager.createWorldAsync(data).thenAccept(world -> {
    if (world != null) {
        // Svět byl vytvořen
    }
});

// Asynchronní načtení světa
worldManager.loadWorldAsync("myworld").thenAccept(world -> {
    // Svět načten
});
```

## Optimalizace

- **ConcurrentHashMap** - Thread-safe ukládání dat bez zámků
- **CompletableFuture** - Moderní asynchronní programování
- **Records** - Immutable datové třídy s nízkou pamětí
- **Lazy loading** - Světy se načítají jen když jsou potřeba
- **Batch operations** - Hromadné operace při startu serveru

## Požadavky

- Java 21+
- Spigot/Paper 1.21+

## Autoři

- Meyba._.
- Jezevcik20

## Licence

MIT License
