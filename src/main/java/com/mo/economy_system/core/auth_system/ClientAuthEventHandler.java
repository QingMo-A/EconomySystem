//package com.mo.economy_system.core.auth_system;
//
//import com.mo.economy_system.EconomySystem;
//import com.mo.economy_system.core.auth_system.network.ClientAuthHandler;
//import net.minecraft.client.Minecraft;
//import net.minecraftforge.api.distmarker.Dist;
//import net.minecraftforge.api.distmarker.OnlyIn;
//import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
//import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.common.Mod;
//
///**
// * 客户端认证事件处理器
// * 注意：现在不需要手动弹出登录界面，
// * 登录界面会在收到服务端的AuthChallenge包后自动弹出
// */
//@Mod.EventBusSubscriber(modid = EconomySystem.MODID, value = Dist.CLIENT)
//public class ClientAuthEventHandler {
//
//    @OnlyIn(Dist.CLIENT)
//    @SubscribeEvent
//    public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
//        // 玩家断开连接时重置认证状态
//        ClientAuthHandler.setAwaitingAuth(false);
//    }
//}
