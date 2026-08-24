package com.mo.economy_system.common.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class DemandSettlementMailContentTest {
  @Test
  void containsTradeLinkedSourceAndSettlementFacts() {
    UUID tradeId = UUID.randomUUID();
    MarketOrder slice = new MarketOrder(
        MarketOrderType.DEMAND, tradeId, MarketOrderCodecTest.item(), 10, 200,
        "requester", UUID.randomUUID(), 1, 2, false);

    DemandSettlementMailContent content =
        DemandSettlementMailContent.create(slice, "supplier", 200, 54);

    assertEquals("求购订单已交付", content.subject());
    assertTrue(content.source().contains(tradeId.toString()));
    assertTrue(content.body().contains("supplier"));
    assertTrue(content.body().contains("交付数量: 10"));
    assertTrue(content.body().contains("单位价格: 20"));
    assertTrue(content.body().contains("本次支付: 200"));
    assertTrue(content.body().contains("剩余求购: 54"));
  }
}
