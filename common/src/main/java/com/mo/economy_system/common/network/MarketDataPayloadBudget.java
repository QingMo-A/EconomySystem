package com.mo.economy_system.common.network;
import com.mo.economy_system.platform.item.ItemStackSnapshotCodec;import com.mo.economy_system.platform.item.ItemStackSnapshotValidator;import java.nio.charset.StandardCharsets;
public final class MarketDataPayloadBudget{
 private MarketDataPayloadBudget(){}
 static void requireWithinLimit(MarketDataResponseMessage.MarketDataResponseMessageUnchecked m){long bytes=64;for(MarketOrderSnapshot o:m.orders()){bytes+=96L+o.ownerName().getBytes(StandardCharsets.UTF_8).length;bytes+=ItemStackSnapshotValidator.estimatedBytes(ItemStackSnapshotCodec.encode(o.item()).orElseThrow());if(bytes>EconomyNetworkLimits.MAX_MARKET_RESPONSE_ESTIMATED_BYTES)throw new IllegalArgumentException("market response exceeds byte budget");}}
 public static void requireWithinLimit(MarketDataResponseMessage m){requireWithinLimit(new MarketDataResponseMessage.MarketDataResponseMessageUnchecked(m.kind(),m.requestId(),m.marketRevision(),m.offset(),m.limit(),m.totalMatched(),m.totalSales(),m.totalDemand(),m.orders()));}
}
