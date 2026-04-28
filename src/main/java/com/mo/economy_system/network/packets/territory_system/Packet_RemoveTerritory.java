package com.mo.economy_system.network.packets.territory_system;

import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.mo.economy_system.compat.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class Packet_RemoveTerritory {
    private final UUID territoryID;

    public Packet_RemoveTerritory(UUID territoryID) {
        this.territoryID = territoryID;
    }

    public static void encode(Packet_RemoveTerritory msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.territoryID);
    }

    public static Packet_RemoveTerritory decode(FriendlyByteBuf buf) {
        return new Packet_RemoveTerritory(buf.readUUID());
    }

    public static void handle(Packet_RemoveTerritory msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // 获取目标领地
            var territory = TerritoryManager.getTerritoryByID(msg.territoryID);
            if (territory == null) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.TERRITORY_NOT_FOUND));
                return;
            }

            // 检查权限（只有领地所有者才能删除）
            if (!territory.isOwner(player.getUUID())) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.TERRITORY_NO_OWNER_PERMISSION));
                return;
            }

            // 从管理器中移除领地
            TerritoryManager.removeTerritory(msg.territoryID);
            player.sendSystemMessage(Component.translatable(Util_MessageKeys.TERRITORY_REMOVE_SUCCESS, territory.getName()));
        });
        context.setPacketHandled(true);
    }
}
