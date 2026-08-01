package com.mo.economy_system.common.market;

import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CancelDemandOrderServiceTest {
    @Test void ownerAndOperatorRefundOriginalOwnerExactlyOnce(){Fixture owner=new Fixture();assertEquals(CancelDemandOrderResult.SUCCESS,owner.execute());assertEquals(117,owner.account.balance);assertNull(owner.repository.order);assertEquals(CancelDemandOrderResult.NOT_FOUND,owner.execute());assertEquals(1,owner.account.credits);
        Fixture op=new Fixture();op.actor=UUID.randomUUID();op.operator=true;assertEquals(CancelDemandOrderResult.SUCCESS,op.execute());assertEquals(op.owner,op.account.lastOwner);}
    @Test void validationNeverMutates(){Fixture outsider=new Fixture();outsider.actor=UUID.randomUUID();assertEquals(CancelDemandOrderResult.NOT_OWNER,outsider.execute());assertState(outsider);
        Fixture delivered=new Fixture();delivered.repository.order=delivered.order(MarketOrderType.DEMAND,true);assertEquals(CancelDemandOrderResult.ALREADY_DELIVERED,delivered.execute());assertState(delivered);
        Fixture sales=new Fixture();sales.repository.order=sales.order(MarketOrderType.SALES,false);assertEquals(CancelDemandOrderResult.WRONG_ORDER_TYPE,sales.execute());assertState(sales);
        Fixture limit=new Fixture();limit.account.canCredit=false;assertEquals(CancelDemandOrderResult.OWNER_BALANCE_LIMIT,limit.execute());assertState(limit);}
    @Test void removeFailureDoesNotRefund(){Fixture f=new Fixture();f.repository.removeStatus=DemandOrderRemovalStatus.PERSIST_FAILED;assertEquals(CancelDemandOrderResult.ORDER_REMOVE_FAILED,f.execute());assertState(f);}
    @Test void refundFailureRestoresOrderAndRollbackFailureIsExplicit(){Fixture rejected=new Fixture();rejected.account.creditResult=BalanceMutationResult.BALANCE_LIMIT;assertEquals(CancelDemandOrderResult.REFUND_FAILED,rejected.execute());assertNotNull(rejected.repository.order);
        Fixture thrown=new Fixture();thrown.account.creditThrows=true;assertEquals(CancelDemandOrderResult.REFUND_FAILED,thrown.execute());assertNotNull(thrown.repository.order);
        Fixture rollback=new Fixture();rollback.account.creditResult=BalanceMutationResult.PERSIST_FAILED;rollback.repository.restoreResult=MarketOrderRestoreResult.PERSIST_FAILED;assertEquals(CancelDemandOrderResult.ROLLBACK_FAILED,rollback.execute());assertNull(rollback.repository.order);}
    private static void assertState(Fixture f){assertNotNull(f.repository.order);assertEquals(100,f.account.balance);assertEquals(0,f.account.credits);}
    private static final class Fixture{UUID owner=UUID.randomUUID(),actor=owner;boolean operator;FakeAccount account=new FakeAccount();FakeRepository repository=new FakeRepository();Fixture(){repository.order=order(MarketOrderType.DEMAND,false);}MarketOrder order(MarketOrderType type,boolean delivered){MarketOrder base=new MarketOrder(type,UUID.randomUUID(),MarketOrderCodecTest.item(),2,17,"buyer",owner,1,2,delivered);return base;}CancelDemandOrderResult execute(){return CancelDemandOrderService.execute(repository.order==null?UUID.randomUUID():repository.order.tradeId(),new CancelDemandOrderService.Context(actor,operator,account,repository,CancelDemandOrderService.FailureReporter.noop()));}}
    private static final class FakeAccount implements CancelDemandOrderService.Account{int balance=100,credits;boolean canCredit=true,creditThrows;UUID lastOwner;BalanceMutationResult creditResult=BalanceMutationResult.SUCCESS;public boolean canCreditExact(UUID owner,int amount){return canCredit;}public BalanceMutationResult creditExact(UUID owner,int amount){credits++;lastOwner=owner;if(creditThrows)throw new IllegalStateException();if(creditResult==BalanceMutationResult.SUCCESS)balance+=amount;return creditResult;}}
    private static final class FakeRepository implements CancelDemandOrderService.Repository{MarketOrder order;DemandOrderRemovalStatus removeStatus=DemandOrderRemovalStatus.REMOVED;MarketOrderRestoreResult restoreResult=MarketOrderRestoreResult.RESTORED;public MarketOrder find(UUID id){return order;}public DemandOrderRemovalResult removeUndeliveredDemand(UUID id){if(removeStatus!=DemandOrderRemovalStatus.REMOVED)return DemandOrderRemovalResult.failure(removeStatus);MarketOrder removed=order;order=null;return DemandOrderRemovalResult.removed(new MarketOrderRemoval(removed,()->{if(restoreResult==MarketOrderRestoreResult.RESTORED)order=removed;return restoreResult;}));}}
}
