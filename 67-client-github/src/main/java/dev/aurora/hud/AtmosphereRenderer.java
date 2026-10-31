package dev.aurora.hud;

import net.minecraft.client.gui.DrawContext;

/** Soft sky-only aurora, stars, and custom rain accents. */
public final class AtmosphereRenderer {
    private AtmosphereRenderer() { }

    public static void render(DrawContext ctx, int width, int height, boolean aurora, boolean rain) {
        if (width <= 0 || height <= 0 || (!aurora && !rain)) return;

        double time = System.currentTimeMillis() / 1000.0;
        if (aurora) {
            // Narrow gradient strips follow a continuous wave, avoiding the coarse blocks
            // produced by the previous 56-panel overlay.
            int segments = Math.max(180, Math.min(480, width / 3));
            int strip = (int) Math.ceil(width / (double) segments) + 1;
            for (int i = 0; i < segments; i++) {
                int x = i * width / segments;
                double wave = Math.sin(i * 0.022 + time * 0.20) * height * 0.045
                        + Math.sin(i * 0.009 - time * 0.13) * height * 0.032;
                int top = (int) (height * 0.09 + wave);
                int band = Math.max(48, height / 4);
                int middle = top + band / 2;
                int end = top + band;
                int edge = 0x004DEBFF;
                int glow = 0x274DEBFF;
                ctx.fillGradient(x, top, x + strip, middle, edge, glow);
                ctx.fillGradient(x, middle, x + strip, end, glow, edge);
            }

            for (int i = 0; i < 28; i++) {
                int seed = i * 7919;
                int x = Math.floorMod(seed, width);
                int y = Math.floorMod(seed * 31, Math.max(1, height / 3));
                int alpha = 32 + (int) (30 * (0.5 + 0.5 * Math.sin(time * 1.2 + i)));
                ctx.fill(x, y, x + 2, y + 2, (alpha << 24) | 0xB9EDFF);
            }
        }

        if (rain) {
            int drops = Math.max(32, Math.min(52, width / 36));
            for (int i = 0; i < drops; i++) {
                int seed = i * 104729;
                int x = Math.floorMod((int) (seed + time * (32 + i % 9)), width);
                int y = Math.floorMod((int) (seed * 13L + time * (150 + i % 95)), Math.max(1, height * 3 / 4));
                int length = 10 + i % 14;
                double drift = 2.0 + (i % 4) * 0.55;
                int alpha = 28 + (i % 4) * 7;
                EspRenderer.line(ctx, x, y, x - drift, y + length, (alpha << 24) | 0xBCEEFF, 2);
            }
        }
    }
}

