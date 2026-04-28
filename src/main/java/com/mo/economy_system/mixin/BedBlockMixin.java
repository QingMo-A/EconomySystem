package com.mo.economy_system.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(BedBlock.class)
public class BedBlockMixin {

    // 注入 BedBlock 的 use 方法，绕过白天和怪物限制
    /*@Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void allowSleeping(BlockState blockState, Level level, BlockPos blockPos, Player player,
                               InteractionHand interactionHand, BlockHitResult blockHitResult,
                               CallbackInfoReturnable<InteractionResult> cir) {
        if (!level.isClientSide) {
            // 让玩家进入睡眠状态，不管白天黑夜
            player.setSleepingPos(blockPos); // 设定玩家睡觉位置
            player.startSleeping(blockPos);

            // 重置玩家幻翼生成倒计时
            CompoundTag persistentData = player.getPersistentData();
            persistentData.putLong("TimeSinceRest", 0); // 重置休息时间

            // 阻止原有逻辑（如跳过夜晚的处理）
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }*/
    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void allowSleeping(BlockState blockState, Level level, BlockPos blockPos, Player player,
                               BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
        // 先执行原有逻辑，检查床的方向、占用、重生点等
        if (!level.isClientSide) {
            if (blockState.getValue(BedBlock.PART) != BedPart.HEAD) {
                blockPos = blockPos.relative(blockState.getValue(BedBlock.FACING));
                blockState = level.getBlockState(blockPos);
                // 使用 BedBlock 类的类型进行判断
                if (!(blockState.getBlock() instanceof BedBlock)) {
                    cir.setReturnValue(InteractionResult.CONSUME);
                    return;
                }
            }

            // 检查是否可以设置重生点
            if (!BedBlock.canSetSpawn(level)) {
                level.removeBlock(blockPos, false);
                BlockPos oppositePos = blockPos.relative(blockState.getValue(BedBlock.FACING).getOpposite());
                // 使用 blockState.getBlock() 来判断
                if (level.getBlockState(oppositePos).getBlock() instanceof BedBlock) {
                    level.removeBlock(oppositePos, false);
                }

                // 播放爆炸效果
                Vec3 center = blockPos.getCenter();
                level.explode(null, level.damageSources().badRespawnPointExplosion(center), null, center, 5.0F, true, Level.ExplosionInteraction.BLOCK);
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            } else if (blockState.getValue(BedBlock.OCCUPIED)) {
                // 床已经被占用
                if (!kickVillagerOutOfBed(level, blockPos)) {
                        player.displayClientMessage(Component.translatable("block.minecraft.bed.occupied"), true);
                }

                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            } else {
                // 让玩家进入睡眠状态，不管白天黑夜
                player.setSleepingPos(blockPos); // 设置玩家的睡觉位置
                player.startSleeping(blockPos); // 启动睡眠

                // 设置玩家的重生点
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.setRespawnPosition(level.dimension(), blockPos, 0.0F, false, true);
                }

                // 重置玩家幻翼生成倒计时
                CompoundTag persistentData = player.getPersistentData();
                persistentData.putLong("TimeSinceRest", 0); // 重置休息时间
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
        }
    }

    private boolean kickVillagerOutOfBed(Level level, BlockPos blockPos) {
        List<Villager> $$2 = level.getEntitiesOfClass(Villager.class, new AABB(blockPos), LivingEntity::isSleeping);
        if ($$2.isEmpty()) {
            return false;
        } else {
            ((Villager)$$2.get(0)).stopSleeping();
            return true;
        }
    }
}
