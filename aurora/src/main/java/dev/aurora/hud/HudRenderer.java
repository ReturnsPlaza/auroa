package dev.aurora.hud;

import dev.aurora.AuroraClient;
import dev.aurora.gui.Colors;
import dev.aurora.gui.Palette;
import dev.aurora.module.Module;
import dev.aurora.module.impl.Atmosphere;
import dev.aurora.module.impl.CustomHotbar;
import dev.aurora.module.impl.CrosshairModule;
import dev.aurora.module.impl.Equipment;
import dev.aurora.module.impl.Friends;
import dev.aurora.module.impl.Keystrokes;
import dev.aurora.module.impl.HudModule;
import dev.aurora.module.impl.InterfaceModule;
import dev.aurora.module.impl.RadarModule;
import dev.aurora.module.impl.TargetHud;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class HudRenderer {
    private HudRenderer() {
    }

    private static final int PILL_BORDER_DEFAULT = 0xFF2A3150;
    private static final int PILL_BG_DEFAULT = 0xFF151A2B;
    private static final int PILL_TEXT_DEFAULT = 0xFFDDE3F5;
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
        boolean neon = "Neon Pink".equals(ui.theme());
        int accent = neon ? 0xFFFF2E88 : Palette.of(ui.accent());

        Atmosphere atmosphere = AuroraClient.modules.get(Atmosphere.class);
        if (atmosphere.isEnabled()) {
            drawAtmosphere(ctx, mc, atmosphere, neon ? 0xFFFF2E88 : 0xFFA06BFF);
        }

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

        Equipment equipment = AuroraClient.modules.get(Equipment.class);
        equipment.element.shown = equipment.isEnabled();
        if (equipment.isEnabled()) {
            drawEquipment(ctx, mc, equipment, ui.theme());
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
        ctx.fill(x, y, x + w, y + 13, (opacity << 24) | (colors[0] & 0xFFFFFF));
        ctx.fill(x + 1, y + 1, x + w - 1, y + 12, (opacity << 24) | (colors[1] & 0xFFFFFF));
        ctx.drawText(tr, s, x + 5, y + 3, textColor, false);
    }

    private static int rowWidth(TextRenderer tr, List<String> items) {
        int w = 0;
        for (String s : items) {
            w += tr.getWidth(s) + 10 + 3;
        }
        return Math.max(0, w - 3);
    }

    private static void drawInfo(DrawContext ctx, MinecraftClient mc, HudModule hud, int accent, String theme) {
        TextRenderer tr = mc.textRenderer;

        List<String> top = new ArrayList<>();
        if (hud.showBrand()) {
            top.add("AURORA");
        }
        if (hud.showFps()) {
            top.add(mc.getCurrentFps() + " FPS");
        }
        if (hud.showClock()) {
            top.add(LocalTime.now().format(CLOCK));
        }

        List<String> bottom = new ArrayList<>();
        if (hud.showCoords()) {
            bottom.add(String.format(Locale.ROOT, "%d %d %d XYZ",
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
        int textColor = getThemeColors(theme)[2];
        for (List<String> row : List.of(top, bottom)) {
            if (row.isEmpty()) {
                continue;
            }
            int px = x;
            for (String s : row) {
                int w = tr.getWidth(s) + 10;
                pill(ctx, tr, px, y, w, s, s.equals("AURORA") ? accent : textColor, opacity, theme);
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
            ctx.fill(px, y, px + pw, y + 13, (opacity << 24) | (colors[0] & 0xFFFFFF));
            ctx.fill(px + 1, y + 1, px + pw - 1, y + 12, (opacity << 24) | (colors[1] & 0xFFFFFF));
            ctx.fill(px + pw - 3, y + 1, px + pw - 1, y + 12, accent);
            ctx.drawText(tr, s, px + 5, y + 3, colors[2], false);
            y += 15;
        }
    }

    // ---------------------------------------------------------------- target hud

    private static void drawTarget(DrawContext ctx, MinecraftClient mc, TargetHud t, int opacity, int accent, String theme) {
        int[] colors = getThemeColors(theme);
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

        ctx.fill(x, y, x + w, y + 30, colors[0]);
        ctx.fill(x + 1, y + 1, x + w - 1, y + 29, (opacity << 24) | (colors[1] & 0xFFFFFF));
        ctx.fill(x + 1, y + 1, x + 3, y + 29, accent);
        ctx.drawText(tr, name, x + 7, y + 4, colors[2], false);
        ctx.drawText(tr, hpText, x + 7, y + 14, 0xFF8C919B, false);

        int barW = w - 14;
        double frac = Math.max(0, Math.min(1, hp / max));
        int color = frac > 0.5 ? 0xFF5CE07B : (frac > 0.25 ? 0xFFFFC83D : 0xFFFF4A4A);
        ctx.fill(x + 7, y + 24, x + 7 + barW, y + 27, 0xFF2A2F3F);
        ctx.fill(x + 7, y + 24, x + 7 + (int) Math.round(barW * frac), y + 27, color);
    }

    // ---------------------------------------------------------------- hotbar

    private static void drawHotbar(DrawContext ctx, MinecraftClient mc, CustomHotbar hb, String theme) {
        if (mc.player == null) {
            return;
        }
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        boolean neon = "Neon Pink".equals(theme);
        int[] colors = getThemeColors(theme);
        int selectedColor = neon ? 0xFFFF2E88 : 0xFFC9A7FF; // light purple, pink in Neon Pink
        int alpha = hb.alpha();
        int selected = mc.player.getInventory().getSelectedSlot();

        int total = 9 * 22 + 8 * 2;
        int x0 = (sw - total) / 2;
        int y0 = sh - 22 - 4;
        for (int i = 0; i < 9; i++) {
            int x = x0 + i * 24;
            boolean sel = i == selected;
            ctx.fill(x, y0, x + 22, y0 + 22, (alpha << 24) | ((sel ? selectedColor : colors[0]) & 0xFFFFFF));
            ctx.fill(x + 1, y0 + 1, x + 21, y0 + 21, (alpha << 24) | (colors[1] & 0xFFFFFF));
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                ctx.drawItem(stack, x + 3, y0 + 3);
                ctx.drawStackOverlay(mc.textRenderer, stack, x + 3, y0 + 3);
            }
        }
    }

    // ---------------------------------------------------------------- atmosphere

    private static void drawAtmosphere(DrawContext ctx, MinecraftClient mc, Atmosphere atm, int color) {
        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();
        int rgb = color & 0xFFFFFF;

        // Sky glow: fades out from the top of the screen.
        int bands = 24;
        int bandH = Math.max(1, (int) (h * 0.6) / bands);
        int strength = atm.tint();
        for (int i = 0; i < bands; i++) {
            double t = 1.0 - (double) i / bands;
            int a = (int) (strength * t * t);
            if (a > 0) {
                int y = i * bandH;
                ctx.fill(0, y, w, y + bandH, (a << 24) | rgb);
            }
        }

        // Aurora band: two soft wavy lines that drift over time.
        if (atm.aurora()) {
            double phase = System.currentTimeMillis() / 900.0;
            int base = (int) (h * 0.22);
            for (int layer = 0; layer < 2; layer++) {
                int a = layer == 0 ? 70 : 40;
                for (int x = 0; x < w; x += 6) {
                    double y = base + layer * 14 + Math.sin(x / 90.0 + phase + layer) * 14;
                    ctx.fill(x, (int) y, x + 6, (int) y + 3, (a << 24) | 0xC9A7FF);
                }
            }
        }

        // Screen outline.
        if (atm.outline()) {
            int c = 0xFF000000 | rgb;
            ctx.fill(0, 0, w, 2, c);
            ctx.fill(0, h - 2, w, h, c);
            ctx.fill(0, 0, 2, h, c);
            ctx.fill(w - 2, 0, w, h, c);
        }
    }

    // ---------------------------------------------------------------- equipment

    private static void slot(DrawContext ctx, int x, int y, ItemStack stack, int[] colors) {
        ctx.fill(x, y, x + 22, y + 22, 0xFF000000 | (colors[0] & 0xFFFFFF));
        ctx.fill(x + 1, y + 1, x + 21, y + 21, 0xFF000000 | (colors[1] & 0xFFFFFF));
        if (!stack.isEmpty()) {
            ctx.drawItem(stack, x + 3, y + 3);
        }
    }

    private static void drawEquipment(DrawContext ctx, MinecraftClient mc, Equipment eq, String theme) {
        HudLayout.Element el = eq.element;
        int x = el.x(mc.getWindow().getScaledWidth());
        int y = el.y(mc.getWindow().getScaledHeight());
        int[] colors = getThemeColors(theme);

        List<ItemStack> armor = new ArrayList<>();
        for (ItemStack s : mc.player.getArmorItems()) {
            armor.add(s);
        }
        Collections.reverse(armor); // helmet first, boots last

        slot(ctx, x, y, mc.player.getOffHandStack(), colors);
        slot(ctx, x + 25, y, mc.player.getMainHandStack(), colors);
        for (int i = 0; i < 4; i++) {
            ItemStack s = i < armor.size() ? armor.get(i) : ItemStack.EMPTY;
            slot(ctx, x + 56 + i * 25, y, s, colors);
        }
    }

    // ---------------------------------------------------------------- keystrokes

    private static void drawKey(DrawContext ctx, TextRenderer tr, int x, int y, int w, String label,
                                boolean pressed, int accent, int[] colors, int alpha) {
        ctx.fill(x, y, x + w, y + 18, (alpha << 24) | (colors[0] & 0xFFFFFF));
        ctx.fill(x + 1, y + 1, x + w - 1, y + 17, pressed
                ? ((alpha << 24) | (accent & 0xFFFFFF))
                : ((alpha << 24) | (colors[1] & 0xFFFFFF)));
        int tc = pressed ? 0xFFFFFFFF : colors[2];
        ctx.drawText(tr, label, x + (w - tr.getWidth(label)) / 2, y + 5, tc, false);
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
            ctx.drawText(tr, s, x + (58 - tr.getWidth(s)) / 2, y + 62, colors[2], false);
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
        int cell = 14;
        int gx = 6;
        int gy = mc.getWindow().getScaledHeight() - 6 - 3 * cell - 14;
        int here = neon ? 0xFFFF2E88 : 0xFF4C8DFF;

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
        String label = "CHUNK " + cp.x + ", " + cp.z;
        ctx.drawText(tr, label, gx, gy - 12, colors[2], true);
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
