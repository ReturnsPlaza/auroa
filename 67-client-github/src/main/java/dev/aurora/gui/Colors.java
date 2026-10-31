package dev.aurora.gui;

public final class Colors {
    private Colors() {
    }

    /** Multiplies the alpha of an ARGB colour by mul (0..1). */
    public static int alpha(int argb, double mul) {
        mul = Math.max(0, Math.min(1, mul));
        int a = (int) Math.round(((argb >>> 24) & 0xFF) * mul);
        return (a << 24) | (argb & 0xFFFFFF);
    }

    /** Blends two ARGB colours. t = 0 gives a, t = 1 gives b. */
    public static int lerp(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int aa = (int) Math.round(((a >>> 24) & 0xFF) + (((b >>> 24) & 0xFF) - ((a >>> 24) & 0xFF)) * t);
        int ar = (int) Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int ag = (int) Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int ab = (int) Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return (aa << 24) | (ar << 16) | (ag << 8) | ab;
    }
}
