package dev.aurora.hud;

import dev.aurora.AuroraClient;
import dev.aurora.module.impl.HudModule;
import dev.aurora.module.impl.RadarModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HudRenderer {
    private HudRenderer() {
    }

    private static final int ACCENT = 0xFF4C8DFF;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static int tickCount;
    private static long windowStart = System.nanoTime();
    private static double ticksPerSecond = 20.0;

    private static double lastX;
    private static double lastZ;
    private static boolean hasLast;
    private static double speed;

    /** Called at the end of every client tick. */
    public static void tickStats(MinecraftClient mc) {
        long now = System.nanoTime();
        tickCount++;
        if (now - windowStart >= 1_000_000_000L) {
            ticksPerSecond = tickCount * 1_000_000_000.0 / (now - windowStart);
            tickCount = 0;
            windowStart = now;
        }

        if (mc.player != null) {
            double x = mc.player.getX();
            double z = mc.player.getZ();
            if (hasLast) {
                double instant = Math.hypot(x - lastX, z - lastZ) * 20.0;
                speed += (instant - speed) * 0.2;
            }
            lastX = x;
            lastZ = z;
            hasLast = true;
        } else {
            hasLast = false;
        }
    }

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden) {
            return;
        }

        int y = 4;
        HudModule hud = AuroraClient.modules.get(HudModule.class);
        if (hud.isEnabled()) {
            y = drawInfo(ctx, mc, hud, y);
        }

        RadarModule radar = AuroraClient.modules.get(RadarModule.class);
        if (radar.isEnabled()) {
            drawRadar(ctx, mc, radar, 6, y + 2);
        }
    }

    // ---------------------------------------------------------------- info pills

    private static int drawInfo(DrawContext ctx, MinecraftClient mc, HudModule hud, int y) {
        TextRenderer tr = mc.textRenderer;

        List<String> top = new ArrayList<>();
        if (hud.showFps()) {
            top.add(mc.getCurrentFps() + " FPS");
        }
        if (hud.showClock()) {
            top.add(LocalTime.now().format(CLOCK));
        }
        y = drawRow(ctx, tr, y, top);

        List<String> bottom = new ArrayList<>();
        if (hud.showPing()) {
            bottom.add(ping(mc) + " PING");
        }
        if (hud.showTicks()) {
            bottom.add(String.format(Locale.ROOT, "%.1f TICKS", ticksPerSecond));
        }
        if (hud.showSpeed()) {
            bottom.add(String.format(Locale.ROOT, "%.1f BPS", speed));
        }
        return drawRow(ctx, tr, y, bottom);
    }

    private static int ping(MinecraftClient mc) {
        if (mc.getNetworkHandler() == null || mc.player == null) {
            return 0;
        }
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry == null ? 0 : entry.getLatency();
    }

    private static int drawRow(DrawContext ctx, TextRenderer tr, int y, List<String> items) {
        if (items.isEmpty()) {
            return y;
        }
        int x = 4;
        for (String s : items) {
            int w = tr.getWidth(s) + 9;
            ctx.fill(x, y, x + w, y + 12, 0xB0101216);
            ctx.fill(x, y, x + 1, y + 12, ACCENT);
            ctx.drawText(tr, s, x + 5, y + 2, 0xFFE8EAEE, false);
            x += w + 3;
        }
        return y + 15;
    }

    // ---------------------------------------------------------------- radar

    private static void drawRadar(DrawContext ctx, MinecraftClient mc, RadarModule radar, int x, int y) {
        int r = radar.radius();
        int cx = x + r;
        int cy = y + r;
        double range = radar.range();

        // Disc with a ring, drawn one scanline at a time.
        for (int dy = -r; dy < r; dy++) {
            int half = (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
            int next = (int) Math.round(Math.sqrt(Math.max(0.0, (double) r * r - (double) (dy + 1) * (dy + 1))));
            int edge = Math.min(half * 2, Math.max(1, Math.abs(half - next)));
            ctx.fill(cx - half, cy + dy, cx + half, cy + dy + 1, 0x9A5C5B7E);
            ctx.fill(cx - half, cy + dy, cx - half + edge, cy + dy + 1, 0xFFB9B4E8);
            ctx.fill(cx + half - edge, cy + dy, cx + half, cy + dy + 1, 0xFFB9B4E8);
        }
        int inner = r / 2;
        for (int dy = -inner; dy < inner; dy++) {
            int half = (int) Math.round(Math.sqrt((double) inner * inner - (double) dy * dy));
            ctx.fill(cx - half, cy + dy, cx + half, cy + dy + 1, 0x1CFFFFFF);
        }
        ctx.fill(cx - r + 2, cy, cx + r - 2, cy + 1, 0x18FFFFFF);
        ctx.fill(cx, cy - r + 2, cx + 1, cy + r - 2, 0x18FFFFFF);

        double yaw = Math.toRadians(mc.player.getYaw());
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double scale = r / range;

        // Compass "N" that rotates with the view (north is -Z).
        double nf = -cos;
        double nr = sin;
        int nx = cx + (int) Math.round(nr * (r - 7));
        int ny = cy - (int) Math.round(nf * (r - 7));
        ctx.drawText(mc.textRenderer, "N", nx - 2, ny - 4, 0xFFFFFFFF, false);

        double px = mc.player.getX();
        double pz = mc.player.getZ();
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !(e instanceof LivingEntity)) {
                continue;
            }
            boolean isPlayer = e instanceof PlayerEntity;
            if (!isPlayer && !radar.showMobs()) {
                continue;
            }
            double dx = e.getX() - px;
            double dz = e.getZ() - pz;
            if (Math.hypot(dx, dz) > range) {
                continue;
            }
            double forward = -dx * sin + dz * cos;
            double right = -dx * cos - dz * sin;
            int sx = cx + (int) Math.round(right * scale);
            int sy = cy - (int) Math.round(forward * scale);
            int color = isPlayer ? 0xFFFF5555 : (e instanceof HostileEntity ? 0xFFFFAA33 : 0xFF7BE07B);
            ctx.fill(sx - 1, sy - 1, sx + 2, sy + 2, color);
        }

        // You, in the middle.
        ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);
    }
}
