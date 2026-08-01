package com.mo.economy_system.core.economy_system.market;

import com.mo.economy_system.common.market.*;
import com.mo.economy_system.platform.item.ItemStackSnapshot;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MarketManagerFacadeTest {
    @Test void rebindingCannotMutateThePreviousWorldOrWriteAnOldViewBack() {
        MarketSavedData first=new MarketSavedData(),second=new MarketSavedData();MarketOrder firstOrder=order(),secondOrder=order();
        first.addOrder(firstOrder);MarketManager.bind(first);assertSame(first,MarketManager.boundDataForTest());
        second.addOrder(secondOrder);MarketManager.bind(second);assertSame(second,MarketManager.boundDataForTest());
        assertTrue(MarketManager.removeMarketItemById(secondOrder.tradeId()));
        assertEquals(List.of(firstOrder),first.getOrders());assertTrue(second.getOrders().isEmpty());
    }
    private static MarketOrder order(){return new MarketOrder(MarketOrderType.SALES,UUID.randomUUID(),item(),1,1,"s",UUID.randomUUID(),1,2,false);}
    private static ItemStackSnapshot item(){return ItemStackSnapshot.create("minecraft:stone",1,Optional.empty(),List.of(),Map.of(),Map.of(),true,true,0,0,false,true,OptionalInt.empty(),true,OptionalInt.empty(),new CompoundTag()).orElseThrow();}
}
