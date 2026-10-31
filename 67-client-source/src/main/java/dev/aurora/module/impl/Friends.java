package dev.aurora.module.impl;

import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.ListSetting;

import java.util.List;
import java.util.Locale;

/** Keeps a list of friends. They show in their own colour on the radar. Use /aurora friend add <name>. */
public class Friends extends Module {
    private final ListSetting names = addHidden(new ListSetting("names"));

    public Friends() {
        super("Friends", "Friends show in a different colour on the radar", Category.PVP);
        loadEnabled(true);
    }

    public boolean isFriend(String name) {
        return isEnabled() && names.get().contains(name.toLowerCase(Locale.ROOT));
    }

    public boolean add(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (names.get().contains(n)) {
            return false;
        }
        names.get().add(n);
        return true;
    }

    public boolean remove(String name) {
        return names.get().remove(name.toLowerCase(Locale.ROOT));
    }

    public List<String> list() {
        return names.get();
    }
}
