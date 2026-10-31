package dev.aurora.mixin;

import dev.aurora.AuroraClient;
import dev.aurora.module.impl.CrosshairModule;
import dev.aurora.module.impl.CustomHotbar;
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

    /** Hides the vanilla hotbar while the Aurora Hotbar module is on with "Hide vanilla" set. */
    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true, require = 0)
    private void aurora$hideVanillaHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (AuroraClient.modules == null) {
            return;
        }
        CustomHotbar hb = AuroraClient.modules.get(CustomHotbar.class);
        if (hb.isEnabled() && hb.hideVanilla()) {
            ci.cancel();
        }
    }
}
