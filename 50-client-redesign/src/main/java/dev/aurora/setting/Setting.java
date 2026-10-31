package dev.aurora.setting;

public abstract class Setting<T> {
    public final String name;
    protected T value;
    /** Hidden settings are saved to the config but not shown in the menu. */
    public boolean hidden;

    protected Setting(String name, T value) {
        this.name = name;
        this.value = value;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }

    public abstract String serialize();

    public abstract void deserialize(String s);
}
