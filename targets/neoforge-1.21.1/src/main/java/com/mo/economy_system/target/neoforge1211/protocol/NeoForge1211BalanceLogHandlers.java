package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.client.ClientBalanceLogState;
import com.mo.economy_system.common.network.BalanceLogRequestMessage;
import com.mo.economy_system.common.network.BalanceLogResponseMessage;
import com.mo.economy_system.core.economy_system.BalanceLogPage;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** NeoForge behavior for the loader-neutral balance-log page messages. */
public final class NeoForge1211BalanceLogHandlers {
    private NeoForge1211BalanceLogHandlers() {
    }

    public static void handleRequest(BalanceLogRequestMessage message, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer
                    ? serverPlayer
                    : null;
            if (player == null) {
                return;
            }

            EconomySavedData data = EconomySavedData.getInstance(player.serverLevel());
            BalanceLogPage page = data.getBalanceLogs(
                    player.getUUID(),
                    message.category(),
                    message.offset(),
                    message.limit()
            );
            EconomySystem_NetworkManager.sendToClient(
                    player,
                    new BalanceLogResponseMessage(
                            page.category(),
                            page.offset(),
                            page.limit(),
                            page.total(),
                            page.logs()
                    )
            );
        });
    }

    public static void handleResponse(BalanceLogResponseMessage message, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientBalanceLogState.update(message);
        });
    }
}
