package dev.aurora.gui;

import dev.aurora.AuroraClient;
import dev.aurora.Config;
import dev.aurora.Keys;
import dev.aurora.module.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Saved configs. With no module, it manages whole-client configs.
 * With a module, it manages that module's presets only.
 * Left click the top row to save the current settings as a new entry.
 * Left click a name to load it. Right click a name to delete it.
 */
public class ConfigScreen extends Screen {
    private static final int W = 240;
    private static final int ROWS = 14;
    private static final int ROW_H = 13;
    private static final int PINK = 0xFFFF2E88;
    private static final int PURPLE = 0xFFA06BFF;
    private static final int PANEL = 0xE8100A14;
    private static final int TEXT = 0xFFE8EAEE;
    private static final int DIM = 0xFF8C919B;

    private final Screen parent;
    private final Module target;
    private final boolean[] prev = new boolean[400];
    private List<String> names = List.of();
    private int page;
    private boolean wasLeft;
    private boolean wasRight;
    private double mouseX;
    private double mouseY;
    private String status = "Click a name to load it";

    public ConfigScreen(Screen parent) {
        this(parent, null);
    }

    public ConfigScreen(Screen parent, Module target) {
        super(Text.literal("Configs"));
        this.parent = parent;
        this.target = target;
        refresh();
    }

    private void refresh() {
        names = target == null ? Config.names() : Config.moduleNames(target);
        page = Math.max(0, Math.min(pages() - 1, page));
    }

    private String nextName() {
        return target == null ? Config.nextName() : Config.nextModuleName(target);
    }

    /** Entry 0 is the save row, the rest are names. */
    private int entries() {
        return names.size() + 1;
    }

    private int pages() {
        return Math.max(1, (entries() + ROWS - 1) / ROWS);
    }

    private static long window() {
        return MinecraftClient.getInstance().getWindow().getHandle();
    }

    private static boolean mouseDown(int button) {
        return GLFW.glfwGetMouseButton(window(), button) == GLFW.GLFW_PRESS;
    }

    @Override
    protected void init() {
        // A click still held from opening this screen should not count.
        wasLeft = mouseDown(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        wasRight = mouseDown(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
        prev[GLFW.GLFW_KEY_ESCAPE] = Keys.down(GLFW.GLFW_KEY_ESCAPE);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return 24;
    }

    private int listTop() {
        return top() + 30;
    }

    private int listBottom() {
        return listTop() + ROWS * ROW_H;
    }

    private void updateMouse() {
        MinecraftClient mc = MinecraftClient.getInstance();
        double scale = mc.getWindow().getScaleFactor();
        mouseX = mc.mouse.getX() / scale;
        mouseY = mc.mouse.getY() / scale;
    }

    private void click(boolean rightButton, double mx, double my) {
        int x = left();
        if (mx >= x && mx < x + W && my >= listTop() && my < listBottom()) {
            int idx = page * ROWS + (int) ((my - listTop()) / ROW_H);
            if (idx >= entries()) {
                return;
            }
            if (idx == 0) {
                if (!rightButton) {
                    String n = nextName();
                    if (target == null) {
                        Config.saveNamed(AuroraClient.modules, n);
                    } else {
                        Config.saveModule(target, n);
                    }
                    status = "Saved " + n;
                    refresh();
                }
                return;
            }
            String name = names.get(idx - 1);
            if (rightButton) {
                boolean ok = target == null ? Config.delete(name) : Config.deleteModule(target, name);
                status = ok ? "Deleted " + name : "Could not delete " + name;
                refresh();
            } else {
                boolean ok = target == null
                        ? Config.loadNamed(AuroraClient.modules, name)
                        : Config.loadModule(target, name);
                status = ok ? "Loaded " + name : "Could not load " + name;
            }
            return;
        }
        int cy = listBottom() + 4;
        if (!rightButton && my >= cy && my < cy + 14) {
            if (mx >= x + 8 && mx < x + 30) {
                page = Math.max(0, page - 1);
            } else if (mx >= x + W - 30 && mx < x + W - 8) {
                page = Math.min(pages() - 1, page + 1);
            }
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        boolean esc = Keys.down(GLFW.GLFW_KEY_ESCAPE);
        if (esc && !prev[GLFW.GLFW_KEY_ESCAPE]) {
            close();
        }
        prev[GLFW.GLFW_KEY_ESCAPE] = esc;

        updateMouse();
        boolean left = mouseDown(GLFW.GLFW_MOUSE_BUTTON_LEFT);
        boolean right = mouseDown(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
        if (left && !wasLeft) {
            click(false, mouseX, mouseY);
        }
        if (right && !wasRight) {
            click(true, mouseX, mouseY);
        }
        wasLeft = left;
        wasRight = right;

        ctx.fill(0, 0, width, height, 0x99000000);

        int x = left();
        int y = top();
        int h = listBottom() - y + 56;
        Shapes.roundFill(ctx, x - 1, y - 1, W + 2, h + 2, 9, PURPLE);
        Shapes.roundFill(ctx, x, y, W, h, 8, PANEL);

        String title = target == null ? "Configs" : target.name + " presets";
        Fonts.draw(ctx, title, x + 10, y + 6, PINK, false);
        String count = names.size() + " saved";
        Fonts.draw(ctx, count, x + W - 10 - Fonts.width(count), y + 6, DIM, false);

        int start = page * ROWS;
        for (int i = 0; i < ROWS; i++) {
            int idx = start + i;
            if (idx >= entries()) {
                break;
            }
            int ry = listTop() + i * ROW_H;
            boolean hov = mouseX >= x + 4 && mouseX < x + W - 4 && mouseY >= ry && mouseY < ry + ROW_H;
            if (hov) {
                Shapes.roundFill(ctx, x + 4, ry, W - 8, ROW_H - 1, 4, 0x28FFFFFF);
            }
            if (idx == 0) {
                Fonts.draw(ctx, "+ Save current as " + nextName(), x + 12, ry + 2, PINK, false);
            } else {
                Fonts.draw(ctx, names.get(idx - 1), x + 12, ry + 2, TEXT, false);
            }
        }

        int cy = listBottom() + 4;
        Fonts.draw(ctx, "<", x + 14, cy + 3, PINK, false);
        String pageText = (page + 1) + " / " + pages();
        Fonts.draw(ctx, pageText, x + (W - Fonts.width(pageText)) / 2, cy + 3, DIM, false);
        Fonts.draw(ctx, ">", x + W - 18, cy + 3, PINK, false);

        Fonts.draw(ctx, status, x + 10, cy + 20, TEXT, false);
        Fonts.draw(ctx, "Right click deletes. Esc goes back.", x + 10, cy + 32, DIM, false);
        if (target == null) {
            Fonts.draw(ctx, "To share, send a .properties file from", x + 10, cy + 42, DIM, false);
            Fonts.draw(ctx, "config/aurora/configs.", x + 10, cy + 52, DIM, false);
        } else {
            Fonts.draw(ctx, "Presets only change " + target.name + ".", x + 10, cy + 42, DIM, false);
        }
    }
}
