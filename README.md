# AquaCompat

AquaCompat is a NeoForge 1.21.1 compatibility mod for Aquaculture 2. It adds the Farmer's Delight fish-processing recipes, keeps Neptunium knife bonuses consistent with added JEI partial integration, and fixes Aquaculture's moist farmland from Neptunium Hoe so modded crops can treat it as farmland.


## What?

- Adds cutting-board recipes for Aquaculture fish and for the vanilla cod and salmon cases that use the Neptunium Fillet Knife
- Preserves Neptunium knife bonus output through filleting and cutting-board processing, including chance-based extra slices
- does not override recipes when another same cutting-board recipe exists
- Removes stale or misleading JEI entries so the recipe list matches in-game behavior
- Makes Aquaculture's Neptunium farmland count as farmland for modded seeds, etc.
- Somewhat configurable

## Installation

Drop AquaCompat into the `mods` folder with:

- Minecraft 1.21.1
- NeoForge 21.1.x
- Aquaculture 2 `2.7.15+`

Optional integrations:

- Farmer's Delight `1.2.4+` for cutting-board recipe support
- JEI `19.20.0+` for recipe-list cleanup

## Configuration

The common config controls the Neptunium knife bonus, including the multiplier, rounding, item and tag allow/block lists, cooked-fish handling, and whether AquaCompat recipes should yield to competing cutting-board recipes.

For pack-specific balancing, use datapack overrides under `data/*/neptunium_fillet_bonus`. Datapack values take priority over the config fallback.

## Development

Toolchain:

- Java 21
- Gradle
- NeoForge ModDev
- Parchment mappings

Build:

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

Generated resources in `src/generated/resources` are checked in on purpose. The compatibility sweep script is [`scripts/compat-matrix.ps1`](scripts/compat-matrix.ps1).

## Issues

Report bugs [report here](https://github.com/Makces/AquaCompat/issues), not in Aquaculture 2, Farmer's Delight, or JEI trackers.

Include:

- Minecraft, NeoForge, and Aquaculture 2 versions
- Whether Farmer's Delight and JEI are installed
- Steps to reproduce
- Expected result and actual result
- Relevant log output
- Whether removing AquaCompat makes the problem go away
