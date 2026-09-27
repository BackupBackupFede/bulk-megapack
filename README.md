# Bulk Megapack

One gesture, one whole stack. Shift-click a station's result and it keeps going until your stack
runs out, instead of stopping after a single item.

**No rule is changed.** Same costs, same stocks, same XP, same odds — every pass is vanilla's own
code, run again. Each station can be switched off on its own.

## What it does

| Gesture | What one action now does |
|---|---|
| Shift-click a **villager** trade | trades until you run out of payment |
| Shift-click the **stonecutter** result | cuts every matching block in your inventory |
| Shift-click the **loom** result | applies the pattern to every identical banner you carry |
| Shift-click the **cartography table** result | turns every empty map you carry into a copy |
| Sneak + right-click a **composter** | the whole stack in hand goes in, at vanilla odds |

A station is left as you found it: when the run stops, anything still sitting in its input slots
goes back to your inventory.

**What this mod deliberately does not touch:** the grindstone, the smithing table and the anvil.
They consume gear rather than stacks, so a bulk gesture there could eat equipment you were keeping
— and the anvil would spend your XP without asking.

## Playing well with others

A gesture already provided by another installed mod switches itself off, so mods that own it keep
their own behaviour. Set `ignoreOtherMods` to `true` in the config to override that.

## Config

`config/bulkmegapack.json`, written on first launch: one switch per station, plus `enabled` for the
lot.

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
