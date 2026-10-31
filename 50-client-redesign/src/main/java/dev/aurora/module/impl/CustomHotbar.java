package dev.aurora.module.impl;

import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.BooleanSetting;
import dev.aurora.setting.NumberSetting;

/** Cyan glass hotbar drawn by HudRenderer. Replaces the vanilla bar while enabled. */
public class CustomHotbar extends Module {
    private final BooleanSetting hideVanilla = add(new BooleanSetting("Hide vanilla", true));
    private final NumberSetting alpha = add(new NumberSetting("Opacity", 220, 0, 255, 5));

    public CustomHotbar() {
        super("Hotbar", "Cyan glass hotbar with a gliding selected slot", Category.RENDER);
        loadEnabled(true);
    }

    public boolean hideVanilla() {
        return hideVanilla.on();
    }

    public int alpha() {
        return alpha.asInt();
    }
}
