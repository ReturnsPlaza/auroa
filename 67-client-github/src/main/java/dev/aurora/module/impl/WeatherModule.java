package dev.aurora.module.impl;

import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.BooleanSetting;

/** Clears rain and thunder on your screen. Applied by WorldMixin. */
public class WeatherModule extends Module {
    private final BooleanSetting clear = add(new BooleanSetting("Clear sky", true));

    public WeatherModule() {
        super("Weather", "Clears rain and thunder from your view", Category.WORLD);
    }

    public boolean clearSky() {
        return clear.on();
    }
}
