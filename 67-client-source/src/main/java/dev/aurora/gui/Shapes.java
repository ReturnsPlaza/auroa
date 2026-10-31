package dev.aurora.gui;

import net.minecraft.client.gui.DrawContext;

/**
 * Filled shapes with rounded corners. Each row is drawn as a strip, and the
 * partly covered edge pixel gets a matching alpha, so the curve looks anti-aliased.
 */
public final class Shapes {
    private Shapes() {
    }

    public static void roundFill(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        int a = color >>> 24;
        if (w <= 0 || h <= 0 || a == 0) {
            return;
        }
        int rgb = color & 0xFFFFFF;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int row = 0; row < h; row++) {
            int d = Math.min(row, h - 1 - row);
            double ins = 0;
            if (d < r) {
                double dy = r - d - 0.5;
                ins = r - Math.sqrt(Math.max(0.0, (double) r * r - dy * dy));
            }
            int i = (int) Math.floor(ins);
            double f = ins - i;
            int left = x + i;
            int right = x + w - i; // exclusive
            if (right <= left) {
                continue;
            }
            int py = y + row;
            if (f > 0.05 && right - left >= 2) {
                int edge = (((int) Math.round(a * (1 - f))) << 24) | rgb;
                ctx.fill(left, py, left + 1, py + 1, edge);
                ctx.fill(right - 1, py, right, py + 1, edge);
                left++;
                right--;
            }
            if (right > left) {
                ctx.fill(left, py, right, py + 1, color);
            }
        }
    }
}
