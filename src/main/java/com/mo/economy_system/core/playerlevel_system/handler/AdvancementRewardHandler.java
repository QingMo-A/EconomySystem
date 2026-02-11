package com.mo.economy_system.core.playerlevel_system.handler;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerlevel_system.overalllevel.PlayerLevelManager;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.FrameType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 进度（成就）奖励处理器
 * 玩家完成进度时根据进度类型给予不同的经验奖励
 */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID)
public class AdvancementRewardHandler {

    // ==================== 配置项 ====================
    /** 普通任务（TASK）经验奖励 */
    private static final long TASK_EXPERIENCE_REWARD = 100L;

    /** 目标（GOAL）经验奖励 */
    private static final long GOAL_EXPERIENCE_REWARD = 200L;

    /** 挑战（CHALLENGE）经验奖励 */
    private static final long CHALLENGE_EXPERIENCE_REWARD = 1000L;

    /** 隐藏进度额外经验加成（如"为什么会这样呢"等隐藏挑战） */
    private static final long HIDDEN_BONUS_EXPERIENCE = 10000L;

    /** 隐藏进度（如配方解锁）处理方式：true=忽略不给经验，false=给少量经验 */
    private static final boolean HIDE_HIDDEN_ADVANCEMENTS = true;
    // =================================================

    /**
     * 玩家完成进度时触发
     */
    @SubscribeEvent
    public static void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Advancement advancement = event.getAdvancement();

        // 忽略没有显示信息的进度（如配方解锁）
        if (advancement.getDisplay() == null) {
            if (HIDE_HIDDEN_ADVANCEMENTS) {
                return;
            }
            // 隐藏进度给少量经验
            PlayerLevelManager.addPlayerExperienceServer(player, TASK_EXPERIENCE_REWARD / 2);
            return;
        }

        // 根据进度类型给予不同经验奖励
        FrameType frameType = advancement.getDisplay().getFrame();
        boolean isHidden = advancement.getDisplay().isHidden();
        long expReward = calculateExperienceReward(frameType, isHidden, advancement);

        // 发放经验奖励
        PlayerLevelManager.addPlayerExperienceServer(player, expReward);

        // 注意：聊天消息由PlayerAdvancementsMixin拦截并显示在右侧

        EconomySystem.LOGGER.info("玩家 {} 完成进度 {} (类型: {}, 隐藏: {})，获得 {} 经验",
                player.getScoreboardName(), advancement.getId(), frameType.getName(), isHidden, expReward);
    }

    /**
     * 根据进度类型计算经验奖励
     * @param frameType 进度框架类型
     * @param isHidden 是否为隐藏进度
     * @param advancement 进度对象（用于检查命名空间）
     * @return 经验值
     */
    private static long calculateExperienceReward(FrameType frameType, boolean isHidden, Advancement advancement) {
        long baseReward = switch (frameType) {
            case TASK -> TASK_EXPERIENCE_REWARD;
            case GOAL -> GOAL_EXPERIENCE_REWARD;
            case CHALLENGE -> CHALLENGE_EXPERIENCE_REWARD;
        };
        // 仅原版（minecraft命名空间）的隐藏进度给予额外加成
        boolean isVanilla = "minecraft".equals(advancement.getId().getNamespace());
        if (isHidden && isVanilla) {
            return baseReward + HIDDEN_BONUS_EXPERIENCE;
        }
        return baseReward;
    }
}