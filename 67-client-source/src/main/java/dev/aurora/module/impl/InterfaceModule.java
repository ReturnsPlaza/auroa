package dev.aurora.module.impl;

import dev.aurora.gui.Palette;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.BooleanSetting;
import dev.aurora.setting.ModeSetting;
import dev.aurora.setting.NumberSetting;

public class InterfaceModule extends Module {
    private final ModeSetting theme = add(new ModeSetting("Theme", "Default", Palette.THEMES));
    private final ModeSetting accent = add(new ModeSetting("Accent", "Cyan", "Cyan"));
    private final NumberSetting speed = add(new NumberSetting("Animation speed", 14, 4, 30, 1));
    private final BooleanSetting dim = add(new BooleanSetting("Dim background", true));
    private final BooleanSetting chunkGrid = add(new BooleanSetting("Chunk grid", true));

    public InterfaceModule() {
        super("Aurora+", "Menu colour, animation speed and background", Category.MISC);
        togglable = false;
        loadEnabled(true);
    }

    public String theme() {
        return theme.get();
    }

    public String accent() {
        return accent.get();
    }

    public double speed() {
        return speed.asDouble();
    }

    public boolean dimBackground() {
        return dim.on();
    }

    public boolean showChunkGrid() {
        return chunkGrid.on();
    }
}
