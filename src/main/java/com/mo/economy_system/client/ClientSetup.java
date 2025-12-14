package com.mo.economy_system.client;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.entity.EconomySystem_Entities;
import com.mo.economy_system.entity.EconomySystem_ModelLayers;
import com.mo.economy_system.entity.entities.model.HiveZombieModel;
import com.mo.economy_system.entity.entities.render.HiveZombieRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 这里可以放一些客户端设置
    }

    // 注册模型层定义
    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(EconomySystem_ModelLayers.HIVE_ZOMBIE, HiveZombieModel::createBodyLayer);
        event.registerLayerDefinition(EconomySystem_ModelLayers.HIVE_ZOMBIE_INNER_ARMOR, HiveZombieModel::createBodyLayer);
        event.registerLayerDefinition(EconomySystem_ModelLayers.HIVE_ZOMBIE_OUTER_ARMOR, HiveZombieModel::createBodyLayer);
    }

    // 注册渲染器
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EconomySystem_Entities.HIVE_ZOMBIE.get(), HiveZombieRenderer::new);
    }
}
