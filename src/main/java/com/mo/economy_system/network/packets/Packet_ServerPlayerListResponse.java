package com.mo.economy_system.network.packets;

import com.mo.economy_system.screen.territory_system.Screen_InvitePlayer;
import com.mo.economy_system.screen.server_screen.ServerInformationDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import com.mo.economy_system.compat.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class Packet_ServerPlayerListResponse {
    private final List<Map.Entry<UUID, String>> accounts; // 新增字段

    public Packet_ServerPlayerListResponse(List<Map.Entry<UUID, String>> accounts) {
        this.accounts = accounts;
    }

    public static void encode(Packet_ServerPlayerListResponse msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.accounts.size());
        for (Map.Entry<UUID, String> entry : msg.accounts) {
            buf.writeUUID(entry.getKey());
            buf.writeUtf(entry.getValue());
        }
    }

    public static Packet_ServerPlayerListResponse decode(FriendlyByteBuf buf) {
        int size = buf.readInt();    // 再读取账户数量

        List<Map.Entry<UUID, String>> accounts = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            UUID playerUUID = buf.readUUID();
            String name = buf.readUtf();
            accounts.add(new AbstractMap.SimpleEntry<>(playerUUID, name));
        }

        return new Packet_ServerPlayerListResponse(accounts);
    }

    public static void handle(Packet_ServerPlayerListResponse msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            Screen screen = minecraft.screen;
            if (screen instanceof Screen_InvitePlayer invitePlayer) {
                invitePlayer.update(msg.accounts);
            }
            // 更新服务器信息面板的在线玩家数
            ServerInformationDisplay.ONLINE_PLAYERS = msg.accounts.size();
        });
        context.setPacketHandled(true);
    }
}
