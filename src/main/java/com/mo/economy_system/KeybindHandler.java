package com.mo.economy_system;

import com.mo.economy_system.screen.Screen_Home;
import com.mo.economy_system.server.serverui.ServerInformationDisplay;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KeybindHandler {

    // 创建按键映射，绑定到 "I" 键
    public static final KeyMapping OPEN_SCREEN_KEY = new KeyMapping(
            "key.economy_system.open_screen",  // 键位描述
            GLFW.GLFW_KEY_I,                   // 默认绑定的键位（I键）
            "key.categories.economy_system"              // 键位分类
    );
    public static final KeyMapping TOGGLE_UI_KEY = new KeyMapping(
            "key.economy_system.open_screen_m",
            GLFW.GLFW_KEY_M,
            "key.categories.economy_system"
    );

    // 注册按键绑定到 Minecraft 系统
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SCREEN_KEY);
        event.register(TOGGLE_UI_KEY);
    }

    // 监听按键事件
    @Mod.EventBusSubscriber(modid = "economy_system", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class KeyInputHandler {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            Minecraft mc = Minecraft.getInstance();
            // 检测按下 "I" 键
            if (OPEN_SCREEN_KEY.isDown()) {
                // 打开自定义界面
                Minecraft.getInstance().setScreen(new Screen_Home());
            }
//            if (OPEN_SCREEN_KEY_M.consumeClick()) {
//                Minecraft.getInstance().setScreen(new Start_UI());
//            }
            if (TOGGLE_UI_KEY.consumeClick()) {
                ServerInformationDisplay.toggleUI(); // UI开关
                mc.player.sendSystemMessage(
                        ServerInformationDisplay.isShowUI() ?
                                Component.literal("§a信息面板已开启，再次按下M关闭！") :
                                Component.literal("§c信息面板已关闭，再次按下M开启！")
                );
            }
        }
    }
}
