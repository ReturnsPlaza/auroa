package dev.aurora.module.impl;

import dev.aurora.module.Category;
import dev.aurora.module.Module;
import dev.aurora.setting.BooleanSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

/** Highlights entities through walls using the game's own glowing outline. */
public class EntityEsp extends Module {
    private final BooleanSetting players = add(new BooleanSetting("Players", true));
    private final BooleanSetting hostile = add(new BooleanSetting("Hostile", true));
    private final BooleanSetting passive = add(new BooleanSetting("Passive", false));

    private final List<Entity> glowing = new ArrayList<>();

    public EntityEsp() {
        super("Entity ESP", "Outlines entities so you can see them through walls", Category.RENDER);
    }

    @Override
    public void onTick() {
        if (mc.world == null || mc.player == null) {
            return;
        }
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player) {
                continue;
            }
            boolean want = (players.on() && e instanceof PlayerEntity)
                    || (hostile.on() && e instanceof HostileEntity)
                    || (passive.on() && e instanceof PassiveEntity);
            if (want) {
                e.setGlowing(true);
                if (!glowing.contains(e)) {
                    glowing.add(e);
                }
            } else if (glowing.remove(e)) {
                e.setGlowing(false);
            }
        }
        glowing.removeIf(Entity::isRemoved);
    }

    @Override
    protected void onDisable() {
        for (Entity e : glowing) {
            e.setGlowing(false);
        }
        glowing.clear();
    }
}
