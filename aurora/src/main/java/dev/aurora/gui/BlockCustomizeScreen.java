package dev.aurora.gui;

import dev.aurora.AuroraClient;
import dev.aurora.Keys;
import dev.aurora.module.impl.BlockEsp;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Every block in the game, searchable. Click a block to add or remove it from Block ESP's custom list.
 * Type to search (letters, numbers, underscore). Backspace deletes. Left and right arrows change page.
 * Escape goes back to the menu.
 */
public class BlockCustomizeScreen extends Screen {
    private static final int W = 240;
    private static final int ROWS = 14;
    private static final int ROW_H = 13;
    private static final int PINK = 0xFFFF2E88;
    private static final int PURPLE = 0xFFA06BFF;
    private static final int PANEL = 0xE8100A14;
    private static final int TEXT = 0xFFE8EAEE;
    private static final int DIM = 0xFF8C919B;

    private final Screen parent;
    private final List<Identifier> all = new ArrayList<>();
    private final StringBuilder query = new StringBuilder();
    private List<Identifier> filtered = new ArrayList<>();
    private final boolean[] prev = new boolean[400];
    private int page;
    private boolean wasLeft;
    private double mouseX;
    private double mouseY;

    public BlockCustomizeScreen(Screen parent) {
        super(Text.literal("Customize blocks"));
        this.parent = parent;
        for (Identifier id : Registries.BLOCK.getIds()) {
            all.add(id);
        }
        all.sort(Comparator.comparing(Identifier::toString));
        refilter();
    }

    private static BlockEsp esp() {
        return AuroraClient.modules.get(BlockEsp.class);
    }

    @Override
    protected void init() {
        // Do not treat a click that is still held from opening this screen as a click on a block.
        wasLeft = GLFW.glfwGetMouseButton(window(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        for (int k = 45; k <= 90; k++) {
            prev[k] = Keys.down(k);
        }
        for (int k = 256; k <= 263; k++) {
            prev[k] = Keys.down(k);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // Escape is handled in pollKeys so it only steps back to the menu once.
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    // ---------------------------------------------------------------- data

    private static long window() {
        return MinecraftClient.getInstance().getWindow().getHandle();
    }

    private int pages() {
        return Math.max(1, (filtered.size() + ROWS - 1) / ROWS);
    }

    private void refilter() {
        String q = query.toString();
        List<Identifier> out = new ArrayList<>();
        for (Identifier id : all) {
            if (q.isEmpty() || id.getPath().contains(q)) {
                out.add(id);
            }
        }
        filtered = out;
    }

    // ---------------------------------------------------------------- input (polled)

    private boolean edge(int k) {
        boolean down = Keys.down(k);
        boolean fresh = down && !prev[k];
        prev[k] = down;
        return fresh;
    }

    private void pollKeys() {
        boolean changed = false;
        for (int k = 65; k <= 90; k++) {
            if (edge(k)) {
                query.append((char) ('a' + k - 65));
                changed = true;
            }
        }
        for (int k = 48; k <= 57; k++) {
            if (edge(k)) {
                query.append((char) k);
                changed = true;
            }
        }
        if (edge(45)) {
            query.append('_');
            changed = true;
        }
        if (edge(259) && query.length() > 0) {
            query.setLength(query.length() - 1);
            changed = true;
        }
        if (edge(262)) {
            page++;
        }
        if (edge(263)) {
            page--;
        }
        if (edge(256)) {
            close();
        }
        if (changed) {
            page = 0;
            refilter();
        }
        page = Math.max(0, Math.min(pages() - 1, page));
    }

    private void updateMouse() {
        MinecraftClient mc = MinecraftClient.getInstance();
        double scale = mc.getWindow().getScaleFactor();
        mouseX = mc.mouse.getX() / scale;
        mouseY = mc.mouse.getY() / scale;
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return 24;
    }

    private int listTop() {
        return top() + 34;
    }

    private int listBottom() {
        return listTop() + ROWS * ROW_H;
    }

    private void click(double mx, double my) {
        int x = left();
        if (mx >= x && mx < x + W && my >= listTop() && my < listBottom()) {
            int idx = page * ROWS + (int) ((my - listTop()) / ROW_H);
            if (idx < filtered.size()) {
                Identifier id = filtered.get(idx);
                BlockEsp e = esp();
                if (e.customBlocks().contains(id.toString())) {
                    e.removeBlock(id);
                } else {
                    e.addBlock(id);
                }
            }
            return;
        }
        int controlsY = listBottom() + 4;
        if (my >= controlsY && my < controlsY + 14) {
            if (mx >= x + 8 && mx < x + 30) {
                page = Math.max(0, page - 1);
            } else if (mx >= x + W - 30 && mx < x + W - 8) {
                page = Math.min(pages() - 1, page + 1);
            }
        }
    }

    // ---------------------------------------------------------------- rendering

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        pollKeys();
        updateMouse();
        boolean left = GLFW.glfwGetMouseButton(window(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (left && !wasLeft) {
            click(mouseX, mouseY);
        }
        wasLeft = left;

        ctx.fill(0, 0, width, height, 0x99000000);

        int x = left();
        int y = top();
        int h = listBottom() - y + 36;
        ctx.fill(x, y, x + W, y + h, PANEL);
        ctx.fill(x, y, x + W, y + 1, PURPLE);
        ctx.fill(x, y + h - 1, x + W, y + h, PURPLE);
        ctx.fill(x, y, x + 1, y + h, PURPLE);
        ctx.fill(x + W - 1, y, x + W, y + h, PURPLE);

        var tr = MinecraftClient.getInstance().textRenderer;
        ctx.drawText(tr, "Customize blocks", x + 8, y + 6, PINK, false);
        String count = esp().customBlocks().size() + " selected";
        ctx.drawText(tr, count, x + W - 8 - tr.getWidth(count), y + 6, DIM, false);

        // Search box.
        ctx.fill(x + 8, y + 18, x + W - 8, y + 30, 0xFF1A1D26);
        String shown = query.toString() + "_";
        ctx.drawText(tr, query.length() == 0 ? "type to search..." : shown, x + 12, y + 20,
                query.length() == 0 ? DIM : TEXT, false);

        // Block rows.
        List<String> selected = esp().customBlocks();
        int start = page * ROWS;
        for (int i = 0; i < ROWS; i++) {
            int idx = start + i;
            if (idx >= filtered.size()) {
                break;
            }
            Identifier id = filtered.get(idx);
            int ry = listTop() + i * ROW_H;
            boolean hov = mouseX >= x && mouseX < x + W && mouseY >= ry && mouseY < ry + ROW_H;
            if (hov) {
                ctx.fill(x + 1, ry, x + W - 1, ry + ROW_H, 0x28FFFFFF);
            }
            boolean sel = selected.contains(id.toString());
            ctx.fill(x + 10, ry + 2, x + 20, ry + 11, sel ? PINK : 0xFF3A3E47);
            ctx.drawText(tr, id.toString(), x + 26, ry + 2, sel ? 0xFFFFB3DA : TEXT, false);
        }

        // Page controls.
        int cy = listBottom() + 4;
        ctx.drawText(tr, "<", x + 12, cy + 3, PINK, false);
        String pageText = (page + 1) + " / " + pages();
        ctx.drawText(tr, pageText, x + (W - tr.getWidth(pageText)) / 2, cy + 3, DIM, false);
        ctx.drawText(tr, ">", x + W - 16, cy + 3, PINK, false);

        // Hint.
        String hint = esp().customMode()
                ? "Esc: back to menu"
                : "Set Block ESP Target to Custom to use this list";
        ctx.drawText(tr, hint, x + 8, cy + 18, DIM, false);
    }
}
