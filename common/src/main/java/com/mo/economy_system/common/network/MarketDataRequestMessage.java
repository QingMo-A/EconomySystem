package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.Objects;

public record MarketDataRequestMessage(long requestId, MarketDataRequestPurpose purpose, int offset,
                                       int limit, MarketOrderFilter filter, String query)
        implements EconomyNetworkMessage {
    public MarketDataRequestMessage {
        Objects.requireNonNull(purpose); Objects.requireNonNull(filter);
        query = Objects.requireNonNull(query).trim();
        if (requestId < 0 || query.length() > EconomyNetworkLimits.MAX_MARKET_QUERY_LENGTH)
            throw new IllegalArgumentException("invalid market request");
        if (purpose == MarketDataRequestPurpose.SUMMARY) {
            if (offset != 0 || limit != 0 || filter != MarketOrderFilter.ALL || !query.isEmpty())
                throw new IllegalArgumentException("invalid summary request");
        } else if (offset < 0 || limit < 1 || limit > EconomyNetworkLimits.MAX_MARKET_PAGE_SIZE) {
            throw new IllegalArgumentException("invalid page request");
        }
    }
    public static MarketDataRequestMessage summary(long id) {
        return new MarketDataRequestMessage(id, MarketDataRequestPurpose.SUMMARY, 0, 0, MarketOrderFilter.ALL, "");
    }
}
