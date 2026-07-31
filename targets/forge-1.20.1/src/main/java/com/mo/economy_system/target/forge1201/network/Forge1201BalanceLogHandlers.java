package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.client.ClientBalanceLogState;
import com.mo.economy_system.common.network.BalanceLogRequestMessage;
import com.mo.economy_system.common.network.BalanceLogResponseMessage;
import com.mo.economy_system.core.economy_system.BalanceLogPage;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Forge handlers for the loader-neutral balance-log page messages. */
final class Forge1201BalanceLogHandlers {
    private Forge1201BalanceLogHandlers() {
    }

    static void handleRequest(
            BalanceLogRequestMessage message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player == null) {
            context.setPacketHandled(true);
            return;
        }

        EconomySavedData data = EconomySavedData.getInstance(player.serverLevel());
        BalanceLogPage page = data.getBalanceLogs(
                player.getUUID(),
                message.category(),
                message.offset(),
                message.limit()
        );
        Forge1201NetworkChannel.sendToPlayer(
                player,
                new BalanceLogResponseMessage(
                        page.category(),
                        page.offset(),
                        page.limit(),
                        page.total(),
                        page.logs()
                )
        );
        context.setPacketHandled(true);
    }

    static void handleResponse(
            BalanceLogResponseMessage message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        ClientBalanceLogState.update(message);
        context.setPacketHandled(true);
    }
}
