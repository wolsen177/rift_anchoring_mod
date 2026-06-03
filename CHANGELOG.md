# Changelog / 更新日志

## [1.2.1] - 2026-06-03

### Added / 新增
[+] 锚定指引针绑定磁石后自动还原为追溯指针，不再消耗 / Anchor Guide Compass converts back to Recovery Compass after binding (no longer consumed)
[+] 锚定指引针右键点击已绑定的激活裂隙重生锚可还原为追溯指针（解除绑定）/ Anchor Guide Compass right-click on bound active anchor can revert to Recovery Compass (unbinds lodestone)
[+] 配置项 `enableCompassBidirectionalConversion` 控制双向转换开关（默认启用）/ Configuration option `enableCompassBidirectionalConversion` to enable/disable bidirectional compass conversion (default: true)

### Changed / 更改
[~] 锚定指引针使用后不再消失，启用配置时始终返还追溯指针 / Anchor Guide Compass no longer disappears — always returns a Recovery Compass when config enabled

---

## [1.2.0] - 2026-06-02

### Added / 新增
[+] 磁石铁锭合成配方（8个凿纹石砖 + 1铁锭）/ Iron ingot recipe for Lodestone (8 Chiseled Stone Bricks + 1 Iron Ingot)
[+] 回响碎片合成配方（4个紫水晶碎片 + 1幽匿感触体 = 1回响碎片）/ Echo Shard crafting recipe (4 Amethyst Shards + 1 Sculk = 1 Echo Shard)
[+] 配置项 `enableEchoShardRecipe` 控制回响碎片配方开关（默认启用）/ Configuration option `enableEchoShardRecipe` to toggle Echo Shard recipe (default: true)
[+] 配置项 `lodestoneRecipe` 选择铁锭/下界合金/两者都启用 / Configuration option `lodestoneRecipe` to choose between iron/netherite/both recipes (default: both)
[+] 配置项 `enableLodestoneDebugLog` 启用裂隙磁石调试日志 / Configuration option `enableLodestoneDebugLog` for detailed lodestone operation logging
[+] 磁石多方块结构激活特效（粒子爆发 + 音效）/ Lodestone structure formation effects (particle burst + sound)
[+] 磁石光束视觉反馈（未绑定时显示，绑定后隐藏）/ Lodestone beam visual feedback - shows when unbound, hides when bound

### Changed / 更改
[~] 磁石光束绑定到激活锚后隐藏，能量耗尽或锚破坏时重新显示 / Lodestone beam hides when bound, reappears when anchor loses energy or is destroyed
[~] 粒子效果改进（高度提升至10格，密度衰减更自然）/ Improved particle effects for lodestone beam (height increased to 10 blocks, better density falloff)
[~] 绑定光束效果增强（正弦波摆动，粒子流动更平滑）/ Enhanced lodestone binding beam effect (sine wave wobble, smoother particle flow)
[~] 更新 README 文档（中英文双语，完整功能说明）/ Updated README with complete feature documentation and language switching (English/Chinese)

### Fixed / 修复
[~] 修复铁锭配方错误（原为8铁+1石砖，现为8石砖+1铁锭）/ Fixed incorrect iron ingot recipe pattern
[~] 修复跨维度数据泄漏问题，现在正确隔离各维度数据 / Fixed cross-dimensional data leakage in SavedData
[~] 修复状态变更后未立即保存的问题，现在每次状态变更都会写入磁盘 / Fixed state not saving immediately after deactivation
[~] 修复服务器重启后锚未按预期复活的问题 / Fixed anchor respawning after server restart when deactivated

---

## [1.0.0] - 2026-06-02

### Initial Release / 首次发布
[+] 裂隙重生锚多方块结构（3×3黑曜石底座 + 重生锚）/ Rift Respawn Anchor multiblock structure (3x3 obsidian base + Respawn Anchor)
[+] 裂隙磁石多方块结构（3×3黑曜石底座 + 磁石）/ Rift Lodestone multiblock structure (3x3 obsidian base + Lodestone)
[+] 萤石区块加载（消耗型）或无限萤石（永久加载）/ Chunk loading via Glowstone (consumable) or Infinite Glowstone (permanent)
[+] 无限萤石合成（铁砧：萤石+附魔书 或 锻造台：萤石+下界之星+模板）/ Infinite Glowstone crafting via Anvil or Smithing Table
[+] 锚定指引针用于追踪已绑定磁石 / Anchor Guide Compass for tracking bound lodestones
[+] 区块检测棒用于调试区块加载状态 / Chunk Debug Wand for debugging chunk loading status
[+] 持久视觉特效和破坏惩罚 / Persistent visual effects and destruction penalties
[+] 配置系统（能量间隔、合成方式、调试选项）/ Configuration system for energy intervals, crafting methods, and debug options
