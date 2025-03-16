package com.mo.economy_system.network.packets.territory_system;

import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.territory_system.*;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;
import java.util.function.Supplier;

public class Packet_UpgradeTerritoryBuff {
    private final UUID territoryID;
    private final String buffID;

    public Packet_UpgradeTerritoryBuff(UUID territoryID, String buffID) {
        this.territoryID = territoryID;
        this.buffID = buffID;
    }

    // **编码：写入数据**
    public static void encode(Packet_UpgradeTerritoryBuff msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.territoryID);
        buf.writeUtf(msg.buffID);
    }

    // **解码：读取数据**
    public static Packet_UpgradeTerritoryBuff decode(FriendlyByteBuf buf) {
        return new Packet_UpgradeTerritoryBuff(buf.readUUID(), buf.readUtf());
    }

    // **处理服务器逻辑**
    public static void handle(Packet_UpgradeTerritoryBuff msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            EconomySavedData economySavedData = EconomySavedData.getInstance(player.serverLevel());

            Territory territory = TerritoryManager.getTerritoryByID(msg.territoryID);
            if (territory == null) {
                player.sendSystemMessage(Component.literal("❌ 领地不存在！"));
                return;
            }

            TerritoryBuff buff = territory.getBuff(msg.buffID);
            if (buff == null) {
                player.sendSystemMessage(Component.literal("❌ Buff 不存在！"));
                return;
            }

            TerritoryBuffConfig config = TerritoryBuffManager.getBuffConfig(msg.buffID);
            if (config == null) {
                player.sendSystemMessage(Component.literal("❌ Buff 配置错误！"));
                return;
            }

            // 🔹 **计算所需资源**
            int requiredDfCoins = 0;
            int requiredXp = 0;
            for (TerritoryBuffConfig.BuffUpgradeCost cost : config.getUpgradeCost()) {
                if (cost.df_coin > 0) requiredDfCoins += cost.df_coin;
                if (cost.xp > 0) requiredXp += cost.xp;
            }

            // 🔹 **检查 df_coin**
            if (!economySavedData.minBalance(player.getUUID(), requiredDfCoins)) {
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

            // 🔹 **扣除经验**
            player.giveExperienceLevels(-requiredXp);

            // 🔹 **扣除物品**
            for (TerritoryBuffConfig.BuffUpgradeCost cost : config.getUpgradeCost()) {
                for (TerritoryBuffConfig.BuffUpgradeCost.ItemRequirement itemCost : cost.items) {
                    if (itemCost.item != null && itemCost.count > 0) {
                        removeItems(player, itemCost.item, itemCost.count);
                    }
                }
            }

            // **调用升级逻辑**
            TerritoryManager.upgradeBuff(territory.getTerritoryID(), msg.buffID);
            // TerritoryManager.markDirty();
            player.sendSystemMessage(Component.literal("你成功为你的领地升级了增益: " + buff.getDisplayText() + " 到等级 " + buff.getLevel() + "!"));

            contextSupplier.get().setPacketHandled(true);
        });
    }

    /**
     * 检查玩家是否有足够的指定物品
     */
    private static boolean hasEnoughItems(ServerPlayer player, String itemID, int count) {
        int found = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemID))) {
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
            if (stack.getItem() == ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemID))) {
                int removeAmount = Math.min(stack.getCount(), count);
                stack.shrink(removeAmount);
                count -= removeAmount;
                if (count <= 0) break;
            }
        }
    }
}
