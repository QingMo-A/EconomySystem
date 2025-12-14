package com.mo.economy_system.entity.entities.render;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.entity.EconomySystem_ModelLayers;
import com.mo.economy_system.entity.entities.HiveZombieEntity;
import com.mo.economy_system.entity.entities.model.HiveZombieModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

public class HiveZombieRenderer extends MobRenderer<HiveZombieEntity, HiveZombieModel<HiveZombieEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(EconomySystem.MODID, "textures/entity/hive_zombie.png");

    public HiveZombieRenderer(EntityRendererProvider.Context context) {
        super(context, new HiveZombieModel<>(context.bakeLayer(EconomySystem_ModelLayers.HIVE_ZOMBIE)), 0.5f);

        // 添加盔甲层（可选）
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HiveZombieModel<>(context.bakeLayer(EconomySystem_ModelLayers.HIVE_ZOMBIE_INNER_ARMOR)),
                new HiveZombieModel<>(context.bakeLayer(EconomySystem_ModelLayers.HIVE_ZOMBIE_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(HiveZombieEntity entity) {
        return TEXTURE;
    }
}
