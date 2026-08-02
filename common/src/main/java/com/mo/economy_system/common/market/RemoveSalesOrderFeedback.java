package com.mo.economy_system.common.market;

public final class RemoveSalesOrderFeedback {
  private RemoveSalesOrderFeedback() {}

  public static String key(RemoveSalesOrderResult result) {
    return switch (result) {
      case SUCCESS -> "message.market.remove_sales.success";
      case NOT_FOUND -> "message.market.remove_sales.not_found";
      case WRONG_ORDER_TYPE -> "message.market.remove_sales.wrong_type";
      case NOT_OWNER -> "message.market.remove_sales.not_owner";
      case OWNER_OFFLINE -> "message.market.remove_sales.owner_offline";
      case INVENTORY_FULL -> "message.market.remove_sales.inventory_full";
      case ORDER_CHANGED -> "message.market.remove_sales.order_changed";
      case ORDER_REMOVE_FAILED -> "message.market.remove_sales.persist_failed";
      case ITEM_RESTORE_FAILED, INVALID_SNAPSHOT -> "message.market.remove_sales.item_failed";
      case ROLLBACK_FAILED -> "message.market.remove_sales.rollback_failed";
      default -> "message.market.remove_sales.failed";
    };
  }
}
