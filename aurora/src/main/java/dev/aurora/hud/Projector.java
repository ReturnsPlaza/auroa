package dev.aurora.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;

/**
 * Turns world positions into GUI coordinates using the camera's position,
 * rotation and the FOV setting. Because it works on the 2D screen layer,
 * everything drawn with it shows through walls.
 */
public final class Projector {
    public static final double NEAR = 0.05;

    public final int width;
    public final int height;
    public final double camX;
    public final double camY;
    public final double camZ;

    private final double rx;
    private final double rz;
    private final double ux;
    private final double uy;
    private final double uz;
    private final double fx;
    private final double fy;
    private final double fz;
    private final double tanHalf;
    private final double aspect;

    public Projector(MinecraftClient mc) {
        Camera cam = mc.gameRenderer.getCamera();
        Vec3d pos = cam.getCameraPos();
        camX = pos.x;
        camY = pos.y;
        camZ = pos.z;

        double yaw = Math.toRadians(cam.getYaw());
        double pitch = Math.toRadians(cam.getPitch());
        double sy = Math.sin(yaw);
        double cyaw = Math.cos(yaw);
        double sp = Math.sin(pitch);
        double cp = Math.cos(pitch);

        fx = -sy * cp;
        fy = -sp;
        fz = cyaw * cp;
        rx = -cyaw;
        rz = -sy;
        ux = -sy * sp;
        uy = cp;
        uz = cyaw * sp;

        tanHalf = Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
        width = mc.getWindow().getScaledWidth();
        height = mc.getWindow().getScaledHeight();
        aspect = (double) mc.getWindow().getFramebufferWidth() / (double) mc.getWindow().getFramebufferHeight();
    }

    /** World position to camera space: out = {right, up, depth}. */
    public void toCamera(double x, double y, double z, double[] out) {
        double dx = x - camX;
        double dy = y - camY;
        double dz = z - camZ;
        out[0] = dx * rx + dz * rz;
        out[1] = dx * ux + dy * uy + dz * uz;
        out[2] = dx * fx + dy * fy + dz * fz;
    }

    /** Camera space (depth must be above NEAR) to GUI pixels. */
    public void toScreen(double[] cam, double[] out) {
        out[0] = width / 2.0 * (1 + cam[0] / (cam[2] * tanHalf * aspect));
        out[1] = height / 2.0 * (1 - cam[1] / (cam[2] * tanHalf));
    }
}
