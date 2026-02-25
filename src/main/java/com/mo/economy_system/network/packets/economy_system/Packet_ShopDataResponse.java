package com.mo.economy_system.network.packets.economy_system;

import com.mo.economy_system.screen.economy_system.shop.Screen_Shop;
import com.mo.economy_system.core.economy_system.shop.ShopItem;
import com.mo.economy_system.screen.components.newUI.a1111_Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Packet_ShopDataResponse {
    private final List<ShopItem> shopItems;

    public Packet_ShopDataResponse(List<ShopItem> shopItems) {
        this.shopItems = shopItems;
    }

    public static void encode(Packet_ShopDataResponse msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.shopItems.size());
        for (ShopItem item : msg.shopItems) {
            buf.writeNbt(item.toNBT());
        }
    }

    public static Packet_ShopDataResponse decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<ShopItem> shopItems = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            CompoundTag nbt = buf.readNbt();
            if (nbt != null) {
                shopItems.add(ShopItem.fromNBT(nbt));
            }
        }
        return new Packet_ShopDataResponse(shopItems);
    }

    public static void handle(Packet_ShopDataResponse msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Util_Message.sendDebugMessage("收到来自服务器的商品数据: " + msg.shopItems.size() + " 个");
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof Screen_Shop screenShop) {
                screenShop.updateShopItems(msg.shopItems);
            } else if (Minecraft.getInstance().screen instanceof a1111_Screen screen){
                screen.updateShopItems(msg.shopItems);
            }
        });
        context.setPacketHandled(true);
    }
}
