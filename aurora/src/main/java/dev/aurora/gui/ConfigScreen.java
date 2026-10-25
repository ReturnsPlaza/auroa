package dev.aurora.gui;

import dev.aurora.AuroraClient;
import dev.aurora.Config;
import dev.aurora.Keys;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Saved configs. Left click the top row to save the current settings as a new config.
 * Left click a name to load it. Right click a name to delete it.
 * To use someone else's config, drop their .properties file into config/aurora/configs.
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
    private final boolean[] prev = new boolean[400];
    private List<String> names = List.of();
    private int page;
    private boolean wasLeft;
    private boolean wasRight;
    private double mouseX;
    private double mouseY;
    private String status = "Click a config to load it";

    public ConfigScreen(Screen parent) {
        super(Text.literal("Configs"));
        this.parent = parent;
        refresh();
    }

    private void refresh() {
        names = Config.names();
        page = Math.max(0, Math.min(pages() - 1, page));
    }

    /** Entry 0 is the save row, the rest are config names. */
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
        for (int k = 256; k <= 256; k++) {
            prev[k] = Keys.down(k);
        }
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
                    String n = Config.nextName();
                    Config.saveNamed(AuroraClient.modules, n);
                    status = "Saved " + n;
                    refresh();
                }
                return;
            }
            String name = names.get(idx - 1);
            if (rightButton) {
                Config.delete(name);
                status = "Deleted " + name;
                refresh();
            } else if (Config.loadNamed(AuroraClient.modules, name)) {
                status = "Loaded " + name;
            } else {
                status = "Could not load " + name;
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
        if (Keys.down(GLFW.GLFW_KEY_ESCAPE)) {
            if (!prev[GLFW.GLFW_KEY_ESCAPE]) {
                close();
            }
        }
        prev[GLFW.GLFW_KEY_ESCAPE] = Keys.down(GLFW.GLFW_KEY_ESCAPE);

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
        ctx.fill(x, y, x + W, y + h, PANEL);
        ctx.fill(x, y, x + W, y + 1, PURPLE);
        ctx.fill(x, y + h - 1, x + W, y + h, PURPLE);
        ctx.fill(x, y, x + 1, y + h, PURPLE);
        ctx.fill(x + W - 1, y, x + W, y + h, PURPLE);

        var tr = MinecraftClient.getInstance().textRenderer;
        ctx.drawText(tr, "Configs", x + 8, y + 6, PINK, false);
        String count = names.size() + " saved";
        ctx.drawText(tr, count, x + W - 8 - tr.getWidth(count), y + 6, DIM, false);

        int start = page * ROWS;
        for (int i = 0; i < ROWS; i++) {
            int idx = start + i;
            if (idx >= entries()) {
                break;
            }
            int ry = listTop() + i * ROW_H;
            boolean hov = mouseX >= x && mouseX < x + W && mouseY >= ry && mouseY < ry + ROW_H;
            if (hov) {
                ctx.fill(x + 1, ry, x + W - 1, ry + ROW_H, 0x28FFFFFF);
            }
            if (idx == 0) {
                ctx.drawText(tr, "+ Save current as " + Config.nextName(), x + 10, ry + 2, PINK, false);
            } else {
                ctx.drawText(tr, names.get(idx - 1), x + 10, ry + 2, TEXT, false);
            }
        }

        int cy = listBottom() + 4;
        ctx.drawText(tr, "<", x + 12, cy + 3, PINK, false);
        String pageText = (page + 1) + " / " + pages();
        ctx.drawText(tr, pageText, x + (W - tr.getWidth(pageText)) / 2, cy + 3, DIM, false);
        ctx.drawText(tr, ">", x + W - 16, cy + 3, PINK, false);

        ctx.drawText(tr, status, x + 8, cy + 20, TEXT, false);
        ctx.drawText(tr, "Right click deletes. Esc goes back.", x + 8, cy + 32, DIM, false);
        ctx.drawText(tr, "To share, send a .properties file from", x + 8, cy + 42, DIM, false);
        ctx.drawText(tr, "config/aurora/configs.", x + 8, cy + 52, DIM, false);
    }
}
