package com.mo.economy_system;

import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.playerattribute_system.strength_system.Packet_SprintKeyPress;
import com.mo.economy_system.screen.Screen_Home;
import com.mo.economy_system.server.serverui.ServerInformationDisplay;
import com.mo.economy_system.core.task_system.taskui.TaskUI;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
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
    //疾跑键
    public static final KeyMapping SPRINT_KEY = new KeyMapping(
            "key.sprint",
            GLFW.GLFW_KEY_LEFT_CONTROL,
            "key.categories.movement"
    );

    // 注册按键绑定到 Minecraft 系统
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SCREEN_KEY);
        event.register(INFORMATION_UI_KEY);
        event.register(TASK_UI_KEY);
        event.register(SPRINT_KEY);
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
            if (INFORMATION_UI_KEY.consumeClick()) {
                if (mc.isSingleplayer()) {
                    return;
                }
                ServerInformationDisplay.toggleUI();
                mc.player.sendSystemMessage(
                        ServerInformationDisplay.isShowUI() ?
                                Component.literal("§a[EconomySystem]信息面板已开启，再次按下O关闭！") :
                                Component.literal("§c[EconomySystem]信息面板已关闭，再次按下O开启！")
                );
            }
            if (TASK_UI_KEY.consumeClick()) {
                TaskUI.toggleUI();
            }
            if (event.getAction() == GLFW.GLFW_PRESS && SPRINT_KEY.consumeClick()) {
                EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_SprintKeyPress());  //发送网络包
            }
        }
    }
}