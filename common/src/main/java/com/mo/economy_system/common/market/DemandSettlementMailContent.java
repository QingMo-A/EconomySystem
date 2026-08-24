package com.mo.economy_system.common.market;

import java.util.Objects;

/** Loader-neutral persisted text/source for a demand-order mailbox settlement. */
public record DemandSettlementMailContent(String source, String subject, String body) {
  public static final String SUBJECT = "求购订单已交付";
  private static final String SOURCE_PREFIX = "market.demand.fulfilled:";

  public DemandSettlementMailContent {
    Objects.requireNonNull(source, "source");
    Objects.requireNonNull(subject, "subject");
    Objects.requireNonNull(body, "body");
  }

  public static DemandSettlementMailContent create(
      MarketOrder fulfilledSlice, String supplierName, int amount, int remainingQuantity) {
    Objects.requireNonNull(fulfilledSlice, "fulfilledSlice");
    String safeSupplier = Objects.requireNonNullElse(supplierName, "");
    String unitPrice = MarketOrderPricing.exactUnitPrice(fulfilledSlice)
        .stream().mapToObj(Integer::toString).findFirst()
        .orElse(fulfilledSlice.totalPrice() + "/" + fulfilledSlice.quantity());
    String body = "供应者: " + safeSupplier
        + "\n交付数量: " + fulfilledSlice.quantity()
        + "\n单位价格: " + unitPrice
        + "\n本次支付: " + amount
        + "\n剩余求购: " + Math.max(0, remainingQuantity);
    return new DemandSettlementMailContent(
        SOURCE_PREFIX + fulfilledSlice.tradeId(), SUBJECT, body);
  }
}
