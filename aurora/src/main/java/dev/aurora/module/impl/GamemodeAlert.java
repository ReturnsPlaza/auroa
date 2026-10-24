package dev.aurora.module.impl;

import dev.aurora.hud.Alerts;
import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.BooleanSetting;
import dev.aurora.setting.NumberSetting;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Tells you when someone's gamemode changes (for example a friend switching to
 * creative or spectator). It only reads the player list the server already sends you.
 */
public class GamemodeAlert extends Module {
    private final BooleanSetting chat = add(new BooleanSetting("Chat message", true));
    private final NumberSetting seconds = add(new NumberSetting("Show for (s)", 5, 2, 12, 1));

    private final Map<UUID, GameMode> known = new HashMap<>();
    private ClientWorld lastWorld;
    private boolean primed;

    public GamemodeAlert() {
        super("Gamemode Alert", "Shows who changes gamemode, with their name and head", Category.MISC);
    }

    @Override
    public void onTick() {
        if (mc.world == null || mc.player == null || mc.getNetworkHandler() == null) {
            return;
        }
        // New world or server: learn everyone's current mode first without alerting.
        if (mc.world != lastWorld) {
            lastWorld = mc.world;
            known.clear();
            primed = false;
            Alerts.clear();
        }

        UUID self = mc.player.getUuid();
        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            UUID id = entry.getProfile().id();
            GameMode now = entry.getGameMode();
            if (now == null) {
                continue;
            }
            GameMode before = known.put(id, now);
            if (!primed || before == null || before == now || id.equals(self)) {
                continue;
            }
            String name = entry.getProfile().name();
            String line = label(before) + " -> " + label(now);
            Alerts.push(name, line, entry.getSkinTextures(), seconds.asInt());
            if (chat.on()) {
                mc.player.sendMessage(Text.literal("[Aurora] " + name + " changed gamemode: " + line), false);
            }
        }
        primed = true;
    }

    @Override
    protected void onDisable() {
        known.clear();
        primed = false;
        lastWorld = null;
        Alerts.clear();
    }

    private static String label(GameMode mode) {
        return mode.getId().toUpperCase(Locale.ROOT);
    }
}
