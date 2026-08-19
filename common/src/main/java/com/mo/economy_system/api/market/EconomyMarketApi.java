package com.mo.economy_system.api.market;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Stable read-only public view of EconomySystem's player market. */
public interface EconomyMarketApi {
  List<OrderView> orders();

  Optional<OrderView> order(UUID tradeId);

  List<OrderView> ordersByOwner(UUID ownerId);

  List<OrderView> ordersByType(OrderType type);

  enum OrderType {
    SALES,
    DEMAND
  }

  /** Immutable market snapshot. The public API intentionally does not expose internal item/NBT objects. */
  record OrderView(
      OrderType type,
      UUID tradeId,
      String itemId,
      int quantity,
      int totalPrice,
      String ownerName,
      UUID ownerId,
      long listingTime,
      long expirationTime,
      boolean delivered) {
    public OrderView {
      Objects.requireNonNull(type, "type");
      Objects.requireNonNull(tradeId, "tradeId");
      itemId = Objects.requireNonNull(itemId, "itemId");
      ownerName = Objects.requireNonNullElse(ownerName, "");
      Objects.requireNonNull(ownerId, "ownerId");
      if (quantity <= 0 || totalPrice <= 0) throw new IllegalArgumentException("invalid market amounts");
      if (expirationTime < listingTime) throw new IllegalArgumentException("invalid market timestamps");
    }

    public double unitPrice() {
      return totalPrice / (double) quantity;
    }

    public boolean expired(long nowEpochMillis) {
      return nowEpochMillis >= expirationTime;
    }
  }
}
