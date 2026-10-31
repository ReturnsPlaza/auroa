package dev.aurora;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Small helpers for reading and naming keyboard keys. */
public final class Keys {
    private Keys() {
    }

    /** Mouse buttons are stored as MOUSE_BASE + button index, so they never clash with keyboard codes. */
    public static final int MOUSE_BASE = 1000;

    private static final int[] VALID = build();

    private static void range(List<Integer> list, int from, int to) {
        for (int i = from; i <= to; i++) {
            list.add(i);
        }
    }

    private static int[] build() {
        List<Integer> l = new ArrayList<>();
        range(l, 32, 32);
        range(l, 39, 39);
        range(l, 44, 57);
        range(l, 59, 59);
        range(l, 61, 61);
        range(l, 65, 96);
        range(l, 256, 269);
        range(l, 280, 284);
        range(l, 290, 314);
        range(l, 320, 336);
        range(l, 340, 348);
        // Middle and side mouse buttons (buttons 2-7). Left and right stay reserved for the menu.
        range(l, MOUSE_BASE + 2, MOUSE_BASE + 7);
        int[] out = new int[l.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = l.get(i);
        }
        return out;
    }

    /** Every key code that can be bound. */
    public static int[] all() {
        return VALID;
    }

    public static boolean down(int key) {
        if (key < 0) {
            return false;
        }
        long window = MinecraftClient.getInstance().getWindow().getHandle();
        if (key >= MOUSE_BASE) {
            return GLFW.glfwGetMouseButton(window, key - MOUSE_BASE) == GLFW.GLFW_PRESS;
        }
        return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }

    public static String name(int key) {
        if (key < 0) {
            return "NONE";
        }
        if (key >= MOUSE_BASE) {
            return "MOUSE " + (key - MOUSE_BASE + 1);
        }
        switch (key) {
            case GLFW.GLFW_KEY_SPACE:
                return "SPACE";
            case GLFW.GLFW_KEY_TAB:
                return "TAB";
            case GLFW.GLFW_KEY_ENTER:
                return "ENTER";
            case GLFW.GLFW_KEY_BACKSPACE:
                return "BACKSPACE";
            case GLFW.GLFW_KEY_INSERT:
                return "INSERT";
            case GLFW.GLFW_KEY_DELETE:
                return "DELETE";
            case GLFW.GLFW_KEY_HOME:
                return "HOME";
            case GLFW.GLFW_KEY_END:
                return "END";
            case GLFW.GLFW_KEY_PAGE_UP:
                return "PAGE UP";
            case GLFW.GLFW_KEY_PAGE_DOWN:
                return "PAGE DOWN";
            case GLFW.GLFW_KEY_UP:
                return "UP";
            case GLFW.GLFW_KEY_DOWN:
                return "DOWN";
            case GLFW.GLFW_KEY_LEFT:
                return "LEFT";
            case GLFW.GLFW_KEY_RIGHT:
                return "RIGHT";
            case GLFW.GLFW_KEY_CAPS_LOCK:
                return "CAPS";
            case GLFW.GLFW_KEY_LEFT_SHIFT:
                return "L-SHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT:
                return "R-SHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL:
                return "L-CTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL:
                return "R-CTRL";
            case GLFW.GLFW_KEY_LEFT_ALT:
                return "L-ALT";
            case GLFW.GLFW_KEY_RIGHT_ALT:
                return "R-ALT";
            case GLFW.GLFW_KEY_LEFT_SUPER:
                return "L-WIN";
            case GLFW.GLFW_KEY_RIGHT_SUPER:
                return "R-WIN";
            default:
                break;
        }
        if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25) {
            return "F" + (key - GLFW.GLFW_KEY_F1 + 1);
        }
        if (key >= GLFW.GLFW_KEY_KP_0 && key <= GLFW.GLFW_KEY_KP_9) {
            return "NUM " + (key - GLFW.GLFW_KEY_KP_0);
        }
        String n = GLFW.glfwGetKeyName(key, 0);
        if (n != null && !n.isEmpty()) {
            return n.toUpperCase(Locale.ROOT);
        }
        return "KEY " + key;
    }
}
