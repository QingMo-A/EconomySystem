//package com.mo.economy_system.core.playerattributes_system.ui;
//
//import com.mo.economy_system.EconomySystem;
//import net.minecraft.client.Minecraft;
//import net.minecraftforge.api.distmarker.Dist;
//import net.minecraftforge.event.TickEvent;
//import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.common.Mod;
//
//// 注解配置正确
//@Mod.EventBusSubscriber(
//        modid = EconomySystem.MODID,
//        value = Dist.CLIENT,
//        bus = Mod.EventBusSubscriber.Bus.FORGE
//)
//public class ClientEventHandler {
//    // 性能优化4：静态标记位，避免重复创建Screen实例
//    private static boolean hasInitCustomScreen = false;
//
//    // 客户端每帧触发，轻量校验，兜底显示
//    @SubscribeEvent
//    public static void onClientTick(TickEvent.ClientTickEvent event) {
//        Minecraft mc = Minecraft.getInstance();
//        // 仅在「玩家有效 + 未初始化Screen + 无其他Screen遮挡」时触发
//        if (mc.player == null || hasInitCustomScreen || mc.screen != null) {
//            return;
//        }
//
//        // 客户端主线程执行，确保线程安全
//        mc.execute(() -> {
//            // 实例化并显示自定义Screen
//            CustomPlayerStatusScreen customScreen = new CustomPlayerStatusScreen();
//            mc.setScreen(customScreen);
//            CustomPlayerStatusScreen.isEnabled = true;
//            // 标记已初始化，避免重复创建
//            hasInitCustomScreen = true;
//        });
//    }
//
//    // 可选：玩家退出世界时重置标记位，下次进入世界可重新显示
//    @SubscribeEvent
//    public static void onPlayerLogout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
//        hasInitCustomScreen = false;
//        CustomPlayerStatusScreen.isEnabled = true;
//    }
//}