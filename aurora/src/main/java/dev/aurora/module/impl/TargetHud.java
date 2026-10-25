package dev.aurora.module.impl;

import dev.aurora.hud.HudLayout;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.NumberSetting;

/** Shows the name and health of the mob or player you are looking at. Drawn by HudRenderer. */
public class TargetHud extends Module {
    private final NumberSetting posX = addHidden(new NumberSetting("posX", -1, -1, 5000, 1));
    private final NumberSetting posY = addHidden(new NumberSetting("posY", -1, -1, 5000, 1));

    public final HudLayout.Element element = HudLayout.register("Target HUD", posX, posY, 0, 0, false, true);

    public TargetHud() {
        super("Target HUD", "Shows name and health of what you are looking at", Category.RENDER);
        element.w = 110;
        element.h = 30;
    }
}
