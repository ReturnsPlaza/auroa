package dev.aurora.mixin;

import dev.aurora.AuroraClient;
import dev.aurora.module.impl.CustomSky;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Changes the biome sky color used to render the actual sky dome. */
@Mixin(Biome.class)
public abstract class SkyMixin {
    @Inject(method = "getSkyColor(F)I", at = @At("HEAD"), cancellable = true)
    private void aurora$customSky(float temperature, CallbackInfoReturnable<Integer> cir) {
        if (AuroraClient.modules == null) {
            return;
        }
        CustomSky sky = AuroraClient.modules.get(CustomSky.class);
        if (sky.isEnabled()) {
            cir.setReturnValue(sky.color());
        }
    }
}

