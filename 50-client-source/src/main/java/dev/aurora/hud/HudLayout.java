package dev.aurora.hud;

import dev.aurora.setting.NumberSetting;

import java.util.ArrayList;
import java.util.List;

/** The HUD pieces you can drag around while the menu is open. Positions are saved in the config. */
public final class HudLayout {
    private HudLayout() {
    }

    public static final List<Element> ELEMENTS = new ArrayList<>();

    public static Element register(String name, NumberSetting sx, NumberSetting sy,
                                   int defX, int defY, boolean anchorRight, boolean centerDefault) {
        Element e = new Element(name, sx, sy, defX, defY, anchorRight, centerDefault);
        ELEMENTS.add(e);
        return e;
    }

    public static final class Element {
        public final String name;
        public int w = 60;
        public int h = 14;
        public boolean shown;

        private final NumberSetting sx;
        private final NumberSetting sy;
        private final int defX;
        private final int defY;
        private final boolean anchorRight;
        private final boolean centerDefault;

        private Element(String name, NumberSetting sx, NumberSetting sy,
                        int defX, int defY, boolean anchorRight, boolean centerDefault) {
            this.name = name;
            this.sx = sx;
            this.sy = sy;
            this.defX = defX;
            this.defY = defY;
            this.anchorRight = anchorRight;
            this.centerDefault = centerDefault;
        }

        public int x(int screenW) {
            int v = sx.asInt();
            if (v < 0) {
                if (centerDefault) {
                    v = screenW / 2 + 18;
                } else if (anchorRight) {
                    v = screenW - w - 4;
                } else {
                    v = defX;
                }
            }
            return Math.max(0, Math.min(Math.max(0, screenW - w), v));
        }

        public int y(int screenH) {
            int v = sy.asInt();
            if (v < 0) {
                v = centerDefault ? screenH / 2 + 14 : defY;
            }
            return Math.max(0, Math.min(Math.max(0, screenH - h), v));
        }

        public void setPos(int x, int y) {
            sx.set((double) x);
            sy.set((double) y);
        }
    }
}
