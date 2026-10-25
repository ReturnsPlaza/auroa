package dev.aurora.module.impl;

import dev.aurora.gui.Palette;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.ModeSetting;

/** Recolours the sky to one purple tone. The colour is applied by SkyMixin. */
public class CustomSky extends Module {
    private final ModeSetting colour = add(new ModeSetting("Sky colour", "Purple", Palette.NAMES));

    public CustomSky() {
        super("Custom sky", "Recolours the sky to a purple tone", Category.RENDER);
        loadEnabled(true);
    }

    public int color() {
        return Palette.of(colour.get());
    }
}
