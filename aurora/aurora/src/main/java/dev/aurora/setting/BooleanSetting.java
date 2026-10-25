package dev.aurora.setting;

public class BooleanSetting extends Setting<Boolean> {
    public BooleanSetting(String name, boolean def) {
        super(name, def);
    }

    public boolean on() {
        return value;
    }

    public void toggle() {
        value = !value;
    }

    @Override
    public String serialize() {
        return Boolean.toString(value);
    }

    @Override
    public void deserialize(String s) {
        value = Boolean.parseBoolean(s);
    }
}
