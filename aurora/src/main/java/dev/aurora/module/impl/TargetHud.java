package dev.aurora.module.impl;

import dev.aurora.module.Category;
import dev.aurora.module.Module;

/** Shows the name and health of the mob or player you are looking at. Drawn by HudRenderer. */
public class TargetHud extends Module {
    public TargetHud() {
        super("Target HUD", "Shows name and health of what you are looking at", Category.RENDER);
    }
}
