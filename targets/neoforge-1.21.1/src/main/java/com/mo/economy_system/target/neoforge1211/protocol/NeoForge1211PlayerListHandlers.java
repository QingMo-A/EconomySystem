package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.client.ClientPlayerListState;
import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.common.network.ServerPlayerListRequestMessage;
import com.mo.economy_system.common.network.ServerPlayerListResponseMessage;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.utils.Util_Player;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** NeoForge-side behavior for the common server-player-list message pair. */
public final class NeoForge1211PlayerListHandlers {
    private NeoForge1211PlayerListHandlers() {
    }

    public static void handleRequest(
            ServerPlayerListRequestMessage message,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer
                    ? serverPlayer
                    : null;
            if (player == null) {
                return;
            }

            EconomySavedData data = EconomySavedData.getInstance(player.serverLevel());
            List<PlayerSummary> players = Util_Player.getAllPlayerName(data, player.server).stream()
                    .map(entry -> new PlayerSummary(entry.getKey(), entry.getValue()))
                    .toList();
            EconomySystem_NetworkManager.sendToClient(
                    player,
                    new ServerPlayerListResponseMessage(players)
            );
        });
    }

    public static void handleResponse(
            ServerPlayerListResponseMessage message,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            ClientPlayerListState.update(message);
            ClientOnly.apply(message);
        });
    }

    /** Kept lazy so a dedicated server never resolves client-only screen classes. */
    private static final class ClientOnly {
        private ClientOnly() {
        }

        private static void apply(ServerPlayerListResponseMessage message) {
            List<Map.Entry<UUID, String>> players = message.players().stream()
                    .<Map.Entry<UUID, String>>map(player -> new AbstractMap.SimpleImmutableEntry<>(
                            player.playerId(),
                            player.playerName()
                    ))
                    .toList();
            net.minecraft.client.gui.screens.Screen screen =
                    net.minecraft.client.Minecraft.getInstance().screen;
            if (screen instanceof com.mo.economy_system.screen.territory_system.Screen_InvitePlayer invitePlayer) {
                invitePlayer.update(players);
            } else if (screen instanceof com.mo.economy_system.screen.territory_system.Screen_TerritoryPlayerAction playerAction) {
                playerAction.update(players);
            }
        }
    }
}
