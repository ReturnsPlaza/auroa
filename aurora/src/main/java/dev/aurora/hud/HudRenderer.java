package dev.aurora.hud;

import dev.aurora.AuroraClient;
import dev.aurora.module.impl.HudModule;
import dev.aurora.module.impl.RadarModule;
import dev.aurora.module.impl.TargetHud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HudRenderer {
    private HudRenderer() {
    }

    private static final int ACCENT = 0xFF4C8DFF;
    private static final int PILL_BG = 0xC0151A2B;
    private static final int PILL_BORDER = 0xFF2A3150;
    private static final int PILL_TEXT = 0xFFDDE3F5;
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

        EspRenderer.render(ctx, mc);

        int y = 4;
        HudModule hud = AuroraClient.modules.get(HudModule.class);
        if (hud.isEnabled()) {
            y = drawInfo(ctx, mc, hud, y);
        }

        RadarModule radar = AuroraClient.modules.get(RadarModule.class);
        if (radar.isEnabled()) {
            drawRadar(ctx, mc, radar, 6, y + 2);
        }

        TargetHud target = AuroraClient.modules.get(TargetHud.class);
        if (target.isEnabled()) {
            drawTarget(ctx, mc);
        }

        Alerts.render(ctx, mc);
    }

    // ---------------------------------------------------------------- info pills

    private static int drawInfo(DrawContext ctx, MinecraftClient mc, HudModule hud, int y) {
        TextRenderer tr = mc.textRenderer;

        List<String> top = new ArrayList<>();
        top.add("AURORA+");
        if (hud.showFps()) {
            top.add(mc.getCurrentFps() + " FPS");
        }
        if (hud.showClock()) {
            top.add(LocalTime.now().format(CLOCK));
        }
        y = drawRow(ctx, tr, y, top);

        List<String> bottom = new ArrayList<>();
        if (hud.showCoords()) {
            bottom.add(String.format(Locale.ROOT, "%d %d %d",
                    (int) Math.floor(mc.player.getX()),
                    (int) Math.floor(mc.player.getY()),
                    (int) Math.floor(mc.player.getZ())));
        }
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
            int w = tr.getWidth(s) + 10;
            ctx.fill(x, y, x + w, y + 13, PILL_BORDER);
            ctx.fill(x + 1, y + 1, x + w - 1, y + 12, PILL_BG);
            ctx.drawText(tr, s, x + 5, y + 3, s.equals("AURORA+") ? ACCENT : PILL_TEXT, false);
            x += w + 3;
        }
        return y + 16;
    }

    // ---------------------------------------------------------------- target hud

    private static void drawTarget(DrawContext ctx, MinecraftClient mc) {
        if (!(mc.crosshairTarget instanceof EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity le)) {
            return;
        }
        TextRenderer tr = mc.textRenderer;
        String name = le.getName().getString().toUpperCase(Locale.ROOT);
        float hp = le.getHealth();
        float max = Math.max(1f, le.getMaxHealth());
        String hpText = String.format(Locale.ROOT, "%.1f / %.1f HP", hp, max);

        int w = Math.max(110, Math.max(tr.getWidth(name), tr.getWidth(hpText)) + 12);
        int x = mc.getWindow().getScaledWidth() / 2 + 18;
        int y = mc.getWindow().getScaledHeight() / 2 + 14;

        ctx.fill(x, y, x + w, y + 30, PILL_BORDER);
        ctx.fill(x + 1, y + 1, x + w - 1, y + 29, PILL_BG);
        ctx.drawText(tr, name, x + 5, y + 4, PILL_TEXT, false);
        ctx.drawText(tr, hpText, x + 5, y + 14, 0xFF8C919B, false);

        int barW = w - 10;
        double frac = Math.max(0, Math.min(1, hp / max));
        int color = frac > 0.5 ? 0xFF5CE07B : (frac > 0.25 ? 0xFFFFC83D : 0xFFFF4A4A);
        ctx.fill(x + 5, y + 24, x + 5 + barW, y + 27, 0xFF2A2F3F);
        ctx.fill(x + 5, y + 24, x + 5 + (int) Math.round(barW * frac), y + 27, color);
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
            ctx.fill(cx - half, cy + dy, cx + half, cy + dy + 1, 0x886C69D6);
            ctx.fill(cx - half, cy + dy, cx - half + edge, cy + dy + 1, 0xFFA9A6F0);
            ctx.fill(cx + half - edge, cy + dy, cx + half, cy + dy + 1, 0xFFA9A6F0);
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
            if (!e.isAlive() || e instanceof net.minecraft.entity.decoration.ArmorStandEntity) {
                continue;
            }
            boolean isPlayer = e instanceof PlayerEntity;
            boolean isHostile = e instanceof net.minecraft.entity.mob.Monster;
            if (isPlayer) {
                if (!radar.showPlayers() || ((PlayerEntity) e).isSpectator()) {
                    continue;
                }
            } else if (isHostile) {
                if (!radar.showHostile()) {
                    continue;
                }
            } else if (!radar.showPassive()) {
                continue;
            }
            if (Math.abs(e.getY() - mc.player.getY()) > radar.height()) {
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
            int color = isPlayer ? 0xFFFF5555 : (isHostile ? 0xFFFFAA33 : 0xFF7BE07B);
            ctx.fill(sx - 1, sy - 1, sx + 2, sy + 2, color);
        }

        // You, in the middle.
        ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);
    }
}
