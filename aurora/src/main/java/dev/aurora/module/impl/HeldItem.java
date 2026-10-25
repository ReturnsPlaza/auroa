package dev.aurora.module.impl;

import dev.aurora.hud.HudLayout;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.NumberSetting;

/** Shows the item in your main hand, its name and durability. Drawn by HudRenderer. */
public class HeldItem extends Module {
    private final NumberSetting posX = addHidden(new NumberSetting("posX", -1, -1, 5000, 1));
    private final NumberSetting posY = addHidden(new NumberSetting("posY", -1, -1, 5000, 1));

    public final HudLayout.Element element = HudLayout.register("Held item", posX, posY, 6, 110, false, false);

    public HeldItem() {
        super("Held item", "Shows the item in your hand with its durability", Category.RENDER);
        element.w = 110;
        element.h = 24;
    }
}
