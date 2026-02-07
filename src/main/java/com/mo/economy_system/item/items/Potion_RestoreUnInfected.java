package com.mo.economy_system.item.items;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesData;
import com.mo.economy_system.core.playerattributes_system.PlayerAttributesDataManager;
import com.mo.economy_system.core.playerattributes_system.death.RespawnPointSyncManager;
import com.mo.economy_system.core.playerattributes_system.infection.PlayerInfectionClientSync;
import com.mo.economy_system.core.playerattributes_system.infection.PlayerInfectionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 基因复苏药剂
 * 使用后解除感染者身份，并将感染值重置为0
 */
public class Potion_RestoreUnInfected extends Item {

    public Potion_RestoreUnInfected(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        // 灰色：感染后的滋味不好受吧..
        tooltip.add(Component.literal("§7感染后的滋味不好受吧.."));
        // 黄色：右键使用
        tooltip.add(Component.literal("§e右键即可使用"));
        // 金色：使用后您将解除感染者状态，且感染值清0
        tooltip.add(Component.literal("§6使用后您将解除感染者状态，且感染值清0"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 只在服务端处理逻辑
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        // 获取玩家属性数据
        PlayerAttributesData attributesData = PlayerAttributesDataManager.getPlayerAttributesData(serverPlayer.getUUID());
        if (attributesData == null) {
            serverPlayer.sendSystemMessage(Component.literal("§c无法获取玩家数据！"));
            return InteractionResultHolder.fail(stack);
        }

        // 检查是否为感染者
        if (!attributesData.isInfected()) {
            serverPlayer.sendSystemMessage(Component.literal("§c你并不是感染者，无需使用此药剂！"));
            return InteractionResultHolder.fail(stack);
        }

        // 解除感染状态
        attributesData.setInfected(false);
        attributesData.setCurrentInfection(0);

        // 保存数据
        PlayerAttributesDataManager.updatePlayerAttributesData(serverPlayer, attributesData);

        // 同步到客户端
        PlayerInfectionClientSync.sendInfectionDataToClient(serverPlayer, 0);
        // 同步感染者状态到客户端（UI显示需要）
        RespawnPointSyncManager.syncRespawnPointToClient(serverPlayer);

        // 消耗物品
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            player.setItemInHand(hand, stack);
        }

        // 发送成功消息
        serverPlayer.sendSystemMessage(Component.literal("§a§l基因复苏成功！你已不再是感染者。"));

        EconomySystem.LOGGER.info("玩家 {} 使用基因复苏药剂，感染状态已解除", serverPlayer.getScoreboardName());

        return InteractionResultHolder.success(stack);
    }
}