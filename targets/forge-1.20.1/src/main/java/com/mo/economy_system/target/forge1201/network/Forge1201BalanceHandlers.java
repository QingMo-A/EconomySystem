package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.client.ClientBalanceState;
import com.mo.economy_system.common.network.AccountBalance;
import com.mo.economy_system.common.network.BalanceRequestMessage;
import com.mo.economy_system.common.network.BalanceResponseMessage;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.utils.Util_Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Forge handlers for the first loader-neutral balance request/response pair. */
final class Forge1201BalanceHandlers {
    private Forge1201BalanceHandlers() {
    }

    static void handleRequest(
            BalanceRequestMessage message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player == null) {
            context.setPacketHandled(true);
            return;
        }

        EconomySavedData data = EconomySavedData.getInstance(player.serverLevel());
        List<AccountBalance> accountBalances = new ArrayList<>();
        if (message.includeAccountList()) {
            for (Map.Entry<UUID, Integer> entry : data.getAllAccounts()) {
                accountBalances.add(new AccountBalance(
                        Util_Player.getPlayerNameFromUUID(player.server, entry.getKey()),
                        entry.getValue()
                ));
            }
        }

        Forge1201NetworkChannel.sendToPlayer(
                player,
                new BalanceResponseMessage(data.getBalance(player.getUUID()), accountBalances)
        );
        context.setPacketHandled(true);
    }

    static void handleResponse(
            BalanceResponseMessage message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        ClientBalanceState.update(message);
        contextSupplier.get().setPacketHandled(true);
    }
}
