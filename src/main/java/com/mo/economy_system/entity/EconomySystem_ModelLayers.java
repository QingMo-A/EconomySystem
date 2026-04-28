package com.mo.economy_system.entity;

import com.mo.economy_system.EconomySystem;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public class EconomySystem_ModelLayers {
    // 注册模型层
    public static final ModelLayerLocation HIVE_ZOMBIE =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(EconomySystem.MODID, "hive_zombie"), "main");

    public static final ModelLayerLocation HIVE_ZOMBIE_INNER_ARMOR =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(EconomySystem.MODID, "hive_zombie"), "inner_armor");

    public static final ModelLayerLocation HIVE_ZOMBIE_OUTER_ARMOR =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(EconomySystem.MODID, "hive_zombie"), "outer_armor");
}
