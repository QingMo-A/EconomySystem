package com.mo.economy_system.common.market;
import java.util.List;
public record MarketLedgerView(long revision, List<MarketOrder> orders) {
    public MarketLedgerView { if (revision < 0) throw new IllegalArgumentException("negative revision"); orders=List.copyOf(orders); }
}
