# Bulk Megapack

Shift-click once and the station keeps going. Right-click once and the obvious thing happens.
One mod for the gestures Minecraft almost has.

**No rule is changed.** Same costs, same stocks, same XP, same drop chances. Everything here is
vanilla's own code, run again — and every gesture can be switched off on its own.

## What it does

### At a station — shift-click the result

| Station | What one click now does |
|---|---|
| Villager | trades until you run out of payment |
| Stonecutter | cuts every matching block in your inventory |
| Smithing table | upgrades or trims every identical piece you carry |
| Loom | applies the pattern to every identical banner |
| Cartography table | turns every empty map you carry into a copy |
| Grindstone | disenchants a whole inventory of loot, one XP roll per item |

The anvil is deliberately left alone: it charges levels, and chaining it would spend your XP
without asking.

### In the world

| Gesture | What happens |
|---|---|
| Right-click a grown crop | harvests it and replants it, keeping one seed from the drop |
| Sneak + right-click a composter | the whole stack goes in, one item at a time, vanilla odds |
| Chain stops with items left in a station | they are handed back to your inventory |
| Sneak + right-click a repeater, **empty-handed** | the delay steps **backwards** instead of wrapping forward |
| Sneak + right-click a filled item frame | the item turns back one step |
| Open a double door | its other half opens with it |
| Run out of what you were holding | the next identical stack moves into your hand |

## Playing well with others

Any gesture already provided by another installed mod is switched off automatically — Quark,
Mouse Tweaks, RightClickHarvest, Double Doors, Stack Refill and friends keep their own behaviour.
Set `ignoreOtherMods` to `true` in the config to override that.

## Config

`config/bulkmegapack.json`, written on first launch. One switch per gesture, plus `enabled` for
the lot. `grindstoneIncludeHotbar` is `false` by default, so the grindstone never touches the tools
you keep on your hotbar. Renamed items are never ground either.

## Sides

Server-side logic throughout. On a server, only the server needs it: vanilla clients can join and
still get every gesture. In single player, installing it is enough.

## Building

```bash
./gradlew build                             # the default line (minecraft_version in gradle.properties)
./gradlew build -Pminecraft_version=1.21.1  # any other line of MATRIX
```

Jars land in `fabric/build/libs/` and `neoforge/build/libs/`, one per loader per Minecraft version.
The Gradle process itself needs JDK 25, even when building 1.21.1 (its toolchain is Java 21 and is
downloaded automatically).

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-25'
```

## Licence

MIT.
