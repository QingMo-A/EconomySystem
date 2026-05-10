package com.mo.economy_system.network.packets.territory_system;

import net.minecraft.core.registries.BuiltInRegistries;

import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.territory_system.*;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;


import java.util.UUID;

public class Packet_UnlockTerritoryBuff implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_UnlockTerritoryBuff> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "territory_system/packet_unlock_territory_buff"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_UnlockTerritoryBuff> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_UnlockTerritoryBuff.encode(packet, buf), Packet_UnlockTerritoryBuff::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    private final UUID territoryID;
    private final String buffID;

    public Packet_UnlockTerritoryBuff(UUID territoryID, String buffID) {
        this.territoryID = territoryID;
        this.buffID = buffID;
    }

    public static void encode(Packet_UnlockTerritoryBuff msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.territoryID);
        buf.writeUtf(msg.buffID);
    }

    public static Packet_UnlockTerritoryBuff decode(FriendlyByteBuf buf) {
        return new Packet_UnlockTerritoryBuff(buf.readUUID(), buf.readUtf());
    }

    public static void handle(Packet_UnlockTerritoryBuff msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player == null) return;

            EconomySavedData economySavedData = EconomySavedData.getInstance(player.serverLevel());

            Territory territory = TerritoryManager.getTerritoryByID(msg.territoryID);
            if (territory == null) {
                player.sendSystemMessage(Component.literal("❌ 领地不存在！"));
                return;
            }
            if (!territory.isOwner(player.getUUID()) || !territory.getDimension().equals(player.serverLevel().dimension())) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.TERRITORY_NO_OWNER_PERMISSION));
                return;
            }

            TerritoryBuff buff = territory.getBuff(msg.buffID);
            if (buff == null) {
                player.sendSystemMessage(Component.literal("❌ Buff 不存在！"));
                return;
            }
            if (buff.isUnlocked()) {
                player.sendSystemMessage(Component.literal("❌ Buff 已经解锁！"));
                return;
            }

            TerritoryBuffConfig config = TerritoryBuffManager.getBuffConfig(msg.buffID);
            if (config == null) {
                player.sendSystemMessage(Component.literal("❌ Buff 配置错误！"));
                return;
            }

            // 🔹 **计算所需资源**
            long requiredDfCoinsLong = 0L;
            long requiredXpLong = 0L;
            for (TerritoryBuffConfig.BuffUpgradeCost cost : config.getUpgradeCost()) {
                if (cost.df_coin > 0) requiredDfCoinsLong += cost.df_coin;
                if (cost.xp > 0) requiredXpLong += cost.xp;
            }
            if (requiredDfCoinsLong > EconomySavedData.MAX_BALANCE || requiredXpLong > Integer.MAX_VALUE) {
                player.sendSystemMessage(Component.literal("❌ Buff 消耗配置过大！"));
                return;
            }
            int requiredDfCoins = (int) requiredDfCoinsLong;
            int requiredXp = (int) requiredXpLong;

            // 🔹 **检查 df_coin**
            if (requiredDfCoins > 0 && !economySavedData.hasEnoughBalance(player.getUUID(), requiredDfCoins)) {
                player.sendSystemMessage(Component.literal("❌ 梦鱼币不足!"));
                return;
            }

            // 🔹 **检查经验**
            if (player.experienceLevel < requiredXp) {
                player.sendSystemMessage(Component.literal("❌ 经验不足!"));
                return;
            }

            // 🔹 **检查物品**
            for (TerritoryBuffConfig.BuffUpgradeCost cost : config.getUpgradeCost()) {
                for (TerritoryBuffConfig.BuffUpgradeCost.ItemRequirement itemCost : cost.items) {
                    if (!hasEnoughItems(player, itemCost.item, itemCost.count)) {
                        player.sendSystemMessage(Component.literal("❌ 你缺少 " + itemCost.count + " 个 " + itemCost.item));
                        return;
                    }
                }
            }

        if (requiredDfCoins > 0 && !economySavedData.minBalance(player.getUUID(), requiredDfCoins, "领地", "解锁领地增益")) {
                player.sendSystemMessage(Component.literal("❌ 梦鱼币不足!"));
                return;
            }

            // 🔹 **扣除经验**
            if (requiredXp > 0) {
                player.giveExperienceLevels(-requiredXp);
            }

            // 🔹 **扣除物品**
            for (TerritoryBuffConfig.BuffUpgradeCost cost : config.getUpgradeCost()) {
                for (TerritoryBuffConfig.BuffUpgradeCost.ItemRequirement itemCost : cost.items) {
                    if (itemCost.item != null && itemCost.count > 0) {
                        removeItems(player, itemCost.item, itemCost.count);
                    }
                }
            }

            // 执行 Buff 解锁逻辑
            if (TerritoryManager.unlockBuff(territory.getTerritoryID(), msg.buffID)) {
                player.sendSystemMessage(Component.literal("你为你的领地解锁了增益: " + territory.getBuff(msg.buffID).getDisplayText()));
            } else {
                player.sendSystemMessage(Component.literal("❌ Buff 解锁失败！"));
            }
        });
    }

    /**
     * 检查玩家是否有足够的指定物品
     */
    private static boolean hasEnoughItems(ServerPlayer player, String itemID, int count) {
        int found = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemID))) {
                found += stack.getCount();
                if (found >= count) return true;
            }
        }
        return false;
    }

    /**
     * 从玩家物品栏移除指定物品
     */
    private static void removeItems(ServerPlayer player, String itemID, int count) {
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (stack.getItem() == BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemID))) {
                int removeAmount = Math.min(stack.getCount(), count);
                stack.shrink(removeAmount);
                count -= removeAmount;
                if (count <= 0) break;
            }
        }
    }
}

