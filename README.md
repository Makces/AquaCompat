# AquaCompat

AquaCompat is a small compatibility mod for NeoForge 1.21.1. It sits between Aquaculture 2, Farmer's Delight, and JEI and fixes the places where fish processing gets awkward.

## What it does

- Adds Farmer's Delight cutting-board recipes for Aquaculture fish and for the vanilla cod and salmon cases that use the Neptunium Fillet Knife
- Carries the Neptunium Fillet Knife bonus through Aquaculture filleting and cutting-board outputs, including chance-based extra slices
- Backs off when another cutting-board recipe should win, so AquaCompat does not force its own recipe to the front
- Cleans up JEI so the recipe list matches the behavior in game instead of showing stale or misleading entries
- Includes a small fix for Aquaculture's moist farmland so ordinary crops and similar plants can treat it as farmland

This is not a content expansion. The point is to make existing mods agree with each other.

## Requirements

AquaCompat requires Minecraft 1.21.1, NeoForge, and [Aquaculture 2](https://www.curseforge.com/minecraft/mc-mods/aquaculture).

[Farmer's Delight](https://www.curseforge.com/minecraft/mc-mods/farmers-delight) and [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) are optional. If Farmer's Delight is missing, the cutting-board side of the mod has nothing to patch. If JEI is missing, only the JEI cleanup layer is skipped.

## Configuration

The common config is mostly about the Neptunium knife bonus. You can turn it off, change the multiplier, decide how rounding works, whitelist or blacklist items and tags, choose whether cooked fish should participate, and control whether AquaCompat recipes should yield to competing cutting-board recipes.

There is also datapack support for per-fish overrides under `data/*/neptunium_fillet_bonus`. Those definitions take priority over the fallback multiplier from the config.

## For players and pack makers

If you already run Aquaculture with Farmer's Delight, this mod is meant to make filleting and cutting-board recipes feel like they belong in the same pack.

For pack work, the common config is enough if you just want to tune the bonus globally. If you want different fish to use different values, use the datapack hook instead.

## Development

Toolchain:

- Java 21
- Gradle
- NeoForge ModDev
- Parchment mappings

Build the mod:

```powershell
./gradlew build
```

Run the dev client:

```powershell
./gradlew runClient
```

Run GameTests:

```powershell
./gradlew runGameTestServer
```

Run data generation:

```powershell
./gradlew runData
```

Generated resources in `src/generated/resources` are checked in on purpose. The compatibility sweep script lives at [scripts/compat-matrix.ps1](scripts/compat-matrix.ps1).

## Issues

When reporting a bug, include:

- Log when the bug happened.
- Steps to reproduce the bug (if possible)
- if removing Aquacompat fixes the bug
