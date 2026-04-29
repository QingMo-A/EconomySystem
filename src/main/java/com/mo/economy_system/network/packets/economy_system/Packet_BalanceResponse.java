package com.mo.economy_system.network.packets.economy_system;

import com.mo.economy_system.client.cache.ClientCacheManager;
import com.mo.economy_system.screen.Screen_Home;
import com.mo.economy_system.screen.components.newUI.a1111_Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.*;

public class Packet_BalanceResponse implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_BalanceResponse> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "economy_system/packet_balance_response"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_BalanceResponse> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_BalanceResponse.encode(packet, buf), Packet_BalanceResponse::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    private final int balance;
    private final List<Map.Entry<String, Integer>> accounts; // 新增字段

    public Packet_BalanceResponse(int balance, List<Map.Entry<String, Integer>> accounts) {
        this.balance = balance;
        this.accounts = accounts;
    }

    public int getBalance() {
        return this.balance;
    }

    // BalanceResponsePacket.java
    public static void encode(Packet_BalanceResponse msg, FriendlyByteBuf buf) {
        // 正确顺序：先写 balance，再写账户数据
        buf.writeInt(msg.balance);
        buf.writeInt(msg.accounts.size());
        for (Map.Entry<String, Integer> entry : msg.accounts) {
            buf.writeUtf(entry.getKey());
            buf.writeInt(entry.getValue());
        }
    }

    public static Packet_BalanceResponse decode(FriendlyByteBuf buf) {
        // 正确顺序：先读 balance，再读账户数据
        int balance = buf.readInt(); // 先读取 balance
        int size = buf.readInt();    // 再读取账户数量

        List<Map.Entry<String, Integer>> accounts = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String name = buf.readUtf();
            int amount = buf.readInt();
            accounts.add(new AbstractMap.SimpleEntry<>(name, amount));
        }

        return new Packet_BalanceResponse(balance, accounts);
    }


    public static void handle(Packet_BalanceResponse msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 同步到ClientCacheManager
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                ClientCacheManager.setPlayerBalance(mc.player.getUUID(), msg.getBalance());
            }
            Screen screen = mc.screen;
            if (screen instanceof Screen_Home screenHome) {
                screenHome.updateBalance(msg.balance, msg.accounts); // 更新界面余额
            } else if (Minecraft.getInstance().screen instanceof a1111_Screen screenA){
                screenA.updateBalance(msg.balance, msg.accounts); // 更新界面余额
            }
        });
    }



}
