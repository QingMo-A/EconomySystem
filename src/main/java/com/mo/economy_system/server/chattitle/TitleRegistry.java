package com.mo.economy_system.server.chattitle;

import com.mo.economy_system.EconomySystem;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TitleRegistry {
    // 初始化：在Mod启动时加载配置
    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 异步加载配置（避免阻塞主线程）
            TitleConfig.loadConfig();
        });
    }

    // 对外提供获取称号的方法（给Capability调用）
    public static Title getDefaultTitle() {
        return TitleConfig.getTitleById(0); // 默认称号ID=0
    }

    public static Title getTitleById(int titleId) {
        return TitleConfig.getTitleById(titleId);
    }

    public static Title getTitleByName(String titleName) {
        return TitleConfig.getTitleByName(titleName);
    }
}