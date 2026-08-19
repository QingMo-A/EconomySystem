package com.mo.economy_system.common.api;

import com.mo.economy_system.api.account.EconomyAccountApi;
import com.mo.economy_system.api.market.EconomyMarketApi;
import com.mo.economy_system.api.territory.EconomyTerritoryApi;
import com.mo.economy_system.common.market.MarketOrder;
import com.mo.economy_system.common.market.MarketOrderType;
import com.mo.economy_system.common.territory.TerritorySnapshots;
import com.mo.economy_system.core.economy_system.BalanceLogPage;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import com.mo.economy_system.core.economy_system.BalanceTransferResult;
import java.util.Objects;

/** Internal mapping helpers from mutable implementation models to stable public API DTOs. */
public final class EconomyApiMappings {
  private EconomyApiMappings() {}

  public static EconomyAccountApi.MutationStatus mutation(BalanceMutationResult result) {
    return EconomyAccountApi.MutationStatus.valueOf(Objects.requireNonNull(result, "result").name());
  }

  public static EconomyAccountApi.TransferStatus transfer(BalanceTransferResult result) {
    return EconomyAccountApi.TransferStatus.valueOf(Objects.requireNonNull(result, "result").name());
  }

  public static EconomyAccountApi.LogPage logPage(BalanceLogPage page, String requestedFilter) {
    return new EconomyAccountApi.LogPage(
        page.logs().stream()
            .map(entry -> new EconomyAccountApi.LogEntry(
                entry.timeMillis(), entry.category(), entry.reason(), entry.delta(),
                entry.beforeBalance(), entry.afterBalance()))
            .toList(),
        requestedFilter == null ? "" : requestedFilter,
        page.offset(), page.limit(), page.total());
  }

  public static EconomyMarketApi.OrderView marketOrder(MarketOrder order) {
    return new EconomyMarketApi.OrderView(
        order.type() == MarketOrderType.SALES
            ? EconomyMarketApi.OrderType.SALES : EconomyMarketApi.OrderType.DEMAND,
        order.tradeId(), order.item().itemId(), order.quantity(), order.totalPrice(),
        order.sellerName(), order.sellerId(), order.listingTime(), order.expirationTime(), order.delivered());
  }

  public static EconomyTerritoryApi.TerritoryView territory(TerritorySnapshots.Owned owned) {
    TerritorySnapshots.Summary summary = owned.summary();
    return new EconomyTerritoryApi.TerritoryView(
        summary.territoryId(), summary.ownerId(), summary.ownerName(), summary.name(),
        new EconomyTerritoryApi.Position(summary.pos1().x(), summary.pos1().y(), summary.pos1().z()),
        new EconomyTerritoryApi.Position(summary.pos2().x(), summary.pos2().y(), summary.pos2().z()),
        summary.dimensionId(),
        owned.authorizedMembers().stream().map(TerritorySnapshots.Member::playerId).toList());
  }
}
