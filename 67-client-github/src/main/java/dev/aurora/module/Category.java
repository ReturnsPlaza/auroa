package dev.aurora.module;

public enum Category {
    PLAYER("Player"),
    PVP("PvP"),
    RENDER("Render"),
    WORLD("World"),
    MISC("Misc");

    public final String display;

    Category(String display) {
        this.display = display;
    }
}
