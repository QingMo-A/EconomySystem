package com.mo.economy_system;

import com.mo.economy_system.screen.Screen_Home;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
public class KeybindHandler {

    public static final KeyMapping OPEN_SCREEN_KEY = new KeyMapping(
            "key.economy_system.open_screen",
            GLFW.GLFW_KEY_I,
            "key.categories.economy_system"
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SCREEN_KEY);
    }

    @EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
    public static class KeyInputHandler {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (OPEN_SCREEN_KEY.consumeClick()) {
                Minecraft.getInstance().setScreen(new Screen_Home());
            }
        }
    }
}
