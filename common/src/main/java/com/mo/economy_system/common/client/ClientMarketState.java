package com.mo.economy_system.common.client;

import com.mo.economy_system.common.network.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class ClientMarketState {
    private static final AtomicLong IDS=new AtomicLong();
    private static final AtomicReference<Snapshot> CURRENT=new AtomicReference<>(new Snapshot(0,0,List.of(),0,0,0,-1,false));
    private ClientMarketState(){}
    public static long nextPageRequestId(){long id=IDS.incrementAndGet(); CURRENT.updateAndGet(s->new Snapshot(s.totalSales,s.totalDemand,s.orders,s.totalMatched,s.offset,s.limit,id,s.stale));return id;}
    public static Snapshot snapshot(){return CURRENT.get();}
    public static boolean apply(MarketDataResponseMessage m){Objects.requireNonNull(m);return switch(m.kind()){
        case SUMMARY->{CURRENT.updateAndGet(s->new Snapshot(m.totalSales(),m.totalDemand(),s.orders,s.totalMatched,s.offset,s.limit,s.latestRequestId,s.stale));yield true;}
        case INVALIDATED->{CURRENT.updateAndGet(s->new Snapshot(m.totalSales(),m.totalDemand(),s.orders,s.totalMatched,s.offset,s.limit,s.latestRequestId,true));yield true;}
        case PAGE->{if(m.requestId()<CURRENT.get().latestRequestId)yield false;IDS.accumulateAndGet(m.requestId(),Math::max);CURRENT.set(new Snapshot(m.totalSales(),m.totalDemand(),m.orders(),m.totalMatched(),m.offset(),m.limit(),m.requestId(),false));yield true;}
    };}
    public record Snapshot(int totalSales,int totalDemand,List<MarketOrderSnapshot> orders,int totalMatched,int offset,int limit,long latestRequestId,boolean stale){public Snapshot{orders=List.copyOf(orders);}}
}
