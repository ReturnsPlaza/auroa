package dev.aurora.module.impl;

import dev.aurora.hud.HudLayout;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.NumberSetting;

/** Offhand (left), held item (middle) and armor (right). Drawn by HudRenderer. */
public class Equipment extends Module {
    private final NumberSetting posX = addHidden(new NumberSetting("posX", -1, -1, 5000, 1));
    private final NumberSetting posY = addHidden(new NumberSetting("posY", -1, -1, 5000, 1));

    public final HudLayout.Element element = HudLayout.register("Equipment", posX, posY, 0, 0, false, true);

    public Equipment() {
        super("Equipment", "Shows offhand, held item and armor", Category.RENDER);
        element.w = 153;
        element.h = 22;
    }
}
