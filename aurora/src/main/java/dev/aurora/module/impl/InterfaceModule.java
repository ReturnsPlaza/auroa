package dev.aurora.module.impl;

import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.BooleanSetting;
import dev.aurora.setting.NumberSetting;

public class InterfaceModule extends Module {
    private final NumberSetting speed = add(new NumberSetting("Animation speed", 14, 4, 30, 1));
    private final BooleanSetting dim = add(new BooleanSetting("Dim background", true));

    public InterfaceModule() {
        super("Interface", "Menu animation speed and background", Category.CLIENT);
        togglable = false;
        loadEnabled(true);
    }

    public double speed() {
        return speed.asDouble();
    }

    public boolean dimBackground() {
        return dim.on();
    }
}
