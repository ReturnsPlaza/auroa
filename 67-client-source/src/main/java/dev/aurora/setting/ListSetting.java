package dev.aurora.setting;

import java.util.ArrayList;
import java.util.List;

/** A saved list of text values (block ids, friend names). Not shown in the menu. */
public class ListSetting extends Setting<List<String>> {
    public ListSetting(String name) {
        super(name, new ArrayList<>());
        hidden = true;
    }

    @Override
    public String serialize() {
        return String.join(",", value);
    }

    @Override
    public void deserialize(String s) {
        value.clear();
        for (String part : s.split(",")) {
            String p = part.trim();
            if (!p.isEmpty()) {
                value.add(p);
            }
        }
    }
}
