package com.mo.economy_system.common.client;
import com.mo.economy_system.common.network.*;import org.junit.jupiter.api.Test;import java.util.List;import static org.junit.jupiter.api.Assertions.*;
class ClientMarketStateTest{
 @Test void staleResponseCannotOverwriteNewerPage(){long ten=ClientMarketState.nextPageRequestId();long eleven=ClientMarketState.nextPageRequestId();assertTrue(eleven>ten);assertTrue(ClientMarketState.apply(page(eleven,11)));assertFalse(ClientMarketState.apply(page(ten,10)));assertEquals(11,ClientMarketState.snapshot().totalMatched());}
 @Test void invalidationPreservesPageAndSummaryDoesNotReplaceIt(){long id=ClientMarketState.nextPageRequestId();ClientMarketState.apply(page(id,3));var before=ClientMarketState.snapshot().orders();ClientMarketState.apply(MarketDataResponseMessage.invalidated(4,5));assertTrue(ClientMarketState.snapshot().stale());assertEquals(before,ClientMarketState.snapshot().orders());ClientMarketState.apply(new MarketDataResponseMessage(MarketDataResponseKind.SUMMARY,0,0,0,0,7,8,List.of()));assertEquals(before,ClientMarketState.snapshot().orders());assertEquals(7,ClientMarketState.snapshot().totalSales());}
 private static MarketDataResponseMessage page(long id,int total){return new MarketDataResponseMessage(MarketDataResponseKind.PAGE,id,0,10,total,0,0,List.of());}
}
