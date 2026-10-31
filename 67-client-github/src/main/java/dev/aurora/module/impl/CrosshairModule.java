package dev.aurora.module.impl;

import dev.aurora.gui.Palette;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.BooleanSetting;
import dev.aurora.setting.ModeSetting;
import dev.aurora.setting.NumberSetting;

/** Custom crosshair. While enabled, the vanilla crosshair is hidden (see InGameHudMixin). */
public class CrosshairModule extends Module {
    private final NumberSetting size = add(new NumberSetting("Size", 6, 1, 30, 1));
    private final NumberSetting gap = add(new NumberSetting("Gap", 3, 0, 20, 1));
    private final NumberSetting thickness = add(new NumberSetting("Thickness", 1, 1, 4, 1));
    private final BooleanSetting dot = add(new BooleanSetting("Center dot", false));
    private final ModeSetting color = add(new ModeSetting("Color", "White", Palette.NAMES));

    public CrosshairModule() {
        super("Crosshair", "Custom crosshair. Replaces the vanilla one while on", Category.RENDER);
    }

    public int size() {
        return size.asInt();
    }

    public int gap() {
        return gap.asInt();
    }

    public int thickness() {
        return thickness.asInt();
    }

    public boolean dot() {
        return dot.on();
    }

    public String colorName() {
        return color.get();
    }
}
