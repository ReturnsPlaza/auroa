package dev.aurora.module.impl;

import dev.aurora.hud.EspTarget;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.ModeSetting;
import dev.aurora.setting.NumberSetting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Outlines a chosen kind of block (ores by default) around you, through walls. */
public class BlockEsp extends Module {
    private final ModeSetting target = add(new ModeSetting("Target", "Ores", "Ores", "Diamond", "Debris", "Spawners"));
    private final NumberSetting range = add(new NumberSetting("Range", 14, 6, 28, 2));

    private volatile List<EspTarget> targets = Collections.emptyList();
    private int timer;

    public BlockEsp() {
        super("Block ESP", "Outlines ores or other blocks near you through walls", Category.RENDER);
    }

    public List<EspTarget> targets() {
        return targets;
    }

    @Override
    public void onTick() {
        if (mc.world == null || mc.player == null) {
            return;
        }
        if (timer-- > 0) {
            return;
        }
        timer = 15;

        List<EspTarget> found = new ArrayList<>();
        BlockPos origin = mc.player.getBlockPos();
        int r = range.asInt();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    pos.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    BlockState state = mc.world.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    int color = colorOf(state);
                    if (color != 0) {
                        found.add(new EspTarget(pos.toImmutable(), color));
                        if (found.size() >= 500) {
                            targets = found;
                            return;
                        }
                    }
                }
            }
        }
        targets = found;
    }

    private int colorOf(BlockState s) {
        if (target.is("Diamond")) {
            return s.isIn(BlockTags.DIAMOND_ORES) ? 0xFF4DEBFF : 0;
        }
        if (target.is("Debris")) {
            return s.isOf(Blocks.ANCIENT_DEBRIS) ? 0xFFC47A3A : 0;
        }
        if (target.is("Spawners")) {
            return s.isOf(Blocks.SPAWNER) ? 0xFFFF4DFF : 0;
        }
        // Ores
        if (s.isIn(BlockTags.DIAMOND_ORES)) {
            return 0xFF4DEBFF;
        }
        if (s.isIn(BlockTags.EMERALD_ORES)) {
            return 0xFF35E06A;
        }
        if (s.isIn(BlockTags.GOLD_ORES) || s.isOf(Blocks.NETHER_GOLD_ORE)) {
            return 0xFFFFD83D;
        }
        if (s.isIn(BlockTags.IRON_ORES)) {
            return 0xFFD8AF93;
        }
        if (s.isIn(BlockTags.COPPER_ORES)) {
            return 0xFFE07B3C;
        }
        if (s.isIn(BlockTags.REDSTONE_ORES)) {
            return 0xFFFF3B3B;
        }
        if (s.isIn(BlockTags.LAPIS_ORES)) {
            return 0xFF3D6BFF;
        }
        if (s.isIn(BlockTags.COAL_ORES)) {
            return 0xFF9AA0A6;
        }
        if (s.isOf(Blocks.NETHER_QUARTZ_ORE)) {
            return 0xFFF2EBDD;
        }
        if (s.isOf(Blocks.ANCIENT_DEBRIS)) {
            return 0xFFC47A3A;
        }
        return 0;
    }

    @Override
    protected void onDisable() {
        targets = Collections.emptyList();
        timer = 0;
    }
}
