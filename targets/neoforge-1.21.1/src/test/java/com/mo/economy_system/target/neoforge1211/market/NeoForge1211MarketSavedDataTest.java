package com.mo.economy_system.target.neoforge1211.market;

import com.mo.economy_system.common.market.*;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import com.mo.economy_system.platform.item.ItemStackSnapshot;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class NeoForge1211MarketSavedDataTest {
    @Test void roundTripsStableOrderWithExactExpiration() {
        MarketOrder original = new MarketOrder(MarketOrderType.SALES, UUID.randomUUID(), item(), 7, 101, "seller", UUID.randomUUID(), 10, 123456, false);
        CompoundTag root = new CompoundTag(); ListTag list = new ListTag(); list.add(MarketOrderCodec.encode(original)); root.put("marketItems", list);
        MarketSavedData data = MarketSavedData.load(root, null);
        CompoundTag saved = data.save(new CompoundTag(), null);
        MarketOrder decoded = MarketOrderCodec.decodeCurrent(saved.getList("marketItems", Tag.TAG_COMPOUND).getCompound(0)).orElseThrow();
        assertEquals(original, decoded); assertEquals(123456, decoded.expirationTime());
    }
    @Test void readsBothLegacyJavaTypeNamesWithVersionedSnapshots() {
        MarketOrder sale = new MarketOrder(MarketOrderType.SALES,UUID.randomUUID(),item(),65,100,"s",UUID.randomUUID(),1,999,false);
        MarketOrder demand = new MarketOrder(MarketOrderType.DEMAND,UUID.randomUUID(),item(),3,20,"d",UUID.randomUUID(),2,888,true);
        CompoundTag saleTag=MarketOrderCodec.encode(sale);saleTag.putString("type","com.mo.economy_system.core.economy_system.market.SalesOrder");
        CompoundTag demandTag=MarketOrderCodec.encode(demand);demandTag.putString("type","com.mo.economy_system.core.economy_system.market.DemandOrder");
        CompoundTag root=new CompoundTag();ListTag list=new ListTag();list.add(saleTag);list.add(demandTag);root.put("marketItems",list);
        List<MarketOrder> orders=MarketSavedData.load(root,null).getOrders();
        assertEquals(65,orders.get(0).quantity());assertEquals(999,orders.get(0).expirationTime());
        assertTrue(orders.get(1).delivered());assertEquals(888,orders.get(1).expirationTime());
    }
    @Test void malformedVersionedOrderFailsBeforeAnyRewrite() {
        CompoundTag valid=MarketOrderCodec.encode(new MarketOrder(MarketOrderType.SALES,UUID.randomUUID(),item(),1,1,"s",UUID.randomUUID(),1,2,false));
        CompoundTag broken=valid.copy();broken.putString("future","unsupported");
        CompoundTag root=new CompoundTag();ListTag list=new ListTag();list.add(valid);list.add(broken);root.put("marketItems",list);
        assertThrows(IllegalArgumentException.class,()->MarketSavedData.load(root,null));
        assertEquals(2,root.getList("marketItems",Tag.TAG_COMPOUND).size());
    }
    @Test void deliveredTransitionPersistsAndPreservesSnapshotAndExpiration() {
        MarketOrder demand=new MarketOrder(MarketOrderType.DEMAND,UUID.randomUUID(),item(),70,25,"d",UUID.randomUUID(),5,777,false);
        MarketOrder sale=new MarketOrder(MarketOrderType.SALES,UUID.randomUUID(),item(),1,2,"s",UUID.randomUUID(),6,888,false);
        CompoundTag root=new CompoundTag();ListTag list=new ListTag();list.add(MarketOrderCodec.encode(demand));list.add(MarketOrderCodec.encode(sale));root.put("marketItems",list);
        MarketSavedData data=MarketSavedData.load(root,null);assertEquals(DemandDeliveryTransitionResult.UPDATED,data.markDemandDelivered(demand.tradeId()));
        assertEquals(DemandDeliveryTransitionResult.ALREADY_DELIVERED,data.markDemandDelivered(demand.tradeId()));
        MarketSavedData loaded=MarketSavedData.load(data.save(new CompoundTag(),null),null);assertEquals(2,loaded.getOrders().size());
        MarketOrder updated=loaded.getOrders().get(0);assertTrue(updated.delivered());assertEquals(777,updated.expirationTime());assertEquals(demand.item(),updated.item());
        assertEquals(DemandDeliveryTransitionResult.WRONG_ORDER_TYPE,loaded.markDemandDelivered(sale.tradeId()));
    }
    private static ItemStackSnapshot item() { return ItemStackSnapshot.create("minecraft:stone", 1, Optional.empty(), List.of(), Map.of(), Map.of(), true, true, 0, 0, false, true, OptionalInt.empty(), true, OptionalInt.empty(), new CompoundTag()).orElseThrow(); }
}
