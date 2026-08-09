package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.network.AccountBalance;
import com.mo.economy_system.common.network.BalanceRequestMessage;
import com.mo.economy_system.common.network.BalanceResponseMessage;
import com.mo.economy_system.common.client.ClientBalanceState;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.target.neoforge1211.player.NeoForge1211PlayerLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** NeoForge-side behavior for the common balance request/response pair. */
public final class NeoForge1211BalanceHandlers {
    private NeoForge1211BalanceHandlers() {
    }

    public static void handleRequest(BalanceRequestMessage message, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer
                    ? serverPlayer
                    : null;
            if (player == null) {
                return;
            }

            ServerLevel serverLevel = player.serverLevel();
            EconomySavedData data = EconomySavedData.getInstance(serverLevel);
            int balance = data.getBalance(player.getUUID());
            List<AccountBalance> accountBalances = new ArrayList<>();

            if (message.includeAccountList()) {
                for (Map.Entry<UUID, Integer> entry : data.getAllAccounts()) {
                    String playerName = NeoForge1211PlayerLookup.profileName(
                            player.server,
                            entry.getKey()
                    );
                    accountBalances.add(new AccountBalance(playerName, entry.getValue()));
                }
            }

            EconomySystem_NetworkManager.sendToClient(
                    player,
                    new BalanceResponseMessage(balance, accountBalances)
            );
        });
    }

    public static void handleResponse(BalanceResponseMessage message, IPayloadContext context) {
        context.enqueueWork(() -> ClientOnly.apply(message));
    }

    /** Kept lazy so a dedicated server never resolves client-only classes. */
    private static final class ClientOnly {
        private ClientOnly() {
        }

        private static void apply(BalanceResponseMessage message) {
            ClientBalanceState.update(message);
        }
    }
}
