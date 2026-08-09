package com.mo.economy_system.ui.shop;

import com.mo.economy_system.common.network.ShopItemSnapshot;
import java.util.Objects;

/** One immutable shop card backed by the server snapshot. */
public record ShopRow(ShopItemSnapshot item) {
  public ShopRow {
    Objects.requireNonNull(item, "item");
  }
}
