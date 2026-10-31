Warning: truncated output (original token count: 6644)
Total output lines: 622

package dev.aurora.hud;

import dev.aurora.gui.Fonts;

import dev.aurora.AuroraClient;
import dev.aurora.gui.Colors;
import dev.aurora.gui.Palette;
import dev.aurora.gui.Anim;
import dev.aurora.gui.Shapes;
import dev.aurora.module.Module;
import dev.aurora.module.impl.CustomHotbar;
import dev.aurora.module.impl.CustomSky;
import dev.aurora.module.impl.CrosshairModule;
import dev.aurora.module.impl.Friends;
import dev.aurora.module.impl.EntityEsp;
import dev.aurora.module.impl.Keystrokes;
import dev.aurora.module.impl.Nametags;
import dev.aurora.module.impl.HudModule;
import dev.aurora.module.impl.InterfaceModule;
import dev.aurora.module.impl.RadarModule;
import dev.aurora.module.impl.TargetHud;
import dev.aurora.module.impl.WeatherModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.ChunkPos;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class HudRenderer {
    private HudRenderer() {
    }

    private static final int PILL_BORDER_DEFAULT = 0xFF2B6F80;
    private static final int PILL_BG_DEFAULT = 0xFF0B1419;
    private static final int PILL_TEXT_DEFAULT = 0xFFD8F4FF;
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

        CustomSky customSky = AuroraClient.modules.get(CustomSky.class);
        WeatherModule weather = AuroraClient.modules.get(WeatherModule.class);
        boolean openSky = mc.world.isSkyVisible(mc.player.getBlockPos()) && mc.player.getPitch() < 45.0f;
        boolean customRain = mc.world.isRaining() && (!weather.isEnabled() || !weather.clearSky());
        AtmosphereRenderer.render(ctx, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(),
                customSky.isEnabled() && openSky, customRain && openSky);

        EspRenderer.render(ctx, mc);

        InterfaceModule ui = AuroraClient.modules.get(InterfaceModule.class);
        boolean neon = "Neon Pink".equals(ui.theme());
        int accent = neon ? 0xFFFF2E88 : Palette.of(ui.accent());


        HudModule hud = AuroraClient.modules.get(HudModule.class);
        hud.infoElement.shown = hud.isEnabled();
        hud.listElement.shown = hud.isEnabled() && hud.showModuleList();
        if (hud.isEnabled()) {
            drawInfo(ctx, mc, hud, accent, ui.theme());
            if (hud.showModuleList()) {
                drawModuleList(ctx, mc, hud, accent, ui.theme());
            }
        }

        RadarModule radar = AuroraClient.modules.get(RadarModule.class);
        radar.element.shown = radar.isEnabled();
        if (radar.isEnabled()) {
            drawRadar(ctx, mc, radar, accent, ui.theme());
        }

        TargetHud target = AuroraClient.modules.get(TargetHud.class);
        target.element.shown = target.isEnabled();
        if (target.isEnabled()) {
            drawTarget(ctx, mc, target, hud.opacity(), accent, ui.theme());
        }

        Nametags nametags = AuroraClient.modules.get(Nametags.class);
        if (nametags.isEnabled()) {
            drawNametags(ctx, mc, nametags);
        }

        EntityEsp entityEsp = AuroraClient.modules.get(EntityEsp.class);
        if (entityEsp.isEnabled() && entityEsp.tracers()) {
            drawEntityTracers(ctx, mc, entityEsp.glowing());
        }


        Keystrokes keys = AuroraClient.modules.get(Keystrokes.class);
        keys.element.shown = keys.isEnabled();
        if (keys.isEnabled()) {
            drawKeystrokes(ctx, mc, keys, accent, ui.theme());
        }

        CrosshairModule crosshair = AuroraClient.modules.get(CrosshairModule.class);
        if (crosshair.isEnabled()) {
            drawCrosshair(ctx, mc, crosshair, neon ? 0xFFFF2E88 : Palette.of(crosshair.colorName()));
        }

        CustomHotbar hotbar = AuroraClient.modules.get(CustomHotbar.class);
        if (hotbar.isEnabled()) {
            drawHotbar(ctx, mc, hotbar, ui.theme());
        }

        if (ui.showChunkGrid()) {
            drawChunkGrid(ctx, mc, ui.theme());
        }

        Alerts.render(ctx, mc);
    }

    // ---------------------------------------------------------------- theme

    private static int[] getThemeColors(String theme) {
        if ("Neon Pink".equals(theme)) {
            return new int[]{0xFFFF2E88, 0xFF1A0B1F, 0xFFFFB3DA}; // border, bg, text
        }
        return new int[]{PILL_BORDER_DEFAULT, PILL_BG_DEFAULT, PILL_TEXT_DEFAULT};
    }

    // ---------------------------------------------------------------- pills

    private static void pill(DrawContext ctx, TextRenderer tr, int x, int y, int w, String s,
                             int textColor, int opacity, String theme) {
        int[] colors = getThemeColors(theme);
        Shapes.roundFill(ctx, x, y, w, 13, 6, (opacity << 24) | (colors[0] & 0xFFFFFF));
        Shapes.roundFill(ctx, x + 1, y + 1, w - 2, 11, 5, (opacity << 24) | (colors[1] & 0xFFFFFF));
        Fonts.draw(ctx, s, x + 5, y + 3, textColor, false);
    }

    private static int rowWidth(TextRenderer tr, List<String> items) {
        int w = 0;
        for (String s : items) {
            w += Fonts.width(s) + 10 + 3;
        }
        return Math.max(0, w - 3);
    }

    private static void drawInfo(DrawContext ctx, MinecraftClient mc, HudModule hud, int accent, String theme) {
        TextRenderer tr = mc.textRenderer;

        String nameTag = "67 client";
        List<String> top = new ArrayList<>();
        if (hud.showBrand()) {
            top.add(nameTag);
        }
        if (hud.showFps()) {
            top.add(mc.getCurrentFps() + " FPS");
        }
        if (hud.showCoords()) {
            top.add(String.format(Locale.ROOT, "%d %d %d",
                    (int) Math.floor(mc.player.getX()),
                    (int) Math.floor(mc.player.getY()),
                    (int) Math.floor(mc.player.getZ())));
        }

        List<String> bottom = new ArrayList<>();

        int rows = (top.isEmpty() ? 0 : 1) + (bottom.isEmpty() ? 0 : 1);
        HudLayout.Element el = hud.infoElement;
        el.w = Math.max(40, Math.max(rowWidth(tr, top), rowWidth(tr, bottom)));
        el.h = Math.max(13, rows * 16 - 3);

        int x = el.x(mc.getWindow().getScaledWidth());
        int y = el.y(mc.getWindow().getScaledHeight());
        int opacity = hud.opacity();
        int textColor = "Neon Pink".equals(theme) ? 0xFFFFB3DA : 0xFFCFF6FF;
        int nameColor = "Neon Pink".equals(theme) ? 0xFFFF2E88 : 0xFF5FD4F0;
        for (List<String> row : List.of(top, bottom)) {
            if (row.isEmpty()) {
                continue;
            }
            int px = x;
            for (String s : row) {
                int w = Fonts.width(s) + 10;
                pill(ctx, tr, px, y, w, s, s.equals(nameTag) ? nameColor : textColor, opacity, theme);
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

    private static void drawModuleList(DrawContext ctx, MinecraftClient mc, HudModule hud, int accent, String theme) {
        int[] colors = getThemeColors(theme);
        TextRenderer tr = mc.textRenderer;
        List<String> names = new ArrayList<>();
        for (Module m : AuroraClient.modules.all()) {
            if (m.isEnabled() && m.togglable && m != hud) {
                names.add(m.name);
            }
        }
        names.sort(Comparator.comparingInt((String s) -> Fonts.width(s)).reversed());

        HudLayout.Element el = hud.listElement;
        int w = 40;
        for (String s : names) {
            w = Math.max(w, Fonts.width(s) + 12);
        }
        el.w = w;
        el.h = Math.max(13, names.size() * 15 - 2);

        int x = el.x(mc.getWindow().getScaledWidth());
        int y = el.y(mc.getWindow().getScaledHeight());
        int opacity = hud.opacity();
        for (String s : names) {
            int pw = Fonts.width(s) + 12;
            int px = x + el.w - pw;
           …1644 tokens truncated… < -12 || sy > p.height) {
                continue;
            }
            Shapes.roundFill(ctx, sx - tw / 2 - 4, sy - 4, tw + 8, 12, 5, 0xB0100A14);
            Fonts.draw(ctx, name, sx - tw / 2, sy - 2, isPlayer ? 0xFFE0B3FF : 0xFFFFFFFF, true);
        }
    }


    // ---------------------------------------------------------------- keystrokes

    private static void drawKey(DrawContext ctx, TextRenderer tr, int x, int y, int w, String label,
                                boolean pressed, int accent, int[] colors, int alpha) {
        Shapes.roundFill(ctx, x, y, w, 18, 6, (alpha << 24) | (colors[0] & 0xFFFFFF));
        Shapes.roundFill(ctx, x + 1, y + 1, w - 2, 16, 5, pressed
                ? ((alpha << 24) | (accent & 0xFFFFFF))
                : ((alpha << 24) | (colors[1] & 0xFFFFFF)));
        int tc = pressed ? 0xFFFFFFFF : colors[2];
        Fonts.draw(ctx, label, x + (w - Fonts.width(label)) / 2, y + 5, tc, false);
    }

    private static void drawKeystrokes(DrawContext ctx, MinecraftClient mc, Keystrokes ks, int accent, String theme) {
        TextRenderer tr = mc.textRenderer;
        boolean cps = ks.showCps();
        HudLayout.Element el = ks.element;
        el.w = 58;
        el.h = cps ? 74 : 58;
        int x = el.x(mc.getWindow().getScaledWidth());
        int y = el.y(mc.getWindow().getScaledHeight());
        int[] colors = getThemeColors(theme);
        int alpha = ks.alpha();

        drawKey(ctx, tr, x + 20, y, 18, "W", mc.options.forwardKey.isPressed(), accent, colors, alpha);
        drawKey(ctx, tr, x, y + 20, 18, "A", mc.options.leftKey.isPressed(), accent, colors, alpha);
        drawKey(ctx, tr, x + 20, y + 20, 18, "S", mc.options.backKey.isPressed(), accent, colors, alpha);
        drawKey(ctx, tr, x + 40, y + 20, 18, "D", mc.options.rightKey.isPressed(), accent, colors, alpha);
        drawKey(ctx, tr, x, y + 40, 28, "LMB", mc.options.attackKey.isPressed(), accent, colors, alpha);
        drawKey(ctx, tr, x + 30, y + 40, 28, "RMB", mc.options.useKey.isPressed(), accent, colors, alpha);
        if (cps) {
            String s = ks.cps(true) + " CPS   " + ks.cps(false) + " CPS";
            Fonts.draw(ctx, s, x + (58 - Fonts.width(s)) / 2, y + 62, colors[2], false);
        }
    }

    // ---------------------------------------------------------------- crosshair

    private static void drawCrosshair(DrawContext ctx, MinecraftClient mc, CrosshairModule ch, int color) {
        int cx = mc.getWindow().getScaledWidth() / 2;
        int cy = mc.getWindow().getScaledHeight() / 2;
        int size = ch.size();
        int gap = ch.gap();
        int t = ch.thickness();
        int h = t / 2;
        ctx.fill(cx - gap - size, cy - h, cx - gap, cy - h + t, color);
        ctx.fill(cx + gap, cy - h, cx + gap + size, cy - h + t, color);
        ctx.fill(cx - h, cy - gap - size, cx - h + t, cy - gap, color);
        ctx.fill(cx - h, cy + gap, cx - h + t, cy + gap + size, color);
        if (ch.dot()) {
            ctx.fill(cx - h, cy - h, cx - h + t, cy - h + t, color);
        }
    }

    // ---------------------------------------------------------------- chunk grid

    /** A 3x3 chunk map around the player with the current chunk highlighted. */
    private static void drawChunkGrid(DrawContext ctx, MinecraftClient mc, String theme) {
        TextRenderer tr = mc.textRenderer;
        ChunkPos cp = mc.player.getChunkPos();
        boolean neon = "Neon Pink".equals(theme);
        int[] colors = getThemeColors(theme);
        int cell = 18;
        int gx = 6;
        int gy = mc.getWindow().getScaledHeight() - 6 - 3 * cell - 14;
        int here = neon ? 0xFFFF2E88 : 0xFF5FD4F0;

        ctx.fill(gx - 2, gy - 2, gx + 3 * cell + 2, gy + 3 * cell + 2, colors[0]);
        ctx.fill(gx - 1, gy - 1, gx + 3 * cell + 1, gy + 3 * cell + 1, colors[1]);
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int x0 = gx + (dx + 1) * cell;
                int y0 = gy + (dz + 1) * cell;
                boolean center = dx == 0 && dz == 0;
                ctx.fill(x0 + 1, y0 + 1, x0 + cell - 1, y0 + cell - 1, center ? here : 0x30FFFFFF);
            }
        }
        // Exact spot inside your chunk: a dot in the center cell, scaled 0-15 across the chunk.
        int lx = Math.floorMod((int) Math.floor(mc.player.getX()), 16);
        int lz = Math.floorMod((int) Math.floor(mc.player.getZ()), 16);
        int inner = cell - 2;
        int dotX = gx + cell + 1 + (int) Math.round((lx / 15.0) * (inner - 3));
        int dotY = gy + cell + 1 + (int) Math.round((lz / 15.0) * (inner - 3));
        Shapes.roundFill(ctx, dotX - 1, dotY - 1, 5, 5, 2, 0xFFFFFFFF);
        Shapes.roundFill(ctx, dotX, dotY, 3, 3, 1, neon ? 0xFFFF2E88 : 0xFF5FD4F0);
        String label = "CHUNK " + cp.x + ", " + cp.z + "  LOCAL " + lx + ", " + lz;
        Fonts.draw(ctx, label, gx, gy - 12, colors[2], true);
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

    private static void drawRadar(DrawContext ctx, MinecraftClient mc, RadarModule radar, int accent, String theme) {
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

        int rgb = ("Neon Pink".equals(theme) ? 0xFF2E88 : Palette.of(radar.colorName())) & 0xFFFFFF;
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
                Fonts.draw(ctx, letters[i], lx - 2, ly - 4, i == 0 ? accent : 0xFFE8EAEE, false);
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
            Fonts.draw(ctx, s, cx - Fonts.width(s) / 2, cy + r + 4, 0xFFFFFFFF, true);
        }
    }
}

