package dev.aurora;

import dev.aurora.module.Module;
import dev.aurora.module.ModuleManager;
import dev.aurora.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Saves which modules are on, their settings and keybinds to config/aurora.properties. */
public final class Config {
    private Config() {
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("aurora.properties");
    }

    public static void load(ModuleManager manager) {
        Path path = file();
        if (!Files.exists(path)) {
            return;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
        } catch (IOException e) {
            return;
        }
        for (Module m : manager.all()) {
            String enabled = props.getProperty(m.name + ".enabled");
            if (enabled != null && m.togglable) {
                m.loadEnabled(Boolean.parseBoolean(enabled));
            }
            String key = props.getProperty(m.name + ".key");
            if (key != null) {
                try {
                    m.key = Integer.parseInt(key);
                } catch (NumberFormatException ignored) {
                }
            }
            for (Setting<?> s : m.settings()) {
                String v = props.getProperty(m.name + "." + s.name);
                if (v != null) {
                    s.deserialize(v);
                }
            }
        }
    }

    public static void save(ModuleManager manager) {
        Properties props = new Properties();
        for (Module m : manager.all()) {
            if (m.togglable) {
                props.setProperty(m.name + ".enabled", Boolean.toString(m.isEnabled()));
            }
            props.setProperty(m.name + ".key", Integer.toString(m.key));
            for (Setting<?> s : m.settings()) {
                props.setProperty(m.name + "." + s.name, s.serialize());
            }
        }
        try (OutputStream out = Files.newOutputStream(file())) {
            props.store(out, "Aurora settings");
        } catch (IOException ignored) {
        }
    }
}
