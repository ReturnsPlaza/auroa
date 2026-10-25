package dev.aurora.module;

import dev.aurora.Keys;
import dev.aurora.module.impl.AutoEat;
import dev.aurora.module.impl.AutoLog;
import dev.aurora.module.impl.AutoMine;
import dev.aurora.module.impl.AutoTool;
import dev.aurora.module.impl.Atmosphere;
import dev.aurora.module.impl.CustomHotbar;
import dev.aurora.module.impl.BlockEsp;
import dev.aurora.module.impl.CrosshairModule;
import dev.aurora.module.impl.Keystrokes;
import dev.aurora.module.impl.EntityEsp;
import dev.aurora.module.impl.FastPlace;
import dev.aurora.module.impl.Freecam;
import dev.aurora.module.impl.FreeLook;
import dev.aurora.module.impl.Friends;
import dev.aurora.module.impl.Fullbright;
import dev.aurora.module.impl.GamemodeAlert;
import dev.aurora.module.impl.HudModule;
import dev.aurora.module.impl.InterfaceModule;
import dev.aurora.module.impl.RadarModule;
import dev.aurora.module.impl.Sprint;
import dev.aurora.module.impl.StorageEsp;
import dev.aurora.module.impl.TargetHud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    private final Map<Module, Boolean> held = new HashMap<>();

    public void registerDefaults() {
        // Misc
        register(new Sprint());
        register(new FastPlace());
        register(new AutoTool());
        register(new AutoEat());
        register(new AutoMine());
        register(new AutoLog());
        register(new FreeLook());
        register(new Freecam());
        register(new GamemodeAlert());
        // Render
        register(new Fullbright());
        register(new EntityEsp());
        register(new StorageEsp());
        register(new BlockEsp());
        register(new TargetHud());
        register(new HudModule());
        register(new RadarModule());
        register(new Keystrokes());
        register(new CrosshairModule());
        register(new Atmosphere());
        register(new CustomHotbar());
        // Client
        register(new InterfaceModule());
        register(new Friends());
    }

    public void register(Module module) {
        modules.add(module);
    }

    public List<Module> all() {
        return modules;
    }

    public List<Module> in(Category category) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) {
            if (m.category == category) {
                out.add(m);
            }
        }
        return out;
    }

    public <T extends Module> T get(Class<T> type) {
        for (Module m : modules) {
            if (type.isInstance(m)) {
                return type.cast(m);
            }
        }
        throw new IllegalStateException("Module not registered: " + type.getSimpleName());
    }

    /** Handles module keybinds. allowed is false while a screen is open or no world is loaded. */
    public void pollKeys(boolean allowed) {
        for (Module m : modules) {
            if (m.key < 0 || !m.togglable) {
                continue;
            }
            boolean down = Keys.down(m.key);
            boolean was = held.getOrDefault(m, false);
            held.put(m, down);
            if (m.holdKey) {
                if (allowed && down && !was) {
                    m.setEnabled(true);
                } else if (!down && was) {
                    m.setEnabled(false);
                }
            } else if (allowed && down && !was) {
                m.toggle();
            }
        }
    }

    public void tick() {
        for (Module m : modules) {
            if (m.isEnabled()) {
                m.onTick();
            }
        }
    }
}
