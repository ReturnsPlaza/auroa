package dev.aurora.module.impl;

import dev.aurora.module.Category;
import dev.aurora.module.Module;

public class Sprint extends Module {
    public Sprint() {
        super("Sprint", "Sprints automatically while you walk forward", Category.PLAYER);
    }

    @Override
    public void onTick() {
        if (mc.currentScreen != null || mc.player == null) {
            return;
        }
        if (mc.options.forwardKey.isPressed() && !mc.player.isSneaking()) {
            mc.options.sprintKey.setPressed(true);
        }
    }

    @Override
    protected void onDisable() {
        mc.options.sprintKey.setPressed(false);
    }
}
