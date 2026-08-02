package com.mo.economy_system.protocol;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MarketProtocolDiscriminatorTest {
    @Test void canonicalOrderProtocolEntriesNeverMove(){List<EconomyMessageSpec> specs=List.of(EconomyProtocol.PURCHASE_SALES_ORDER,EconomyProtocol.CONFIRM_DEMAND_ORDER,EconomyProtocol.DELIVER_DEMAND_ORDER,EconomyProtocol.REMOVE_SALES_ORDER,EconomyProtocol.REMOVE_DEMAND_ORDER);assertEquals(List.of(12,13,14,15,16),specs.stream().map(EconomyMessageSpec::discriminator).toList());assertTrue(specs.stream().allMatch(spec->spec.direction()==EconomyMessageDirection.CLIENT_TO_SERVER));assertEquals(List.of("economy_system:economy_system/sales_order/packet_purchase_sales_order","economy_system:economy_system/demand_order/packet_confirm_demand_order","economy_system:economy_system/demand_order/packet_deliver_demand_order","economy_system:economy_system/sales_order/packet_remove_sales_order","economy_system:economy_system/demand_order/packet_remove_demand_order"),specs.stream().map(EconomyMessageSpec::id).toList());assertEquals(specs,EconomyProtocol.ALL.subList(12,17));}
}
