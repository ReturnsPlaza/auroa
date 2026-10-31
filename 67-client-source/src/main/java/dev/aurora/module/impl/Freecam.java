package dev.aurora.module.impl;

import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.NumberSetting;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Detaches the camera from your body so you can fly around and look.
 * Your character stays where it is. Mouse look still turns your character's head,
 * and the original view is restored when you turn the module off.
 */
public class Freecam extends Module {
    private final NumberSetting speed = add(new NumberSetting("Speed", 1.0, 0.2, 4.0, 0.1));

    private double x;
    private double y;
    private double z;
    private double prevX;
    private double prevY;
    private double prevZ;
    private double vx;
    private double vy;
    private double vz;
    private float savedYaw;
    private float savedPitch;

    public Freecam() {
        super("Freecam", "Fly a detached camera while your character stays put", Category.WORLD);
    }

    @Override
    protected void onEnable() {
        if (mc.player == null || mc.gameRenderer == null) {
            setEnabled(false);
            return;
        }
        var pos = mc.gameRenderer.getCamera().getCameraPos();
        x = prevX = pos.x;
        y = prevY = pos.y;
        z = prevZ = pos.z;
        vx = vy = vz = 0;
        savedYaw = mc.player.getYaw();
        savedPitch = mc.player.getPitch();
    }

    @Override
    protected void onDisable() {
        if (mc.player != null) {
            mc.player.setYaw(savedYaw);
            mc.player.setPitch(savedPitch);
        }
        vx = vy = vz = 0;
    }

    @Override
    public void onTick() {
        if (mc.player == null) {
            return;
        }
        prevX = x;
        prevY = y;
        prevZ = z;

        double forward = 0;
        double strafe = 0;
        double lift = 0;
        if (mc.currentScreen == null) {
            forward = (down(mc.options.forwardKey) ? 1 : 0) - (down(mc.options.backKey) ? 1 : 0);
            strafe = (down(mc.options.rightKey) ? 1 : 0) - (down(mc.options.leftKey) ? 1 : 0);
            lift = (down(mc.options.jumpKey) ? 1 : 0) - (down(mc.options.sneakKey) ? 1 : 0);
        }

        double yaw = Math.toRadians(mc.player.getYaw());
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        double rx = -Math.cos(yaw);
        double rz = -Math.sin(yaw);

        double tx = forward * fx + strafe * rx;
        double tz = forward * fz + strafe * rz;
        double len = Math.sqrt(tx * tx + tz * tz + lift * lift);
        if (len > 1) {
            tx /= len;
            tz /= len;
            lift /= len;
        }
        double s = speed.asDouble();
        // Ease toward the target speed so starting and stopping feel smooth.
        vx += (tx * s - vx) * 0.3;
        vy += (lift * s - vy) * 0.3;
        vz += (tz * s - vz) * 0.3;
        x += vx;
        y += vy;
        z += vz;

        // Keep the character from walking while the camera flies.
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
        mc.options.sprintKey.setPressed(false);
        // Stop the character's leftover momentum so the body stays put. Gravity still applies.
        mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
    }

    public double camX(float t) {
        return prevX + (x - prevX) * t;
    }

    public double camY(float t) {
        return prevY + (y - prevY) * t;
    }

    public double camZ(float t) {
        return prevZ + (z - prevZ) * t;
    }

    private static boolean down(KeyBinding key) {
        InputUtil.Key bound = KeyBindingHelper.getBoundKeyOf(key);
        long window = mc.getWindow().getHandle();
        if (bound.getCategory() == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, bound.getCode()) == GLFW.GLFW_PRESS;
        }
        if (bound.getCode() < 0) {
            return false;
        }
        return GLFW.glfwGetKey(window, bound.getCode()) == GLFW.GLFW_PRESS;
    }
}
