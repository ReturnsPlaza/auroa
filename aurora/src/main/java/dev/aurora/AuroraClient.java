package dev.aurora;

import dev.aurora.gui.ClickGuiScreen;
import dev.aurora.hud.HudRenderer;
import dev.aurora.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class AuroraClient implements ClientModInitializer {
    public static final String ID = "aurora";

    public static ModuleManager modules;
    public static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        modules = new ModuleManager();
        modules.registerDefaults();
        Config.load(modules);

        // Right Shift opens (and closes) the menu. Change it under Options > Controls > Key Binds.
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.aurora.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyBinding.Category.create(Identifier.of(ID, "main"))));

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.currentScreen == null && client.player != null) {
                    client.setScreen(ClickGuiScreen.get());
                }
            }
            if (client.player != null && client.world != null) {
                modules.tick();
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(HudRenderer::tickStats);

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.of(ID, "hud"),
                HudRenderer::render);

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Config.save(modules));
    }
}
