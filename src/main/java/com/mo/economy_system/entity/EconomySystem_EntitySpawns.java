package com.mo.economy_system.entity;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.entity.entities.HiveZombieEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EconomySystem_EntitySpawns {

    // 注册生成位置规则和生成条件
    @SubscribeEvent
    public static void onRegisterSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(
                EconomySystem_Entities.HIVE_ZOMBIE.get(),
                SpawnPlacements.Type.ON_GROUND,            //地面上生成
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, //不穿透树叶
                EconomySystem_EntitySpawns::canSpawn,                // 自定义生成条件
                SpawnPlacementRegisterEvent.Operation.REPLACE
        );
    }

    //生成条件判断方法
    public static boolean canSpawn(
            EntityType<HiveZombieEntity> type,
            LevelAccessor world,
            MobSpawnType reason,
            BlockPos pos,
            RandomSource random
    ) {
        //仅在晚上生成
        long dayTime = world.dayTime() % 24000;
        boolean isNight = dayTime > 13000 && dayTime < 23000;

        //脚下方块必须是grass
        boolean isValidGround = world.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK) ||
                world.getBlockState(pos.below()).is(Blocks.DIRT);

        //亮度条件
        boolean isDarkEnough = world.getRawBrightness(pos, 0) <= 30;

        //和平模式不生成
        boolean isNotPeaceful = world.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL;

        //所有条件同时满足
        return isNight && isValidGround && isDarkEnough && isNotPeaceful;
    }
}