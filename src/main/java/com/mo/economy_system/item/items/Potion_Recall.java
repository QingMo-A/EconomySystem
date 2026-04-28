package com.mo.economy_system.item.items;

import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;

public class Potion_Recall extends Item {
    public Potion_Recall(Properties properties) {
        super(properties.food(new FoodProperties.Builder()
                .alwaysEdible() // 无论是否饱食都能喝
                .nutrition(0) // 不提供营养值
                .saturationModifier(0.0F) // 无饱和度
                .build()));
    }

    // 设置使用时的动画为饮用
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK; // 使用时显示“饮用”的动画
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            player.startUsingItem(hand); // 启动饮用动画
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            ServerLevel currentLevel = (ServerLevel) level; // 当前所在维度的 ServerLevel

            // 获取玩家的出生点和出生维度
            BlockPos respawnPosition = player.getRespawnPosition();
            ResourceKey<Level> respawnDimensionKey = player.getRespawnDimension();

            // 如果出生点未设置，使用默认世界出生点
            if (respawnPosition == null) {
                respawnPosition = level.getSharedSpawnPos(); // 默认出生点
                respawnDimensionKey = Level.OVERWORLD; // 默认维度为主世界
            }

            // 获取目标维度的 ServerLevel
            ServerLevel respawnLevel = currentLevel.getServer().getLevel(respawnDimensionKey);
            // 检查目标维度是否存在
            if (respawnLevel == null) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.RECALL_POTION_ERROR_DIMENSION_NOT_FOUND));
                return stack;
            }

            // 强制加载目标区块
            if (!respawnLevel.isLoaded(respawnPosition)) {
                respawnLevel.getChunkSource().addRegionTicket(
                        net.minecraft.server.level.TicketType.POST_TELEPORT,
                        new net.minecraft.world.level.ChunkPos(respawnPosition),
                        1,
                        player.getId()
                );
            }

            // 传送前粒子效果
            currentLevel.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY(), player.getZ(), 50, 1, 1, 1, 0.1);

            /*// 如果需要切换维度
            if (!currentLevel.dimension().equals(respawnDimensionKey)) {
                // 切换维度
                player.changeDimension(respawnLevel);
            }

            // 在目标维度传送到出生点
            player.teleportTo(respawnPosition.getX() + 0.5, respawnPosition.getY(), respawnPosition.getZ() + 0.5);*/
            // 执行传送
            try {
                player.teleportTo(
                        respawnLevel,
                        respawnPosition.getX() + 0.5,
                        respawnPosition.getY() + 1,
                        respawnPosition.getZ() + 0.5,
                        player.getYRot(),
                        player.getXRot()
                );

                // 确保目标区块刷新
                respawnLevel.getChunkSource().updateChunkForced(
                        new net.minecraft.world.level.ChunkPos(respawnPosition),
                        true
                );

            } catch (Exception e) {
                e.printStackTrace();
            }

            // 传送后粒子效果
            respawnLevel.sendParticles(ParticleTypes.PORTAL, respawnPosition.getX(), respawnPosition.getY(), respawnPosition.getZ(), 50, 1, 1, 1, 0.1);

            // 播放音效
            respawnLevel.playSound(null, respawnPosition, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);

            // 消耗物品
            stack.shrink(1);
        }

        return super.finishUsingItem(stack, level, entity);
    }
}
