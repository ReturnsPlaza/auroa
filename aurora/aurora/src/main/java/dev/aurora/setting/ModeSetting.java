package dev.aurora.setting;

import java.util.List;

public class ModeSetting extends Setting<String> {
    private final List<String> modes;

    public ModeSetting(String name, String def, String... modes) {
        super(name, def);
        this.modes = List.of(modes);
    }

    public void cycle() {
        int i = modes.indexOf(value);
        value = modes.get((i + 1) % modes.size());
    }

    public boolean is(String mode) {
        return value.equals(mode);
    }

    @Override
    public String serialize() {
        return value;
    }

    @Override
    public void deserialize(String s) {
        if (modes.contains(s)) {
            value = s;
        }
    }
}
