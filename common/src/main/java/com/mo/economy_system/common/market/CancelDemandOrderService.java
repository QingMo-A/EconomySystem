package com.mo.economy_system.common.market;

import com.mo.economy_system.core.economy_system.BalanceMutationResult;

import java.util.Objects;
import java.util.UUID;

public final class CancelDemandOrderService {
    private CancelDemandOrderService() {}

    public static CancelDemandOrderResult execute(UUID tradeId, Context context) {
        if (tradeId==null||context==null) return CancelDemandOrderResult.INVALID_CONTEXT;
        MarketOrder order=context.repository().find(tradeId);
        if(order==null)return CancelDemandOrderResult.NOT_FOUND;
        if(order.type()!=MarketOrderType.DEMAND)return CancelDemandOrderResult.WRONG_ORDER_TYPE;
        if(order.delivered())return CancelDemandOrderResult.ALREADY_DELIVERED;
        if(!context.operator()&&!order.sellerId().equals(context.actorId()))return CancelDemandOrderResult.NOT_OWNER;
        if(order.totalPrice()<=0)return CancelDemandOrderResult.INVALID_PRICE;
        if(!context.account().canCreditExact(order.sellerId(),order.totalPrice()))return CancelDemandOrderResult.OWNER_BALANCE_LIMIT;
        DemandOrderRemovalResult removed=context.repository().removeUndeliveredDemand(tradeId);
        if(removed.status()!=DemandOrderRemovalStatus.REMOVED||removed.removal()==null)return mapRemoval(removed.status());
        BalanceMutationResult refund;
        RuntimeException refundError=null;
        try{refund=context.account().creditExact(order.sellerId(),order.totalPrice());}
        catch(RuntimeException exception){refund=BalanceMutationResult.PERSIST_FAILED;refundError=exception;}
        if(refund==BalanceMutationResult.SUCCESS)return CancelDemandOrderResult.SUCCESS;
        MarketOrderRestoreResult restored;
        try{restored=removed.removal().restore().restore();}
        catch(RuntimeException exception){restored=MarketOrderRestoreResult.PERSIST_FAILED;}
        CancelDemandOrderResult result=restored==MarketOrderRestoreResult.RESTORED
                ? CancelDemandOrderResult.REFUND_FAILED:CancelDemandOrderResult.ROLLBACK_FAILED;
        report(context,tradeId,result,refund,restored,refundError);
        return result;
    }

    private static CancelDemandOrderResult mapRemoval(DemandOrderRemovalStatus status){return switch(status){
        case NOT_FOUND->CancelDemandOrderResult.NOT_FOUND;case WRONG_ORDER_TYPE->CancelDemandOrderResult.WRONG_ORDER_TYPE;
        case ALREADY_DELIVERED->CancelDemandOrderResult.ALREADY_DELIVERED;default->CancelDemandOrderResult.ORDER_REMOVE_FAILED;};}
    private static void report(Context context,UUID id,CancelDemandOrderResult result,BalanceMutationResult refund,MarketOrderRestoreResult restore,RuntimeException cause){
        try{context.reporter().report(id,result,refund,restore,cause);}catch(RuntimeException ignored){} }
    public record Context(UUID actorId,boolean operator,Account account,Repository repository,FailureReporter reporter){public Context{Objects.requireNonNull(actorId);Objects.requireNonNull(account);Objects.requireNonNull(repository);Objects.requireNonNull(reporter);}}
    public interface Account{boolean canCreditExact(UUID owner,int amount);BalanceMutationResult creditExact(UUID owner,int amount);}
    public interface Repository{MarketOrder find(UUID id);DemandOrderRemovalResult removeUndeliveredDemand(UUID id);}
    public interface FailureReporter{void report(UUID id,CancelDemandOrderResult result,BalanceMutationResult refund,MarketOrderRestoreResult restore,RuntimeException cause);static FailureReporter noop(){return(id,result,refund,restore,cause)->{};}}
}
