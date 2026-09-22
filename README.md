# Multiloader mod template — NeoForge + Fabric, 1.21.1 + 26.2

One repository, two loaders, two Minecraft versions, three publishing platforms.
Every configuration in here has been built and verified, not just written.

## Clone it

```powershell
.\new-mod.ps1 -ModId cooltweak -ModName "Cool Tweak" -Destination ..\cool-tweak
```

Then edit the `TODO` descriptions in `fabric.mod.json` and `neoforge.mods.toml`.

## Build

```bash
./gradlew build                            # the default line (minecraft_version in gradle.properties)
./gradlew build -Pminecraft_version=1.21.1 # any other line of the MATRIX
```

Jars land in `fabric/build/libs/` and `neoforge/build/libs/`, named
`<mod_id>-<loader>-<mcversion>-<modversion>.jar`, so all four coexist.

Requires JDK 25 for the Gradle process itself (Gradle 9.5 + Loom), even when building 1.21.1,
whose *toolchain* is Java 21 — the foojay resolver downloads that one automatically.

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-25'
```

## Adding a Minecraft version

**One file.** Add a row to `MATRIX` in `build.gradle` — CI reads that list straight out of the
file, so there is nothing else to keep in sync.

## The three tiers

Pick one deliberately per mod; it decides the porting cost, not the market.

| Tier | Content | Port cost 1.21.1 <-> 26.2 |
|------|---------|---------------------------|
| **A — server only** | mechanics, worldgen, biomes, structures, enchantments, loot, commands | ~nil, often the same source recompiled |
| **B — both sides** | blocks, items, entities | 2-3x, concentrated in rendering |
| **C — client only** | perf, QoL, HUD | highest, but the Fabric audience is huge |

Default to A when the mod allows it. Only static registries (blocks, items, entities, block
entities, fluids, effects) force a client install; data-driven registries (biomes, structures,
dimensions, enchantments, recipes, loot, damage types) are sent by the server and work against a
vanilla client.

**Making it Tier A:** delete `common/.../client/`, `BulkMegapackFabricClient`,
`BulkMegapackNeoForgeClient`, and the `"client"` entrypoint in `fabric.mod.json`. Declare
`client_side: unsupported` on Modrinth — server admins filter on it. A Tier A mod with no
resources is also the only case where you may drop the Fabric API dependency.

## Fabric API

Declared by default, and not for convenience. Fabric Loader on its own neither unfreezes the
vanilla registries before `onInitialize` nor mounts a mod's `assets/` as a resource pack — the
second failure is silent, and shows up in game as missing textures and raw translation keys. Any
mod that ships a texture, a lang file or a static registry entry needs it.

Watch `Reloading ResourceManager:` in the log: it must name your mod. `vanilla` alone means none
of your resources were loaded.

## Publishing

`release.yml` publishes to Modrinth + CurseForge + GitHub Releases when a tag is pushed. It stays
dormant until configured; each platform is skipped when its token is missing.

- Repo **variables**: `MODRINTH_ID`, `CURSEFORGE_ID`
- Repo **secrets**: `MODRINTH_TOKEN`, `CURSEFORGE_TOKEN`

```bash
git tag v0.1.0 && git push --tags
```

One version is published **per Minecraft version**, numbered `1.0.0+1.21.1`, because mc-publish
creates a single version per call and reads its loaders and game versions from the primary file
only — hand it four jars and you get one entry mislabelled after whichever came first. The GitHub
release is a separate job so that it happens exactly once, with every jar attached.

`release.yml` only runs at tag time, so its bugs stay invisible until the release you care about.
Read it before the first tag of a new project, and watch the run: a platform whose token is missing
is skipped **silently**, and the job still goes green.

## Gotchas worth keeping

- **Loom has two plugin IDs.** `net.fabricmc.fabric-loom` is `LoomNoRemapGradlePlugin`: it sets
  `disableObfuscation = true` *and finalizes it*, so `officialMojangMappings()` then throws
  "Cannot use Mojang mappings in a non-obfuscated environment" — and no Gradle property can undo
  it. Obfuscated Minecraft (1.21.x) needs the classic `fabric-loom` ID. One Loom version serves
  both eras; see the comment in `fabric/build.gradle`.
- **26.x ships de-obfuscated.** No `mappings` line, and `implementation` rather than
  `modImplementation` for fabric-loader — there is nothing to remap.
- **Gradle 9 breaks `expand(project.properties)`.** Filter to string values first (done in
  `build.gradle`), or the build fails fingerprinting `allprojects`.
- **`modImplementation` does not exist under the no-remap plugin** either, so 26.x declares Fabric
  API with plain `implementation`. The error names a missing method on `DefaultDependencyHandler`
  and never mentions the plugin.
- **`pack.mcmeta` is mandatory** as soon as the mod ships `assets/` or `data/`. Without it NeoForge
  mounts the pack and reads nothing from it, with no error. Its `pack_format` is per MC version and
  lives in `MATRIX`.
- **`ResourceLocation` became `Identifier` in 26.x.** Never name that type in shared code; the
  `Registry.register(reg, "ns:path", value)` String overload is identical across every version.
- **First build of a new MC version takes 2-15 min** (Minecraft gets decompiled), then seconds.
