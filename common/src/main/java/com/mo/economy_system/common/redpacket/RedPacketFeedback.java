package com.mo.economy_system.common.redpacket;

/** Stable translation keys and result-to-message policy shared by both targets. */
public final class RedPacketFeedback {
  public static final String INSUFFICIENT_BALANCE = "message.red_packet.insufficient_balance";
  public static final String ALREADY_ACTIVE = "message.red_packet.already_active";
  public static final String CREATED = "message.red_packet.created_successfully";
  public static final String NO_AVAILABLE = "message.red_packet.no_available";
  public static final String ALREADY_CLAIMED = "message.red_packet.already_claimed";
  public static final String CLAIM_SUCCESS = "message.red_packet.claim_success";
  public static final String CLAIM_BUTTON = "message.red_packet.claim_button";
  public static final String CLAIM_HOVER = "message.red_packet.claim_hover";
  public static final String BROADCAST = "message.red_packet.broadcast";
  public static final String NO_ACTIVE = "message.red_packet.no_active";
  public static final String CANCELLED = "message.red_packet.cancelled";
  public static final String FULLY_CLAIMED = "message.red_packet.fully_claimed";
  public static final String EXPIRED_REFUNDED = "message.red_packet.expired_refunded";
  public static final String EXPIRED_BROADCAST = "message.red_packet.expired_broadcast";
  public static final String CLAIM_BROADCAST = "message.red_packet.claim_broadcast";
  public static final String BALANCE_LIMIT = "message.red_packet.balance_limit";
  public static final String TRANSACTION_FAILED = "message.red_packet.transaction_failed";
  public static final String STATE_UNKNOWN = "message.red_packet.state_unknown";

  private RedPacketFeedback() {}

  public static String createFailureKey(RedPacketService.CreateResult result) {
    return switch (result) {
      case ALREADY_ACTIVE -> ALREADY_ACTIVE;
      case INSUFFICIENT_FUNDS -> INSUFFICIENT_BALANCE;
      case INVALID_AMOUNT, INVALID_DURATION, INVALID_PLAYER_COUNT -> TRANSACTION_FAILED;
      case PERSIST_FAILED -> TRANSACTION_FAILED;
      case STATE_UNKNOWN -> STATE_UNKNOWN;
      case SUCCESS -> CREATED;
    };
  }

  public static String claimFailureKey(RedPacketService.ClaimResult result) {
    return switch (result) {
      case NO_AVAILABLE -> NO_AVAILABLE;
      case ALREADY_CLAIMED -> ALREADY_CLAIMED;
      case BALANCE_LIMIT -> BALANCE_LIMIT;
      case PERSIST_FAILED -> TRANSACTION_FAILED;
      case STATE_UNKNOWN -> STATE_UNKNOWN;
      case SUCCESS -> CLAIM_SUCCESS;
    };
  }

  public static String cancelFailureKey(RedPacketService.CancelResult result) {
    return switch (result) {
      case NO_ACTIVE -> NO_ACTIVE;
      case BALANCE_LIMIT -> BALANCE_LIMIT;
      case PERSIST_FAILED -> TRANSACTION_FAILED;
      case STATE_UNKNOWN -> STATE_UNKNOWN;
      case SUCCESS -> CANCELLED;
    };
  }
}
