package com.mo.economy_system.network.packets.economy_system;

import com.mo.economy_system.core.economy_system.delivery_box.DeliveryBoxSavedData;
import com.mo.economy_system.core.economy_system.delivery_box.DeliveryItem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.economy_system.sales_order.Packet_PurchaseSalesOrder;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import com.mo.economy_system.compat.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class Packet_DeliveryBoxClaimItem {
    private final UUID dataId; // 物资的唯一 ID

    public Packet_DeliveryBoxClaimItem(UUID dataId) {
        this.dataId = dataId;
    }

    public static void encode(Packet_DeliveryBoxClaimItem msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.dataId);
    }

    public static Packet_DeliveryBoxClaimItem decode(FriendlyByteBuf buf) {
        return new Packet_DeliveryBoxClaimItem(buf.readUUID());
    }

    public static void handle(Packet_DeliveryBoxClaimItem msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            DeliveryBoxSavedData deliveryBoxSavedData = DeliveryBoxSavedData.getInstance(player.serverLevel());

            DeliveryItem deliveryItem = deliveryBoxSavedData.getItem(player.getUUID(), msg.dataId);
            if (deliveryItem == null) {
                player.sendSystemMessage(Component.literal("不存在的物品"));
                return;
            }

            ItemStack item = deliveryItem.getItemStack().copy();
            if (!canFitInInventory(player.getInventory(), item)) {
                player.sendSystemMessage(Component.literal("物品栏已满, 请清理后重试"));
                return;
            }

            deliveryBoxSavedData.removeItem(player.getUUID(), msg.dataId);
            if (!player.getInventory().add(item)) {
                player.drop(item, false); // 如果背包满了，直接丢在地�?
            }

            // 通知玩家成功购买
            player.sendSystemMessage(Component.literal("领取成功"));
            // 通知客户端刷新市场界�?
            EconomySystem_NetworkManager.INSTANCE.sendToServer(new Packet_DeliveryBoxDataRequest());
        });
    }

    private static boolean canFitInInventory(Inventory inventory, ItemStack stack) {
        int remaining = stack.getCount();
        int maxStackSize = Math.min(inventory.getMaxStackSize(), stack.getMaxStackSize());
        int totalSlots = inventory.getContainerSize();
        int offhandIndex = totalSlots - 1;
        int armorStart = totalSlots >= 5 ? totalSlots - 1 - 4 : totalSlots;
        int armorEnd = totalSlots >= 5 ? totalSlots - 2 : -1;

        for (int i = 0; i < totalSlots; i++) {
            boolean isArmorSlot = i >= armorStart && i <= armorEnd;
            boolean isOffhandSlot = i == offhandIndex && totalSlots >= 1;
            if (isArmorSlot) {
                continue;
            }

            ItemStack slotStack = inventory.getItem(i);
            if (slotStack.isEmpty()) {
                if (!isOffhandSlot) {
                    remaining -= Math.min(maxStackSize, remaining);
                }
            } else if (ItemStack.isSameItemSameComponents(slotStack, stack)) {
                int slotLimit = Math.min(inventory.getMaxStackSize(), slotStack.getMaxStackSize());
                int space = slotLimit - slotStack.getCount();
                if (space > 0) {
                    remaining -= Math.min(space, remaining);
                }
            }

            if (remaining <= 0) {
                return true;
            }
        }

        return remaining <= 0;
    }
}
