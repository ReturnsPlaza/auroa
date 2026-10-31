package dev.aurora.module.impl;

import dev.aurora.module.Category;
import dev.aurora.module.Module;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class Fullbright extends Module {
    private boolean added;

    public Fullbright() {
        super("Fullbright", "Maximum brightness everywhere", Category.RENDER);
    }

    @Override
    public void onTick() {
        ClientPlayerEntity player = mc.player;
        if (player == null) {
            return;
        }
        StatusEffectInstance current = player.getStatusEffect(StatusEffects.NIGHT_VISION);
        if (current == null || (added && current.getDuration() < 400)) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1200, 0, false, false, false));
            added = true;
        }
    }

    @Override
    protected void onDisable() {
        if (added && mc.player != null) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        added = false;
    }
}
