package com.mo.economy_system.network.packets.territory_system;

import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mo.economy_system.utils.Util_Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class Packet_TerritoryDataRequest {
    public Packet_TerritoryDataRequest() {}

    public static void encode(Packet_TerritoryDataRequest msg, FriendlyByteBuf buf) {}

    public static Packet_TerritoryDataRequest decode(FriendlyByteBuf buf) {
        return new Packet_TerritoryDataRequest();
    }

    public static void handle(Packet_TerritoryDataRequest msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // 获取当前玩家
            var player = context.getSender();
            if (player != null) {

                var ownedTerritories = TerritoryManager.getTerritoriesByOwner(player.getUUID());
                var authorizedTerritories = TerritoryManager.getAuthorizedTerritories(player.getUUID());

                // 返回领地数据
                EconomySystem_NetworkManager.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new Packet_TerritoryDataResponse(ownedTerritories, authorizedTerritories));
            }
        });
        context.setPacketHandled(true);
    }
}
