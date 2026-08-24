package com.mo.economy_system.common.market;

import com.mo.economy_system.common.network.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Server-side market query pipeline: filter -> search -> sort -> page. */
public final class MarketDataQueryService {
  private MarketDataQueryService() {}

  public static MarketDataResponseMessage query(
      MarketLedgerView view, UUID requester, MarketDataRequestMessage request) {
    Objects.requireNonNull(view);
    Objects.requireNonNull(requester);
    Objects.requireNonNull(request);
    List<MarketOrder> orders = view.orders();

    int sales = 0;
    int demand = 0;
    for (MarketOrder order : orders) {
      if (order.type() == MarketOrderType.SALES) sales++;
      else demand++;
    }
    if (request.purpose() == MarketDataRequestPurpose.SUMMARY) {
      return new MarketDataResponseMessage(MarketDataResponseKind.SUMMARY, request.requestId(),
          view.revision(), 0, 0, 0, sales, demand, List.of());
    }

    String query = request.query().toLowerCase(Locale.ROOT);
    List<MarketOrder> matched = new ArrayList<>();
    for (MarketOrder order : orders) {
      boolean filter = switch (request.filter()) {
        case ALL -> true;
        case MINE -> order.sellerId().equals(requester);
        case SALES -> order.type() == MarketOrderType.SALES;
        case DEMAND -> order.type() == MarketOrderType.DEMAND;
      };
      if (!filter) continue;
      if (!query.isEmpty()
          && !order.item().itemId().toLowerCase(Locale.ROOT).contains(query)
          && !order.sellerName().toLowerCase(Locale.ROOT).contains(query)
          && !order.tradeId().toString().toLowerCase(Locale.ROOT).contains(query)) {
        continue;
      }
      matched.add(order);
    }

    matched.sort(comparator(request.sort()));
    int total = matched.size();
    int effectiveOffset = request.offset();
    if (request.focusTradeId() != null) {
      for (int index = 0; index < matched.size(); index++) {
        if (matched.get(index).tradeId().equals(request.focusTradeId())) {
          effectiveOffset = (index / request.limit()) * request.limit();
          break;
        }
      }
    }
    if (total == 0) {
      effectiveOffset = 0;
    } else if (effectiveOffset >= total) {
      effectiveOffset = ((total - 1) / request.limit()) * request.limit();
    }
    int from = Math.min(effectiveOffset, total);
    int to = (int) Math.min((long) from + request.limit(), total);
    List<MarketOrderSnapshot> page = matched.subList(from, to).stream()
        .map(MarketOrderSnapshot::from)
        .toList();
    return new MarketDataResponseMessage(MarketDataResponseKind.PAGE, request.requestId(),
        view.revision(), effectiveOffset, request.limit(), total, sales, demand, page);
  }

  @Deprecated
  public static MarketDataResponseMessage query(
      List<MarketOrder> source, UUID requester, MarketDataRequestMessage request) {
    return query(new MarketLedgerView(0, source), requester, request);
  }

  static Comparator<MarketOrder> comparator(MarketOrderSort sort) {
    Objects.requireNonNull(sort, "sort");
    return switch (sort) {
      case DEFAULT -> Comparator.comparingLong(MarketOrder::listingTime)
          .thenComparing(MarketOrder::tradeId);
      case UNIT_PRICE_ASC -> MarketDataQueryService::compareUnitPriceAsc;
      case UNIT_PRICE_DESC -> (left, right) -> compareUnitPriceDesc(left, right);
      case NEWEST -> Comparator.comparingLong(MarketOrder::listingTime).reversed()
          .thenComparing(MarketOrder::tradeId);
      case EXPIRING_SOON -> Comparator.comparingLong(MarketOrder::expirationTime)
          .thenComparing(MarketOrder::tradeId);
    };
  }

  private static int compareUnitPriceAsc(MarketOrder left, MarketOrder right) {
    long leftScaled = (long) left.totalPrice() * right.quantity();
    long rightScaled = (long) right.totalPrice() * left.quantity();
    int price = Long.compare(leftScaled, rightScaled);
    if (price != 0) return price;
    int newestTie = Long.compare(right.listingTime(), left.listingTime());
    return newestTie != 0 ? newestTie : left.tradeId().compareTo(right.tradeId());
  }

  private static int compareUnitPriceDesc(MarketOrder left, MarketOrder right) {
    long leftScaled = (long) left.totalPrice() * right.quantity();
    long rightScaled = (long) right.totalPrice() * left.quantity();
    int price = Long.compare(rightScaled, leftScaled);
    if (price != 0) return price;
    int newestTie = Long.compare(right.listingTime(), left.listingTime());
    return newestTie != 0 ? newestTie : left.tradeId().compareTo(right.tradeId());
  }
}
