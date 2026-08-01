package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.*;

public record MarketDataResponseMessage(MarketDataResponseKind kind,long requestId,long marketRevision,int offset,int limit,
                                        int totalMatched,int totalSales,int totalDemand,List<MarketOrderSnapshot> orders)
        implements EconomyNetworkMessage {
    public MarketDataResponseMessage {
        Objects.requireNonNull(kind); orders=List.copyOf(Objects.requireNonNull(orders));
        long total=(long)totalSales+totalDemand;
        if(marketRevision<0||totalMatched<0||totalSales<0||totalDemand<0||totalMatched>total||total>EconomyNetworkLimits.MAX_MARKET_ORDERS)
            throw new IllegalArgumentException("invalid market totals");
        Set<UUID> ids=new HashSet<>();for(MarketOrderSnapshot order:orders){Objects.requireNonNull(order);if(!ids.add(order.tradeId()))throw new IllegalArgumentException("duplicate trade id");}
        switch(kind){
            case SUMMARY->{if(requestId<0||offset!=0||limit!=0||totalMatched!=0||!orders.isEmpty())throw new IllegalArgumentException("invalid summary");}
            case INVALIDATED->{if(requestId!=-1||offset!=0||limit!=0||totalMatched!=0||!orders.isEmpty())throw new IllegalArgumentException("invalid invalidation");}
            case PAGE->{if(requestId<0||offset<0||limit!=EconomyNetworkLimits.MAX_MARKET_PAGE_SIZE)throw new IllegalArgumentException("invalid page");int expected=offset>=totalMatched?0:Math.min(limit,totalMatched-offset);if(orders.size()!=expected)throw new IllegalArgumentException("incomplete page");}
        }
        MarketDataPayloadBudget.requireWithinLimit(new MarketDataResponseMessageUnchecked(kind,requestId,marketRevision,offset,limit,totalMatched,totalSales,totalDemand,orders));
    }
    public static MarketDataResponseMessage invalidated(long revision,int sales,int demand){return new MarketDataResponseMessage(MarketDataResponseKind.INVALIDATED,-1,revision,0,0,0,sales,demand,List.of());}
    record MarketDataResponseMessageUnchecked(MarketDataResponseKind kind,long requestId,long marketRevision,int offset,int limit,int totalMatched,int totalSales,int totalDemand,List<MarketOrderSnapshot> orders){}
}
