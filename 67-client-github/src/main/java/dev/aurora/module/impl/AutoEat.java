package dev.aurora.module.impl;

import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.NumberSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class AutoEat extends Module {
    private final NumberSetting threshold = add(new NumberSetting("Eat below", 14, 1, 19, 1));
    private boolean eating;
    private int previousSlot = -1;

    public AutoEat() {
        super("Auto Eat", "Eats safe food from your hotbar when you get hungry", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (mc.player == null) {
            return;
        }
        PlayerInventory inv = mc.player.getInventory();
        int food = mc.player.getHungerManager().getFoodLevel();

        if (eating) {
            if (mc.currentScreen != null || food >= 20 || !isSafeFood(inv.getStack(inv.getSelectedSlot()))) {
                stop();
            } else {
                mc.options.useKey.setPressed(true);
            }
            return;
        }

        if (mc.currentScreen != null || food > threshold.asInt()) {
            return;
        }
        for (int i = 0; i < 9; i++) {
            if (isSafeFood(inv.getStack(i))) {
                previousSlot = inv.getSelectedSlot();
                inv.setSelectedSlot(i);
                eating = true;
                mc.options.useKey.setPressed(true);
                return;
            }
        }
    }

    private void stop() {
        mc.options.useKey.setPressed(false);
        if (previousSlot != -1 && mc.player != null) {
            mc.player.getInventory().setSelectedSlot(previousSlot);
        }
        previousSlot = -1;
        eating = false;
    }

    @Override
    protected void onDisable() {
        if (eating) {
            stop();
        }
    }

    private static boolean isSafeFood(ItemStack stack) {
        if (stack.isEmpty() || !stack.contains(DataComponentTypes.FOOD)) {
            return false;
        }
        // Keep the valuable and harmful items out of the automatic menu.
        return !(stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)
                || stack.isOf(Items.GOLDEN_APPLE)
                || stack.isOf(Items.CHORUS_FRUIT)
                || stack.isOf(Items.ROTTEN_FLESH)
                || stack.isOf(Items.SPIDER_EYE)
                || stack.isOf(Items.PUFFERFISH)
                || stack.isOf(Items.POISONOUS_POTATO)
                || stack.isOf(Items.SUSPICIOUS_STEW));
    }
}
