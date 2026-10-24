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
    public final int width;
    public final int height;

    private final double px;
    private final double py;
    private final double pz;
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
        px = pos.x;
        py = pos.y;
        pz = pos.z;

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

    /** Returns false when the point is behind the camera. out receives {x, y} in GUI pixels. */
    public boolean project(double x, double y, double z, double[] out) {
        double dx = x - px;
        double dy = y - py;
        double dz = z - pz;
        double depth = dx * fx + dy * fy + dz * fz;
        if (depth < 0.05) {
            return false;
        }
        double xc = dx * rx + dz * rz;
        double yc = dx * ux + dy * uy + dz * uz;
        out[0] = width / 2.0 * (1 + xc / (depth * tanHalf * aspect));
        out[1] = height / 2.0 * (1 - yc / (depth * tanHalf));
        return true;
    }
}
