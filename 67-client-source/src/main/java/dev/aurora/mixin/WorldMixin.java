package dev.aurora.mixin;

import dev.aurora.AuroraClient;
import dev.aurora.module.impl.WeatherModule;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes rain and thunder read as zero while the Aurora Weather module has Clear sky on. require = 0 keeps a name mismatch from crashing. */
@Mixin(World.class)
public abstract class WorldMixin {
    @Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true, require = 0)
    private void aurora$clearRain(float delta, CallbackInfoReturnable<Float> cir) {
        if (clearSky()) {
            cir.setReturnValue(0.0f);
        }
    }

    @Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true, require = 0)
    private void aurora$clearThunder(float delta, CallbackInfoReturnable<Float> cir) {
        if (clearSky()) {
            cir.setReturnValue(0.0f);
        }
    }

    private static boolean clearSky() {
        return AuroraClient.modules != null
                && AuroraClient.modules.get(WeatherModule.class).isEnabled()
                && AuroraClient.modules.get(WeatherModule.class).clearSky();
    }
}
