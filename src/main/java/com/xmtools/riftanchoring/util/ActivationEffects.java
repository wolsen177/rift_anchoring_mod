package com.xmtools.riftanchoring.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class ActivationEffects {

    private static final Random RANDOM = new Random();

    private static final List<BlockState> GLITCH_BLOCK_STATES = new ArrayList<>();

    static {
        for (Block block : BuiltInRegistries.BLOCK) {
            BlockState state = block.defaultBlockState();
            if (state.isAir()) continue;
            if (state.getBlock() == Blocks.OBSIDIAN) continue;
            GLITCH_BLOCK_STATES.add(state);
            if (GLITCH_BLOCK_STATES.size() >= 64) break;
        }
    }

    private ActivationEffects() {}

    public static void triggerActivation(ServerLevel level, BlockPos anchorPos) {
        spawnExpandingRuneBurst(level, anchorPos);
        spawnGlitchFlash(level, anchorPos, 40);
        spawnFireworkBurst(level, anchorPos);
        level.playSound(null, anchorPos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.6F, 1.8F);
        level.playSound(null, anchorPos, SoundEvents.CONDUIT_ACTIVATE, SoundSource.BLOCKS, 0.7F, 0.9F);
        level.playSound(null, anchorPos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.4F, 1.3F);
    }

    public static void triggerAngelActivation(ServerLevel level, BlockPos anchorPos) {
        spawnAngelRuneBurst(level, anchorPos);
        spawnGlitchFlash(level, anchorPos, 30);
        spawnFeatherBurst(level, anchorPos);
        level.playSound(null, anchorPos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.5F);
        level.playSound(null, anchorPos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.0F, 1.4F);
        level.playSound(null, anchorPos, SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.BLOCKS, 0.7F, 1.8F);
        level.playSound(null, anchorPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 1.2F);
        level.playSound(null, anchorPos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.5F, 1.6F);
    }

    public static void triggerDeactivation(ServerLevel level, BlockPos anchorPos) {
        spawnCollapsingRuneBurst(level, anchorPos);
        spawnGlitchFlash(level, anchorPos, 25);
        level.playSound(null, anchorPos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.5F, 0.6F);
        level.playSound(null, anchorPos, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.BLOCKS, 0.5F, 0.8F);
    }

    public static void triggerDestructionPenalty(ServerLevel level, BlockPos anchorPos) {
        spawnGracefulCollapse(level, anchorPos);
        spawnCollapsingRuneBurst(level, anchorPos);
        spawnGlitchFlash(level, anchorPos, 30);
        level.playSound(null, anchorPos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.6F, 0.5F);
        level.playSound(null, anchorPos, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.BLOCKS, 0.4F, 0.6F);
        level.playSound(null, anchorPos, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 0.7F, 1.0F);
    }

    public static void triggerAngelDestructionPenalty(ServerLevel level, BlockPos anchorPos) {
        spawnAngelGracefulCollapse(level, anchorPos);
        spawnGlitchFlash(level, anchorPos, 20);
        level.playSound(null, anchorPos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.6F, 0.4F);
        level.playSound(null, anchorPos, SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.BLOCKS, 0.5F, 0.5F);
        level.playSound(null, anchorPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.5F, 0.3F);
        level.playSound(null, anchorPos, SoundEvents.ENDER_EYE_DEATH, SoundSource.BLOCKS, 0.4F, 0.6F);
    }

    private static void spawnAngelRuneBurst(ServerLevel level, BlockPos anchorPos) {
        double cx = anchorPos.getX() + 0.5;
        double cy = anchorPos.getY() + 0.5;
        double cz = anchorPos.getZ() + 0.5;

        for (int ring = 0; ring < 3; ring++) {
            double radius = 0.5 + ring * 1.0;
            int count = 24 + ring * 16;
            for (int i = 0; i < count; i++) {
                double angle = (double) i / count * Math.PI * 2;
                double x = cx + Math.cos(angle) * radius;
                double z = cz + Math.sin(angle) * radius;
                double y = cy + ring * 0.8;
                level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0.03, 0, 0.02);
                if (i % 4 == 0) {
                    level.sendParticles(ParticleTypes.GLOW, x, y, z, 1, 0, 0.05, 0, 0.02);
                }
            }
        }

        for (int i = 0; i < 60; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double radius = RANDOM.nextDouble() * 3.0;
            double x = cx + Math.cos(angle) * radius;
            double z = cz + Math.sin(angle) * radius;
            double y = cy + RANDOM.nextDouble() * 4.0;
            level.sendParticles(ParticleTypes.WHITE_ASH, x, y, z, 1, 
                    (RANDOM.nextDouble() - 0.5) * 0.1, 
                    0.03 + RANDOM.nextDouble() * 0.06, 
                    (RANDOM.nextDouble() - 0.5) * 0.1, 0.03);
        }

        for (int i = 0; i < 10; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double r = RANDOM.nextDouble() * 1.2;
            level.sendParticles(ParticleTypes.CLOUD, cx + Math.cos(angle) * r, 
                    cy + RANDOM.nextDouble() * 1.5, 
                    cz + Math.sin(angle) * r, 1, 0, 0.02, 0, 0.02);
        }
    }

    private static void spawnFeatherBurst(ServerLevel level, BlockPos anchorPos) {
        double cx = anchorPos.getX() + 0.5;
        double cy = anchorPos.getY() + 1.5;
        double cz = anchorPos.getZ() + 0.5;

        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI * 2 / 8;
            for (int j = 0; j < 6; j++) {
                double dist = 0.3 + j * 0.35;
                double x = cx + Math.cos(angle) * dist;
                double z = cz + Math.sin(angle) * dist;
                level.sendParticles(ParticleTypes.FIREWORK, x, cy + j * 0.15, z, 1, 0, 0.05, 0, 0.03);
            }
        }

        for (int i = 0; i < 30; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double r = RANDOM.nextDouble() * 2.0;
            level.sendParticles(ParticleTypes.WHITE_ASH, 
                    cx + Math.cos(angle) * r, cy + RANDOM.nextDouble() * 2.5, cz + Math.sin(angle) * r, 
                    1, (RANDOM.nextDouble() - 0.5) * 0.15, 0.02, (RANDOM.nextDouble() - 0.5) * 0.15, 0.02);
        }
    }

    private static void spawnExpandingRuneBurst(ServerLevel level, BlockPos anchorPos) {
        double cx = anchorPos.getX() + 0.5;
        double cy = anchorPos.getY() + 0.5;
        double cz = anchorPos.getZ() + 0.5;

        for (int s = 0; s < 4; s++) {
            double angleOffset = s * Math.PI * 2.0 / 4;
            int points = 50;
            for (int i = 0; i < points; i++) {
                double t = (double) i / points;
                double radius = t * 4.5;
                double angle = angleOffset + t * Math.PI * 6;
                double height = t * 2.5;

                double x = cx + Math.cos(angle) * radius;
                double z = cz + Math.sin(angle) * radius;
                double y = cy + height;

                double vx = Math.cos(angle) * 0.2;
                double vy = 0.1 + t * 0.2;
                double vz = Math.sin(angle) * 0.2;

                level.sendParticles(ParticleTypes.ENCHANT, x, y, z, 1, vx, vy, vz, 0.1);
                if (i % 3 == 0) {
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 1, vx * 0.3, vy * 0.3, vz * 0.3, 0.02);
                }
                if (i % 5 == 0) {
                    level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0.01);
                }
                if (i % 7 == 0) {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, vx * 0.5, vy * 0.5, vz * 0.5, 0.03);
                }
            }
        }

        for (int i = 0; i < 40; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double radius = RANDOM.nextDouble() * 2.0;
            double x = cx + Math.cos(angle) * radius;
            double z = cz + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.WITCH, x, cy, z, 1, 0, 0.15, 0, 0.06);
        }

        for (int i = 0; i < 25; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double radius = RANDOM.nextDouble() * 1.5;
            double x = cx + Math.cos(angle) * radius;
            double z = cz + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.GLOW_SQUID_INK, x, cy + RANDOM.nextDouble() * 2, z, 1, 0, 0.03, 0, 0.02);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, cy + RANDOM.nextDouble(), z, 1, 0, 0.02, 0, 0.02);
        }

        for (int i = 0; i < 12; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double r = RANDOM.nextDouble() * 0.8;
            level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, cx + Math.cos(angle) * r, cy + RANDOM.nextDouble(), cz + Math.sin(angle) * r, 1, 0, 0.1, 0, 0.05);
        }
    }

    private static void spawnCollapsingRuneBurst(ServerLevel level, BlockPos anchorPos) {
        double cx = anchorPos.getX() + 0.5;
        double cy = anchorPos.getY() + 0.5;
        double cz = anchorPos.getZ() + 0.5;

        for (int s = 0; s < 4; s++) {
            double angleOffset = s * Math.PI * 2.0 / 4;
            int points = 40;
            for (int i = 0; i < points; i++) {
                double t = 1.0 - (double) i / points;
                double radius = t * 3.5;
                double angle = angleOffset + t * Math.PI * 4;
                double height = t * 2.0;

                double x = cx + Math.cos(angle) * radius;
                double z = cz + Math.sin(angle) * radius;
                double y = cy + height;

                double vx = Math.cos(angle) * -0.15;
                double vy = -(0.05 + t * 0.1);
                double vz = Math.sin(angle) * -0.15;

                level.sendParticles(ParticleTypes.SOUL, x, y, z, 1, vx, vy, vz, 0.06);
                if (i % 4 == 0) {
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 1, vx * 0.5, vy * 0.5, vz * 0.5, 0.03);
                }
                if (i % 6 == 0) {
                    level.sendParticles(ParticleTypes.WHITE_ASH, x, y, z, 1, vx * 0.3, vy * 0.8, vz * 0.3, 0.02);
                }
            }
        }

        for (int i = 0; i < 20; i++) {
            double x = cx + (RANDOM.nextDouble() - 0.5) * 4;
            double z = cz + (RANDOM.nextDouble() - 0.5) * 4;
            level.sendParticles(ParticleTypes.SMOKE, x, cy, z, 1, 0, 0.05, 0, 0.02);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, cy + RANDOM.nextDouble() * 2, z, 1, 0, -0.05, 0, 0.01);
        }
    }

    private static void spawnFireworkBurst(ServerLevel level, BlockPos anchorPos) {
        double cx = anchorPos.getX() + 0.5;
        double cy = anchorPos.getY() + 1.5;
        double cz = anchorPos.getZ() + 0.5;
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI * 2 / 8;
            double r = 0.3;
            level.sendParticles(ParticleTypes.FIREWORK, cx + Math.cos(angle) * r, cy, cz + Math.sin(angle) * r, 1, 0, 0.3, 0, 0.08);
        }
    }

    private static void spawnGracefulCollapse(ServerLevel level, BlockPos anchorPos) {
        double cx = anchorPos.getX() + 0.5;
        double cy = anchorPos.getY() + 0.5;
        double cz = anchorPos.getZ() + 0.5;

        for (int ring = 0; ring < 3; ring++) {
            double rBase = 1.5 + ring * 1.0;
            int count = 16 + ring * 8;
            for (int i = 0; i < count; i++) {
                double angle = (double) i / count * Math.PI * 2;
                double x = cx + Math.cos(angle) * rBase;
                double z = cz + Math.sin(angle) * rBase;
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, cy, z, 1, 0, -0.03, 0, 0.04);
                if (i % 4 == 0) {
                    level.sendParticles(ParticleTypes.SOUL, x, cy, z, 1, Math.cos(angle) * 0.05, -0.02, Math.sin(angle) * 0.05, 0.03);
                }
            }
        }

        for (int i = 0; i < 30; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double r = RANDOM.nextDouble() * 3.0;
            double x = cx + Math.cos(angle) * r;
            double z = cz + Math.sin(angle) * r;
            double y = cy + (RANDOM.nextDouble() - 0.5) * 2;
            level.sendParticles(ParticleTypes.SCULK_SOUL, x, y, z, 1, 0, -0.06, 0, 0.03);
        }

        for (int i = 0; i < 15; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double r = RANDOM.nextDouble() * 1.5;
            double x = cx + Math.cos(angle) * r;
            double z = cz + Math.sin(angle) * r;
            level.sendParticles(ParticleTypes.END_ROD, x, cy + RANDOM.nextDouble() * 1.5, z, 1, 
                    (RANDOM.nextDouble() - 0.5) * 0.04, 0, (RANDOM.nextDouble() - 0.5) * 0.04, 0.02);
        }

        for (int i = 0; i < 10; i++) {
            double x = cx + (RANDOM.nextDouble() - 0.5) * 2.5;
            double z = cz + (RANDOM.nextDouble() - 0.5) * 2.5;
            level.sendParticles(ParticleTypes.WHITE_ASH, x, cy + RANDOM.nextDouble() * 1.5, z, 1, 0, -0.02, 0, 0.01);
        }
    }

    private static void spawnAngelGracefulCollapse(ServerLevel level, BlockPos anchorPos) {
        double cx = anchorPos.getX() + 0.5;
        double cy = anchorPos.getY() + 0.5;
        double cz = anchorPos.getZ() + 0.5;

        for (int ring = 0; ring < 3; ring++) {
            double rBase = 1.5 + ring * 1.0;
            int count = 16 + ring * 8;
            for (int i = 0; i < count; i++) {
                double angle = (double) i / count * Math.PI * 2;
                double x = cx + Math.cos(angle) * rBase;
                double z = cz + Math.sin(angle) * rBase;
                level.sendParticles(ParticleTypes.WHITE_ASH, x, cy, z, 1, 0, -0.02, 0, 0.03);
                if (i % 3 == 0) {
                    level.sendParticles(ParticleTypes.END_ROD, x, cy, z, 1, Math.cos(angle) * 0.03, 0, Math.sin(angle) * 0.03, 0.02);
                }
            }
        }

        for (int i = 0; i < 40; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double r = RANDOM.nextDouble() * 3.5;
            double x = cx + Math.cos(angle) * r;
            double z = cz + Math.sin(angle) * r;
            double y = cy + (RANDOM.nextDouble() - 0.5) * 3;
            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0, -0.03, 0, 0.02);
            if (i % 4 == 0) {
                level.sendParticles(ParticleTypes.FIREWORK, x, y, z, 1, 0, 0.01, 0, 0.01);
            }
        }

        for (int i = 0; i < 20; i++) {
            double x = cx + (RANDOM.nextDouble() - 0.5) * 2.0;
            double z = cz + (RANDOM.nextDouble() - 0.5) * 2.0;
            level.sendParticles(ParticleTypes.GLOW, x, cy + RANDOM.nextDouble() * 2, z, 1, 0, -0.05, 0, 0.015);
        }

        for (int i = 0; i < 8; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double r = RANDOM.nextDouble() * 1.0;
            level.sendParticles(ParticleTypes.HEART, cx + Math.cos(angle) * r, cy + RANDOM.nextDouble() * 1.5, cz + Math.sin(angle) * r, 1, 0, 0.02, 0, 0.02);
        }
    }

    private static void spawnGlitchFlash(ServerLevel level, BlockPos anchorPos, int particlesPerBlock) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos obsidianPos = anchorPos.below().offset(dx, 0, dz);
                double bx = obsidianPos.getX() + 0.5;
                double by = obsidianPos.getY() + 0.5;
                double bz = obsidianPos.getZ() + 0.5;

                for (int i = 0; i < particlesPerBlock; i++) {
                    BlockState randomState = GLITCH_BLOCK_STATES.get(RANDOM.nextInt(GLITCH_BLOCK_STATES.size()));
                    BlockParticleOption option = new BlockParticleOption(ParticleTypes.BLOCK, randomState);
                    double ox = (RANDOM.nextDouble() - 0.5) * 0.95;
                    double oy = (RANDOM.nextDouble() - 0.5) * 0.95;
                    double oz = (RANDOM.nextDouble() - 0.5) * 0.95;
                    level.sendParticles(option, bx + ox, by + oy, bz + oz, 1,
                            (RANDOM.nextDouble() - 0.5) * 0.4,
                            RANDOM.nextDouble() * 0.3,
                            (RANDOM.nextDouble() - 0.5) * 0.4, 0.6);
                }
            }
        }
    }

    public static void spawnGlitchTick(ServerLevel level, BlockPos anchorPos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos obsidianPos = anchorPos.below().offset(dx, 0, dz);
                double bx = obsidianPos.getX() + 0.5;
                double by = obsidianPos.getY() + 0.5;
                double bz = obsidianPos.getZ() + 0.5;

                BlockState randomState = GLITCH_BLOCK_STATES.get(RANDOM.nextInt(GLITCH_BLOCK_STATES.size()));
                BlockParticleOption option = new BlockParticleOption(ParticleTypes.BLOCK, randomState);

                for (int face = 0; face < 5; face++) {
                    double ox = (RANDOM.nextDouble() - 0.5) * 0.96;
                    double oy = (RANDOM.nextDouble() - 0.5) * 0.96;
                    double oz = (RANDOM.nextDouble() - 0.5) * 0.96;
                    level.sendParticles(option, bx + ox, by + oy, bz + oz, 1, 0, 0.03, 0, 0.4);
                }

                double cx = obsidianPos.getX() + 0.5;
                double cy = obsidianPos.getY() + 0.5;
                double cz = obsidianPos.getZ() + 0.5;
                level.sendParticles(ParticleTypes.ENCHANT, cx, cy, cz, 4, 0.18, 0.18, 0.18, 0.06);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, cx, cy, cz, 2, 0.1, 0.1, 0.1, 0.03);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, cx, cy, cz, 1, 0.2, 0.2, 0.2, 0.04);
            }
        }
    }

    public static void spawnPersistentEffects(ServerLevel level, BlockPos anchorPos) {
        double cx = anchorPos.getX() + 0.5;
        double cy = anchorPos.getY() + 0.7;
        double cz = anchorPos.getZ() + 0.5;

        long gameTime = level.getGameTime();

        for (int i = 0; i < 3; i++) {
            double angle = (gameTime * 0.02 + i * Math.PI * 2 / 3) % (Math.PI * 2);
            double radius = 0.8;
            double x = cx + Math.cos(angle) * radius;
            double z = cz + Math.sin(angle) * radius;
            double y = cy + Math.sin(gameTime * 0.03 + i) * 0.3;
            level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0.005);
            level.sendParticles(ParticleTypes.GLOW, x, y, z, 1, 0, 0, 0, 0.005);
        }

        for (int i = 0; i < 2; i++) {
            double angle = (gameTime * -0.015 + i * Math.PI) % (Math.PI * 2);
            double radius = 1.2;
            double x = cx + Math.cos(angle) * radius;
            double z = cz + Math.sin(angle) * radius;
            double y = cy + 0.5;
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 1, 0, 0, 0, 0.003);
        }

        if (gameTime % 5 == 0) {
            double x = cx + (RANDOM.nextDouble() - 0.5) * 0.8;
            double z = cz + (RANDOM.nextDouble() - 0.5) * 0.8;
            level.sendParticles(ParticleTypes.INSTANT_EFFECT, x, cy, z, 1, 0, 0.05, 0, 0.01);
        }

        if (gameTime % 10 == 0) {
            for (int i = 0; i < 3; i++) {
                double x = cx + (RANDOM.nextDouble() - 0.5) * 1.5;
                double z = cz + (RANDOM.nextDouble() - 0.5) * 1.5;
                level.sendParticles(ParticleTypes.GLOW, x, cy + RANDOM.nextDouble() * 0.5, z, 1, 0, 0.02, 0, 0.01);
            }
        }

        if (gameTime % 15 == 0) {
            double x = cx + (RANDOM.nextDouble() - 0.5) * 2.0;
            double z = cz + (RANDOM.nextDouble() - 0.5) * 2.0;
            level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, x, cy + 0.5, z, 1, 0, 0, 0, 0.01);
        }
    }

    public static void spawnAngelPersistentEffects(ServerLevel level, BlockPos anchorPos) {
        double cx = anchorPos.getX() + 0.5;
        double cy = anchorPos.getY() + 0.7;
        double cz = anchorPos.getZ() + 0.5;

        long gameTime = level.getGameTime();

        for (int i = 0; i < 4; i++) {
            double angle = (gameTime * 0.015 + i * Math.PI * 2 / 4) % (Math.PI * 2);
            double radius = 0.7;
            double x = cx + Math.cos(angle) * radius;
            double z = cz + Math.sin(angle) * radius;
            double y = cy + Math.sin(gameTime * 0.025 + i) * 0.4;
            level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0.003);
            level.sendParticles(ParticleTypes.GLOW, x, y, z, 1, 0, 0, 0, 0.003);
            if (i % 2 == 0) {
                level.sendParticles(ParticleTypes.WHITE_ASH, x, y + 0.3, z, 1, 0, 0, 0, 0.002);
            }
        }

        for (int i = 0; i < 3; i++) {
            double angle = (gameTime * -0.02 + i * Math.PI * 2 / 3) % (Math.PI * 2);
            double radius = 1.3;
            double x = cx + Math.cos(angle) * radius;
            double z = cz + Math.sin(angle) * radius;
            double y = cy + 0.6 + Math.sin(gameTime * 0.02) * 0.2;
            level.sendParticles(ParticleTypes.FIREWORK, x, y, z, 1, 0, 0, 0, 0.002);
        }

        if (gameTime % 3 == 0) {
            double x = cx + (RANDOM.nextDouble() - 0.5) * 0.6;
            double z = cz + (RANDOM.nextDouble() - 0.5) * 0.6;
            level.sendParticles(ParticleTypes.WHITE_ASH, x, cy + RANDOM.nextDouble() * 1.0, z, 1, 0, 0.06, 0, 0.01);
        }

        if (gameTime % 8 == 0) {
            for (int i = 0; i < 2; i++) {
                double x = cx + (RANDOM.nextDouble() - 0.5) * 2.0;
                double z = cz + (RANDOM.nextDouble() - 0.5) * 2.0;
                level.sendParticles(ParticleTypes.CLOUD, x, cy + RANDOM.nextDouble() * 0.8, z, 1, 0, 0.01, 0, 0.008);
            }
        }

        if (gameTime % 12 == 0) {
            for (int i = 0; i < 6; i++) {
                double angle = RANDOM.nextDouble() * Math.PI * 2;
                double r = RANDOM.nextDouble() * 2.5;
                double x = cx + Math.cos(angle) * r;
                double z = cz + Math.sin(angle) * r;
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, cy + RANDOM.nextDouble() * 2, z, 1, 0, 0.1, 0, 0.02);
            }
        }

        if (gameTime % 20 == 0) {
            double x = cx + (RANDOM.nextDouble() - 0.5) * 3.0;
            double z = cz + (RANDOM.nextDouble() - 0.5) * 3.0;
            level.sendParticles(ParticleTypes.HEART, x, cy + RANDOM.nextDouble() * 2.5, z, 1, 0, 0.08, 0, 0.01);
        }
    }

    public static void triggerLodestoneStructureFormed(ServerLevel level, BlockPos pos) {
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;

        for (int ring = 0; ring < 2; ring++) {
            double radius = 0.5 + ring * 0.6;
            int count = 16;
            for (int i = 0; i < count; i++) {
                double angle = (double) i / count * Math.PI * 2;
                double x = cx + Math.cos(angle) * radius;
                double z = cz + Math.sin(angle) * radius;
                level.sendParticles(ParticleTypes.END_ROD, x, cy, z, 1,
                        Math.cos(angle) * 0.05, 0.15, Math.sin(angle) * 0.05, 0.04);
            }
        }

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5;
        for (double h = 0; h < 5.0; h += 0.15) {
            double intensity = 1.0 - h / 5.0;
            if (RANDOM.nextFloat() < 0.4 * intensity) {
                level.sendParticles(ParticleTypes.END_ROD, x + (RANDOM.nextDouble() - 0.5) * 0.1, y + h, z + (RANDOM.nextDouble() - 0.5) * 0.1, 1, 0, 0.06, 0, 0.005);
            }
            if (RANDOM.nextFloat() < 0.08) {
                level.sendParticles(ParticleTypes.GLOW, x + (RANDOM.nextDouble() - 0.5) * 0.3, y + h, z + (RANDOM.nextDouble() - 0.5) * 0.3, 1, 0, 0.04, 0, 0.01);
            }
        }

        for (int i = 0; i < 12; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double r = RANDOM.nextDouble() * 2.0;
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, cx + Math.cos(angle) * r, cy + RANDOM.nextDouble() * 2.0, cz + Math.sin(angle) * r, 1, 0, 0.03, 0, 0.01);
        }

        level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.BLOCKS, 0.5F, 1.8F);
        level.playSound(null, pos, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.BLOCKS, 0.4F, 1.2F);
    }

    public static void triggerLodestoneActivation(ServerLevel level, BlockPos pos) {
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;

        for (int ring = 0; ring < 3; ring++) {
            double radius = 0.6 + ring * 1.0;
            int count = 20 + ring * 12;
            for (int i = 0; i < count; i++) {
                double angle = (double) i / count * Math.PI * 2;
                double x = cx + Math.cos(angle) * radius;
                double z = cz + Math.sin(angle) * radius;
                level.sendParticles(ParticleTypes.ENCHANT, x, cy + ring * 0.6, z, 1,
                        Math.cos(angle) * 0.08, 0.08, Math.sin(angle) * 0.08, 0.10);
                if (i % 3 == 0) {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, cy + ring * 0.3, z, 1, 0, 0.15, 0, 0.06);
                }
                if (i % 5 == 0) {
                    level.sendParticles(ParticleTypes.END_ROD, x, cy + ring * 0.3, z, 1, 0, 0.05, 0, 0.02);
                }
            }
        }

        for (int i = 0; i < 30; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double r = RANDOM.nextDouble() * 3.0;
            double x = cx + Math.cos(angle) * r;
            double z = cz + Math.sin(angle) * r;
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, cy + RANDOM.nextDouble() * 2.0, z, 1, 0, 0.05, 0, 0.04);
            if (i % 4 == 0) {
                level.sendParticles(ParticleTypes.GLOW, x, cy + RANDOM.nextDouble() * 1.5, z, 1, 0, 0.03, 0, 0.03);
            }
            if (i % 6 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, cy + RANDOM.nextDouble(), z, 1, 0, 0.08, 0, 0.04);
            }
        }

        spawnLodestoneActivationBeam(level, pos);

        level.playSound(null, pos, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.BLOCKS, 1.0F, 1.5F);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.5F, 1.0F);
        level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.4F, 1.2F);
    }

    public static void spawnLodestonePersistentEffects(ServerLevel level, BlockPos pos) {
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.7;
        double cz = pos.getZ() + 0.5;
        long gameTime = level.getGameTime();

        for (int i = 0; i < 3; i++) {
            double angle = (gameTime * 0.03 + i * Math.PI * 2 / 3) % (Math.PI * 2);
            double radius = 0.6;
            double x = cx + Math.cos(angle) * radius;
            double z = cz + Math.sin(angle) * radius;
            double y = cy + Math.sin(gameTime * 0.04 + i) * 0.2;
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 1, 0, 0, 0, 0.005);
            level.sendParticles(ParticleTypes.ENCHANT, x, y, z, 1, 0, 0, 0, 0.005);
        }

        for (int i = 0; i < 2; i++) {
            double angle = (gameTime * -0.025 + i * Math.PI) % (Math.PI * 2);
            double radius = 1.0;
            double x = cx + Math.cos(angle) * radius;
            double z = cz + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, cy + 0.3, z, 1, 0, 0, 0, 0.003);
        }

        if (gameTime % 5 == 0) {
            double x = cx + (RANDOM.nextDouble() - 0.5) * 0.8;
            double z = cz + (RANDOM.nextDouble() - 0.5) * 0.8;
            level.sendParticles(ParticleTypes.GLOW, x, cy, z, 1, 0, 0.05, 0, 0.01);
        }

        if (gameTime % 8 == 0) {
            double x = cx + (RANDOM.nextDouble() - 0.5) * 1.2;
            double z = cz + (RANDOM.nextDouble() - 0.5) * 1.2;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, cy + 0.3, z, 1, 0, 0.03, 0, 0.02);
        }

        spawnLodestoneBeam(level, pos);

        if (gameTime % 10 == 0) {
            level.playSound(null, pos, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.BLOCKS, 0.15F, 0.6F + RANDOM.nextFloat() * 0.2F);
        }
    }

    public static void triggerLodestoneDestruction(ServerLevel level, BlockPos pos) {
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;

        for (int i = 0; i < 30; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double r = RANDOM.nextDouble() * 2.5;
            double x = cx + Math.cos(angle) * r;
            double z = cz + Math.sin(angle) * r;
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, cy + RANDOM.nextDouble() * 2.0, z, 1,
                    (RANDOM.nextDouble() - 0.5) * 0.15, -0.06, (RANDOM.nextDouble() - 0.5) * 0.15, 0.04);
            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, cy + RANDOM.nextDouble(), z, 1, 0, 0, 0, 0.03);
            }
            if (i % 5 == 0) {
                level.sendParticles(ParticleTypes.END_ROD, x, cy + RANDOM.nextDouble() * 1.5, z, 1, 0, -0.05, 0, 0.02);
            }
        }

        for (int i = 0; i < 20; i++) {
            double x = cx + (RANDOM.nextDouble() - 0.5) * 3.0;
            double z = cz + (RANDOM.nextDouble() - 0.5) * 3.0;
            level.sendParticles(ParticleTypes.SMOKE, x, cy, z, 1, 0, 0.03, 0, 0.015);
            if (i % 4 == 0) {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, cy + RANDOM.nextDouble(), z, 1, 0, -0.02, 0, 0.01);
            }
        }

        level.playSound(null, pos, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.BLOCKS, 0.6F, 0.3F);
        level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.5F, 0.5F);
        level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.3F, 0.4F);
    }

    private static void spawnLodestoneActivationBeam(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5;

        for (double h = 0; h < 12.0; h += 0.08) {
            double intensity = 1.0 - h / 12.0;
            int count = h < 2.0 ? 4 : (h < 6.0 ? 3 : 2);
            for (int i = 0; i < count; i++) {
                double ox = (RANDOM.nextDouble() - 0.5) * 0.12 * intensity;
                double oz = (RANDOM.nextDouble() - 0.5) * 0.12 * intensity;
                level.sendParticles(ParticleTypes.END_ROD, x + ox, y + h, z + oz, 1, 0, 0.1, 0, 0.006);
            }
            if (RANDOM.nextFloat() < 0.18) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x + (RANDOM.nextDouble() - 0.5) * 0.5, y + h, z + (RANDOM.nextDouble() - 0.5) * 0.5, 1, 0, 0.15, 0, 0.04);
            }
            if (RANDOM.nextFloat() < 0.08) {
                level.sendParticles(ParticleTypes.GLOW, x + (RANDOM.nextDouble() - 0.5) * 0.5, y + h, z + (RANDOM.nextDouble() - 0.5) * 0.5, 1, 0, 0.05, 0, 0.02);
            }
            if (RANDOM.nextFloat() < 0.04) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, x + (RANDOM.nextDouble() - 0.5) * 0.8, y + h, z + (RANDOM.nextDouble() - 0.5) * 0.8, 1, 0, 0.05, 0, 0.01);
            }
        }

        level.sendParticles(ParticleTypes.FLASH, x, y + 6.0, z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y + 6.0, z, 12, 0.4, 0.4, 0.4, 0.06);
        level.sendParticles(ParticleTypes.ENCHANT, x, y + 6.0, z, 8, 0.3, 0.3, 0.3, 0.05);
    }

    private static void spawnLodestoneBeam(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5;
        long gameTime = level.getGameTime();

        for (double h = 0; h < 10.0; h += 0.15) {
            double intensity = 1.0 - h / 10.0;
            double wobble = Math.sin(gameTime * 0.04 + h * 0.5) * 0.06;
            double wobble2 = Math.cos(gameTime * 0.04 + h * 0.5) * 0.06;
            if (RANDOM.nextFloat() < 0.3 * intensity + 0.1) {
                level.sendParticles(ParticleTypes.END_ROD, x + wobble, y + h, z + wobble2, 1, 0, 0.02, 0, 0.002);
            }
            if (RANDOM.nextFloat() < 0.06) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, x + wobble * 2, y + h, z + wobble2 * 2, 1, 0, 0.01, 0, 0.003);
            }
        }

        if (gameTime % 4 == 0) {
            level.sendParticles(ParticleTypes.GLOW, x, y, z, 1, 0, 0.02, 0, 0.008);
        }
        if (gameTime % 6 == 0) {
            level.sendParticles(ParticleTypes.ENCHANT, x, y + 5.0, z, 2, 0.15, 0.05, 0.15, 0.02);
        }
        if (gameTime % 10 == 0) {
            double px = x + (RANDOM.nextDouble() - 0.5) * 0.6;
            double pz = z + (RANDOM.nextDouble() - 0.5) * 0.6;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, y + RANDOM.nextDouble() * 8.0, pz, 1, 0, 0.05, 0, 0.01);
        }
    }

    public static void spawnLodestoneBindBeam(ServerLevel level, BlockPos lodestonePos, BlockPos anchorPos, float progress) {
        double lx = lodestonePos.getX() + 0.5;
        double ly = lodestonePos.getY() + 1.0;
        double lz = lodestonePos.getZ() + 0.5;

        double ax = anchorPos.getX() + 0.5;
        double ay = anchorPos.getY() + 0.5;
        double az = anchorPos.getZ() + 0.5;

        int steps = 40;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double x = lx + (ax - lx) * t;
            double y = ly + (ay - ly) * t;
            double z = lz + (az - lz) * t;

            double wobbleX = Math.sin(t * Math.PI * 2 * 3 + progress * Math.PI * 2) * 0.08;
            double wobbleZ = Math.cos(t * Math.PI * 2 * 3 + progress * Math.PI * 2) * 0.08;

            level.sendParticles(ParticleTypes.END_ROD, x + wobbleX, y, z + wobbleZ, 1, 0, 0, 0, 0.005);
            if (RANDOM.nextFloat() < 0.15) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0, 0, 0, 0.01);
            }
            if (RANDOM.nextFloat() < 0.08) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y, z, 1, 0.02, 0.02, 0.02, 0.008);
            }
        }

        level.sendParticles(ParticleTypes.GLOW, lx, ly, lz, 3, 0.08, 0.08, 0.08, 0.015);
        level.sendParticles(ParticleTypes.GLOW, ax, ay, az, 3, 0.08, 0.08, 0.08, 0.015);
        level.sendParticles(ParticleTypes.ENCHANT, lx, ly, lz, 4, 0.1, 0.1, 0.1, 0.05);
        level.sendParticles(ParticleTypes.ENCHANT, ax, ay, az, 4, 0.1, 0.1, 0.1, 0.05);

        if (progress < 0.03F) {
            level.playSound(null, lodestonePos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.7F, 1.4F);
            level.playSound(null, anchorPos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.7F, 1.2F);
        }
    }
}
