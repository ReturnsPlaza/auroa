package dev.aurora.module.impl;

import dev.aurora.gui.Palette;
import dev.aurora.hud.HudLayout;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.BooleanSetting;
import dev.aurora.setting.ModeSetting;
import dev.aurora.setting.NumberSetting;

public class RadarModule extends Module {
    private final NumberSetting size = add(new NumberSetting("Size", 46, 24, 100, 2));
    private final NumberSetting range = add(new NumberSetting("Range", 48, 16, 160, 4));
    private final NumberSetting height = add(new NumberSetting("Height limit", 24, 4, 128, 4));
    private final BooleanSetting players = add(new BooleanSetting("Players", true));
    private final BooleanSetting hostile = add(new BooleanSetting("Hostile", false));
    private final BooleanSetting passive = add(new BooleanSetting("Passive", false));
    private final BooleanSetting compass = add(new BooleanSetting("Compass", true));
    private final BooleanSetting coords = add(new BooleanSetting("Coordinates", true));
    private final BooleanSetting rings = add(new BooleanSetting("Rings", true));
    private final BooleanSetting rotate = add(new BooleanSetting("Rotate with view", true));
    private final NumberSetting dot = add(new NumberSetting("Dot size", 3, 1, 5, 1));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", 130, 30, 255, 5));
    private final ModeSetting color = add(new ModeSetting("Color", "Purple", Palette.NAMES));

    private final NumberSetting posX = addHidden(new NumberSetting("posX", -1, -1, 5000, 1));
    private final NumberSetting posY = addHidden(new NumberSetting("posY", -1, -1, 5000, 1));

    public final HudLayout.Element element = HudLayout.register("Radar", posX, posY, 6, 42, false, false);

    public RadarModule() {
        super("Radar", "A round radar showing nearby players and mobs", Category.RENDER);
        loadEnabled(true);
    }

    public int radius() {
        return size.asInt();
    }

    public double range() {
        return range.asDouble();
    }

    public double height() {
        return height.asDouble();
    }

    public boolean showPlayers() {
        return players.on();
    }

    public boolean showHostile() {
        return hostile.on();
    }

    public boolean showPassive() {
        return passive.on();
    }

    public boolean showCompass() {
        return compass.on();
    }

    public boolean showCoords() {
        return coords.on();
    }

    public boolean showRings() {
        return rings.on();
    }

    public boolean rotate() {
        return rotate.on();
    }

    public int dot() {
        return dot.asInt();
    }

    public int opacity() {
        return opacity.asInt();
    }

    public String colorName() {
        return color.get();
    }
}
