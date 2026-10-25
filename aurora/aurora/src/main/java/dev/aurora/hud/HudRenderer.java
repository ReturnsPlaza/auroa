package dev.aurora.hud;

import dev.aurora.AuroraClient;
import dev.aurora.gui.Colors;
import dev.aurora.gui.Palette;
import dev.aurora.module.Module;
import dev.aurora.module.impl.Friends;
import dev.aurora.module.impl.HudModule;
import dev.aurora.module.impl.InterfaceModule;
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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class HudRenderer {
    private HudRenderer() {
    }

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

        InterfaceModule ui = AuroraClient.modules.get(InterfaceModule.class);
        int accent = Palette.of(ui.accent());

        HudModule hud = AuroraClient.modules.get(HudModule.class);
        hud.infoElement.shown = hud.isEnabled();
        hud.listElement.shown = hud.isEnabled() && hud.showModuleList();
        if (hud.isEnabled()) {
            drawInfo(ctx, mc, hud, accent);
            if (hud.showModuleList()) {
                drawModuleList(ctx, mc, hud, accent);
            }
        }

        RadarModule radar = AuroraClient.modules.get(RadarModule.class);
        radar.element.shown = radar.isEnabled();
        if (radar.isEnabled()) {
            drawRadar(ctx, mc, radar, accent);
        }

        TargetHud target = AuroraClient.modules.get(TargetHud.class);
        target.element.shown = target.isEnabled();
        if (target.isEnabled()) {
            drawTarget(ctx, mc, target, hud.opacity(), accent);
        }

        Alerts.render(ctx, mc);
    }

    // ---------------------------------------------------------------- pills

    private static void pill(DrawContext ctx, TextRenderer tr, int x, int y, int w, String s,
                             int textColor, int opacity) {
        ctx.fill(x, y, x + w, y + 13, (opacity << 24) | 0x2A3150);
        ctx.fill(x + 1, y + 1, x + w - 1, y + 12, (opacity << 24) | 0x151A2B);
        ctx.drawText(tr, s, x + 5, y + 3, textColor, false);
    }

    private static int rowWidth(TextRenderer tr, List<String> items) {
        int w = 0;
        for (String s : items) {
            w += tr.getWidth(s) + 10 + 3;
        }
        return Math.max(0, w - 3);
    }

    private static void drawInfo(DrawContext ctx, MinecraftClient mc, HudModule hud, int accent) {
        TextRenderer tr = mc.textRenderer;

        List<String> top = new ArrayList<>();
        if (hud.showBrand()) {
            top.add("AURORA+");
        }
        if (hud.showFps()) {
            top.add(mc.getCurrentFps() + " FPS");
        }
        if (hud.showClock()) {
            top.add(LocalTime.now().format(CLOCK));
        }

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

        int rows = (top.isEmpty() ? 0 : 1) + (bottom.isEmpty() ? 0 : 1);
        HudLayout.Element el = hud.infoElement;
        el.w = Math.max(40, Math.max(rowWidth(tr, top), rowWidth(tr, bottom)));
        el.h = Math.max(13, rows * 16 - 3);

        int x = el.x(mc.getWindow().getScaledWidth());
        int y = el.y(mc.getWindow().getScaledHeight());
        int opacity = hud.opacity();
        for (List<String> row : List.of(top, bottom)) {
            if (row.isEmpty()) {
                continue;
            }
            int px = x;
            for (String s : row) {
                int w = tr.getWidth(s) + 10;
                pill(ctx, tr, px, y, w, s, s.equals("AURORA+") ? accent : PILL_TEXT, opacity);
                px += w + 3;
            }
            y += 16;
        }
    }

    private static int ping(MinecraftClient mc) {
        if (mc.getNetworkHandler() == null || mc.player == null) {
            return 0;
        }
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry == null ? 0 : entry.getLatency();
    }

    private static void drawModuleList(DrawContext ctx, MinecraftClient mc, HudModule hud, int accent) {
        TextRenderer tr = mc.textRenderer;
        List<String> names = new ArrayList<>();
        for (Module m : AuroraClient.modules.all()) {
            if (m.isEnabled() && m.togglable && m != hud) {
                names.add(m.name.toUpperCase(Locale.ROOT));
            }
        }
        names.sort(Comparator.comparingInt((String s) -> tr.getWidth(s)).reversed());

        HudLayout.Element el = hud.listElement;
        int w = 40;
        for (String s : names) {
            w = Math.max(w, tr.getWidth(s) + 12);
        }
        el.w = w;
        el.h = Math.max(13, names.size() * 15 - 2);

        int x = el.x(mc.getWindow().getScaledWidth());
        int y = el.y(mc.getWindow().getScaledHeight());
        int opacity = hud.opacity();
        for (String s : names) {
            int pw = tr.getWidth(s) + 12;
            int px = x + el.w - pw;
            ctx.fill(px, y, px + pw, y + 13, (opacity << 24) | 0x2A3150);
            ctx.fill(px + 1, y + 1, px + pw - 1, y + 12, (opacity << 24) | 0x151A2B);
            ctx.fill(px + pw - 3, y + 1, px + pw - 1, y + 12, accent);
            ctx.drawText(tr, s, px + 5, y + 3, PILL_TEXT, false);
            y += 15;
        }
    }

    // ---------------------------------------------------------------- target hud

    private static void drawTarget(DrawContext ctx, MinecraftClient mc, TargetHud t, int opacity, int accent) {
        HudLayout.Element el = t.element;
        el.w = 110;
        el.h = 30;
        if (!(mc.crosshairTarget instanceof EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity le)) {
            return;
        }
        TextRenderer tr = mc.textRenderer;
        String name = le.getName().getString().toUpperCase(Locale.ROOT);
        float hp = le.getHealth();
        float max = Math.max(1f, le.getMaxHealth());
        String hpText = String.format(Locale.ROOT, "%.1f / %.1f HP", hp, max);

        int w = Math.max(110, Math.max(tr.getWidth(name), tr.getWidth(hpText)) + 12);
        el.w = w;
        int x = el.x(mc.getWindow().getScaledWidth());
        int y = el.y(mc.getWindow().getScaledHeight());

        ctx.fill(x, y, x + w, y + 30, PILL_BORDER);
        ctx.fill(x + 1, y + 1, x + w - 1, y + 29, (opacity << 24) | 0x151A2B);
        ctx.fill(x + 1, y + 1, x + 3, y + 29, accent);
        ctx.drawText(tr, name, x + 7, y + 4, PILL_TEXT, false);
        ctx.drawText(tr, hpText, x + 7, y + 14, 0xFF8C919B, false);

        int barW = w - 14;
        double frac = Math.max(0, Math.min(1, hp / max));
        int color = frac > 0.5 ? 0xFF5CE07B : (frac > 0.25 ? 0xFFFFC83D : 0xFFFF4A4A);
        ctx.fill(x + 7, y + 24, x + 7 + barW, y + 27, 0xFF2A2F3F);
        ctx.fill(x + 7, y + 24, x + 7 + (int) Math.round(barW * frac), y + 27, color);
    }

    // ---------------------------------------------------------------- radar

    private static void disc(DrawContext ctx, int cx, int cy, int r, int fill, int rim) {
        for (int dy = -r; dy < r; dy++) {
            int half = (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
            int next = (int) Math.round(Math.sqrt(Math.max(0.0, (double) r * r - (double) (dy + 1) * (dy + 1))));
            int edge = Math.min(half * 2, Math.max(1, Math.abs(half - next)));
            if ((fill >>> 24) != 0) {
                ctx.fill(cx - half, cy + dy, cx + half, cy + dy + 1, fill);
            }
            if ((rim >>> 24) != 0) {
                ctx.fill(cx - half, cy + dy, cx - half + edge, cy + dy + 1, rim);
                ctx.fill(cx + half - edge, cy + dy, cx + half, cy + dy + 1, rim);
            }
        }
    }

    private static void drawRadar(DrawContext ctx, MinecraftClient mc, RadarModule radar, int accent) {
        TextRenderer tr = mc.textRenderer;
        int r = radar.radius();
        HudLayout.Element el = radar.element;
        el.w = r * 2;
        el.h = r * 2 + (radar.showCoords() ? 14 : 0);

        int x = el.x(mc.getWindow().getScaledWidth());
        int y = el.y(mc.getWindow().getScaledHeight());
        int cx = x + r;
        int cy = y + r;
        double range = radar.range();

        int rgb = Palette.of(radar.colorName()) & 0xFFFFFF;
        int fill = (radar.opacity() << 24) | rgb;
        int rim = Colors.lerp(0xFF000000 | rgb, 0xFFFFFFFF, 0.45);

        disc(ctx, cx, cy, r, fill, rim);
        if (radar.showRings()) {
            disc(ctx, cx, cy, Math.max(2, r / 2), 0, 0x38FFFFFF);
            ctx.fill(cx - r + 2, cy, cx + r - 2, cy + 1, 0x20FFFFFF);
            ctx.fill(cx, cy - r + 2, cx + 1, cy + r - 2, 0x20FFFFFF);
        }

        boolean rotate = radar.rotate();
        double yaw = Math.toRadians(mc.player.getYaw());
        double sin = rotate ? Math.sin(yaw) : 0;
        double cos = rotate ? Math.cos(yaw) : 1;
        double scale = r / range;

        if (radar.showCompass()) {
            String[] letters = {"N", "E", "S", "W"};
            double[][] dirs = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
            for (int i = 0; i < 4; i++) {
                double wx = dirs[i][0];
                double wz = dirs[i][1];
                double fwd = -wx * sin + wz * cos;
                double right = -wx * cos - wz * sin;
                int lx = cx + (int) Math.round(right * (r - 7));
                int ly = cy - (int) Math.round(fwd * (r - 7));
                ctx.drawText(tr, letters[i], lx - 2, ly - 4, i == 0 ? accent : 0xFFE8EAEE, false);
            }
        }

        Friends friends = AuroraClient.modules.get(Friends.class);
        double px = mc.player.getX();
        double pz = mc.player.getZ();
        int dot = radar.dot();
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
            int color;
            if (isPlayer) {
                color = friends.isFriend(((PlayerEntity) e).getGameProfile().name()) ? 0xFF55FFFF : 0xFFFF5555;
            } else {
                color = isHostile ? 0xFFFFAA33 : 0xFF7BE07B;
            }
            int h = dot / 2;
            ctx.fill(sx - h, sy - h, sx - h + dot, sy - h + dot, color);
        }

        // You, in the middle.
        ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);

        if (radar.showCoords()) {
            String s = String.format(Locale.ROOT, "%d, %d, %d",
                    (int) Math.floor(mc.player.getX()),
                    (int) Math.floor(mc.player.getY()),
                    (int) Math.floor(mc.player.getZ()));
            ctx.drawText(tr, s, cx - tr.getWidth(s) / 2, cy + r + 4, 0xFFFFFFFF, true);
        }
    }
}
