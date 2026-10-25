package dev.aurora.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Draws all Aurora text with the bundled Segoe UI Bold font (assets/aurora/font/bold.json). */
public final class Fonts {
    private Fonts() {
    }

    private static final Identifier BOLD = Identifier.of("aurora", "bold");

    private static Text styled(String s) {
        return Text.literal(s).setStyle(Style.EMPTY.withFont(BOLD));
    }

    public static void draw(DrawContext ctx, String s, int x, int y, int color, boolean shadow) {
        ctx.drawText(MinecraftClient.getInstance().textRenderer, styled(s), x, y, color, shadow);
    }

    public static int width(String s) {
        return MinecraftClient.getInstance().textRenderer.getWidth(styled(s));
    }
}
