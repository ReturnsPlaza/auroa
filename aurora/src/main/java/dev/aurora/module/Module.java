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

    /** Modules that only hold settings (like Interface) set this to false. */
    public boolean togglable = true;

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
