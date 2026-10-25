package dev.aurora.module.impl;

import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.BooleanSetting;
import dev.aurora.setting.NumberSetting;

/** Purple sky glow, an aurora band and a purple screen outline. Drawn by HudRenderer. */
public class Atmosphere extends Module {
    private final NumberSetting tint = add(new NumberSetting("Sky tint", 70, 0, 255, 5));
    private final BooleanSetting aurora = add(new BooleanSetting("Aurora band", true));
    private final BooleanSetting outline = add(new BooleanSetting("Screen outline", true));

    public Atmosphere() {
        super("Atmosphere", "Purple sky glow, aurora band and screen outline", Category.RENDER);
    }

    public int tint() {
        return tint.asInt();
    }

    public boolean aurora() {
        return aurora.on();
    }

    public boolean outline() {
        return outline.on();
    }
}
