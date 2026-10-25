package dev.aurora.mixin;

import dev.aurora.AuroraClient;
import dev.aurora.module.impl.Freecam;
import dev.aurora.module.impl.FreeLook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    protected abstract void setPos(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Shadow
    private boolean thirdPerson;

    @Inject(method = "update", at = @At("RETURN"))
    private void aurora$camera(CallbackInfo ci) {
        if (AuroraClient.modules == null) {
            return;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        float t = ((Camera) (Object) this).getLastTickProgress();

        Freecam freecam = AuroraClient.modules.get(Freecam.class);
        if (freecam.isEnabled()) {
            setPos(freecam.camX(t), freecam.camY(t), freecam.camZ(t));
            setRotation(player.getYaw(t), player.getPitch(t));
            this.thirdPerson = true;
            return;
        }

        FreeLook look = AuroraClient.modules.get(FreeLook.class);
        if (look.isEnabled()) {
            setRotation(look.yaw(), look.pitch());
        }
    }
}
