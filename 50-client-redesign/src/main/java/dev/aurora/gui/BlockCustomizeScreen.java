package dev.aurora.gui;

import dev.aurora.AuroraClient;
import dev.aurora.Keys;
import dev.aurora.module.impl.BlockEsp;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Every block in the game with its icon and name. Click a block to add or remove it from Block ESP's list.
 * Type to search by name (for example "acacia" or "oak_log"). Backspace deletes. Left and right arrows change page.
 * Escape goes back to the menu.
 */
public class BlockCustomizeScreen extends Screen {
    private static final int W = 250;
    private static final int ROWS = 12;
    private static final int ROW_H = 18;
    private static final int PINK = 0xFFFF2E88;
    private static final int PURPLE = 0xFF5FD4F0;
    private static final int LAVENDER = 0xFFE0B3FF;
    private static final int PANEL = 0xEC100A14;
    private static final int TEXT = 0xFFE8EAEE;
    private static final int DIM = 0xFF8C919B;

    private record Entry(Identifier id, Block block, String name, String search) {
    }

    private final Screen parent;
    private final List<Entry> all = new ArrayList<>();
    private final StringBuilder query = new StringBuilder();
    private List<Entry> filtered = new ArrayList<>();
    private final boolean[] prev = new boolean[400];
    private int page;
    private boolean wasLeft;
    private double mouseX;
    private double mouseY;

    public BlockCustomizeScreen(Screen parent) {
        super(Text.literal("Customize blocks"));
        this.parent = parent;
        for (Identifier id : Registries.BLOCK.getIds()) {
            Block block = Registries.BLOCK.get(id);
            String name = block.getName().getString();
            String search = (id.getPath() + " " + name).toLowerCase(Locale.ROOT);
            all.add(new Entry(id, block, name, search));
        }
        all.sort(Comparator.comparing(Entry::name));
        refilter();
    }

    private static BlockEsp esp() {
        return AuroraClient.modules.get(BlockEsp.class);
    }

    @Override
    protected void init() {
        // A click still held from opening this screen should not count.
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
        List<Entry> out = new ArrayList<>();
        for (Entry e : all) {
            if (q.isEmpty() || e.search().contains(q)) {
                out.add(e);
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
        return 20;
    }

    private int listTop() {
        return top() + 36;
    }

    private int listBottom() {
        return listTop() + ROWS * ROW_H;
    }

    private void click(double mx, double my) {
        int x = left();
        if (mx >= x && mx < x + W && my >= listTop() && my < listBottom()) {
            int idx = page * ROWS + (int) ((my - listTop()) / ROW_H);
            if (idx < filtered.size()) {
                Entry e = filtered.get(idx);
                BlockEsp b = esp();
                if (b.customBlocks().contains(e.id().toString())) {
                    b.removeBlock(e.id());
                } else {
                    b.addBlock(e.id());
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

        ctx.fill(0, 0, width, height, 0x80000000);

        int x = left();
        int y = top();
        int h = listBottom() - y + 40;
        // Purple rim, then the dark rounded panel inside it.
        Shapes.roundFill(ctx, x - 1, y - 1, W + 2, h + 2, 9, PURPLE);
        Shapes.roundFill(ctx, x, y, W, h, 8, PANEL);

        var tr = MinecraftClient.getInstance().textRenderer;
        Fonts.draw(ctx, "Customize blocks", x + 12, y + 8, PINK, false);
        String count = esp().customBlocks().size() + " selected";
        Fonts.draw(ctx, count, x + W - 12 - Fonts.width(count), y + 8, DIM, false);

        // Search box.
        Shapes.roundFill(ctx, x + 10, y + 21, W - 20, 13, 5, 0xFF1C1826);
        String shown = query.toString() + "_";
        Fonts.draw(ctx, query.length() == 0 ? "type to search..." : shown, x + 15, y + 24,
                query.length() == 0 ? DIM : TEXT, false);

        // Block rows: checkbox, icon, friendly name.
        List<String> selected = esp().customBlocks();
        int start = page * ROWS;
        for (int i = 0; i < ROWS; i++) {
            int idx = start + i;
            if (idx >= filtered.size()) {
                break;
            }
            Entry e = filtered.get(idx);
            int ry = listTop() + i * ROW_H;
            boolean hov = mouseX >= x + 4 && mouseX < x + W - 4 && mouseY >= ry && mouseY < ry + ROW_H - 1;
            if (hov) {
                Shapes.roundFill(ctx, x + 4, ry, W - 8, ROW_H - 1, 4, 0x28FFFFFF);
            }
            boolean sel = selected.contains(e.id().toString());
            Shapes.roundFill(ctx, x + 12, ry + 4, 10, 10, 3, sel ? PINK : 0xFF3A3E47);
            ItemStack stack = new ItemStack(e.block().asItem());
            if (!stack.isEmpty()) {
                ctx.drawItem(stack, x + 30, ry + 1);
            }
            Fonts.draw(ctx, e.name(), x + 52, ry + 5, sel ? LAVENDER : TEXT, false);
        }

        // Page controls.
        int cy = listBottom() + 4;
        Fonts.draw(ctx, "<", x + 14, cy + 3, PINK, false);
        String pageText = (page + 1) + " / " + pages();
        Fonts.draw(ctx, pageText, x + (W - Fonts.width(pageText)) / 2, cy + 3, DIM, false);
        Fonts.draw(ctx, ">", x + W - 18, cy + 3, PINK, false);

        // Hint.
        String hint = esp().customMode()
                ? "Esc: back to menu"
                : "Set Block ESP Target to Custom to use this list";
        Fonts.draw(ctx, hint, x + 10, cy + 18, DIM, false);
    }
}
