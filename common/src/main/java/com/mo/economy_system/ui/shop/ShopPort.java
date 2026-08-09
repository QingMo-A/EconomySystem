package com.mo.economy_system.ui.shop;

/** Target adapter for network sends and target-specific purchase UI. */
public interface ShopPort {
  long nextRequestId();

  void requestCatalog(long requestId);

  void submit(ShopAction action, ShopRow row, int quantity);

  /** Opens the shared quantity confirmation before sending the purchase intent. */
  default void confirm(ShopRow row) {
    submit(ShopAction.BUY, row, 1);
  }
}
