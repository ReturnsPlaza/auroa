package dev.aurora.module;

import dev.aurora.setting.Setting;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    public final String name;
    public final String description;
    public final Category category;

    /** Modules that only hold settings (like Aurora+) set this to false. */
    public boolean togglable = true;

    /** GLFW key code, or -1 for none. */
    public int key = -1;

    /** When true the module is on only while its key is held down. */
    public boolean holdKey;

    private boolean enabled;
    private final List<Setting<?>> settings = new ArrayList<>();

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected <S extends Setting<?>> S add(S setting) {
        settings.add(setting);
        return setting;
    }

    /** Adds a setting that is saved but not shown in the menu. */
    protected <S extends Setting<?>> S addHidden(S setting) {
        setting.hidden = true;
        settings.add(setting);
        return setting;
    }

    public List<Setting<?>> settings() {
        return Collections.unmodifiableList(settings);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean on) {
        if (enabled == on) {
            return;
        }
        enabled = on;
        if (on) {
            onEnable();
        } else {
            onDisable();
        }
    }

    /** Sets the state without running onEnable (used when loading the config). */
    public void loadEnabled(boolean on) {
        enabled = on;
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    /** Runs once per client tick while the module is enabled and a world is loaded. */
    public void onTick() {
    }
}
