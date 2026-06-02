# Rift Anchoring

> 🌐 [中文文档](README_zh_cn.md) | English

**Rift Anchoring** is a Minecraft NeoForge 1.21.1 mod that enables permanent chunk loading through two multiblock structures: the **Rift Respawn Anchor** (core loader) and the **Rift Lodestone** (expansion loader), complete with visual effects and full configuration support.

---

## Features

### Rift Respawn Anchor (Core Loader)

- **3×3 Obsidian Base + Respawn Anchor** — Build a solid 3×3 obsidian platform with a Respawn Anchor on top of the center block
- **Permanent Chunk Loading** — Right-click the anchor with Infinite Glowstone to permanently force-load the entire chunk
- **Infinite Glowstone Crafting** (configurable method):
  - **Anvil**: Glowstone + Enchanted Book (costs 5 XP levels)
  - **Smithing Table**: Glowstone + Nether Star + Netherite Upgrade Template
  - **Both**: Both recipes available (default)
- **No Respawn Point** — Respawn Anchors inside a valid multiblock structure cannot set the player's spawn point
- **Persistent Visual Feedback** — Continuous particle effects while the anchor is active
- **Structure Break Punishment** — If the obsidian base is broken, energy rapidly drains and particles intensify
- **Configurable Energy Drain** — Energy interval configurable (default: 1 hour per charge level)

### Rift Lodestone (Expansion Loader)

- **Extends Chunk Loading** — Must be placed in a chunk **adjacent** (sharing an edge or corner) to a chunk already force-loaded by an active Rift Respawn Anchor
- **3×3 Obsidian Base + Lodestone** — Same structure pattern as the anchor, but with a Lodestone
- **Auto-activation** — Activates automatically when the structure is correct, with particle effects and sound
- **New: Iron Ingot Recipe** — Backports the Minecraft 1.21.5 lodestone recipe (8 iron ingots + 1 chiseled stone bricks). Vanilla netherite recipe is also available
- **Anchor Guide Compass Binding**:
  1. First, upgrade Glowstone to **Infinite Glowstone** (see crafting above)
  2. In an **Anvil**, combine Infinite Glowstone + Enchanted Book → transforms the glowstone
  3. Hold the upgraded glowstone and **right-click** the Rift Respawn Anchor to activate it
  4. Craft a regular **Recovery Compass** (vanilla)
  5. **Right-click** an active Rift Lodestone while holding the Recovery Compass → compass is consumed and a bound **Anchor Guide Compass** is created
  6. The Anchor Guide Compass always points to its bound lodestone and shows coordinates/distance in its tooltip
- **Expansion Limits**: Each Rift Respawn Anchor supports up to **25** Rift Lodestones within a **5×5 chunk radius** (including its own chunk)
- **Break Punishment** — Destroying a lodestone immediately releases its chunk; all Anchor Guide Compasses bound to it become unbound

### Chunk Debug Wand

- **Right-click any block** — Displays chunk loading status, force load level, and nearby loaded chunks
- **Right-click an active Rift Respawn Anchor** — Shows anchor details (energy, infinite mode, remaining time)
- **Right-click an active Rift Lodestone** — Shows bound parent anchor (if any) and linked compass count

---

## Requirements

| Requirement   | Version         |
|---------------|-----------------|
| Minecraft     | 1.21.1          |
| NeoForge      | 21.1.222+       |
| Java          | 21              |

## Installation

1. Install **NeoForge 21.1.222+** for Minecraft 1.21.1
2. Download the latest `.jar` from the [发布](https://github.com/yourusername/RiftAnchoring/releases) page
3. Place the JAR into your `.minecraft/mods/` folder
4. Launch the game

## Usage Guide

### Rift Respawn Anchor

1. Build a **3×3 obsidian platform** on the ground
2. Place a **Respawn Anchor** on top of the center block
3. Craft **Infinite Glowstone** (see crafting section above)
4. Hold Infinite Glowstone and **right-click** the anchor → activates chunk loading
5. Particle effects appear above the anchor while it's active
6. Breaking the obsidian base stops chunk loading and triggers drain punishment

### Rift Lodestone

1. Ensure you have an **active Rift Respawn Anchor** loading its own chunk
2. Find a **neighboring chunk** (touching the anchor's chunk by edge or corner)
3. Build a 3×3 obsidian platform and place a **Lodestone** at the center → structure auto-activates
4. Craft a **Recovery Compass** (vanilla item)
5. Hold the Recovery Compass and **right-click** the active Rift Lodestone → compass is consumed, an **Anchor Guide Compass** is given
6. The Anchor Guide Compass always points to the lodestone and shows its coordinates

### Infinite Glowstone Crafting

**Method 1 — Anvil** (costs 5 XP levels):
Place **Glowstone** + **Enchanted Book** in an anvil.

**Method 2 — Smithing Table** (no XP cost):
Place **Glowstone** + **Nether Star** + **Netherite Upgrade Smithing Template** in a smithing table.

### Breaking a Rift Lodestone

- Its chunk immediately stops being force-loaded
- All Anchor Guide Compasses bound to that lodestone become unbound (item lore resets)
- A shattering sound and particle burst plays

---

## Configuration

The configuration file is located at `.minecraft/config/rift-anchoring-common.toml`:

```toml
# Energy consumption interval in seconds (default: 3600 = 1 hour per charge level)
energyIntervalSeconds = 3600

# Infinite Glowstone crafting method: "anvil", "smithing", or "both" (default: both)
infiniteGlowstoneMethod = "both"

# Lodestone crafting recipe: "iron" (1.21.5 iron recipe), "netherite" (vanilla), or "both" (default: both)
lodestoneRecipe = "both"

# Maximum number of Rift Lodestones per Rift Respawn Anchor (default: 25, max: 25)
maxLodestonesPerAnchor = 25

# Enable detailed debug log output for rift lodestone operations (default: false)
enableLodestoneDebugLog = false

[debug]
# Enable the Chunk Debug Wand item (default: true)
enableDebugWand = true
```

---

## License

This project is licensed under the **GPL-3.0 License**.

---

## Credits

- NeoForge Team
- Minecraft Community
