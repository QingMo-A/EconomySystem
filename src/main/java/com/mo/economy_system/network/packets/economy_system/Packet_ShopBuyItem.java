package com.mo.economy_system.network.packets.economy_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.economy_system.shop.ShopItem;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public class Packet_ShopBuyItem implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_ShopBuyItem> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "economy_system/packet_shop_buy_item"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_ShopBuyItem> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_ShopBuyItem.encode(packet, buf), Packet_ShopBuyItem::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    private final String shopItemId;
    private final int quantity;

    public Packet_ShopBuyItem(String shopItemId, int quantity) {
        this.shopItemId = shopItemId;
        this.quantity = quantity;
    }

    public Packet_ShopBuyItem(String itemID, String itemNbt, int price, int quantity) {
        this.shopItemId = itemID;
        this.quantity = quantity;
    }

    public static void encode(Packet_ShopBuyItem msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.shopItemId);
        buf.writeInt(msg.quantity); // 将购买数量编码
    }

    public static Packet_ShopBuyItem decode(FriendlyByteBuf buf) {
        String shopItemId = buf.readUtf();
        int quantity = buf.readInt(); // 解码购买数量
        return new Packet_ShopBuyItem(shopItemId, quantity);
    }

    public static void handle(Packet_ShopBuyItem msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player == null) return;

            if (msg.quantity <= 0 || msg.quantity > 2304) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.SHOP_BUY_NO_ITEM_MESSAGE_KEY));
                return;
            }

            ShopItem shopItem = EconomySystem.SHOP_MANAGER.findByShopItemId(msg.shopItemId);
            if (shopItem == null || shopItem.getCurrentPrice() <= 0) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.SHOP_INVALID_ITEM_MESSAGE_KEY));
                return;
            }

            EconomySavedData economyData = EconomySavedData.getInstance(player.serverLevel());
            long totalPriceLong = (long) shopItem.getCurrentPrice() * (long) msg.quantity;
            if (totalPriceLong > Integer.MAX_VALUE) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.SHOP_BUY_FAILED_MESSAGE_KEY));
                return;
            }
            int totalPrice = (int) totalPriceLong;

            // 1. 检查余额是否足够
            if (!economyData.hasEnoughBalance(player.getUUID(), totalPrice)) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.SHOP_BUY_FAILED_MESSAGE_KEY));
                return;
            }

            // 2. 检查物品是否有效
            ItemStack template = shopItem.getItemStack(player.serverLevel().registryAccess());
            if (template == null || template.isEmpty()) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.SHOP_INVALID_ITEM_MESSAGE_KEY));
                return;
            }

            int remainingQuantity = msg.quantity;

            // 3. 计算实际需要的槽位（考虑现有堆叠和空槽）
            int requiredSlots = calculateRequiredSlots(player.getInventory(), template, remainingQuantity);

            // 4. 检查是否有足够的槽位
            if (requiredSlots > 0) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.SHOP_BUY_FAILED_INVENTORY_FULL_MESSAGE_KEY));
                return;
            }

            // 5. 执行购买逻辑（扣除余额并添加物品）
            try {
                if (!economyData.minBalance(player.getUUID(), totalPrice)) {
                    player.sendSystemMessage(Component.translatable(Util_MessageKeys.SHOP_BUY_FAILED_MESSAGE_KEY));
                    return;
                }
                addItemsToInventory(player.getInventory(), template, msg.quantity);
                player.sendSystemMessage(Component.translatable(
                        Util_MessageKeys.SHOP_BUY_SUCCESSFULLY_MESSAGE_KEY,
                        totalPrice,
                        msg.quantity,
                        template.getHoverName().getString()
                ));
            } catch (Exception e) {
                economyData.addBalance(player.getUUID(), totalPrice); // 回滚余额
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.SHOP_BUY_ERROR_MESSAGE_KEY));
            }
        });
    }

    // 辅助方法：计算需要的槽位
    private static int calculateRequiredSlots(Inventory inventory, ItemStack template, int quantity) {
        int maxStackSize = template.getMaxStackSize();
        int remaining = quantity;

        // 1. 尝试合并到现有堆叠（仅限可堆叠物品）
        if (maxStackSize > 1) {
            for (ItemStack stack : inventory.items) {
                if (ItemStack.isSameItemSameComponents(stack, template) && stack.getCount() < stack.getMaxStackSize()) {
                    int availableSpace = stack.getMaxStackSize() - stack.getCount();
                    int transfer = Math.min(availableSpace, remaining);
                    remaining -= transfer;
                    if (remaining == 0) return 0; // 无需新槽位
                }
            }
        }

        // 2. 计算剩余需要的新槽位
        int requiredSlots = 0;
        if (remaining > 0) {
            if (maxStackSize == 1) {
                requiredSlots = remaining; // 不可堆叠物品
            } else {
                requiredSlots = (remaining + maxStackSize - 1) / maxStackSize; // 向上取整
            }

            // 检查实际空槽位是否足够
            int freeSlots = 0;
            for (ItemStack stack : inventory.items) {
                if (stack.isEmpty()) freeSlots++;
            }
            if (freeSlots < requiredSlots) {
                return requiredSlots - freeSlots; // 返回不足的槽位数
            }
        }

        return 0; // 槽位足够
    }

    // 辅助方法：将物品添加到背包
    private static void addItemsToInventory(Inventory inventory, ItemStack template, int quantity) {
        int maxStackSize = template.getMaxStackSize();
        int remaining = quantity;

        // 1. 优先填充现有堆叠（仅限可堆叠物品）
        if (maxStackSize > 1) {
            for (ItemStack stack : inventory.items) {
                if (ItemStack.isSameItemSameComponents(stack, template) && stack.getCount() < stack.getMaxStackSize()) {
                    int availableSpace = stack.getMaxStackSize() - stack.getCount();
                    int transfer = Math.min(availableSpace, remaining);
                    stack.grow(transfer);
                    remaining -= transfer;
                    if (remaining == 0) return;
                }
            }
        }

        // 2. 填充新槽位
        while (remaining > 0) {
            int stackSize = Math.min(remaining, maxStackSize);
            ItemStack newStack = template.copy();
            newStack.setCount(stackSize);
            inventory.add(newStack); // 自动处理掉落逻辑
            remaining -= stackSize;
        }
    }
}
