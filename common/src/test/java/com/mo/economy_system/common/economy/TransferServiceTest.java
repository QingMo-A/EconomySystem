package com.mo.economy_system.common.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mo.economy_system.common.network.TransferMessage;
import com.mo.economy_system.core.economy_system.BalanceTransferResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransferServiceTest {
  private static final UUID SENDER = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID RECIPIENT = UUID.fromString("00000000-0000-0000-0000-000000000002");

  @Test
  void successfulTransferUsesThePortForMutationAndFeedback() {
    FakePort port = new FakePort(BalanceTransferResult.SUCCESS);

    BalanceTransferResult result = TransferService.execute(
        new TransferService.PlayerIdentity(SENDER, "sender"),
        new TransferMessage(RECIPIENT, 12),
        port);

    assertEquals(BalanceTransferResult.SUCCESS, result);
    assertEquals("sender->recipient:12", port.transferCall);
    assertEquals(List.of(
        "sender:" + TransferService.TRANSFER_SUCCESS_KEY,
        "recipient:" + TransferService.RECEIVE_SUCCESS_KEY), port.messages);
  }

  @Test
  void unavailableRecipientFailsBeforeCallingTheAccountPort() {
    FakePort port = new FakePort(BalanceTransferResult.SUCCESS);
    port.recipientPresent = false;

    BalanceTransferResult result = TransferService.execute(
        new TransferService.PlayerIdentity(SENDER, "sender"),
        new TransferMessage(RECIPIENT, 12),
        port);

    assertEquals(BalanceTransferResult.TARGET_NOT_AVAILABLE, result);
    assertEquals(null, port.transferCall);
    assertEquals(List.of("sender:" + TransferService.TRANSFER_FAILED_KEY), port.messages);
  }

  private static final class FakePort implements TransferService.TransferPort {
    private final BalanceTransferResult transferResult;
    private final List<String> messages = new ArrayList<>();
    private boolean recipientPresent = true;
    private String transferCall;

    private FakePort(BalanceTransferResult transferResult) {
      this.transferResult = transferResult;
    }

    @Override
    public Optional<TransferService.PlayerIdentity> onlinePlayer(UUID id) {
      return recipientPresent && RECIPIENT.equals(id)
          ? Optional.of(new TransferService.PlayerIdentity(RECIPIENT, "recipient"))
          : Optional.empty();
    }

    @Override
    public BalanceTransferResult transfer(
        UUID senderId, UUID recipientId, int amount, String category,
        String senderReason, String recipientReason) {
      transferCall = "sender->recipient:" + amount;
      return transferResult;
    }

    @Override
    public void send(UUID playerId, String translationKey, Object... arguments) {
      messages.add((SENDER.equals(playerId) ? "sender" : "recipient") + ":" + translationKey);
    }
  }
}
