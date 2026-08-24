package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.Objects;
import java.util.UUID;

public record MarketDataRequestMessage(
    long requestId,
    MarketDataRequestPurpose purpose,
    int offset,
    int limit,
    MarketOrderFilter filter,
    MarketOrderSort sort,
    String query,
    UUID focusTradeId) implements EconomyNetworkMessage {

  public MarketDataRequestMessage {
    Objects.requireNonNull(purpose);
    Objects.requireNonNull(filter);
    Objects.requireNonNull(sort);
    query = Objects.requireNonNull(query).trim();
    if (requestId < 0 || query.length() > EconomyNetworkLimits.MAX_MARKET_QUERY_LENGTH) {
      throw new IllegalArgumentException("invalid market request");
    }
    if (purpose == MarketDataRequestPurpose.SUMMARY) {
      if (offset != 0 || limit != 0 || filter != MarketOrderFilter.ALL
          || sort != MarketOrderSort.DEFAULT || !query.isEmpty() || focusTradeId != null) {
        throw new IllegalArgumentException("invalid summary request");
      }
    } else if (offset < 0 || limit != EconomyNetworkLimits.MAX_MARKET_PAGE_SIZE) {
      throw new IllegalArgumentException("invalid page request");
    }
  }

  /** Compatibility constructor for existing callers; uses no selection focus. */
  public MarketDataRequestMessage(
      long requestId,
      MarketDataRequestPurpose purpose,
      int offset,
      int limit,
      MarketOrderFilter filter,
      MarketOrderSort sort,
      String query) {
    this(requestId, purpose, offset, limit, filter, sort, query, null);
  }

  /** Compatibility constructor for existing callers; uses DEFAULT ordering and no selection focus. */
  public MarketDataRequestMessage(
      long requestId,
      MarketDataRequestPurpose purpose,
      int offset,
      int limit,
      MarketOrderFilter filter,
      String query) {
    this(requestId, purpose, offset, limit, filter, MarketOrderSort.DEFAULT, query, null);
  }

  public static MarketDataRequestMessage summary(long id) {
    return new MarketDataRequestMessage(id, MarketDataRequestPurpose.SUMMARY, 0, 0,
        MarketOrderFilter.ALL, MarketOrderSort.DEFAULT, "", null);
  }
}
