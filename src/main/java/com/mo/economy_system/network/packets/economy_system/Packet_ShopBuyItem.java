package com.mo.economy_system.network.packets.economy_system;

import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.utils.Util_MessageKeys;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;


public class Packet_ShopBuyItem implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_ShopBuyItem> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "economy_system/packet_shop_buy_item"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_ShopBuyItem> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_ShopBuyItem.encode(packet, buf), Packet_ShopBuyItem::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    private final String itemID;
    private final String itemNbt;
    private final int price;
    private final int quantity;

    public Packet_ShopBuyItem(String itemID, String itemNbt, int price, int quantity) {
        this.itemID = itemID;
        this.itemNbt = itemNbt;
        this.price = price;
        this.quantity = quantity;
    }

    public static void encode(Packet_ShopBuyItem msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.itemID);
        buf.writeUtf(msg.itemNbt);
        buf.writeInt(msg.price); // 将价格编码
        buf.writeInt(msg.quantity); // 将购买数量编码
    }

    public static Packet_ShopBuyItem decode(FriendlyByteBuf buf) {
        String itemID = buf.readUtf(); // 解码物品名称
        String itemNbt = buf.readUtf();
        int price = buf.readInt(); // 解码价格
        int quantity = buf.readInt(); // 解码购买数量
        return new Packet_ShopBuyItem(itemID, itemNbt, price, quantity);
    }

    public static void handle(Packet_ShopBuyItem msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player == null) return;

            EconomySavedData economyData = EconomySavedData.getInstance(player.serverLevel());
            int totalPrice = msg.price * msg.quantity;

            // 1. 检查余额是否足够
            if (economyData.getBalance(player.getUUID()) < totalPrice) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.SHOP_BUY_FAILED_MESSAGE_KEY));
                return;
            }

            // 2. 检查物品是否有效
            ItemStack itemStack = null;
            if (msg.itemNbt != null || msg.itemNbt != "null") {
                itemStack = getItemStack(msg.itemID, msg.itemNbt);
            } else {
                itemStack = getItemStack(msg.itemID);
            }

            Item item = itemStack.getItem();

            if (item == null) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.SHOP_INVALID_ITEM_MESSAGE_KEY));
                return;
            }

            int maxStackSize = new ItemStack(item).getMaxStackSize();
            int remainingQuantity = msg.quantity;

            // 3. 计算实际需要的槽位（考虑现有堆叠和空槽）
            int requiredSlots = calculateRequiredSlots(player.getInventory(), item, remainingQuantity);

            // 4. 检查是否有足够的槽位
            if (requiredSlots > 0) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.SHOP_BUY_FAILED_INVENTORY_FULL_MESSAGE_KEY));
                return;
            }

            // 5. 执行购买逻辑（扣除余额并添加物品）
            try {
                economyData.minBalance(player.getUUID(), totalPrice);
                if (com.mo.economy_system.utils.ItemStackDataHelper.getTag(itemStack) != null) {
                    addItemsToInventory(player.getInventory(), item, msg.quantity, com.mo.economy_system.utils.ItemStackDataHelper.getTag(itemStack));
                } else {
                    addItemsToInventory(player.getInventory(), item, msg.quantity);
                }
                player.sendSystemMessage(Component.translatable(
                        Util_MessageKeys.SHOP_BUY_SUCCESSFULLY_MESSAGE_KEY,
                        totalPrice,
                        msg.quantity,
                        item.getDescription().getString()
                ));
            } catch (Exception e) {
                economyData.addBalance(player.getUUID(), totalPrice); // 回滚余额
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.SHOP_BUY_ERROR_MESSAGE_KEY));
            }
        });
    }

    // 辅助方法：计算需要的槽位
    private static int calculateRequiredSlots(Inventory inventory, Item item, int quantity) {
        int maxStackSize = new ItemStack(item).getMaxStackSize();
        int remaining = quantity;

        // 1. 尝试合并到现有堆叠（仅限可堆叠物品）
        if (maxStackSize > 1) {
            for (ItemStack stack : inventory.items) {
                if (stack.getItem() == item && stack.getCount() < stack.getMaxStackSize()) {
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
    private static void addItemsToInventory(Inventory inventory, Item item, int quantity) {
        int maxStackSize = new ItemStack(item).getMaxStackSize();
        int remaining = quantity;

        // 1. 优先填充现有堆叠（仅限可堆叠物品）
        if (maxStackSize > 1) {
            for (ItemStack stack : inventory.items) {
                if (stack.getItem() == item && stack.getCount() < stack.getMaxStackSize()) {
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
            ItemStack newStack = new ItemStack(item, stackSize);
            inventory.add(newStack); // 自动处理掉落逻辑
            remaining -= stackSize;
        }
    }

    // 辅助方法：将物品添加到背包
    private static void addItemsToInventory(Inventory inventory, Item item, int quantity, CompoundTag tag) {
        int maxStackSize = new ItemStack(item).getMaxStackSize();
        int remaining = quantity;

        // 1. 优先填充现有堆叠（仅限可堆叠物品）
        if (maxStackSize > 1) {
            for (ItemStack stack : inventory.items) {
                if (stack.getItem() == item && stack.getCount() < stack.getMaxStackSize()) {
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
            ItemStack newStack = new ItemStack(item, stackSize);
            com.mo.economy_system.utils.ItemStackDataHelper.setTag(newStack, tag);
            inventory.add(newStack); // 自动处理掉落逻辑
            remaining -= stackSize;
        }
    }

    public static ItemStack getItemStack(String itemId, String nbt) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
        if (item == null) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(item);

        // 如果有自定义 NBT，则解析并写入
        if (nbt != null && !nbt.isEmpty()) {
            stack = applyEnchantmentNBT(stack, nbt);
        }
        return stack;
    }

    public static ItemStack getItemStack(String itemId) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemId));
        if (item == null) {
            return ItemStack.EMPTY;
        }
        return item.getDefaultInstance();
    }

    public static ItemStack applyEnchantmentNBT(ItemStack itemStack, String nbtString) {
        // 解析NBT字符串
        CompoundTag userNbt;
        try {
            userNbt = TagParser.parseTag(nbtString);
        } catch (CommandSyntaxException e) {
            System.err.println("NBT格式错误: " + e.getMessage());
            return null;
        }

        // 应用NBT
        if (userNbt != null) {
            com.mo.economy_system.utils.ItemStackDataHelper.setTag(itemStack, userNbt);
        }

        return itemStack;
    }
}
