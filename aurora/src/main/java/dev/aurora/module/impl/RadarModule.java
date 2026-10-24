package dev.aurora.module.impl;

import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.BooleanSetting;
import dev.aurora.setting.NumberSetting;

public class RadarModule extends Module {
    private final NumberSetting radius = add(new NumberSetting("Size", 36, 20, 60, 2));
    private final NumberSetting range = add(new NumberSetting("Range", 40, 16, 128, 4));
    private final BooleanSetting mobs = add(new BooleanSetting("Mobs", true));

    public RadarModule() {
        super("Radar", "A round radar showing nearby players and mobs", Category.RENDER);
        loadEnabled(true);
    }

    public int radius() {
        return radius.asInt();
    }

    public double range() {
        return range.asDouble();
    }

    public boolean showMobs() {
        return mobs.on();
    }
}
