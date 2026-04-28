package com.mo.economy_system.network.packets.territory_system;

import com.mo.economy_system.item.items.Item_ClaimWand;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.mo.economy_system.compat.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class Packet_ModifyMode {
    private final UUID territoryUUID;

    public Packet_ModifyMode(UUID territoryUUID) {
        this.territoryUUID = territoryUUID;
    }

    public static void encode(Packet_ModifyMode msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.territoryUUID);
    }

    public static Packet_ModifyMode decode(FriendlyByteBuf buf) {
        return new Packet_ModifyMode(buf.readUUID());
    }

    public static void handle(Packet_ModifyMode msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            Item_ClaimWand.startResizing(player, msg.territoryUUID);
        });
    }
}
