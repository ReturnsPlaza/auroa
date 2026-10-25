package dev.aurora.hud;

import dev.aurora.AuroraClient;
import dev.aurora.module.impl.BlockEsp;
import dev.aurora.module.impl.EspModule;
import dev.aurora.module.impl.StorageEsp;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Draws block ESP as real box outlines: all 12 edges of each block, with the
 * near plane clipped so boxes beside or under you still draw correctly,
 * plus filled faces and optional tracers. Drawn on the screen layer, so it shows through walls.
 */
public final class EspRenderer {
    private EspRenderer() {
    }

    // Corner order of each face (bit 0 = x, bit 1 = y, bit 2 = z).
    private static final int[][] FACES = {
            {0, 2, 6, 4}, // -X
            {1, 3, 7, 5}, // +X
            {0, 1, 5, 4}, // -Y
            {2, 3, 7, 6}, // +Y
            {0, 1, 3, 2}, // -Z
            {4, 5, 7, 6}, // +Z
    };

    public static void render(DrawContext ctx, MinecraftClient mc) {
        StorageEsp storage = AuroraClient.modules.get(StorageEsp.class);
        BlockEsp blocks = AuroraClient.modules.get(BlockEsp.class);
        if (!storage.isEnabled() && !blocks.isEnabled()) {
            return;
        }
        Projector p = new Projector(mc);
        if (blocks.isEnabled()) {
            drawModule(ctx, p, blocks);
        }
        if (storage.isEnabled()) {
            drawModule(ctx, p, storage);
        }
    }

    private static void drawModule(DrawContext ctx, Projector p, EspModule m) {
        List<EspTarget> list = m.targets();
        double maxSq = m.maxDistance() * m.maxDistance();
        int fillAlpha = m.fillAlpha();
        int thick = m.thickness();

        double[][] cam = new double[8][3];
        double[] a = new double[3];
        double[] b = new double[3];
        double[] sa = new double[2];
        double[] sb = new double[2];
        double[] seg = new double[4];
        int drawn = 0;

        for (EspTarget t : list) {
            if (drawn >= 250) {
                break;
            }
            BlockPos pos = t.pos();
            double dx = pos.getX() + 0.5 - p.camX;
            double dy = pos.getY() + 0.5 - p.camY;
            double dz = pos.getZ() + 0.5 - p.camZ;
            if (dx * dx + dy * dy + dz * dz > maxSq) {
                continue;
            }

            boolean anyFront = false;
            for (int i = 0; i < 8; i++) {
                p.toCamera(pos.getX() + (i & 1), pos.getY() + ((i >> 1) & 1), pos.getZ() + ((i >> 2) & 1), cam[i]);
                if (cam[i][2] > Projector.NEAR) {
                    anyFront = true;
                }
            }
            if (!anyFront) {
                continue;
            }
            int rgb = t.color() & 0xFFFFFF;

            if (m.fill() && fillAlpha > 0) {
                drawFaces(ctx, p, pos, cam, (fillAlpha << 24) | rgb, sa);
            }

            if (m.outline()) {
                int color = 0xFF000000 | rgb;
                for (int i = 0; i < 8; i++) {
                    for (int bit = 1; bit <= 4; bit <<= 1) {
                        if ((i & bit) != 0) {
                            continue;
                        }
                        System.arraycopy(cam[i], 0, a, 0, 3);
                        System.arraycopy(cam[i | bit], 0, b, 0, 3);
                        if (!clipNear(a, b)) {
                            continue;
                        }
                        p.toScreen(a, sa);
                        p.toScreen(b, sb);
                        seg[0] = sa[0];
                        seg[1] = sa[1];
                        seg[2] = sb[0];
                        seg[3] = sb[1];
                        if (clipRect(seg, p.width, p.height)) {
                            line(ctx, seg[0], seg[1], seg[2], seg[3], color, thick);
                        }
                    }
                }
            }

            if (m.tracers()) {
                p.toCamera(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, a);
                if (a[2] > Projector.NEAR) {
                    p.toScreen(a, sa);
                    seg[0] = p.width / 2.0;
                    seg[1] = p.height / 2.0;
                    seg[2] = sa[0];
                    seg[3] = sa[1];
                    if (clipRect(seg, p.width, p.height)) {
                        line(ctx, seg[0], seg[1], seg[2], seg[3], 0xFF000000 | rgb, 1);
                    }
                }
            }
            drawn++;
        }
    }

    // ---------------------------------------------------------------- faces

    private static void drawFaces(DrawContext ctx, Projector p, BlockPos pos, double[][] cam, int color, double[] tmp) {
        boolean[] facing = {
                p.camX < pos.getX(),
                p.camX > pos.getX() + 1,
                p.camY < pos.getY(),
                p.camY > pos.getY() + 1,
                p.camZ < pos.getZ(),
                p.camZ > pos.getZ() + 1,
        };
        double[] xs = new double[4];
        double[] ys = new double[4];
        for (int f = 0; f < 6; f++) {
            if (!facing[f]) {
                continue;
            }
            boolean ok = true;
            for (int k = 0; k < 4; k++) {
                double[] c = cam[FACES[f][k]];
                if (c[2] <= Projector.NEAR) {
                    ok = false;
                    break;
                }
                p.toScreen(c, tmp);
                xs[k] = tmp[0];
                ys[k] = tmp[1];
            }
            if (ok) {
                fillQuad(ctx, xs, ys, color, p.width, p.height);
            }
        }
    }

    private static void fillQuad(DrawContext ctx, double[] xs, double[] ys, int color, int w, int h) {
        double minY = Math.min(Math.min(ys[0], ys[1]), Math.min(ys[2], ys[3]));
        double maxY = Math.max(Math.max(ys[0], ys[1]), Math.max(ys[2], ys[3]));
        int y0 = Math.max(0, (int) Math.floor(minY));
        int y1 = Math.min(h - 1, (int) Math.ceil(maxY));
        for (int y = y0; y <= y1; y++) {
            double cy = y + 0.5;
            double lo = Double.MAX_VALUE;
            double hi = -Double.MAX_VALUE;
            for (int e = 0; e < 4; e++) {
                int j = (e + 1) & 3;
                double ya = ys[e];
                double yb = ys[j];
                if ((ya <= cy && yb > cy) || (yb <= cy && ya > cy)) {
                    double x = xs[e] + (cy - ya) / (yb - ya) * (xs[j] - xs[e]);
                    lo = Math.min(lo, x);
                    hi = Math.max(hi, x);
                }
            }
            if (hi > lo) {
                int xa = Math.max(0, (int) Math.floor(lo));
                int xb = Math.min(w, (int) Math.ceil(hi));
                if (xb > xa) {
                    ctx.fill(xa, y, xb, y + 1, color);
                }
            }
        }
    }

    // ---------------------------------------------------------------- lines

    /** Clips a camera-space segment to the near plane. Returns false if it is fully behind the camera. */
    private static boolean clipNear(double[] a, double[] b) {
        double n = Projector.NEAR;
        if (a[2] < n && b[2] < n) {
            return false;
        }
        if (a[2] < n) {
            double t = (n - a[2]) / (b[2] - a[2]);
            a[0] += (b[0] - a[0]) * t;
            a[1] += (b[1] - a[1]) * t;
            a[2] = n;
        } else if (b[2] < n) {
            double t = (n - b[2]) / (a[2] - b[2]);
            b[0] += (a[0] - b[0]) * t;
            b[1] += (a[1] - b[1]) * t;
            b[2] = n;
        }
        return true;
    }

    /** Liang-Barsky: clips seg = {x1, y1, x2, y2} to the screen rectangle. */
    private static boolean clipRect(double[] seg, double w, double h) {
        double x0 = seg[0];
        double y0 = seg[1];
        double dx = seg[2] - seg[0];
        double dy = seg[3] - seg[1];
        double t0 = 0;
        double t1 = 1;
        double[] pp = {-dx, dx, -dy, dy};
        double[] qq = {x0, w - x0, y0, h - y0};
        for (int i = 0; i < 4; i++) {
            if (pp[i] == 0) {
                if (qq[i] < 0) {
                    return false;
                }
            } else {
                double r = qq[i] / pp[i];
                if (pp[i] < 0) {
                    if (r > t1) {
                        return false;
                    }
                    if (r > t0) {
                        t0 = r;
                    }
                } else {
                    if (r < t0) {
                        return false;
                    }
                    if (r < t1) {
                        t1 = r;
                    }
                }
            }
        }
        seg[0] = x0 + t0 * dx;
        seg[1] = y0 + t0 * dy;
        seg[2] = x0 + t1 * dx;
        seg[3] = y0 + t1 * dy;
        return true;
    }

    /** Draws a line using one fill per row (or column), so it stays cheap. */
    public static void line(DrawContext ctx, double x1, double y1, double x2, double y2, int color, int thick) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        if (Math.abs(dx) >= Math.abs(dy)) {
            int ya = (int) Math.floor(Math.min(y1, y2));
            int yb = (int) Math.floor(Math.max(y1, y2));
            for (int y = ya; y <= yb; y++) {
                double t0 = dy == 0 ? 0 : (y - y1) / dy;
                double t1 = dy == 0 ? 1 : (y + 1 - y1) / dy;
                t0 = Math.max(0, Math.min(1, t0));
                t1 = Math.max(0, Math.min(1, t1));
                double xa = x1 + dx * t0;
                double xb = x1 + dx * t1;
                int xs = (int) Math.floor(Math.min(xa, xb));
                int xe = (int) Math.ceil(Math.max(xa, xb));
                if (xe <= xs) {
                    xe = xs + 1;
                }
                ctx.fill(xs, y, xe, y + thick, color);
            }
        } else {
            int xa = (int) Math.floor(Math.min(x1, x2));
            int xb = (int) Math.floor(Math.max(x1, x2));
            for (int x = xa; x <= xb; x++) {
                double t0 = dx == 0 ? 0 : (x - x1) / dx;
                double t1 = dx == 0 ? 1 : (x + 1 - x1) / dx;
                t0 = Math.max(0, Math.min(1, t0));
                t1 = Math.max(0, Math.min(1, t1));
                double ya = y1 + dy * t0;
                double yb = y1 + dy * t1;
                int ys = (int) Math.floor(Math.min(ya, yb));
                int ye = (int) Math.ceil(Math.max(ya, yb));
                if (ye <= ys) {
                    ye = ys + 1;
                }
                ctx.fill(x, ys, x + thick, ye, color);
            }
        }
    }
}
