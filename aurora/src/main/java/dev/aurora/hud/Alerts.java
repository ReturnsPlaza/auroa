package dev.aurora.hud;

import dev.aurora.gui.Fonts;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.entity.player.SkinTextures;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Big banner pop-ups shown at the top of the screen (name, skin head and a line of text). */
public final class Alerts {
    private Alerts() {
    }

    private static final int FADE_IN = 250;
    private static final int FADE_OUT = 600;

    private record Alert(String name, String line, SkinTextures skin, long start, long duration) {
    }

    private static final List<Alert> ALERTS = new ArrayList<>();

    public static void push(String name, String line, SkinTextures skin, int seconds) {
        ALERTS.add(new Alert(name, line, skin, System.currentTimeMillis(), seconds * 1000L));
        while (ALERTS.size() > 4) {
            ALERTS.remove(0);
        }
    }

    public static void clear() {
        ALERTS.clear();
    }

    public static void render(DrawContext ctx, MinecraftClient mc) {
        if (ALERTS.isEmpty()) {
            return;
        }
        TextRenderer tr = mc.textRenderer;
        int cx = mc.getWindow().getScaledWidth() / 2;
        int y = 22;
        long now = System.currentTimeMillis();

        Iterator<Alert> it = ALERTS.iterator();
        while (it.hasNext()) {
            Alert a = it.next();
            long age = now - a.start();
            if (age > a.duration()) {
                it.remove();
                continue;
            }
            double in = Math.min(1.0, age / (double) FADE_IN);
            double out = Math.min(1.0, (a.duration() - age) / (double) FADE_OUT);
            double alpha = Math.min(in, out);
            // Ease the fade so it feels soft rather than linear.
            alpha = alpha * alpha * (3 - 2 * alpha);
            int al = (int) Math.round(alpha * 255);
            if (al < 8) {
                continue;
            }

            float scale = 2f;
            int nameW = (int) (Fonts.width(a.name()) * scale);
            int lineW = Fonts.width(a.line());
            int textW = Math.max(nameW, lineW);
            int total = 20 + textW;
            int left = cx - total / 2;
            int slide = (int) Math.round((1 - in) * -8);
            int top = y + slide;

            ctx.fill(left - 6, top - 4, left + total + 6, top + 36, (al * 150 / 255) << 24 | 0x0B0D12);
            ctx.fill(left - 6, top - 4, left - 4, top + 36, (al << 24) | 0xFF4A4A);

            if (a.skin() != null) {
                PlayerSkinDrawer.draw(ctx, a.skin(), left, top + 2, 16);
            }

            int tx = left + 20;
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(tx, top);
            ctx.getMatrices().scale(scale, scale);
            Fonts.draw(ctx, a.name(), 0, 0, (al << 24) | 0xFF4A4A, false);
            ctx.getMatrices().popMatrix();
            Fonts.draw(ctx, a.line(), tx, top + 22, (al << 24) | 0xE8EAEE, false);

            y += 44;
        }
    }
}
