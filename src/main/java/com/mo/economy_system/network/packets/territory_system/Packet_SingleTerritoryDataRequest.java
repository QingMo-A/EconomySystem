package com.mo.economy_system.network.packets.territory_system;

import com.mo.economy_system.network.EconomySystem_NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.NetworkEvent;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

public class Packet_SingleTerritoryDataRequest {
    private final UUID territoryID;

    public Packet_SingleTerritoryDataRequest(UUID territoryID) {
        this.territoryID = territoryID;
    }

    public static void encode(Packet_SingleTerritoryDataRequest msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.territoryID);
    }

    public static Packet_SingleTerritoryDataRequest decode(FriendlyByteBuf buf) {
        return new Packet_SingleTerritoryDataRequest(buf.readUUID());
    }

    public static void handle(Packet_SingleTerritoryDataRequest msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // 🔹 获取领地数据
            Territory territory = TerritoryManager.getTerritoryByID(msg.territoryID);
            if (territory == null) {
                System.out.println("❌ 领地数据请求失败：该 ID 不存在！");
                return;
            }

            // 🔹 发送数据回客户端
            Packet_SingleTerritoryDataResponse response = new Packet_SingleTerritoryDataResponse(territory);
            EconomySystem_NetworkManager.INSTANCE.send(player, response);
        });

        context.setPacketHandled(true);
    }
}
