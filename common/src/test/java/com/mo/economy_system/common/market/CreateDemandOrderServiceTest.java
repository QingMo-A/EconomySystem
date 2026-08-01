package com.mo.economy_system.common.market;

import com.mo.economy_system.common.network.CreateDemandOrderMessage;
import com.mo.economy_system.platform.item.ItemStackSnapshot;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CreateDemandOrderServiceTest {
    @Test void successFreezesTotalOnceWithoutTaxOrQuantityMultiplication() {
        Fixture f = new Fixture();
        assertEquals(CreateDemandOrderResult.SUCCESS, f.execute());
        assertEquals(900, f.account.balance); assertEquals(1, f.account.debits); assertEquals(0, f.account.credits);
        assertEquals(1, f.repository.orders.size());
        MarketOrder order = f.repository.orders.get(0);
        assertEquals(MarketOrderType.DEMAND, order.type()); assertEquals(5, order.quantity());
        assertEquals(100, order.totalPrice()); assertEquals(1, order.item().count()); assertFalse(order.delivered());
        assertEquals(f.buyer, order.sellerId()); assertEquals("buyer", order.sellerName());
        assertEquals(10, order.listingTime()); assertEquals(10 + MarketOrder.EXPIRATION_DURATION_MILLIS, order.expirationTime());
    }

    @Test void allPreconditionsLeaveBalanceAndRepositoryUntouched() {
        List<CreateDemandOrderResult> expected = List.of(CreateDemandOrderResult.INVALID_ITEM_ID,
                CreateDemandOrderResult.INVALID_QUANTITY, CreateDemandOrderResult.INVALID_PRICE,
                CreateDemandOrderResult.ITEM_NOT_FOUND, CreateDemandOrderResult.QUANTITY_EXCEEDS_LIMIT,
                CreateDemandOrderResult.REPOSITORY_FULL, CreateDemandOrderResult.INSUFFICIENT_FUNDS,
                CreateDemandOrderResult.TIME_OVERFLOW, CreateDemandOrderResult.ID_GENERATION_FAILED);
        List<Fixture> fixtures = new ArrayList<>();
        Fixture badId=new Fixture();badId.message=new CreateDemandOrderMessage(" ",5,100);fixtures.add(badId);
        Fixture badQty=new Fixture();badQty.message=new CreateDemandOrderMessage("minecraft:stone",0,100);fixtures.add(badQty);
        Fixture badPrice=new Fixture();badPrice.message=new CreateDemandOrderMessage("minecraft:stone",5,0);fixtures.add(badPrice);
        Fixture missing=new Fixture();missing.resolve=DemandItemResolveResult.failure(DemandItemResolveResult.Error.ITEM_NOT_FOUND);fixtures.add(missing);
        Fixture tooMany=new Fixture();tooMany.message=new CreateDemandOrderMessage("minecraft:stone",65,100);fixtures.add(tooMany);
        Fixture full=new Fixture();full.repository.full=true;fixtures.add(full);
        Fixture poor=new Fixture();poor.account.balance=99;fixtures.add(poor);
        Fixture overflow=new Fixture();overflow.now=Long.MAX_VALUE;fixtures.add(overflow);
        Fixture noId=new Fixture();noId.id=null;fixtures.add(noId);
        for(int i=0;i<fixtures.size();i++) { Fixture f=fixtures.get(i);assertEquals(expected.get(i),f.execute());assertEquals(i==6?99:1000,f.account.balance);assertTrue(f.repository.orders.isEmpty());assertEquals(0,f.account.debits); }
    }

    @Test void debitFailuresNeverAddAnOrder() {
        Fixture rejected=new Fixture();rejected.account.debitFalse=true;assertEquals(CreateDemandOrderResult.PAYMENT_FAILED,rejected.execute());assertState(rejected,1000,1,0);
        Fixture thrown=new Fixture();thrown.account.debitThrows=true;assertEquals(CreateDemandOrderResult.PAYMENT_FAILED,thrown.execute());assertState(thrown,1000,1,0);
    }

    @Test void repositoryFailureRefundsExactlyOnceAndLeavesNoOrder() {
        Fixture rejected=new Fixture();rejected.repository.addFalse=true;assertEquals(CreateDemandOrderResult.ORDER_PERSIST_FAILED,rejected.execute());assertState(rejected,1000,1,1);
        Fixture thrown=new Fixture();thrown.repository.addThrows=true;assertEquals(CreateDemandOrderResult.ORDER_PERSIST_FAILED,thrown.execute());assertState(thrown,1000,1,1);
    }

    @Test void refundFailuresAreExplicitAndStillLeaveNoOrder() {
        Fixture rejected=new Fixture();rejected.repository.addFalse=true;rejected.account.creditFalse=true;assertEquals(CreateDemandOrderResult.REFUND_FAILED,rejected.execute());assertState(rejected,900,1,1);
        Fixture thrown=new Fixture();thrown.repository.addFalse=true;thrown.account.creditThrows=true;assertEquals(CreateDemandOrderResult.REFUND_FAILED,thrown.execute());assertState(thrown,900,1,1);
    }

    @Test void duplicateIdUsesAtomicRepositoryContractAndRefunds() {
        Fixture f=new Fixture();f.repository.orders.add(new MarketOrder(MarketOrderType.SALES,f.id,item(),1,1,"x",UUID.randomUUID(),1,2,false));
        assertEquals(CreateDemandOrderResult.ORDER_PERSIST_FAILED,f.execute());assertEquals(1000,f.account.balance);assertEquals(1,f.repository.orders.size());
    }

    @Test void createdOrderCanBeDeliveredOnceByTheCurrentDeliveryService() {
        Fixture f=new Fixture();assertEquals(CreateDemandOrderResult.SUCCESS,f.execute());MarketOrder created=f.repository.orders.get(0);
        int[] supplierBalance={0},items={5},payments={0};
        DemandOrderDeliveryService.Repository repository=new DemandOrderDeliveryService.Repository(){
            public MarketOrder find(UUID id){return f.repository.orders.stream().filter(order->order.tradeId().equals(id)).findFirst().orElse(null);}
            public DemandDeliveryTransitionResult markDelivered(UUID id){MarketOrder current=find(id);if(current==null)return DemandDeliveryTransitionResult.NOT_FOUND;if(current.delivered())return DemandDeliveryTransitionResult.ALREADY_DELIVERED;
                f.repository.orders.set(0,new MarketOrder(current.type(),current.tradeId(),current.item(),current.quantity(),current.totalPrice(),current.sellerName(),current.sellerId(),current.listingTime(),current.expirationTime(),true));return DemandDeliveryTransitionResult.UPDATED;}};
        DemandOrderDeliveryService.Inventory inventory=new DemandOrderDeliveryService.Inventory(){public Object restoreTemplate(MarketOrder order){return new Object();}public long countMatching(Object template){return items[0];}
            public DemandOrderDeliveryService.RemovalResult removeMatching(Object template,int quantity){int before=items[0];items[0]-=quantity;return DemandOrderDeliveryService.RemovalResult.success(()->{items[0]=before;return true;});}};
        DemandOrderDeliveryService.Account account=new DemandOrderDeliveryService.Account(){public boolean credit(int amount){payments[0]++;supplierBalance[0]+=amount;return true;}public boolean debit(int amount){supplierBalance[0]-=amount;return true;}};
        var context=new DemandOrderDeliveryService.Context(UUID.randomUUID(),inventory,account,repository,DemandOrderDeliveryService.FailureReporter.noop());
        assertEquals(DemandOrderDeliveryResult.SUCCESS,DemandOrderDeliveryService.execute(created.tradeId(),context));
        assertEquals(DemandOrderDeliveryResult.ALREADY_DELIVERED,DemandOrderDeliveryService.execute(created.tradeId(),context));
        assertEquals(100,supplierBalance[0]);assertEquals(1,payments[0]);assertEquals(0,items[0]);assertTrue(repository.find(created.tradeId()).delivered());
    }

    private static void assertState(Fixture f,int balance,int debits,int credits){assertEquals(balance,f.account.balance);assertEquals(debits,f.account.debits);assertEquals(credits,f.account.credits);assertTrue(f.repository.orders.isEmpty());}
    private static final class Fixture {
        UUID buyer=UUID.randomUUID(),id=UUID.randomUUID();long now=10;CreateDemandOrderMessage message=new CreateDemandOrderMessage("minecraft:stone",5,100);
        DemandItemResolveResult resolve=DemandItemResolveResult.success(new ResolvedDemandItem("minecraft:stone",item(),64));FakeAccount account=new FakeAccount();FakeRepository repository=new FakeRepository();
        CreateDemandOrderResult execute(){return CreateDemandOrderService.execute(message,new CreateDemandOrderService.Context(raw->resolve,account,repository,buyer,"buyer",()->id,()->now,CreateDemandOrderService.FailureReporter.noop()));}
    }
    private static final class FakeAccount implements CreateDemandOrderService.Account {int balance=1000,debits,credits;boolean debitFalse,debitThrows,creditFalse,creditThrows;
        public boolean canDebit(int amount){return balance>=amount;}public boolean debit(int amount){debits++;if(debitThrows)throw new IllegalStateException();if(debitFalse)return false;balance-=amount;return true;}public boolean credit(int amount){credits++;if(creditThrows)throw new IllegalStateException();if(creditFalse)return false;balance+=amount;return true;}}
    private static final class FakeRepository implements CreateDemandOrderService.Repository {List<MarketOrder> orders=new ArrayList<>();boolean full,addFalse,addThrows;
        public boolean isFull(){return full;}public boolean add(MarketOrder order){if(addThrows)throw new IllegalStateException();if(addFalse||orders.stream().anyMatch(existing->existing.tradeId().equals(order.tradeId())))return false;orders.add(order);return true;}}
    private static ItemStackSnapshot item(){return ItemStackSnapshot.create("minecraft:stone",1,Optional.empty(),List.of(),Map.of(),Map.of(),true,true,0,0,false,true,OptionalInt.empty(),true,OptionalInt.empty(),new CompoundTag()).orElseThrow();}
}
