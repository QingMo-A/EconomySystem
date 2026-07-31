package com.mo.economy_system.common.economy;

import com.mo.economy_system.common.network.TransferMessage;
import com.mo.economy_system.core.economy_system.BalanceTransferResult;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/** Shared online-player transfer behavior used by every loader target. */
public final class TransferService {
    private static final String TRANSFER_CATEGORY = "转账";
    private static final String TRANSFER_SUCCESS_KEY = "message.transfer.transfer_successfully";
    private static final String RECEIVE_SUCCESS_KEY = "message.transfer.receive_successfully";
    private static final String TRANSFER_FAILED_KEY = "message.transfer.transfer_failed";

    private TransferService() {
    }

    public static BalanceTransferResult execute(ServerPlayer sender, TransferMessage message) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(message, "message");

        ServerPlayer recipient = sender.server.getPlayerList().getPlayer(message.targetPlayerId());
        if (recipient == null) {
            sendFailure(sender);
            return BalanceTransferResult.TARGET_NOT_AVAILABLE;
        }

        EconomySavedData data = EconomySavedData.getInstance(sender.serverLevel());
        BalanceTransferResult result = data.transferBalance(
                sender.getUUID(),
                recipient.getUUID(),
                message.amount(),
                TRANSFER_CATEGORY,
                "赠与 " + recipient.getName().getString(),
                "来自 " + sender.getName().getString() + " 的赠与"
        );
        if (result != BalanceTransferResult.SUCCESS) {
            sendFailure(sender);
            return result;
        }

        sender.sendSystemMessage(Component.translatable(
                TRANSFER_SUCCESS_KEY,
                message.amount(),
                recipient.getName().getString()
        ));
        recipient.sendSystemMessage(Component.translatable(
                RECEIVE_SUCCESS_KEY,
                sender.getName().getString(),
                message.amount()
        ));
        return result;
    }

    private static void sendFailure(ServerPlayer sender) {
        sender.sendSystemMessage(Component.translatable(TRANSFER_FAILED_KEY));
    }
}
