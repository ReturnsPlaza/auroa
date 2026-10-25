package dev.aurora.module.impl;

import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.BooleanSetting;
import dev.aurora.setting.NumberSetting;

/** Clean hotbar with a light purple selected slot. Drawn by HudRenderer. Hides the vanilla hotbar while on. */
public class CustomHotbar extends Module {
    private final BooleanSetting hideVanilla = add(new BooleanSetting("Hide vanilla", true));
    private final NumberSetting alpha = add(new NumberSetting("Opacity", 220, 0, 255, 5));

    public CustomHotbar() {
        super("Hotbar", "Clean hotbar with a light purple selected slot", Category.RENDER);
    }

    public boolean hideVanilla() {
        return hideVanilla.on();
    }

    public int alpha() {
        return alpha.asInt();
    }
}
