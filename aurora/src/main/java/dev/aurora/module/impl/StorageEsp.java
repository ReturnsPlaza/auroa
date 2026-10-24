package dev.aurora.module.impl;

import dev.aurora.hud.EspTarget;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.BooleanSetting;
import dev.aurora.setting.NumberSetting;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Outlines chests, barrels, ender chests and shulker boxes through walls. */
public class StorageEsp extends Module {
    private final BooleanSetting chests = add(new BooleanSetting("Chests", true));
    private final BooleanSetting barrels = add(new BooleanSetting("Barrels", true));
    private final BooleanSetting enderChests = add(new BooleanSetting("Ender chests", true));
    private final BooleanSetting shulkers = add(new BooleanSetting("Shulkers", true));
    private final NumberSetting range = add(new NumberSetting("Chunks", 6, 2, 12, 1));

    private volatile List<EspTarget> targets = Collections.emptyList();
    private int timer;

    public StorageEsp() {
        super("Storage ESP", "Outlines chests, barrels and shulkers through walls", Category.RENDER);
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
        timer = 10;

        List<EspTarget> found = new ArrayList<>();
        ChunkPos center = mc.player.getChunkPos();
        int r = range.asInt();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(center.x + dx, center.z + dz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    int color = colorOf(be);
                    if (color != 0) {
                        found.add(new EspTarget(be.getPos(), color));
                    }
                }
            }
        }
        targets = found;
    }

    private int colorOf(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity) {
            return chests.on() ? 0xFFFF4040 : 0;
        }
        if (be instanceof ChestBlockEntity) {
            return chests.on() ? 0xFFFFA52E : 0;
        }
        if (be instanceof BarrelBlockEntity) {
            return barrels.on() ? 0xFFE0B35A : 0;
        }
        if (be instanceof EnderChestBlockEntity) {
            return enderChests.on() ? 0xFFB05CFF : 0;
        }
        if (be instanceof ShulkerBoxBlockEntity) {
            return shulkers.on() ? 0xFF6ED3FF : 0;
        }
        return 0;
    }

    @Override
    protected void onDisable() {
        targets = Collections.emptyList();
        timer = 0;
    }
}
