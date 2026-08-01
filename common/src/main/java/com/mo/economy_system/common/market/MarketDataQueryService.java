package com.mo.economy_system.common.market;

import com.mo.economy_system.common.network.*;
import java.util.*;

public final class MarketDataQueryService {
    private MarketDataQueryService(){}
    public static MarketDataResponseMessage query(MarketLedgerView view,UUID requester,MarketDataRequestMessage request){
        Objects.requireNonNull(view); List<MarketOrder> orders=view.orders(); Objects.requireNonNull(requester); Objects.requireNonNull(request);
        int sales=0,demand=0; for(MarketOrder o:orders)if(o.type()==MarketOrderType.SALES)sales++;else demand++;
        if(request.purpose()==MarketDataRequestPurpose.SUMMARY)return new MarketDataResponseMessage(MarketDataResponseKind.SUMMARY,request.requestId(),view.revision(),0,0,0,sales,demand,List.of());
        String q=request.query().toLowerCase(Locale.ROOT); List<MarketOrderSnapshot> matched=new ArrayList<>();
        for(MarketOrder o:orders){boolean filter=switch(request.filter()){case ALL->true;case MINE->o.sellerId().equals(requester);case SALES->o.type()==MarketOrderType.SALES;case DEMAND->o.type()==MarketOrderType.DEMAND;};
            if(filter&&(q.isEmpty()||o.item().itemId().toLowerCase(Locale.ROOT).contains(q)||o.sellerName().toLowerCase(Locale.ROOT).contains(q)))matched.add(MarketOrderSnapshot.from(o));}
        int total=matched.size(),from=Math.min(request.offset(),total),to=(int)Math.min((long)from+request.limit(),total);
        return new MarketDataResponseMessage(MarketDataResponseKind.PAGE,request.requestId(),view.revision(),request.offset(),request.limit(),total,sales,demand,matched.subList(from,to));
    }
    @Deprecated public static MarketDataResponseMessage query(List<MarketOrder> source,UUID requester,MarketDataRequestMessage request){return query(new MarketLedgerView(0,source),requester,request);}
}
