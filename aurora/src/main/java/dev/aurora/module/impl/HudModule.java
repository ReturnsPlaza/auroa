package dev.aurora.module.impl;

import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.BooleanSetting;

public class HudModule extends Module {
    private final BooleanSetting fps = add(new BooleanSetting("FPS", true));
    private final BooleanSetting clock = add(new BooleanSetting("Clock", true));
    private final BooleanSetting coords = add(new BooleanSetting("Coordinates", true));
    private final BooleanSetting ping = add(new BooleanSetting("Ping", true));
    private final BooleanSetting ticks = add(new BooleanSetting("Ticks", true));
    private final BooleanSetting bps = add(new BooleanSetting("Speed", true));

    public HudModule() {
        super("HUD", "Shows FPS, clock, coordinates, ping, ticks and speed", Category.RENDER);
        loadEnabled(true);
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
}
