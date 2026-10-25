package dev.aurora.module.impl;

import dev.aurora.module.Category;
import dev.aurora.module.Module;

/**
 * Look around with the camera while your character keeps facing the same way.
 * Bind a key and hold it. The mouse movement is handled in EntityMixin and the
 * camera in CameraMixin.
 */
public class FreeLook extends Module {
    private float yaw;
    private float pitch;

    public FreeLook() {
        super("Free Look", "Hold the key to look around without turning your character", Category.MISC);
        holdKey = true;
    }

    @Override
    protected void onEnable() {
        if (mc.player != null) {
            yaw = mc.player.getYaw();
            pitch = mc.player.getPitch();
        }
    }

    public void look(double dx, double dy) {
        yaw += (float) (dx * 0.15);
        pitch = Math.max(-90f, Math.min(90f, pitch + (float) (dy * 0.15)));
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }
}
