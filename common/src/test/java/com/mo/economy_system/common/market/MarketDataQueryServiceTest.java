package com.mo.economy_system.common.market;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.MarketDataRequestMessage;
import com.mo.economy_system.common.network.MarketDataRequestPurpose;
import com.mo.economy_system.common.network.MarketDataResponseKind;
import com.mo.economy_system.common.network.MarketDataResponseMessage;
import com.mo.economy_system.common.network.MarketOrderFilter;
import com.mo.economy_system.common.network.MarketOrderSort;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MarketDataQueryServiceTest {
  @Test
  void filtersSearchesPagesAndPreservesDefaultChronology() {
    UUID me = UUID.randomUUID();
    UUID other = UUID.randomUUID();
    List<MarketOrder> source = List.of(
        order(MarketOrderType.SALES, me, "Alice", 1, 10, 1, 100),
        order(MarketOrderType.DEMAND, other, "Bob", 1, 10, 2, 101),
        order(MarketOrderType.SALES, other, "Carol", 1, 10, 3, 102));

    var mine = query(source, me, MarketOrderFilter.MINE, MarketOrderSort.DEFAULT, "", 0, 9);
    assertEquals(1, mine.totalMatched());
    var sales = query(source, me, MarketOrderFilter.SALES, MarketOrderSort.DEFAULT, "", 0, 9);
    assertEquals(2, sales.totalMatched());
    assertEquals(source.get(0).tradeId(), sales.orders().get(0).tradeId());
    assertEquals(1, query(source, me, MarketOrderFilter.ALL, MarketOrderSort.DEFAULT, "bOb", 0, 9).totalMatched());
    assertEquals(2, sales.totalSales());
    assertEquals(1, sales.totalDemand());
    assertEquals(3, source.size());
  }

  @Test
  void queryAlsoMatchesTradeId() {
    UUID me = UUID.randomUUID();
    MarketOrder order = order(MarketOrderType.SALES, me, "Alice", 1, 10, 1, 2);
    String fragment = order.tradeId().toString().substring(0, 8);
    assertEquals(1, query(List.of(order), me, MarketOrderFilter.ALL,
        MarketOrderSort.DEFAULT, fragment, 0, 9).totalMatched());
  }

  @Test
  void unitPriceSortUsesExactRationalComparisonBeforePagination() {
    UUID me = UUID.randomUUID();
    MarketOrder expensive = order(MarketOrderType.SALES, me, "A", 3, 10, 1, 100); // 3.333...
    MarketOrder cheapest = order(MarketOrderType.SALES, me, "B", 4, 8, 2, 100);  // 2
    MarketOrder middle = order(MarketOrderType.SALES, me, "C", 2, 5, 3, 100);    // 2.5
    List<MarketOrder> deliberatelyUnsorted = List.of(expensive, cheapest, middle);

    MarketDataResponseMessage asc = query(deliberatelyUnsorted, me, MarketOrderFilter.ALL,
        MarketOrderSort.UNIT_PRICE_ASC, "", 0, 9);
    assertEquals(List.of(cheapest.tradeId(), middle.tradeId(), expensive.tradeId()),
        asc.orders().stream().map(o -> o.tradeId()).toList());

    MarketDataResponseMessage secondItemOnly = query(deliberatelyUnsorted, me, MarketOrderFilter.ALL,
        MarketOrderSort.UNIT_PRICE_ASC, "", 1, 9);
    assertEquals(middle.tradeId(), secondItemOnly.orders().get(0).tradeId());

    MarketDataResponseMessage desc = query(deliberatelyUnsorted, me, MarketOrderFilter.ALL,
        MarketOrderSort.UNIT_PRICE_DESC, "", 0, 9);
    assertEquals(List.of(expensive.tradeId(), middle.tradeId(), cheapest.tradeId()),
        desc.orders().stream().map(o -> o.tradeId()).toList());
  }

  @Test
  void newestAndExpiringSoonUseDeterministicServerOrdering() {
    UUID me = UUID.randomUUID();
    MarketOrder first = order(MarketOrderType.SALES, me, "A", 1, 10, 10, 400);
    MarketOrder second = order(MarketOrderType.SALES, me, "B", 1, 10, 30, 300);
    MarketOrder third = order(MarketOrderType.SALES, me, "C", 1, 10, 20, 200);
    List<MarketOrder> source = List.of(first, second, third);

    var newest = query(source, me, MarketOrderFilter.ALL, MarketOrderSort.NEWEST, "", 0, 9);
    assertEquals(List.of(second.tradeId(), third.tradeId(), first.tradeId()),
        newest.orders().stream().map(o -> o.tradeId()).toList());

    var expiry = query(source, me, MarketOrderFilter.ALL, MarketOrderSort.EXPIRING_SOON, "", 0, 9);
    assertEquals(List.of(third.tradeId(), second.tradeId(), first.tradeId()),
        expiry.orders().stream().map(o -> o.tradeId()).toList());
  }

  @Test
  void handlesSummaryAndClampsOffsetBeyondEndToLastValidPage() {
    UUID me = UUID.randomUUID();
    List<MarketOrder> source = List.of(order(MarketOrderType.SALES, me, "A", 1, 10, 1, 2));
    var summary = MarketDataQueryService.query(source, me, MarketDataRequestMessage.summary(2));
    assertEquals(MarketDataResponseKind.SUMMARY, summary.kind());
    var page = MarketDataQueryService.query(source, me,
        new MarketDataRequestMessage(3, MarketDataRequestPurpose.PAGE, Integer.MAX_VALUE, 9,
            MarketOrderFilter.ALL, MarketOrderSort.DEFAULT, ""));
    assertEquals(0, page.offset());
    assertEquals(1, page.orders().size());
  }

  @Test
  void focusedTradeFollowsItsServerSortedPageBeforePagination() {
    UUID me = UUID.randomUUID();
    java.util.ArrayList<MarketOrder> source = new java.util.ArrayList<>();
    for (int i = 0; i < 20; i++) {
      source.add(order(MarketOrderType.SALES, me, "P" + i, 1, i + 1, i, 1_000 + i));
    }
    MarketOrder focused = source.get(15);

    MarketDataResponseMessage page = MarketDataQueryService.query(source, me,
        new MarketDataRequestMessage(4, MarketDataRequestPurpose.PAGE, 0, 9,
            MarketOrderFilter.ALL, MarketOrderSort.DEFAULT, "", focused.tradeId()));

    assertEquals(9, page.offset());
    assertTrue(page.orders().stream().anyMatch(order -> order.tradeId().equals(focused.tradeId())));
    assertEquals(20, page.totalMatched());
  }

  @Test
  void absentFocusDoesNotInventSelectionAndStillClampsRequestedPage() {
    UUID me = UUID.randomUUID();
    List<MarketOrder> source = List.of(
        order(MarketOrderType.SALES, me, "A", 1, 10, 1, 100),
        order(MarketOrderType.SALES, me, "B", 1, 10, 2, 101));

    MarketDataResponseMessage page = MarketDataQueryService.query(source, me,
        new MarketDataRequestMessage(5, MarketDataRequestPurpose.PAGE, 99, 9,
            MarketOrderFilter.ALL, MarketOrderSort.DEFAULT, "", UUID.randomUUID()));

    assertEquals(0, page.offset());
    assertEquals(2, page.orders().size());
  }

  private static MarketDataResponseMessage query(
      List<MarketOrder> source,
      UUID me,
      MarketOrderFilter filter,
      MarketOrderSort sort,
      String query,
      int offset,
      int limit) {
    return MarketDataQueryService.query(source, me,
        new MarketDataRequestMessage(1, MarketDataRequestPurpose.PAGE, offset, limit, filter, sort, query));
  }

  private static MarketOrder order(
      MarketOrderType type,
      UUID owner,
      String name,
      int quantity,
      int totalPrice,
      long listingTime,
      long expirationTime) {
    return new MarketOrder(type, UUID.randomUUID(), MarketOrderCodecTest.item(), quantity, totalPrice,
        name, owner, listingTime, expirationTime, false);
  }
}
