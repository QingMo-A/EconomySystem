package com.mo.economy_system.common.mail;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import org.junit.jupiter.api.Test;

class MailboxCapacityPolicyTest {
  @Test
  void playerDeliverySoftCapPreservesOneMaximumCriticalMailTail() {
    assertTrue(MailboxCapacityPolicy.canAddPlayerDeliveries(
        MailboxCapacityPolicy.PLAYER_DELIVERY_BOX_LIMIT - 1, 1));
    assertFalse(MailboxCapacityPolicy.canAddPlayerDeliveries(
        MailboxCapacityPolicy.PLAYER_DELIVERY_BOX_LIMIT, 1));

    assertTrue(MailboxCapacityPolicy.canAddCriticalDeliveries(
        MailboxCapacityPolicy.PLAYER_DELIVERY_BOX_LIMIT,
        EconomyNetworkLimits.MAX_MAIL_ATTACHMENTS));
    assertFalse(MailboxCapacityPolicy.canAddCriticalDeliveries(
        EconomyNetworkLimits.MAX_DELIVERY_BOX_ENTRIES, 1));
  }

  @Test
  void systemMarketAndCompensationUseHardMetadataLimitButPlayerDoesNot() {
    assertFalse(MailboxCapacityPolicy.canAddPersonal(
        MailType.PLAYER, MailboxCapacityPolicy.PLAYER_MAIL_LIMIT));
    assertTrue(MailboxCapacityPolicy.canAddPersonal(
        MailType.MARKET, MailboxCapacityPolicy.PLAYER_MAIL_LIMIT));
    assertTrue(MailboxCapacityPolicy.canAddPersonal(
        MailType.COMPENSATION, MailboxCapacityPolicy.PLAYER_MAIL_LIMIT));
    assertTrue(MailboxCapacityPolicy.canAddPersonal(
        MailType.SYSTEM, MailboxCapacityPolicy.PLAYER_MAIL_LIMIT));
    assertFalse(MailboxCapacityPolicy.canAddPersonal(
        MailType.MARKET, EconomyNetworkLimits.MAX_MAILS_PER_PLAYER));
  }
}
