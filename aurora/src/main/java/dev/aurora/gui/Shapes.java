package dev.aurora.gui;

import net.minecraft.client.gui.DrawContext;

/** Filled shapes with rounded corners, drawn one row at a time so the corners look smooth. */
public final class Shapes {
    private Shapes() {
    }

    public static void roundFill(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0 || (color >>> 24) == 0) {
            return;
        }
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int row = 0; row < h; row++) {
            int d = Math.min(row, h - 1 - row);
            int inset = 0;
            if (d < r) {
                double dy = r - d - 0.5;
                inset = r - (int) Math.round(Math.sqrt(Math.max(0.0, (double) r * r - dy * dy)));
            }
            if (w - 2 * inset > 0) {
                ctx.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
            }
        }
    }
}
