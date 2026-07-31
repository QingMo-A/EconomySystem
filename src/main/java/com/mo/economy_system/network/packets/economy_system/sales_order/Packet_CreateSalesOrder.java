package com.mo.economy_system.network.packets.economy_system.sales_order;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.economy_system.market.MarketManager;
import com.mo.economy_system.core.economy_system.market.SalesOrder;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.economy_system.Packet_MarketDataResponse;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class Packet_CreateSalesOrder implements net.minecraft.network.protocol.common.custom.CustomPacketPayload, EconomyNetworkMessage {

    public static final Type<Packet_CreateSalesOrder> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EconomySystem.MODID, "economy_system/sales_order/packet_create_sales_order"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_CreateSalesOrder> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_CreateSalesOrder.encode(packet, buf), Packet_CreateSalesOrder::decode);

    private final int slot;
    private final int count;
    private final int price;

    public Packet_CreateSalesOrder(int slot, int count, int price) {
        this.slot = slot;
        this.count = count;
        this.price = price;
    }

    @Override
    public Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(Packet_CreateSalesOrder msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.slot);
        buf.writeInt(msg.count);
        buf.writeInt(msg.price);
    }

    public static Packet_CreateSalesOrder decode(FriendlyByteBuf buf) {
        return new Packet_CreateSalesOrder(buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(Packet_CreateSalesOrder msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            if (msg.price <= 0 || msg.count <= 0) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.LIST_INVALID_PRICE_MESSAGE_KEY));
                return;
            }

            Inventory inventory = player.getInventory();
            if (msg.slot < 0 || msg.slot >= inventory.items.size()) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.LIST_UNMATCHED_ITEM_MESSAGE_KEY));
                return;
            }

            ItemStack selectedStack = inventory.items.get(msg.slot);
            if (selectedStack.isEmpty()) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.LIST_NO_ITEM_IN_HAND_MESSAGE_KEY));
                return;
            }

            ItemStack template = selectedStack.copy();
            int availableCount = countMatchingItems(inventory, template);
            if (availableCount < msg.count) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.LIST_INSUFFICIENT_ITEM_MESSAGE_KEY));
                return;
            }

            int tax = Math.max(1, (int) Math.min(Integer.MAX_VALUE, Math.ceil(msg.price * 0.1D)));
            EconomySavedData economySavedData = EconomySavedData.getInstance(player.serverLevel());
            if (!economySavedData.minBalance(player.getUUID(), tax, "税费", "上架商品税: " + template.getHoverName().getString())) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.LIST_ITEM_TAX_PAYMENT_FAILED_MESSAGE_KEY, tax));
                return;
            }

            removeMatchingItems(inventory, template, msg.count);
            inventory.setChanged();

            ItemStack listedStack = template.copy();
            listedStack.setCount(msg.count);
            String itemId = BuiltInRegistries.ITEM.getKey(listedStack.getItem()).toString();
            SalesOrder salesOrder = new SalesOrder(
                    UUID.randomUUID(),
                    itemId,
                    listedStack,
                    msg.price,
                    player.getName().getString(),
                    player.getUUID(),
                    System.currentTimeMillis()
            );

            MarketManager.addMarketItem(salesOrder);
            MarketManager.saveTo(player.serverLevel());

            player.sendSystemMessage(Component.translatable(Util_MessageKeys.LIST_SUCCESSFULLY_MESSAGE_KEY));
            EconomySystem_NetworkManager.sendToClient(player, new Packet_MarketDataResponse(MarketManager.getMarketItems()));
        });
    }

    private static int countMatchingItems(Inventory inventory, ItemStack template) {
        int count = 0;
        for (ItemStack stack : inventory.items) {
            if (!stack.isEmpty()
                    && EconomyServices.platform().itemStacks().sameItemAndData(stack, template)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void removeMatchingItems(Inventory inventory, ItemStack template, int count) {
        int remaining = count;
        for (ItemStack stack : inventory.items) {
            if (remaining <= 0) {
                return;
            }
            if (!stack.isEmpty()
                    && EconomyServices.platform().itemStacks().sameItemAndData(stack, template)) {
                int removed = Math.min(remaining, stack.getCount());
                stack.shrink(removed);
                remaining -= removed;
            }
        }
    }
}
