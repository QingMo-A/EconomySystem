package com.mo.economy_system.entity;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.entity.entities.HiveZombieEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EconomySystem_Entities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EconomySystem.MODID);

    // 注册自定义丧尸实体
    public static final RegistryObject<EntityType<HiveZombieEntity>> HIVE_ZOMBIE =
            ENTITIES.register("hive_zombie",
                    () -> EntityType.Builder.of(HiveZombieEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)  // 实体尺寸（宽，高）
                            .clientTrackingRange(8)  // 客户端追踪范围
                            .build("hive_zombie"));
}
