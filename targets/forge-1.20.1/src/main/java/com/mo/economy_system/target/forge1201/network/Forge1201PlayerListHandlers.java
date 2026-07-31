package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.client.ClientPlayerListState;
import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.common.network.ServerPlayerListRequestMessage;
import com.mo.economy_system.common.network.ServerPlayerListResponseMessage;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.utils.Util_Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/** Forge handlers for the loader-neutral server-player-list message pair. */
final class Forge1201PlayerListHandlers {
    private Forge1201PlayerListHandlers() {
    }

    static void handleRequest(
            ServerPlayerListRequestMessage message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player == null) {
            context.setPacketHandled(true);
            return;
        }

        EconomySavedData data = EconomySavedData.getInstance(player.serverLevel());
        List<PlayerSummary> players = Util_Player.getAllPlayerName(data, player.server).stream()
                .map(entry -> new PlayerSummary(entry.getKey(), entry.getValue()))
                .toList();
        Forge1201NetworkChannel.sendToPlayer(
                player,
                new ServerPlayerListResponseMessage(players)
        );
        context.setPacketHandled(true);
    }

    static void handleResponse(
            ServerPlayerListResponseMessage message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        ClientPlayerListState.update(message);
        contextSupplier.get().setPacketHandled(true);
    }
}
