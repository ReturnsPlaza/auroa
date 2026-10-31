package dev.aurora.module.impl;

import dev.aurora.hud.EspTarget;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.BooleanSetting;
import dev.aurora.setting.NumberSetting;

import java.util.List;

/** Shared look settings for the block-outline ESP modules. */
public abstract class EspModule extends Module {
    protected final BooleanSetting outline = add(new BooleanSetting("Outline", true));
    protected final BooleanSetting fill = add(new BooleanSetting("Fill", false));
    protected final NumberSetting fillAlpha = add(new NumberSetting("Alpha", 70, 0, 255, 5));
    protected final NumberSetting thickness = add(new NumberSetting("Line width", 2, 1, 3, 1));
    protected final BooleanSetting tracers = add(new BooleanSetting("Tracers", false));
    protected final NumberSetting maxDistance = add(new NumberSetting("Max distance", 128, 16, 256, 8));

    protected EspModule(String name, String description) {
        this(name, description, Category.RENDER);
    }

    protected EspModule(String name, String description, Category category) {
        super(name, description, category);
    }

    public abstract List<EspTarget> targets();

    public boolean outline() {
        return outline.on();
    }

    public boolean fill() {
        return fill.on();
    }

    public int fillAlpha() {
        return fillAlpha.asInt();
    }

    public int thickness() {
        return thickness.asInt();
    }

    public boolean tracers() {
        return tracers.on();
    }

    public double maxDistance() {
        return maxDistance.asDouble();
    }
}
