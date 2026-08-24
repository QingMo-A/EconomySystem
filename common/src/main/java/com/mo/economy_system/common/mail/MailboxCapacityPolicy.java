package com.mo.economy_system.common.mail;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.util.Objects;

/**
 * Shared mailbox capacity policy.
 *
 * <p>The physical limits remain the persistence/wire hard limits. Player-to-player mail uses a
 * lower soft cap so ordinary mail cannot consume the final slots required by critical economic
 * deliveries such as MARKET, COMPENSATION and SYSTEM mail.</p>
 */
public final class MailboxCapacityPolicy {
  /** Metadata slots kept out of reach of ordinary player-to-player mail. */
  public static final int RESERVED_CRITICAL_MAIL_SLOTS =
      Math.min(16, EconomyNetworkLimits.MAX_MAILS_PER_PLAYER);

  /**
   * Delivery entries kept out of reach of ordinary player-to-player mail. One maximum-size market
   * mail can therefore still be staged after the player-mail soft cap is reached.
   */
  public static final int RESERVED_CRITICAL_DELIVERY_SLOTS =
      Math.min(EconomyNetworkLimits.MAX_MAIL_ATTACHMENTS,
          EconomyNetworkLimits.MAX_DELIVERY_BOX_ENTRIES);

  public static final int PLAYER_MAIL_LIMIT =
      EconomyNetworkLimits.MAX_MAILS_PER_PLAYER - RESERVED_CRITICAL_MAIL_SLOTS;

  public static final int PLAYER_DELIVERY_BOX_LIMIT =
      EconomyNetworkLimits.MAX_DELIVERY_BOX_ENTRIES - RESERVED_CRITICAL_DELIVERY_SLOTS;

  private MailboxCapacityPolicy() {}

  public static boolean critical(MailType type) {
    Objects.requireNonNull(type, "type");
    return type == MailType.MARKET || type == MailType.COMPENSATION || type == MailType.SYSTEM;
  }

  public static int personalLimit(MailType type) {
    Objects.requireNonNull(type, "type");
    return critical(type) ? EconomyNetworkLimits.MAX_MAILS_PER_PLAYER : PLAYER_MAIL_LIMIT;
  }

  public static boolean canAddPersonal(MailType type, int currentCount) {
    if (currentCount < 0) throw new IllegalArgumentException("currentCount");
    return currentCount < personalLimit(type);
  }

  public static boolean canAddPlayerDeliveries(int currentCount, int additions) {
    return canAddDeliveries(currentCount, additions, false);
  }

  public static boolean canAddCriticalDeliveries(int currentCount, int additions) {
    return canAddDeliveries(currentCount, additions, true);
  }

  private static boolean canAddDeliveries(int currentCount, int additions, boolean critical) {
    if (currentCount < 0 || additions < 0) throw new IllegalArgumentException("delivery count");
    int limit = critical ? EconomyNetworkLimits.MAX_DELIVERY_BOX_ENTRIES : PLAYER_DELIVERY_BOX_LIMIT;
    return currentCount <= limit && additions <= limit - currentCount;
  }
}
