package com.mo.economy_system.client;

import com.google.common.collect.Maps;
import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.entity.EconomySystem_Entities;
import com.mo.economy_system.entity.EconomySystem_ModelLayers;
import com.mo.economy_system.entity.entities.model.HiveZombieModel;
import com.mo.economy_system.entity.entities.render.HiveZombieRenderer;
import com.mo.economy_system.item.EconomySystem_Items;
import com.mo.economy_system.item.item_model.CustomRendererBakedModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSetup.class);

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

    // 修改模型烘焙结果
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        LOGGER.info("[Blueprint] ModifyBakingResult event fired!");

        try {
            // 获取蓝图模型的资源位置
            ModelResourceLocation blueprintModel = new ModelResourceLocation(
                    new ResourceLocation(EconomySystem.MODID, "blueprint"),
                    "inventory"
            );

            // 获取当前模型
            BakedModel originalModel = event.getModels().get(blueprintModel);

            if (originalModel != null) {
                // 创建包装器模型，启用自定义渲染
                CustomRendererBakedModel customModel = new CustomRendererBakedModel(originalModel);

                // 使用事件提供的 Map 来替换模型
                event.getModels().put(blueprintModel, customModel);
                LOGGER.info("[Blueprint] Successfully replaced blueprint model in ModifyBakingResult!");
            } else {
                LOGGER.error("[Blueprint] Could not find blueprint model!");
            }
        } catch (Exception e) {
            LOGGER.error("[Blueprint] Failed to modify blueprint model in ModifyBakingResult!", e);
        }
    }
}
