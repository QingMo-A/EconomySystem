package com.mo.economy_system.common.market;

import java.util.Objects;

/** Stable, explicit translation routing for demand-order delivery outcomes. */
public final class DemandOrderDeliveryFeedback {
  private DemandOrderDeliveryFeedback() {}

  public static String translationKey(DemandOrderDeliveryResult result) {
    Objects.requireNonNull(result, "result");
    return switch (result) {
      case SUCCESS -> "message.market.deliver_demand.success";
      case NOT_FOUND -> "message.market.deliver_demand.not_found";
      case WRONG_ORDER_TYPE -> "message.market.deliver_demand.wrong_type";
      case ALREADY_DELIVERED -> "message.market.deliver_demand.already_delivered";
      case SELF_DELIVERY -> "message.market.deliver_demand.self";
      case INVALID_PRICE, INVALID_QUANTITY, INVALID_SNAPSHOT, INVALID_CONTEXT ->
          "message.market.deliver_demand.invalid_order";
      case ITEM_RESTORE_FAILED -> "message.market.deliver_demand.item_failed";
      case INVENTORY_CONTEXT_FAILED, INVENTORY_MUTATION_FAILED ->
          "message.market.deliver_demand.inventory_failed";
      case INSUFFICIENT_ITEMS -> "message.market.deliver_demand.insufficient_items";
      case RECIPIENT_BALANCE_LIMIT -> "message.market.deliver_demand.balance_limit";
      case PARTIAL_FILL_UNSUPPORTED -> "message.market.deliver_demand.whole_only";
      case MAILBOX_FULL -> "message.market.deliver_demand.mailbox_full";
      case MAILBOX_DELIVERY_FAILED -> "message.market.deliver_demand.mailbox_failed";
      case MAILBOX_STATE_UNKNOWN -> "message.market.deliver_demand.state_unknown";
      case PAYMENT_FAILED -> "message.market.deliver_demand.payment_failed";
      case PAYMENT_STATE_UNKNOWN -> "message.market.deliver_demand.state_unknown";
      case ORDER_CHANGED -> "message.market.deliver_demand.order_changed";
      case LEDGER_UPDATE_FAILED -> "message.market.deliver_demand.persist_failed";
      case LEDGER_STATE_UNKNOWN -> "message.market.deliver_demand.state_unknown";
      case ROLLBACK_FAILED -> "message.market.deliver_demand.rollback_failed";
    };
  }
}
