package dev.aurora.hud;

import net.minecraft.client.gui.DrawContext;

/** Lightweight screen-space aurora, stars, and rain used behind the HUD and click menu. */
public final class AtmosphereRenderer {
    private AtmosphereRenderer() {
    }

    public static void render(DrawContext ctx, int width, int height) {
        double time = System.currentTimeMillis() / 1000.0;
        int segments = 56;
        int strip = Math.max(2, width / segments + 2);
        for (int i = 0; i < segments; i++) {
            int x = i * width / segments;
            double wave = Math.sin(i * 0.15 + time * 0.22) * height * 0.055
                    + Math.sin(i * 0.065 - time * 0.15) * height * 0.035;
            int top = (int) (height * 0.20 + wave);
            int span = Math.max(30, height / 6);
            ctx.fill(x, top, x + strip, top + span, 0x0C46CBEA);
            ctx.fill(x, top + 8, x + strip, top + span + 12, 0x0847D4F2);
        }

        for (int i = 0; i < 44; i++) {
            int seed = i * 7919;
            int x = Math.floorMod(seed, Math.max(1, width));
            int y = Math.floorMod(seed * 31, Math.max(1, height / 2));
            int alpha = 35 + (int) (22 * (0.5 + 0.5 * Math.sin(time * 1.7 + i)));
            ctx.fill(x, y, x + 1, y + 1, (alpha << 24) | 0x88DFFF);
        }

        int drops = Math.max(28, Math.min(42, width / 42));
        for (int i = 0; i < drops; i++) {
            int seed = i * 104729;
            int x = Math.floorMod((int) (seed + time * (20 + i % 7)), Math.max(1, width));
            int y = Math.floorMod((int) (seed * 13L + time * (140 + i % 80)), Math.max(1, height));
            int length = 9 + i % 15;
            double drift = 2.0 + (i % 4) * 0.7;
            int alpha = 35 + (i % 4) * 9;
            EspRenderer.line(ctx, x, y, x - drift, y + length, (alpha << 24) | 0xA8E9FF, 1);
        }
    }
}
