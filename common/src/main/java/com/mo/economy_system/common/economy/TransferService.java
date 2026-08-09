package com.mo.economy_system.common.economy;

import com.mo.economy_system.common.network.TransferMessage;
import com.mo.economy_system.core.economy_system.BalanceTransferResult;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Loader-neutral, server-authoritative online-player transfer policy. */
public final class TransferService {
    public static final String TRANSFER_CATEGORY = "转账";
    public static final String TRANSFER_SUCCESS_KEY = "message.transfer.transfer_successfully";
    public static final String RECEIVE_SUCCESS_KEY = "message.transfer.receive_successfully";
    public static final String TRANSFER_FAILED_KEY = "message.transfer.transfer_failed";

    private TransferService() {
    }

    public static BalanceTransferResult execute(
            PlayerIdentity sender,
            TransferMessage message,
            TransferPort port) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(port, "port");

        Optional<PlayerIdentity> recipient = port.onlinePlayer(message.targetPlayerId());
        if (recipient.isEmpty()) {
            port.send(sender.id(), TRANSFER_FAILED_KEY);
            return BalanceTransferResult.TARGET_NOT_AVAILABLE;
        }

        PlayerIdentity target = recipient.orElseThrow();
        BalanceTransferResult result = port.transfer(
                sender.id(),
                target.id(),
                message.amount(),
                TRANSFER_CATEGORY,
                "赠与 " + target.name(),
                "来自 " + sender.name() + " 的赠与");
        if (result != BalanceTransferResult.SUCCESS) {
            port.send(sender.id(), TRANSFER_FAILED_KEY);
            return result;
        }

        port.send(sender.id(), TRANSFER_SUCCESS_KEY, message.amount(), target.name());
        port.send(target.id(), RECEIVE_SUCCESS_KEY, sender.name(), message.amount());
        return result;
    }

    public record PlayerIdentity(UUID id, String name) {
        public PlayerIdentity {
            Objects.requireNonNull(id, "id");
            name = Objects.requireNonNull(name, "name");
        }
    }

    /** Target adapter for online-player lookup, atomic account mutation and translated feedback. */
    public interface TransferPort {
        Optional<PlayerIdentity> onlinePlayer(UUID id);

        BalanceTransferResult transfer(
                UUID senderId,
                UUID recipientId,
                int amount,
                String category,
                String senderReason,
                String recipientReason);

        void send(UUID playerId, String translationKey, Object... arguments);
    }
}
