package dev.aurora.gui;

/**
 * A value that glides toward a target. It is frame-rate independent, so
 * the motion looks the same at 60 FPS and at 300 FPS.
 */
public final class Anim {
    private double value;
    private long last = System.nanoTime();

    public Anim(double start) {
        this.value = start;
    }

    public double update(double target, double speed) {
        long now = System.nanoTime();
        double dt = Math.min((now - last) / 1_000_000_000.0, 0.1);
        last = now;
        value += (target - value) * (1.0 - Math.exp(-speed * dt));
        if (Math.abs(target - value) < 0.0005) {
            value = target;
        }
        return value;
    }

    public double get() {
        return value;
    }

    public void set(double v) {
        value = v;
        last = System.nanoTime();
    }
}
