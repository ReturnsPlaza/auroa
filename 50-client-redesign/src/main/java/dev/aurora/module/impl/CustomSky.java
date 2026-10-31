package dev.aurora.module.impl;

import dev.aurora.gui.Palette;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.ModeSetting;

/** Recolours the sky to one purple tone. The colour is applied by SkyMixin. */
public class CustomSky extends Module {
    private final ModeSetting colour = add(new ModeSetting("Sky colour", "Cyan", Palette.NAMES));

    public CustomSky() {
        super("Custom sky", "Deep aurora-tinted sky that slowly shifts in brightness", Category.WORLD);
        loadEnabled(true);
    }

    /** A dark version of the chosen colour that breathes slowly, like aurora light moving across the sky. */
    public int color() {
        int base = Palette.of(colour.get());
        double t = System.currentTimeMillis() / 4000.0;
        double k = 0.20 + 0.08 * (Math.sin(t) + 1) / 2;
        int r = (int) (((base >> 16) & 0xFF) * k);
        int g = (int) (((base >> 8) & 0xFF) * k);
        int b = (int) ((base & 0xFF) * k);
        return (r << 16) | (g << 8) | b;
    }
}
