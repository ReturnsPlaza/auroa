package dev.aurora.module.impl;

import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.BooleanSetting;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

public class AutoTool extends Module {
    private final BooleanSetting switchBack = add(new BooleanSetting("Switch back", true));
    private int previousSlot = -1;

    public AutoTool() {
        super("Auto Tool", "Picks the fastest tool in your hotbar when mining", Category.MISC);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) {
            return;
        }
        PlayerInventory inv = mc.player.getInventory();

        boolean mining = mc.currentScreen == null
                && mc.options.attackKey.isPressed()
                && mc.crosshairTarget instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK;

        if (mining) {
            BlockHitResult hit = (BlockHitResult) mc.crosshairTarget;
            BlockState state = mc.world.getBlockState(hit.getBlockPos());
            if (state.isAir()) {
                return;
            }
            int current = inv.getSelectedSlot();
            int best = current;
            float bestSpeed = inv.getStack(current).getMiningSpeedMultiplier(state);
            for (int i = 0; i < 9; i++) {
                ItemStack stack = inv.getStack(i);
                float speed = stack.getMiningSpeedMultiplier(state);
                if (speed > bestSpeed + 0.01f) {
                    bestSpeed = speed;
                    best = i;
                }
            }
            if (best != current) {
                if (previousSlot == -1) {
                    previousSlot = current;
                }
                inv.setSelectedSlot(best);
            }
        } else if (previousSlot != -1) {
            if (switchBack.on()) {
                inv.setSelectedSlot(previousSlot);
            }
            previousSlot = -1;
        }
    }

    @Override
    protected void onDisable() {
        previousSlot = -1;
    }
}
