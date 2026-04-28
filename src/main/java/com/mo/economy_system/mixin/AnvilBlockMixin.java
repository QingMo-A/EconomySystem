package com.mo.economy_system.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilBlock.class)
public abstract class AnvilBlockMixin extends FallingBlock {

    public AnvilBlockMixin(Properties p_53205_) {
        super(p_53205_);
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void repairAnvil(BlockState blockState, Level level, BlockPos blockPos, Player player,
                             InteractionHand hand, BlockHitResult hitResult,
                             CallbackInfoReturnable<InteractionResult> cir) {
        if (!level.isClientSide && (hand == InteractionHand.MAIN_HAND || hand == InteractionHand.OFF_HAND)) {
            // 获取主手和副手的物品
            ItemStack mainHandItem = player.getMainHandItem();
            ItemStack offHandItem = player.getOffhandItem();

            // 检查是否有铁锭，优先使用主手
            boolean useMainHand = mainHandItem.is(Items.IRON_INGOT);
            boolean useOffHand = !useMainHand && offHandItem.is(Items.IRON_INGOT);

            if (useMainHand || useOffHand) {
                // 选择使用的物品栈
                ItemStack usedItem = useMainHand ? mainHandItem : offHandItem;

                // 检查铁砧是否受损
                if (blockState.is(Blocks.DAMAGED_ANVIL) || blockState.is(Blocks.CHIPPED_ANVIL)) {
                    // 获取当前铁砧的朝向
                    Direction currentFacing = blockState.getValue(AnvilBlock.FACING);

                    // 根据当前状态确定修复后的铁砧
                    BlockState repairedAnvilState = blockState.is(Blocks.DAMAGED_ANVIL)
                            ? Blocks.CHIPPED_ANVIL.defaultBlockState()
                            : Blocks.ANVIL.defaultBlockState();

                    // 保持铁砧的朝向
                    repairedAnvilState = repairedAnvilState.setValue(AnvilBlock.FACING, currentFacing);

                    // 替换铁砧
                    level.setBlockAndUpdate(blockPos, repairedAnvilState);

                    // 消耗铁锭
                    usedItem.shrink(1);

                    // 播放修复音效
                    level.playSound(null, blockPos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);

                    // 阻止后续逻辑
                    cir.setReturnValue(InteractionResult.SUCCESS);

                }
            }
        }
    }
}

