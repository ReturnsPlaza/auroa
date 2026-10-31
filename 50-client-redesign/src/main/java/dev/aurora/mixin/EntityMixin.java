package dev.aurora.mixin;

import dev.aurora.AuroraClient;
import dev.aurora.module.impl.Freecam;
import dev.aurora.module.impl.FreeLook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While Free Look is on, mouse movement turns the camera instead of your character. */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true, require = 0)
    private void aurora$freeLook(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if (AuroraClient.modules == null) {
            return;
        }
        if ((Object) this != MinecraftClient.getInstance().player) {
            return;
        }
        FreeLook look = AuroraClient.modules.get(FreeLook.class);
        if (!look.isEnabled() || AuroraClient.modules.get(Freecam.class).isEnabled()) {
            return;
        }
        look.look(cursorDeltaX, cursorDeltaY);
        ci.cancel();
    }
}
