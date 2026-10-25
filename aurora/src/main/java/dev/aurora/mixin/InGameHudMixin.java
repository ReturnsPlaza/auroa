package dev.aurora.mixin;

import dev.aurora.AuroraClient;
import dev.aurora.module.impl.CrosshairModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides the vanilla crosshair while the Aurora Crosshair module is on. require = 0 so a mismatch never crashes the game. */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void aurora$hideVanillaCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (AuroraClient.modules != null && AuroraClient.modules.get(CrosshairModule.class).isEnabled()) {
            ci.cancel();
        }
    }
}
