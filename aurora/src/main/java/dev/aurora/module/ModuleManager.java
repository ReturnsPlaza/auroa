package dev.aurora.module;

import dev.aurora.module.impl.AutoEat;
import dev.aurora.module.impl.AutoTool;
import dev.aurora.module.impl.EntityEsp;
import dev.aurora.module.impl.FastPlace;
import dev.aurora.module.impl.Fullbright;
import dev.aurora.module.impl.HudModule;
import dev.aurora.module.impl.InterfaceModule;
import dev.aurora.module.impl.RadarModule;
import dev.aurora.module.impl.Sprint;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    public void registerDefaults() {
        // Misc
        register(new Sprint());
        register(new FastPlace());
        register(new AutoTool());
        register(new AutoEat());
        // Render
        register(new Fullbright());
        register(new EntityEsp());
        register(new HudModule());
        register(new RadarModule());
        // Client
        register(new InterfaceModule());
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

    public void tick() {
        for (Module m : modules) {
            if (m.isEnabled()) {
                m.onTick();
            }
        }
    }
}
