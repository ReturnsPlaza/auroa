package dev.aurora.setting;

public class NumberSetting extends Setting<Double> {
    public final double min;
    public final double max;
    public final double step;

    public NumberSetting(String name, double def, double min, double max, double step) {
        super(name, def);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    @Override
    public void set(Double v) {
        value = snap(v);
    }

    private double snap(double v) {
        v = Math.max(min, Math.min(max, v));
        if (step > 0) {
            v = Math.round((v - min) / step) * step + min;
        }
        return Math.max(min, Math.min(max, v));
    }

    public double fraction() {
        return (value - min) / (max - min);
    }

    public void setFraction(double f) {
        set(min + Math.max(0, Math.min(1, f)) * (max - min));
    }

    public int asInt() {
        return (int) Math.round(value);
    }

    public double asDouble() {
        return value;
    }

    @Override
    public String serialize() {
        return Double.toString(value);
    }

    @Override
    public void deserialize(String s) {
        try {
            set(Double.parseDouble(s));
        } catch (NumberFormatException ignored) {
        }
    }
}
