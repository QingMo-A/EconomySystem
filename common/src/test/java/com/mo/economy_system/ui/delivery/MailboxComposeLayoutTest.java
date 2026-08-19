package com.mo.economy_system.ui.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.network.PlayerSummary;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MailboxComposeLayoutTest {
  @Test
  void composeUsesVerticalFormAndNineColumnInventory() {
    MailboxComposeLayout.Layout layout = MailboxComposeLayout.calculate(640, 360, null);

    assertTrue(layout.formPanel().bottom() < layout.inventoryPanel().y());
    assertEquals(420, layout.formPanel().width());
    assertEquals((640 - layout.formPanel().width()) / 2, layout.formPanel().x());
    assertEquals(layout.formPanel().x(), layout.inventoryPanel().x());
    assertEquals(layout.formPanel().width(), layout.inventoryPanel().width());
    assertEquals(36, layout.slots().size());
    for (MailboxComposeLayout.Slot slot : layout.slots()) {
      assertTrue(layout.inventoryPanel().contains(slot.rect()));
    }
    assertEquals(layout.slots().get(0).rect().y(), layout.slots().get(8).rect().y());
    assertEquals(layout.slots().get(0).rect().x(), layout.slots().get(9).rect().x());
    assertTrue(layout.slots().get(9).rect().y() > layout.slots().get(0).rect().y());
    assertTrue(layout.sendButton().y() >= layout.inventoryPanel().bottom());
  }

  @Test
  void recipientCompletionMatchesNameAndUuidAndExcludesSelf() {
    UUID self = UUID.fromString("11111111-1111-1111-1111-111111111111");
    UUID aliceId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    MailboxRecipientCompletion completion = new MailboxRecipientCompletion();
    List<PlayerSummary> players = List.of(
        new PlayerSummary(self, "SelfPlayer"),
        new PlayerSummary(aliceId, "Alice"),
        new PlayerSummary(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"), "Bob"));

    assertEquals(List.of("Alice"), completion.suggestions(players, "ali", self).stream()
        .map(PlayerSummary::playerName).toList());
    assertEquals(List.of("Alice"), completion.suggestions(players, "aaaaaaaa", self).stream()
        .map(PlayerSummary::playerName).toList());
    assertFalse(completion.suggestions(players, "self", self).stream()
        .anyMatch(player -> player.playerId().equals(self)));
  }
}
