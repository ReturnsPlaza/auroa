package dev.aurora.module.impl;

import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.BooleanSetting;
import dev.aurora.setting.NumberSetting;

/** Name labels above players and mobs, visible through walls. Drawn by HudRenderer. */
public class Nametags extends Module {
    private final BooleanSetting players = add(new BooleanSetting("Players", true));
    private final BooleanSetting mobs = add(new BooleanSetting("Mobs", false));
    private final NumberSetting range = add(new NumberSetting("Range", 48, 8, 128, 8));

    public Nametags() {
        super("Nametags", "Shows names above players and mobs", Category.RENDER);
    }

    public boolean players() {
        return players.on();
    }

    public boolean mobs() {
        return mobs.on();
    }

    public double range() {
        return range.asDouble();
    }
}
