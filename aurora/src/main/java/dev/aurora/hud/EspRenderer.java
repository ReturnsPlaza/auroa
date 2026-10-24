package dev.aurora.hud;

import dev.aurora.AuroraClient;
import dev.aurora.module.impl.BlockEsp;
import dev.aurora.module.impl.StorageEsp;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/** Draws the Storage ESP and Block ESP boxes on the screen layer, so they show through walls. */
public final class EspRenderer {
    private EspRenderer() {
    }

    public static void render(DrawContext ctx, MinecraftClient mc) {
        StorageEsp storage = AuroraClient.modules.get(StorageEsp.class);
        BlockEsp blocks = AuroraClient.modules.get(BlockEsp.class);
        if (!storage.isEnabled() && !blocks.isEnabled()) {
            return;
        }
        Projector p = new Projector(mc);
        if (blocks.isEnabled()) {
            draw(ctx, p, blocks.targets());
        }
        if (storage.isEnabled()) {
            draw(ctx, p, storage.targets());
        }
    }

    private static void draw(DrawContext ctx, Projector p, List<EspTarget> targets) {
        double[] out = new double[2];
        int drawn = 0;
        for (EspTarget t : targets) {
            if (drawn >= 400) {
                return;
            }
            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double maxY = -Double.MAX_VALUE;
            boolean visible = true;
            for (int i = 0; i < 8; i++) {
                double x = t.pos().getX() + (i & 1);
                double y = t.pos().getY() + ((i >> 1) & 1);
                double z = t.pos().getZ() + ((i >> 2) & 1);
                if (!p.project(x, y, z, out)) {
                    visible = false;
                    break;
                }
                minX = Math.min(minX, out[0]);
                maxX = Math.max(maxX, out[0]);
                minY = Math.min(minY, out[1]);
                maxY = Math.max(maxY, out[1]);
            }
            if (!visible || maxX < 0 || maxY < 0 || minX > p.width || minY > p.height) {
                continue;
            }
            if (maxX - minX > p.width * 1.5 || maxY - minY > p.height * 1.5) {
                continue;
            }
            int x1 = (int) Math.floor(minX);
            int y1 = (int) Math.floor(minY);
            int x2 = Math.max(x1 + 2, (int) Math.ceil(maxX));
            int y2 = Math.max(y1 + 2, (int) Math.ceil(maxY));
            int rgb = t.color() & 0xFFFFFF;

            ctx.fill(x1, y1, x2, y2, 0x30000000 | rgb);
            ctx.fill(x1, y1, x2, y1 + 1, 0xFF000000 | rgb);
            ctx.fill(x1, y2 - 1, x2, y2, 0xFF000000 | rgb);
            ctx.fill(x1, y1, x1 + 1, y2, 0xFF000000 | rgb);
            ctx.fill(x2 - 1, y1, x2, y2, 0xFF000000 | rgb);
            drawn++;
        }
    }
}
