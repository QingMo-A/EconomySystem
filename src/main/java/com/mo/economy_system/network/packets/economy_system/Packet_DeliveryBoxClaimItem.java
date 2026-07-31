package com.mo.economy_system.network.packets.economy_system;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import com.mo.economy_system.core.economy_system.delivery_box.DeliveryBoxSavedData;
import com.mo.economy_system.core.economy_system.delivery_box.DeliveryItem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.platform.EconomyServices;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class Packet_DeliveryBoxClaimItem implements net.minecraft.network.protocol.common.custom.CustomPacketPayload, EconomyNetworkMessage {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_DeliveryBoxClaimItem> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "economy_system/packet_delivery_box_claim_item"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_DeliveryBoxClaimItem> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_DeliveryBoxClaimItem.encode(packet, buf), Packet_DeliveryBoxClaimItem::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
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

    public static void handle(Packet_DeliveryBoxClaimItem msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
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
            EconomySystem_NetworkManager.sendToClient(player, new Packet_DeliveryBoxDataResponse(deliveryBoxSavedData.getItems(player.getUUID())));
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
            } else if (EconomyServices.platform().itemStacks().sameItemAndData(slotStack, stack)) {
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
