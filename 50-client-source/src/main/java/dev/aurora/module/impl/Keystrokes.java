package dev.aurora.module.impl;

import dev.aurora.hud.HudLayout;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.BooleanSetting;
import dev.aurora.setting.NumberSetting;

import java.util.ArrayDeque;
import java.util.Deque;

/** WASD and mouse-button display with clicks per second. Drawn by HudRenderer. */
public class Keystrokes extends Module {
    private final NumberSetting posX = addHidden(new NumberSetting("posX", -1, -1, 5000, 1));
    private final NumberSetting posY = addHidden(new NumberSetting("posY", -1, -1, 5000, 1));
    private final BooleanSetting showCps = add(new BooleanSetting("Show CPS", true));
    private final NumberSetting alpha = add(new NumberSetting("Opacity", 200, 0, 255, 5));

    public final HudLayout.Element element = HudLayout.register("Keystrokes", posX, posY, 6, 80, false, false);

    private static final Deque<Long> LEFT = new ArrayDeque<>();
    private static final Deque<Long> RIGHT = new ArrayDeque<>();
    private boolean prevLeft;
    private boolean prevRight;

    public Keystrokes() {
        super("Keystrokes", "Shows WASD and mouse clicks with CPS", Category.RENDER);
        element.w = 58;
        element.h = 58;
    }

    @Override
    public void onTick() {
        if (mc.player == null) {
            return;
        }
        boolean left = mc.options.attackKey.isPressed();
        boolean right = mc.options.useKey.isPressed();
        long now = System.currentTimeMillis();
        if (left && !prevLeft) {
            LEFT.addLast(now);
        }
        if (right && !prevRight) {
            RIGHT.addLast(now);
        }
        prevLeft = left;
        prevRight = right;
    }

    public boolean showCps() {
        return showCps.on();
    }

    public int alpha() {
        return alpha.asInt();
    }

    /** Clicks in the last second. */
    public int cps(boolean leftButton) {
        Deque<Long> d = leftButton ? LEFT : RIGHT;
        long now = System.currentTimeMillis();
        while (!d.isEmpty() && now - d.peekFirst() > 1000) {
            d.pollFirst();
        }
        return d.size();
    }
}
