package dev.aurora.hud;

import net.minecraft.util.math.BlockPos;

/** A block to outline (position and ARGB colour). */
public record EspTarget(BlockPos pos, int color) {
}
