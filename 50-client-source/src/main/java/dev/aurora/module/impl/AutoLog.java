package dev.aurora.module.impl;

import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.NumberSetting;
import net.minecraft.text.Text;

/** Leaves the world when your health drops to the set level. */
public class AutoLog extends Module {
    private final NumberSetting health = add(new NumberSetting("Health", 6, 1, 19, 1));

    public AutoLog() {
        super("Auto Log", "Leaves the world when your health gets low", Category.MISC);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }
        if (mc.player.getHealth() <= health.asInt()) {
            setEnabled(false);
            mc.getNetworkHandler().getConnection().disconnect(Text.literal("[goon client] Auto Log: health was low"));
        }
    }
}
