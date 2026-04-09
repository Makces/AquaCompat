# AquaCompat

Compatibility and integration patches for Aquaculture 2 and related mods on NeoForge 1.21.1.

This project is aimed at two groups:

- Players who just want their fishing and food mods to behave better together
- Developers or pack authors who want to inspect, build, or extend the mod

## What It Does

AquaCompat focuses on smoothing out interactions around Aquaculture.

Current work in this repository includes:

- Neptunium Fillet Knife bonus handling for fish filleting
- Farmer's Delight cutting board integration for Aquaculture fish
- JEI-side recipe visibility and conflict cleanup for overlapping recipes
- Data-driven hooks and generated resources for compatibility behavior

## For Players

### Requirements

- Minecraft 1.21.1
- NeoForge
- [Aquaculture 2](https://www.curseforge.com/minecraft/mc-mods/aquaculture)

Optional integrations currently target:

- [Farmer's Delight](https://www.curseforge.com/minecraft/mc-mods/farmers-delight)
- [Just Enough Items (JEI)](https://www.curseforge.com/minecraft/mc-mods/jei)

### What You Should Expect In-Game

- Better compatibility between Aquaculture fish items and food-processing workflows
- Neptunium knife bonus behavior that can apply to fish slicing and related recipes
- Cleaner recipe presentation when multiple mods provide overlapping outputs

### Configuration

The mod includes common config options for the Neptunium knife bonus system, including:

- Whether the bonus is enabled
- Bonus multiplier amount
- Round-up behavior for small outputs
- Whitelist and blacklist controls by item or tag

If you are building a modpack, these settings let you tune how generous or strict the bonus logic should be.

## For Developers

### Stack

- Java 21
- Gradle
- NeoForge ModDev plugin
- Parchment mappings

### Project Layout

- [`src/main/java`](src/main/java): mod code, integration hooks, mixins, config, and data generators
- [`src/main/resources`](src/main/resources): bundled assets, tags, data, mixin config, and logo
- [`src/generated/resources`](src/generated/resources): generated resources used by the build
- [`scripts`](scripts): local helper scripts such as compatibility matrix tooling

### Build

```powershell
./gradlew build
```

The built jar will be written to `build/libs`.

### Run The Dev Client

```powershell
./gradlew runClient
```

### Generate Data

```powershell
./gradlew runData
```

Generated resources are written into `src/generated/resources`, which is intentionally part of this repository's source set.

### Notes For Contributors

- The repository currently uses an `All Rights Reserved` license setting in its Gradle metadata
- The mod is built around compatibility behavior, so gameplay changes should stay narrow and predictable
- If you change recipe or loot-like behavior, verify both in-game results and JEI presentation

## Repository Status

This repository is being prepared for public release, so expect cleanup and documentation improvements while the codebase settles.
