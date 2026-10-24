package dev.aurora.gui;

import dev.aurora.AuroraClient;
import dev.aurora.Config;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.module.impl.InterfaceModule;
import dev.aurora.setting.BooleanSetting;
import dev.aurora.setting.ModeSetting;
import dev.aurora.setting.NumberSetting;
import dev.aurora.setting.Setting;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The click menu. Mouse and key input is polled straight from GLFW, and every
 * visual change (open, close, toggles, hover, expanding, dragging) is eased
 * with {@link Anim} so nothing snaps.
 *
 * Left click a module to toggle it. Right click a module to open its settings.
 * Drag a panel by its header. Right click a header to collapse the panel.
 */
public class ClickGuiScreen extends Screen {
    private static ClickGuiScreen instance;

    public static ClickGuiScreen get() {
        if (instance == null) {
            instance = new ClickGuiScreen();
        }
        return instance;
    }

    private static final int W = 118;
    private static final int HEADER = 17;
    private static final int ROW = 14;
    private static final int SET_BOOL = 13;
    private static final int SET_NUM = 19;

    private static final int ACCENT = 0xFF4C8DFF;
    private static final int MARK = 0xFFFFC83D;
    private static final int PANEL_BG = 0xD00D0F13;
    private static final int HEADER_BG = 0xE8101319;
    private static final int ROW_HOVER = 0x28FFFFFF;
    private static final int TEXT = 0xFFE8EAEE;
    private static final int DIM = 0xFF8C919B;
    private static final int TRACK_OFF = 0xFF3A3E47;
    private static final int ENABLED = 0xFF8FC4FF;

    private interface Drag {
        void start(double mx, double my);

        void move(double mx, double my);
    }

    private record Hit(double x, double y, double w, double h, Runnable left, Runnable right, Drag drag) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private static final class Panel {
        final Category category;
        double x;
        double y;
        boolean collapsed;
        final Anim collapse = new Anim(1);
        final Anim ax;
        final Anim ay;

        Panel(Category category, double x, double y) {
            this.category = category;
            this.x = x;
            this.y = y;
            this.ax = new Anim(x);
            this.ay = new Anim(y);
        }
    }

    private static final class Row {
        final Anim on;
        final Anim hover = new Anim(0);
        final Anim expand = new Anim(0);
        boolean expanded;

        Row(boolean enabled) {
            on = new Anim(enabled ? 1 : 0);
        }
    }

    private final List<Panel> panels = new ArrayList<>();
    private final Map<Module, Row> rows = new HashMap<>();
    private final Map<Setting<?>, Anim> settingAnims = new HashMap<>();
    private final Anim open = new Anim(0);
    private final Anim tip = new Anim(0);

    private String tipText = "";
    private String hoveredDesc;
    private boolean closing;
    private boolean wasLeft;
    private boolean wasRight;
    private boolean wasKey;
    private List<Hit> hits = new ArrayList<>();
    private List<Hit> nextHits = new ArrayList<>();
    private Hit dragging;
    private double mouseX;
    private double mouseY;
    private double clipTop;
    private double clipBottom;

    private ClickGuiScreen() {
        super(Text.literal("Aurora"));
        int i = 0;
        for (Category c : Category.values()) {
            panels.add(new Panel(c, 24 + i * (W + 10), 28));
            i++;
        }
    }

    // ---------------------------------------------------------------- screen hooks

    @Override
    protected void init() {
        closing = false;
        open.set(0);
        tip.set(0);
        hits = new ArrayList<>();
        dragging = null;
        // The open key is usually still held down, so it only closes the menu after being released and pressed again.
        wasLeft = mouseDown(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        wasRight = mouseDown(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
        wasKey = keyDown(closeKeyCode());
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        // The menu draws its own dim, so the vanilla blur is skipped.
    }

    @Override
    public void close() {
        closing = true;
    }

    private void finishClose() {
        Config.save(AuroraClient.modules);
        super.close();
    }

    // ---------------------------------------------------------------- input (polled)

    private static long window() {
        return MinecraftClient.getInstance().getWindow().getHandle();
    }

    private static boolean mouseDown(int button) {
        return GLFW.glfwGetMouseButton(window(), button) == GLFW.GLFW_PRESS;
    }

    private static boolean keyDown(int key) {
        return GLFW.glfwGetKey(window(), key) == GLFW.GLFW_PRESS;
    }

    private static int closeKeyCode() {
        return KeyBindingHelper.getBoundKeyOf(AuroraClient.openKey).getCode();
    }

    private void updateMouse() {
        MinecraftClient mc = MinecraftClient.getInstance();
        double scale = mc.getWindow().getScaleFactor();
        mouseX = mc.mouse.getX() / scale;
        mouseY = mc.mouse.getY() / scale;
    }

    private void processInput() {
        boolean left = mouseDown(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        boolean right = mouseDown(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
        boolean key = keyDown(closeKeyCode());

        if (key && !wasKey) {
            closing = true;
        }
        if (left && !wasLeft) {
            press(true);
        }
        if (right && !wasRight) {
            press(false);
        }
        if (left && dragging != null && dragging.drag() != null) {
            dragging.drag().move(mouseX, mouseY);
        }
        if (!left) {
            dragging = null;
        }

        wasLeft = left;
        wasRight = right;
        wasKey = key;
    }

    /** Hits were registered back to front during the last frame, so the topmost one is checked first. */
    private void press(boolean left) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!h.contains(mouseX, mouseY)) {
                continue;
            }
            if (left) {
                if (h.drag() != null) {
                    dragging = h;
                    h.drag().start(mouseX, mouseY);
                    h.drag().move(mouseX, mouseY);
                }
                if (h.left() != null) {
                    h.left().run();
                }
            } else if (h.right() != null) {
                h.right().run();
            }
            return;
        }
    }

    private void addHit(double x, double y, double w, double h, Runnable left, Runnable right, Drag drag) {
        double y1 = Math.max(y, clipTop);
        double y2 = Math.min(y + h, clipBottom);
        if (y2 <= y1) {
            return;
        }
        nextHits.add(new Hit(x, y1, w, y2 - y1, left, right, drag));
    }

    private void bringToFront(Panel p) {
        panels.remove(p);
        panels.add(p);
    }

    // ---------------------------------------------------------------- rendering

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        InterfaceModule ui = AuroraClient.modules.get(InterfaceModule.class);
        double speed = ui.speed();

        updateMouse();
        processInput();

        double o = open.update(closing ? 0 : 1, speed);
        if (closing && o < 0.02) {
            finishClose();
            return;
        }

        if (ui.dimBackground()) {
            ctx.fill(0, 0, width, height, Colors.alpha(0xFF000000, 0.42 * o));
        }

        nextHits = new ArrayList<>();
        hoveredDesc = null;

        for (Panel p : new ArrayList<>(panels)) {
            renderPanel(ctx, p, o, speed);
        }
        renderTooltip(ctx, o, speed);

        hits = nextHits;
    }

    private Row row(Module m) {
        return rows.computeIfAbsent(m, k -> new Row(k.isEnabled()));
    }

    private Anim anim(Setting<?> s, double initial) {
        return settingAnims.computeIfAbsent(s, k -> new Anim(initial));
    }

    private static double settingsHeight(Module m) {
        double h = 0;
        for (Setting<?> s : m.settings()) {
            h += (s instanceof NumberSetting) ? SET_NUM : SET_BOOL;
        }
        return h;
    }

    private void renderPanel(DrawContext ctx, Panel p, double o, double speed) {
        double slide = (1 - o) * -16;
        int x = (int) Math.round(p.ax.update(p.x, speed * 1.6));
        int y = (int) Math.round(p.ay.update(p.y, speed * 1.6) + slide);
        double c = p.collapse.update(p.collapsed ? 0 : 1, speed);

        List<Module> mods = AuroraClient.modules.in(p.category);
        double content = 0;
        for (Module m : mods) {
            content += ROW + settingsHeight(m) * row(m).expand.get();
        }
        int visible = (int) Math.round(content * c);

        // Body and header.
        ctx.fill(x, y, x + W, y + HEADER + visible, Colors.alpha(PANEL_BG, o));
        ctx.fill(x, y, x + W, y + HEADER, Colors.alpha(HEADER_BG, o));
        ctx.fill(x, y + HEADER - 1, x + W, y + HEADER, Colors.alpha(ACCENT, 0.9 * o));
        text(ctx, p.category.display, x + 7, y + 5, Colors.alpha(MARK, o));
        text(ctx, p.collapsed ? "+" : "-", x + W - 11, y + 5, Colors.alpha(DIM, o));

        // The panel background swallows clicks so panels underneath are not hit.
        clipTop = y;
        clipBottom = y + HEADER + visible;
        addHit(x, y, W, HEADER + visible, null, null, null);

        final double[] grab = new double[2];
        Drag move = new Drag() {
            @Override
            public void start(double mx, double my) {
                bringToFront(p);
                grab[0] = mx - p.x;
                grab[1] = my - p.y;
            }

            @Override
            public void move(double mx, double my) {
                p.x = Math.max(0, Math.min(width - W, mx - grab[0]));
                p.y = Math.max(0, Math.min(height - HEADER, my - grab[1]));
            }
        };
        addHit(x, y, W, HEADER, null, () -> p.collapsed = !p.collapsed, move);

        if (visible < 1) {
            return;
        }

        int top = y + HEADER;
        ctx.enableScissor(x, top, x + W, top + visible);
        clipTop = top;
        clipBottom = top + visible;

        double yy = top;
        for (Module m : mods) {
            Row r = row(m);
            double on = r.on.update(m.isEnabled() ? 1 : 0, speed);
            double ex = r.expand.update(r.expanded ? 1 : 0, speed);
            int ry = (int) Math.round(yy);

            boolean hov = dragging == null
                    && mouseX >= x && mouseX < x + W
                    && mouseY >= Math.max(ry, clipTop) && mouseY < Math.min(ry + ROW, clipBottom);
            double h = r.hover.update(hov ? 1 : 0, speed * 1.3);
            if (hov) {
                hoveredDesc = m.description;
            }

            ctx.fill(x, ry, x + W, ry + ROW, Colors.alpha(ROW_HOVER, h * o));
            ctx.fill(x, ry + 1, x + 2, ry + ROW - 1, Colors.alpha(MARK, on * o));
            text(ctx, m.name, x + 6 + (int) Math.round(on * 2), ry + 3,
                    Colors.alpha(Colors.lerp(DIM, Colors.lerp(TEXT, ENABLED, on), Math.max(on, h * 0.6)), o));

            if (m.togglable) {
                drawSwitch(ctx, x + W - 24, ry + 3, on, o);
            }
            if (!m.settings().isEmpty()) {
                text(ctx, ex > 0.5 ? "v" : ">", x + W - (m.togglable ? 34 : 14), ry + 3, Colors.alpha(DIM, o));
            }

            addHit(x, ry, W, ROW,
                    () -> {
                        if (m.togglable) {
                            m.toggle();
                        } else if (!m.settings().isEmpty()) {
                            r.expanded = !r.expanded;
                        }
                    },
                    () -> {
                        if (!m.settings().isEmpty()) {
                            r.expanded = !r.expanded;
                        }
                    },
                    null);

            yy += ROW;

            double sh = settingsHeight(m) * ex;
            int sy0 = (int) Math.round(yy);
            int sy1 = (int) Math.round(yy + sh);
            if (ex > 0.01 && sy1 > sy0) {
                ctx.fill(x, sy0, x + W, sy1, Colors.alpha(0x38000000, o));
                ctx.enableScissor(x, sy0, x + W, sy1);
                double savedBottom = clipBottom;
                clipBottom = Math.min(clipBottom, sy1);
                renderSettings(ctx, m, x, sy0, o * ex, speed);
                clipBottom = savedBottom;
                ctx.disableScissor();
            }
            yy += sh;
        }
        ctx.disableScissor();
    }

    private void renderSettings(DrawContext ctx, Module m, int x, int y0, double a, double speed) {
        int y = y0;
        for (Setting<?> s : m.settings()) {
            if (s instanceof BooleanSetting b) {
                double target = b.on() ? 1 : 0;
                double v = anim(b, target).update(target, speed);
                text(ctx, b.name, x + 10, y + 3, Colors.alpha(Colors.lerp(DIM, TEXT, v), a));
                drawSwitch(ctx, x + W - 24, y + 3, v, a);
                addHit(x, y, W, SET_BOOL, b::toggle, null, null);
                y += SET_BOOL;
            } else if (s instanceof ModeSetting md) {
                String val = md.get();
                text(ctx, md.name, x + 10, y + 3, Colors.alpha(DIM, a));
                text(ctx, val, x + W - 8 - textWidth(val), y + 3, Colors.alpha(ACCENT, a));
                addHit(x, y, W, SET_BOOL, md::cycle, null, null);
                y += SET_BOOL;
            } else if (s instanceof NumberSetting n) {
                double shown = anim(n, n.fraction()).update(n.fraction(), speed * 1.5);
                String val = n.step >= 1 ? Integer.toString(n.asInt()) : String.format(Locale.ROOT, "%.1f", n.asDouble());
                text(ctx, n.name, x + 10, y + 2, Colors.alpha(DIM, a));
                text(ctx, val, x + W - 8 - textWidth(val), y + 2, Colors.alpha(TEXT, a));

                final int tx = x + 10;
                final int tw = W - 20;
                int ty = y + 13;
                int fw = (int) Math.round(tw * shown);
                ctx.fill(tx, ty, tx + tw, ty + 3, Colors.alpha(TRACK_OFF, a));
                ctx.fill(tx, ty, tx + fw, ty + 3, Colors.alpha(ACCENT, a));
                ctx.fill(tx + fw - 1, ty - 1, tx + fw + 2, ty + 4, Colors.alpha(0xFFFFFFFF, a));

                addHit(x + 4, y, W - 8, SET_NUM, null, null, new Drag() {
                    @Override
                    public void start(double mx, double my) {
                        n.setFraction((mx - tx) / tw);
                    }

                    @Override
                    public void move(double mx, double my) {
                        n.setFraction((mx - tx) / tw);
                    }
                });
                y += SET_NUM;
            }
        }
    }

    private void drawSwitch(DrawContext ctx, int x, int y, double on, double o) {
        roundRect(ctx, x, y, 18, 8, Colors.alpha(Colors.lerp(TRACK_OFF, ACCENT, on), o));
        int knobX = x + 1 + (int) Math.round(on * 10);
        roundRect(ctx, knobX, y + 1, 6, 6, Colors.alpha(0xFFFFFFFF, o));
    }

    private void renderTooltip(DrawContext ctx, double o, double speed) {
        if (hoveredDesc != null) {
            tipText = hoveredDesc;
        }
        double t = tip.update(hoveredDesc != null ? 1 : 0, speed * 1.5);
        if (t < 0.03 || tipText.isEmpty()) {
            return;
        }
        int tw = textWidth(tipText);
        int tx = (int) Math.max(4, Math.min(mouseX + 12, width - tw - 12));
        int ty = (int) Math.min(mouseY + 14, height - 16);
        double a = t * o;
        ctx.fill(tx - 4, ty - 3, tx + tw + 4, ty + 11, Colors.alpha(0xF2101319, a));
        ctx.fill(tx - 4, ty - 3, tx - 3, ty + 11, Colors.alpha(MARK, a));
        text(ctx, tipText, tx, ty, Colors.alpha(TEXT, a));
    }

    // ---------------------------------------------------------------- drawing helpers

    private static void roundRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x + 1, y, x + w - 1, y + h, color);
        ctx.fill(x, y + 1, x + w, y + h - 1, color);
    }

    private static int textWidth(String s) {
        return MinecraftClient.getInstance().textRenderer.getWidth(s.toUpperCase(Locale.ROOT));
    }

    private static void text(DrawContext ctx, String s, int x, int y, int color) {
        // The text renderer treats a near-zero alpha as fully opaque, so skip it while fading.
        if ((color >>> 24) < 8) {
            return;
        }
        ctx.drawText(MinecraftClient.getInstance().textRenderer, s.toUpperCase(Locale.ROOT), x, y, color, false);
    }
}
