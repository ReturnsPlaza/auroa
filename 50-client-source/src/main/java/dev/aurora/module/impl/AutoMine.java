package dev.aurora.module.impl;

import dev.aurora.module.Category;
import dev.aurora.module.Module;

public class AutoMine extends Module {
    public AutoMine() {
        super("Auto Mine", "Holds the mine button for you", Category.MISC);
    }

    @Override
    public void onTick() {
        if (mc.currentScreen == null) {
            mc.options.attackKey.setPressed(true);
        }
    }

    @Override
    protected void onDisable() {
        mc.options.attackKey.setPressed(false);
    }
}
