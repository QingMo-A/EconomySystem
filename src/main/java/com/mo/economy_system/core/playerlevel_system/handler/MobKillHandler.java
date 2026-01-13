package com.mo.economy_system.core.playerlevel_system.handler;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerlevel_system.overalllevel.PlayerLevelManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

//指定生物有自己的经验，未指定的敌对生物自动生成经验（根据生命值生成）
@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MobKillHandler {
    //指定生物的经验值配置
    private static final Map<String, Integer> CUSTOM_MOB_EXPERIENCE = new HashMap<>();

    static {
        //Boss
        CUSTOM_MOB_EXPERIENCE.put("minecraft:ender_dragon", 500);
        CUSTOM_MOB_EXPERIENCE.put("minecraft:wither", 300);

        //亡灵
        CUSTOM_MOB_EXPERIENCE.put("minecraft:zombie", 15);
        CUSTOM_MOB_EXPERIENCE.put("minecraft:skeleton", 20);
        CUSTOM_MOB_EXPERIENCE.put("minecraft:drowned", 18);
        CUSTOM_MOB_EXPERIENCE.put("minecraft:wither_skeleton", 45);
        CUSTOM_MOB_EXPERIENCE.put("minecraft:zombie_villager", 20);

        //节肢
        CUSTOM_MOB_EXPERIENCE.put("minecraft:spider", 18);
        CUSTOM_MOB_EXPERIENCE.put("minecraft:cave_spider", 15);
        CUSTOM_MOB_EXPERIENCE.put("minecraft:silverfish", 10);

        //苦力怕
        CUSTOM_MOB_EXPERIENCE.put("minecraft:creeper", 25);

        //末影
        CUSTOM_MOB_EXPERIENCE.put("minecraft:enderman", 40);
        CUSTOM_MOB_EXPERIENCE.put("minecraft:endermite", 15);
        CUSTOM_MOB_EXPERIENCE.put("minecraft:shulker", 35);

        //下界
        CUSTOM_MOB_EXPERIENCE.put("minecraft:blaze", 30);
        CUSTOM_MOB_EXPERIENCE.put("minecraft:ghast", 35);
        CUSTOM_MOB_EXPERIENCE.put("minecraft:magma_cube", 20);
        CUSTOM_MOB_EXPERIENCE.put("minecraft:piglin", 22);
        CUSTOM_MOB_EXPERIENCE.put("minecraft:piglin_brute", 40);
        CUSTOM_MOB_EXPERIENCE.put("minecraft:hoglin", 35);
        CUSTOM_MOB_EXPERIENCE.put("minecraft:zombified_piglin", 25);

        //灾厄村民
        CUSTOM_MOB_EXPERIENCE.put("minecraft:vindicator", 35);
        CUSTOM_MOB_EXPERIENCE.put("minecraft:evoker", 45);
        CUSTOM_MOB_EXPERIENCE.put("minecraft:pillager", 25);
        CUSTOM_MOB_EXPERIENCE.put("minecraft:illusioner", 40);
        CUSTOM_MOB_EXPERIENCE.put("minecraft:ravager", 55);

        //守卫者
        CUSTOM_MOB_EXPERIENCE.put("minecraft:guardian", 30);
        CUSTOM_MOB_EXPERIENCE.put("minecraft:elder_guardian", 80);

        //其他
        CUSTOM_MOB_EXPERIENCE.put("minecraft:phantom", 20);
        CUSTOM_MOB_EXPERIENCE.put("minecraft:slime", 12);
        CUSTOM_MOB_EXPERIENCE.put("minecraft:witch", 28);

        //模组生物示例
        // CUSTOM_MOB_EXPERIENCE.put("modid:mob_name", 100);
    }

    //通用经验配置
    //基础倍率：每点生命值 = 多少经验
    private static final double HEALTH_TO_EXP_RATIO = 1.0;

    // Boss 额外倍率（生命值 >= 100 视为 Boss）
    private static final float BOSS_MULTIPLIER = 3.0f;

    @SubscribeEvent
    public static void onMobKill(LivingDeathEvent event) {
        // 检查被击杀的是否是生物
        if (!(event.getEntity() instanceof Mob mob)) return;

        // 击杀者是否是玩家
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) return;

        // 计算经验
        int experience = getExperienceForMob(mob);
        experience = (int) (experience * 0.6);

        if (experience > 0) {
            PlayerLevelManager.addPlayerExperienceServer(player, experience);
        }
    }

    /**
     * 获取生物经验值
     * 优先使用自定义配置，否则使用通用公式
     */
    private static int getExperienceForMob(Mob mob) {
        ResourceLocation mobId = mob.getType().builtInRegistryHolder().key().location();
        String mobIdStr = mobId.toString();

        //优先检查自定义配置
        if (CUSTOM_MOB_EXPERIENCE.containsKey(mobIdStr)) {
            return CUSTOM_MOB_EXPERIENCE.get(mobIdStr);
        }

        //使用通用公式计算
        return calculateDefaultExperience(mob);
    }

    /**
     * 不在生物自定义列表的生物计算经验
     */
    private static int calculateDefaultExperience(Mob mob) {
        EntityType<?> entityType = mob.getType();
        MobCategory category = entityType.getCategory();

        //非敌对生物不给经验
        if (category != MobCategory.MONSTER) {
            return 0;
        }

        double maxHealth = mob.getMaxHealth();
        boolean isBoss = maxHealth >= 100;

        //基础经验 = 生命值 × 倍率
        int baseExp = (int) (maxHealth * HEALTH_TO_EXP_RATIO);

        // Boss 额外倍率
        if (isBoss) {
            baseExp = (int) (baseExp * BOSS_MULTIPLIER);
        }

        //最小值保护
        return Math.max(baseExp, 5);
    }
}
