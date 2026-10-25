package dev.aurora.module.impl;

import dev.aurora.hud.HudLayout;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.BooleanSetting;
import dev.aurora.setting.NumberSetting;

public class HudModule extends Module {
    private final BooleanSetting brand = add(new BooleanSetting("Brand", true));
    private final BooleanSetting fps = add(new BooleanSetting("FPS", true));
    private final BooleanSetting clock = add(new BooleanSetting("Clock", true));
    private final BooleanSetting coords = add(new BooleanSetting("Coordinates", true));
    private final BooleanSetting ping = add(new BooleanSetting("Ping", true));
    private final BooleanSetting ticks = add(new BooleanSetting("Ticks", true));
    private final BooleanSetting bps = add(new BooleanSetting("Speed", true));
    private final BooleanSetting moduleList = add(new BooleanSetting("Module list", true));
    private final NumberSetting opacity = add(new NumberSetting("Opacity", 190, 60, 255, 5));

    private final NumberSetting infoX = addHidden(new NumberSetting("infoX", -1, -1, 5000, 1));
    private final NumberSetting infoY = addHidden(new NumberSetting("infoY", -1, -1, 5000, 1));
    private final NumberSetting listX = addHidden(new NumberSetting("listX", -1, -1, 5000, 1));
    private final NumberSetting listY = addHidden(new NumberSetting("listY", -1, -1, 5000, 1));

    public final HudLayout.Element infoElement =
            HudLayout.register("HUD", infoX, infoY, 4, 4, false, false);
    public final HudLayout.Element listElement =
            HudLayout.register("Module list", listX, listY, 4, 4, true, false);

    public HudModule() {
        super("HUD", "FPS, clock, coordinates, ping, ticks, speed and the module list", Category.RENDER);
        loadEnabled(true);
    }

    public boolean showBrand() {
        return brand.on();
    }

    public boolean showFps() {
        return fps.on();
    }

    public boolean showClock() {
        return clock.on();
    }

    public boolean showCoords() {
        return coords.on();
    }

    public boolean showPing() {
        return ping.on();
    }

    public boolean showTicks() {
        return ticks.on();
    }

    public boolean showSpeed() {
        return bps.on();
    }

    public boolean showModuleList() {
        return moduleList.on();
    }

    public int opacity() {
        return opacity.asInt();
    }
}
