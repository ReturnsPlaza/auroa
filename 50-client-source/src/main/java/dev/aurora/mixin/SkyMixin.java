package dev.aurora.mixin;

import dev.aurora.AuroraClient;
import dev.aurora.module.impl.CustomSky;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces the sky colour with the Custom sky colour while that module is on. require = 0 keeps a name mismatch from crashing. */
@Mixin(ClientWorld.class)
public abstract class SkyMixin {
    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true, require = 0)
    private void aurora$customSky(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        if (AuroraClient.modules == null) {
            return;
        }
        CustomSky sky = AuroraClient.modules.get(CustomSky.class);
        if (sky.isEnabled()) {
            int c = sky.color();
            cir.setReturnValue(new Vec3d(((c >> 16) & 0xFF) / 255.0, ((c >> 8) & 0xFF) / 255.0, (c & 0xFF) / 255.0));
        }
    }
}
