package com.mo.economy_system;

import com.mo.economy_system.screen.Screen_Home;
import com.mo.economy_system.screen.server_screen.ServerInformationDisplay;
import com.mo.economy_system.screen.server_screen.serverscreen.ServerScreenUI;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
public class KeybindHandler {

    // 创建按键映射，绑定到 "I" 键
    public static final KeyMapping OPEN_SCREEN_KEY = new KeyMapping(
            "key.economy_system.open_screen",  // 键位描述
            GLFW.GLFW_KEY_I,                   // 默认绑定的键位（I键）
            "key.categories.economy_system"              // 键位分类
    );
    public static final KeyMapping INFORMATION_UI_KEY = new KeyMapping(
            "key.economy_system.open_screen_o",
            GLFW.GLFW_KEY_O,
            "key.categories.economy_system"
    );
    public static final KeyMapping TASK_UI_KEY = new KeyMapping(
            "key.economy_system.open_screen_u",
            GLFW.GLFW_KEY_U,
            "key.categories.economy_system"
    );

    // 注册按键绑定到 Minecraft 系统
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SCREEN_KEY);
        event.register(INFORMATION_UI_KEY);
        event.register(TASK_UI_KEY);
    }

    // 监听按键事件
    @EventBusSubscriber(modid = "economy_system", value = Dist.CLIENT)
    public static class KeyInputHandler {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            Minecraft mc = Minecraft.getInstance();
            // 检测按下 "I" 键
            if (OPEN_SCREEN_KEY.isDown()) {
                // 打开自定义界面
                Minecraft.getInstance().setScreen(new Screen_Home());
            }
            if (INFORMATION_UI_KEY.consumeClick()) {
                ServerInformationDisplay.toggleUI();
                if (mc.player != null) {
                    mc.player.sendSystemMessage(
                        ServerInformationDisplay.isShowUI() ?
                            Component.literal("§a[EconomySystem]信息面板已开启，再次按下O关闭！") :
                            Component.literal("§c[EconomySystem]信息面板已关闭，再次按下O开启！")
                    );
                }
            }
            if (TASK_UI_KEY.consumeClick()) {
                ServerScreenUI.toggleUI();
            }
        }
    }
}