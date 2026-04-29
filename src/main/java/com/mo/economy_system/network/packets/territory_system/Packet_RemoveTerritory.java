package com.mo.economy_system.network.packets.territory_system;

import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class Packet_RemoveTerritory implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_RemoveTerritory> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "territory_system/packet_remove_territory"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_RemoveTerritory> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_RemoveTerritory.encode(packet, buf), Packet_RemoveTerritory::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
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

    public static void handle(Packet_RemoveTerritory msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
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
    }
}
