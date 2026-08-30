package com.mo.economy_system.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.api.account.EconomyAccountApi;
import com.mo.economy_system.api.mailbox.EconomyMailboxApi;
import com.mo.economy_system.api.market.EconomyMarketApi;
import com.mo.economy_system.api.territory.EconomyTerritoryApi;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PublicApiContractTest {
  @Test
  void transactionNotesRequireNamespacedSources() {
    EconomyAccountApi.TransactionNote note =
        EconomyAccountApi.TransactionNote.of("examplemod:quest_reward", "Quest complete");
    assertEquals("examplemod:quest_reward", note.source());

    assertThrows(IllegalArgumentException.class,
        () -> EconomyAccountApi.TransactionNote.of("quest_reward", "Quest complete"));
    assertThrows(IllegalArgumentException.class,
        () -> EconomyAccountApi.TransactionNote.of("ExampleMod:reward", "Quest complete"));
    assertThrows(IllegalArgumentException.class,
        () -> EconomyAccountApi.TransactionNote.of("examplemod:reward", ""));
  }

  @Test
  void mailDraftAndItemGrantValidatePublicBounds() {
    EconomyMailboxApi.MailDraft draft = EconomyMailboxApi.MailDraft.of(
        "examplemod:event_reward", "Event reward", "Thanks for playing");
    assertEquals("Event reward", draft.subject());
    EconomyMailboxApi.MailDraft paidDraft = EconomyMailboxApi.MailDraft.of(
        "examplemod:coin_reward", "Coin reward", "Thanks for playing", 250);
    assertEquals(250, paidDraft.moneyAmount());

    EconomyMailboxApi.MailItemGrant grant =
        EconomyMailboxApi.MailItemGrant.of("minecraft:diamond", 64);
    assertEquals(64, grant.count());

    assertThrows(IllegalArgumentException.class,
        () -> EconomyMailboxApi.MailDraft.of("event_reward", "Reward", "Body"));
    assertThrows(IllegalArgumentException.class,
        () -> EconomyMailboxApi.MailDraft.of("examplemod:reward", "", "Body"));
    assertThrows(IllegalArgumentException.class,
        () -> EconomyMailboxApi.MailDraft.of("examplemod:reward", "Reward", "Body", -1));
    assertThrows(IllegalArgumentException.class,
        () -> EconomyMailboxApi.MailItemGrant.of("diamond", 1));
    assertThrows(IllegalArgumentException.class,
        () -> EconomyMailboxApi.MailItemGrant.of("minecraft:diamond", 0));
  }

  @Test
  void marketOrderViewExposesStableDerivedHelpers() {
    UUID tradeId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    EconomyMarketApi.OrderView order = new EconomyMarketApi.OrderView(
        EconomyMarketApi.OrderType.SALES,
        tradeId,
        "minecraft:diamond",
        4,
        100,
        "Alice",
        ownerId,
        1_000L,
        2_000L,
        false);

    assertEquals(25.0D, order.unitPrice());
    assertFalse(order.expired(1_999L));
    assertTrue(order.expired(2_000L));
  }

  @Test
  void territoryViewCopiesMemberIdsAndUsesInclusiveBounds() {
    List<UUID> mutableMembers = new ArrayList<>();
    mutableMembers.add(UUID.randomUUID());
    EconomyTerritoryApi.TerritoryView territory = new EconomyTerritoryApi.TerritoryView(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "Owner",
        "Home",
        new EconomyTerritoryApi.Position(10, 60, 10),
        new EconomyTerritoryApi.Position(20, 80, 20),
        "minecraft:overworld",
        mutableMembers);

    mutableMembers.clear();
    assertEquals(1, territory.memberIds().size());
    assertTrue(territory.contains(10, 60, 10));
    assertTrue(territory.contains(20, 80, 20));
    assertFalse(territory.contains(21, 80, 20));
    assertThrows(UnsupportedOperationException.class,
        () -> territory.memberIds().add(UUID.randomUUID()));
  }

  @Test
  void capabilitiesAdvertiseExactlyTheV1Surface() {
    assertTrue(EconomyApiCapabilities.V1.accounts());
    assertTrue(EconomyApiCapabilities.V1.mailbox());
    assertTrue(EconomyApiCapabilities.V1.marketRead());
    assertTrue(EconomyApiCapabilities.V1.territoryRead());
  }
}
