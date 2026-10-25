package dev.aurora.module.impl;

import dev.aurora.hud.HudLayout;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.NumberSetting;

/** Shows each worn armour piece with its durability. Drawn by HudRenderer. */
public class ArmorHud extends Module {
    private final NumberSetting posX = addHidden(new NumberSetting("posX", -1, -1, 5000, 1));
    private final NumberSetting posY = addHidden(new NumberSetting("posY", -1, -1, 5000, 1));

    public final HudLayout.Element element = HudLayout.register("Armor", posX, posY, 6, 40, false, false);

    public ArmorHud() {
        super("Armor", "Shows each worn armour piece with its durability", Category.RENDER);
        element.w = 112;
        element.h = 86;
    }
}
