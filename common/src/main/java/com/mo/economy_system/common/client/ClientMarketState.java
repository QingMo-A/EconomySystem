package com.mo.economy_system.common.client;

import com.mo.economy_system.common.network.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public final class ClientMarketState {
    private static final AtomicLong IDS=new AtomicLong();
    private static Snapshot current=empty();
    private ClientMarketState(){}
    private static Snapshot empty(){return new Snapshot(0,0,0,List.of(),0,0,EconomyNetworkLimits.MAX_MARKET_PAGE_SIZE,-1,-1,false,false,"");}
    public static synchronized long nextPageRequestId(){long id=IDS.incrementAndGet();current=current.withRequests(id,current.latestSummaryRequestId,true);return id;}
    public static synchronized long nextSummaryRequestId(){long id=IDS.incrementAndGet();current=current.withRequests(current.latestPageRequestId,id,current.loading);return id;}
    public static synchronized Snapshot snapshot(){return current;}
    public static synchronized boolean canAcceptPage(MarketDataResponseMessage m){return m.kind()==MarketDataResponseKind.PAGE&&m.requestId()==current.latestPageRequestId&&m.marketRevision()>=current.marketRevision;}
    public static synchronized boolean commitPage(MarketDataResponseMessage m){if(!canAcceptPage(m))return false;current=new Snapshot(m.marketRevision(),m.totalSales(),m.totalDemand(),m.orders(),m.totalMatched(),m.offset(),m.limit(),current.latestPageRequestId,current.latestSummaryRequestId,false,false,"");return true;}
    public static synchronized boolean apply(MarketDataResponseMessage m){Objects.requireNonNull(m);return switch(m.kind()){
        case PAGE->commitPage(m);
        case SUMMARY->{if(m.requestId()!=current.latestSummaryRequestId||m.marketRevision()<current.marketRevision)yield false;current=new Snapshot(m.marketRevision(),m.totalSales(),m.totalDemand(),current.orders,current.totalMatched,current.offset,current.limit,current.latestPageRequestId,current.latestSummaryRequestId,current.loading,current.stale,current.error);yield true;}
        case INVALIDATED->{if(m.marketRevision()<current.marketRevision)yield false;current=new Snapshot(m.marketRevision(),m.totalSales(),m.totalDemand(),current.orders,current.totalMatched,current.offset,current.limit,current.latestPageRequestId,current.latestSummaryRequestId,current.loading,true,current.error);yield true;}
    };}
    public static synchronized boolean pageError(long requestId,long revision,String error){
        Objects.requireNonNull(error);
        if(requestId!=current.latestPageRequestId||revision<current.marketRevision)return false;
        current=new Snapshot(current.marketRevision,current.totalSales,current.totalDemand,current.orders,current.totalMatched,current.offset,current.limit,current.latestPageRequestId,current.latestSummaryRequestId,false,current.stale,error);
        return true;
    }
    public static synchronized void reset(){current=empty();}
    public record Snapshot(long marketRevision,int totalSales,int totalDemand,List<MarketOrderSnapshot> orders,int totalMatched,int offset,int limit,long latestPageRequestId,long latestSummaryRequestId,boolean loading,boolean stale,String error){
        public Snapshot{orders=List.copyOf(orders);Objects.requireNonNull(error);}
        Snapshot withRequests(long page,long summary,boolean loading){return new Snapshot(marketRevision,totalSales,totalDemand,orders,totalMatched,offset,limit,page,summary,loading,stale,error);}
    }
}
