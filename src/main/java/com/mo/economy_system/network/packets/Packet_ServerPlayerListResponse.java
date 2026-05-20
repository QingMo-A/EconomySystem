package com.mo.economy_system.network.packets;

import com.mo.economy_system.screen.territory_system.Screen_InvitePlayer;
import com.mo.economy_system.screen.territory_system.Screen_TerritoryPlayerAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.*;

public class Packet_ServerPlayerListResponse implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_ServerPlayerListResponse> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "packet_server_player_list_response"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_ServerPlayerListResponse> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_ServerPlayerListResponse.encode(packet, buf), Packet_ServerPlayerListResponse::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
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

    public static void handle(Packet_ServerPlayerListResponse msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            Screen screen = minecraft.screen;
            if (screen instanceof Screen_InvitePlayer invitePlayer) {
                invitePlayer.update(msg.accounts);
            } else if (screen instanceof Screen_TerritoryPlayerAction playerAction) {
                playerAction.update(msg.accounts);
            }
        });
    }
}
