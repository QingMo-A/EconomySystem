package com.mo.economy_system.ui.delivery;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.MailboxSendPlayerMessage;
import com.mo.economy_system.common.network.MailboxSendStatus;
import com.mo.economy_system.ui.core.UiNavigation;
import java.util.List;
import org.junit.jupiter.api.Test;

class MailboxComposeControllerTest {
  @Test void selectionIsBoundedAndDropsSlotsThatBecomeEmpty() {
    FakePort port = new FakePort();
    List<MailboxComposeInventoryItem> inventory = java.util.stream.IntStream.range(0, 8)
        .mapToObj(slot -> new MailboxComposeInventoryItem(slot, "minecraft:stone", 1)).toList();
    MailboxComposeController controller = new MailboxComposeController(inventory, port);
    for (int slot = 0; slot < 8; slot++) controller.handle(new MailboxComposeEvent.SlotToggled(slot));
    assertEquals(com.mo.economy_system.common.network.EconomyNetworkLimits.MAX_PLAYER_MAIL_ATTACHMENTS,
        controller.state().selectedSlots().size());
    controller.handle(new MailboxComposeEvent.InventoryChanged(inventory.subList(1, inventory.size())));
    assertFalse(controller.state().selectedSlots().contains(0));
  }

  @Test void invalidContentDoesNotSendAndValidSubmissionIsOneShot() {
    FakePort port = new FakePort();
    MailboxComposeController controller = new MailboxComposeController(List.of(), port);
    controller.handle(new MailboxComposeEvent.ActionClicked(MailboxComposeAction.SEND));
    assertEquals(MailboxSendStatus.INVALID_CONTENT, controller.state().status());
    assertEquals(0, port.calls);
    controller.handle(new MailboxComposeEvent.RecipientChanged("Player"));
    controller.handle(new MailboxComposeEvent.SubjectChanged("Hello"));
    controller.handle(new MailboxComposeEvent.ActionClicked(MailboxComposeAction.SEND));
    controller.handle(new MailboxComposeEvent.ActionClicked(MailboxComposeAction.SEND));
    assertEquals(1, port.calls);
    assertTrue(controller.state().sending());
  }

  @Test void staleResultsAreIgnoredAndSuccessCloses() {
    FakePort port = new FakePort();
    MailboxComposeController controller = new MailboxComposeController(List.of(), port);
    controller.handle(new MailboxComposeEvent.RecipientChanged("Player"));
    controller.handle(new MailboxComposeEvent.BodyChanged("Body"));
    controller.handle(new MailboxComposeEvent.ActionClicked(MailboxComposeAction.SEND));
    controller.handle(new MailboxComposeEvent.SendResult(1, 99, MailboxSendStatus.FAILED));
    assertTrue(controller.state().sending());
    controller.handle(new MailboxComposeEvent.SendResult(1, 7, MailboxSendStatus.SUCCESS));
    assertFalse(controller.state().sending());
    assertInstanceOf(UiNavigation.Back.class, controller.pollNavigation().orElseThrow());
  }

  @Test void moneyAmountIsIncludedInTheSubmittedPlayerMail() {
    FakePort port = new FakePort();
    MailboxComposeController controller = new MailboxComposeController(List.of(), port);
    controller.handle(new MailboxComposeEvent.RecipientChanged("Player"));
    controller.handle(new MailboxComposeEvent.SubjectChanged("Payment"));
    controller.handle(new MailboxComposeEvent.BodyChanged("Here are the coins"));
    controller.handle(new MailboxComposeEvent.MoneyChanged("250"));
    controller.handle(new MailboxComposeEvent.ActionClicked(MailboxComposeAction.SEND));

    assertEquals(1, port.calls);
    assertNotNull(port.message);
    assertEquals(250, port.message.moneyAmount());
  }

  @Test void malformedMoneyAmountIsRejectedBeforeSending() {
    FakePort port = new FakePort();
    MailboxComposeController controller = new MailboxComposeController(List.of(), port);
    controller.handle(new MailboxComposeEvent.RecipientChanged("Player"));
    controller.handle(new MailboxComposeEvent.SubjectChanged("Payment"));
    controller.handle(new MailboxComposeEvent.BodyChanged("Body"));
    controller.handle(new MailboxComposeEvent.MoneyChanged("12x"));
    controller.handle(new MailboxComposeEvent.ActionClicked(MailboxComposeAction.SEND));

    assertEquals(0, port.calls);
    assertEquals(MailboxSendStatus.INVALID_CONTENT, controller.state().status());
    assertFalse(controller.state().sending());
  }

  private static final class FakePort implements MailboxComposePort {
    private int calls;
    private MailboxSendPlayerMessage message;
    @Override public long nextRequestId() { return 7; }
    @Override public void send(MailboxSendPlayerMessage message) {
      calls++;
      this.message = message;
    }
  }
}
