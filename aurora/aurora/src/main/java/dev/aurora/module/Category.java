package dev.aurora.module;

public enum Category {
    MISC("Misc"),
    RENDER("Render"),
    CLIENT("Client");

    public final String display;

    Category(String display) {
        this.display = display;
    }
}
