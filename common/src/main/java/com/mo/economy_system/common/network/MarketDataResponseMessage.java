package com.mo.economy_system.common.network;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.*;

public record MarketDataResponseMessage(MarketDataResponseKind kind,long requestId,int offset,int limit,
                                        int totalMatched,int totalSales,int totalDemand,List<MarketOrderSnapshot> orders)
        implements EconomyNetworkMessage {
    public MarketDataResponseMessage {
        Objects.requireNonNull(kind); orders=List.copyOf(Objects.requireNonNull(orders));
        long total=(long)totalSales+totalDemand;
        if(totalMatched<0||totalSales<0||totalDemand<0||total>EconomyNetworkLimits.MAX_MARKET_ORDERS)
            throw new IllegalArgumentException("invalid market totals");
        if(kind==MarketDataResponseKind.INVALIDATED){if(requestId!=-1||!orders.isEmpty())throw new IllegalArgumentException("invalid invalidation");}
        else if(requestId<0)throw new IllegalArgumentException("invalid request id");
        if(kind==MarketDataResponseKind.SUMMARY && (!orders.isEmpty()||offset!=0||limit!=0))throw new IllegalArgumentException("invalid summary");
        if(kind==MarketDataResponseKind.PAGE){
            if(offset<0||limit<1||limit>EconomyNetworkLimits.MAX_MARKET_PAGE_SIZE||orders.size()>limit
                    ||orders.size()>totalMatched||(!orders.isEmpty()&&(long)offset+orders.size()>totalMatched))throw new IllegalArgumentException("invalid page");
            Set<UUID> ids=new HashSet<>(); for(MarketOrderSnapshot order:orders)if(!ids.add(order.tradeId()))throw new IllegalArgumentException("duplicate trade id");
        } else if(!orders.isEmpty()) throw new IllegalArgumentException("orders only allowed on page");
    }
    public static MarketDataResponseMessage invalidated(int sales,int demand){return new MarketDataResponseMessage(MarketDataResponseKind.INVALIDATED,-1,0,0,0,sales,demand,List.of());}
}
