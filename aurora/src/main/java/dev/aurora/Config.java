package dev.aurora;

import dev.aurora.module.Module;
import dev.aurora.module.ModuleManager;
import dev.aurora.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Saves which modules are on, their settings and keybinds.
 * The live settings are in config/aurora.properties. Named configs live in config/aurora/configs/
 * as <name>.properties, so you can share them by sending the file, or drop someone else's file in there.
 */
public final class Config {
    private Config() {
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("aurora.properties");
    }

    public static Path dir() {
        Path p = FabricLoader.getInstance().getConfigDir().resolve("aurora").resolve("configs");
        try {
            Files.createDirectories(p);
        } catch (IOException ignored) {
        }
        return p;
    }

    private static Path named(String name) {
        return dir().resolve(name + ".properties");
    }

    /** Keeps only letters, numbers, dash and underscore. Returns null if nothing is left. */
    public static String cleanName(String raw) {
        String s = raw.replaceAll("[^A-Za-z0-9_-]", "");
        return s.isEmpty() ? null : s;
    }

    /** Names of all saved configs, without the .properties part. */
    public static List<String> names() {
        List<String> out = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir(), "*.properties")) {
            for (Path p : stream) {
                String f = p.getFileName().toString();
                out.add(f.substring(0, f.length() - ".properties".length()));
            }
        } catch (IOException ignored) {
        }
        out.sort(String::compareToIgnoreCase);
        return out;
    }

    /** A name that is not used yet: config1, config2, ... */
    public static String nextName() {
        for (int i = 1; ; i++) {
            String n = "config" + i;
            if (!Files.exists(named(n))) {
                return n;
            }
        }
    }

    public static boolean delete(String name) {
        try {
            return Files.deleteIfExists(named(name));
        } catch (IOException e) {
            return false;
        }
    }

    public static void load(ModuleManager manager) {
        read(manager, file(), false);
    }

    public static void save(ModuleManager manager) {
        write(manager, file());
    }

    public static void saveNamed(ModuleManager manager, String name) {
        write(manager, named(name));
    }

    /** Applies a saved config right away. Returns false if no config has that name. */
    public static boolean loadNamed(ModuleManager manager, String name) {
        Path p = named(name);
        if (!Files.exists(p)) {
            return false;
        }
        read(manager, p, true);
        return true;
    }

    private static void read(ModuleManager manager, Path path, boolean live) {
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
                boolean on = Boolean.parseBoolean(enabled);
                if (live) {
                    m.setEnabled(on);
                } else {
                    m.loadEnabled(on);
                }
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

    private static void write(ModuleManager manager, Path path) {
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
        try (OutputStream out = Files.newOutputStream(path)) {
            props.store(out, "Aurora settings");
        } catch (IOException ignored) {
        }
    }
}
