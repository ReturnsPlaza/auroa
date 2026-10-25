package dev.aurora.module.impl;

import dev.aurora.mixin.MinecraftClientAccessor;
import dev.aurora.module.Category;
import dev.aurora.module.Module;

public class FastPlace extends Module {
    public FastPlace() {
        super("Fast Place", "Removes the delay between placing blocks", Category.MISC);
    }

    @Override
    public void onTick() {
        ((MinecraftClientAccessor) mc).setItemUseCooldown(0);
    }
}
