package dev.aurora.gui;

/** A few named colours used for accents and ESP colours. */
public final class Palette {
    private Palette() {
    }

    public static final String[] NAMES = {"Blue", "Purple", "Pink", "Red", "Orange", "Gold", "Green", "Cyan", "White"};
    public static final String[] THEMES = {"Default", "Neon Pink"};

    public static int of(String name) {
        return switch (name) {
            case "Purple" -> 0xFFA06BFF;
            case "Pink" -> 0xFFFF6BC8;
            case "Red" -> 0xFFFF4A4A;
            case "Orange" -> 0xFFFF9A3D;
            case "Gold" -> 0xFFFFC83D;
            case "Green" -> 0xFF5CE07B;
            case "Cyan" -> 0xFF4DEBFF;
            case "White" -> 0xFFFFFFFF;
            default -> 0xFFA06BFF;
        };
    }

    /** Neon pink theme colours. */
    public static int neonOf(String name) {
        return switch (name) {
            case "Primary" -> 0xFFFF1493;
            case "Secondary" -> 0xFFAA00FF;
            case "Accent" -> 0xFF00FFFF;
            case "Dark" -> 0xFF0A0E27;
            default -> 0xFFFF1493;
        };
    }

    /** A stable colour picked from the palette based on text (used for blocks with no set colour). */
    public static int hash(String s) {
        return of(NAMES[Math.floorMod(s.hashCode(), NAMES.length)]);
    }
}
