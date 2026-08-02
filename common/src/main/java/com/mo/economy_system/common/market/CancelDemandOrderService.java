package com.mo.economy_system.common.market;

import com.mo.economy_system.common.network.RemoveDemandOrderMessage;
import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import java.util.Objects;
import java.util.UUID;

public final class CancelDemandOrderService {
  private CancelDemandOrderService() {}

  public static CancelDemandOrderOutcome execute(RemoveDemandOrderMessage message, Context context) {
    if (message == null || context == null) return CancelDemandOrderOutcome.validationFailure(CancelDemandOrderResult.INVALID_CONTEXT);
    UUID id = message.tradeId(); MarketOrder expected;
    try { expected = context.repository().find(id); }
    catch (RuntimeException error) { report(context, failure(context,id,null,"find",CancelDemandOrderResult.STATE_UNKNOWN,MarketMutationState.UNKNOWN,null,null,null,error,error,null,null)); return CancelDemandOrderOutcome.uncertainFailure(CancelDemandOrderResult.STATE_UNKNOWN); }
    if (expected == null) return validation(context,id,null,CancelDemandOrderResult.NOT_FOUND);
    UUID requester = expected.sellerId();
    CancelDemandOrderResult invalid = validate(expected, context);
    if (invalid != CancelDemandOrderResult.SUCCESS) return validation(context,id,requester,invalid);
    BalanceMutationResult preview;
    try { preview = context.account().previewCreditExact(requester, expected.totalPrice()); }
    catch (RuntimeException error) { report(context,failure(context,id,requester,"refund-preview",CancelDemandOrderResult.REFUND_FAILED,MarketMutationState.UNCHANGED,null,null,null,error,null,error,null)); return CancelDemandOrderOutcome.validationFailure(CancelDemandOrderResult.REFUND_FAILED); }
    if (preview != BalanceMutationResult.SUCCESS) {
      CancelDemandOrderResult result = preview == BalanceMutationResult.BALANCE_LIMIT ? CancelDemandOrderResult.OWNER_BALANCE_LIMIT : CancelDemandOrderResult.REFUND_FAILED;
      report(context,failure(context,id,requester,"refund-preview",result,MarketMutationState.UNCHANGED,null,preview,null,null,null,null,null));
      return CancelDemandOrderOutcome.validationFailure(result);
    }
    DemandOrderRemovalResult removal;
    try { removal = context.repository().removeUndeliveredDemandIfUnchanged(id, expected); }
    catch (RuntimeException error) { report(context,failure(context,id,requester,"order-remove",CancelDemandOrderResult.STATE_UNKNOWN,MarketMutationState.UNKNOWN,null,null,null,error,error,null,null)); return CancelDemandOrderOutcome.uncertainFailure(CancelDemandOrderResult.STATE_UNKNOWN); }
    if (removal == null) { report(context,failure(context,id,requester,"order-remove",CancelDemandOrderResult.STATE_UNKNOWN,MarketMutationState.UNKNOWN,null,null,null,null,null,null,null)); return CancelDemandOrderOutcome.uncertainFailure(CancelDemandOrderResult.STATE_UNKNOWN); }
    if (removal.status() != DemandOrderRemovalStatus.REMOVED) {
      CancelDemandOrderResult result = map(removal.status());
      MarketMutationState state = removal.status() == DemandOrderRemovalStatus.ORDER_CHANGED ? MarketMutationState.CHANGED : MarketMutationState.UNCHANGED;
      report(context,failure(context,id,requester,"order-remove",result,state,removal.status(),null,null,null,null,null,null));
      return state == MarketMutationState.CHANGED ? CancelDemandOrderOutcome.changedFailure(result,expected) : CancelDemandOrderOutcome.validationFailure(result);
    }
    if (removal.removal() == null || !expected.equals(removal.removal().order())) {
      report(context,failure(context,id,requester,"order-remove",CancelDemandOrderResult.STATE_UNKNOWN,MarketMutationState.UNKNOWN,DemandOrderRemovalStatus.REMOVED,null,null,null,null,null,null));
      return CancelDemandOrderOutcome.uncertainFailure(CancelDemandOrderResult.STATE_UNKNOWN);
    }
    MarketOrder authoritative = removal.removal().order(); BalanceMutationResult refund; RuntimeException refundError = null;
    try { refund = context.account().creditExact(authoritative.sellerId(), authoritative.totalPrice()); }
    catch (RuntimeException error) { refund = null; refundError = error; }
    if (refund == BalanceMutationResult.SUCCESS) return CancelDemandOrderOutcome.success(authoritative);
    MarketOrderRestoreResult restored; RuntimeException restoreError = null;
    try { restored = removal.removal().restore().restore(); }
    catch (RuntimeException error) { restored = null; restoreError = error; }
    if (restored == MarketOrderRestoreResult.RESTORED) {
      CancelDemandOrderResult result = refund == BalanceMutationResult.BALANCE_LIMIT ? CancelDemandOrderResult.OWNER_BALANCE_LIMIT : CancelDemandOrderResult.REFUND_FAILED;
      report(context,failure(context,id,requester,"order-restore",result,MarketMutationState.UNCHANGED,DemandOrderRemovalStatus.REMOVED,refund,restored,refundError,null,refundError,null));
      return CancelDemandOrderOutcome.rolledBackFailure(result,authoritative);
    }
    MarketMutationState state = restoreError == null ? MarketMutationState.CHANGED : MarketMutationState.UNKNOWN;
    report(context,failure(context,id,requester,"order-restore",CancelDemandOrderResult.ROLLBACK_FAILED,state,DemandOrderRemovalStatus.REMOVED,refund,restored,refundError,null,refundError,restoreError));
    return state == MarketMutationState.CHANGED ? CancelDemandOrderOutcome.changedFailure(CancelDemandOrderResult.ROLLBACK_FAILED,authoritative) : CancelDemandOrderOutcome.uncertainFailure(CancelDemandOrderResult.ROLLBACK_FAILED);
  }

  private static CancelDemandOrderResult validate(MarketOrder order, Context context) {
    if(order.type()!=MarketOrderType.DEMAND)return CancelDemandOrderResult.WRONG_ORDER_TYPE;
    if(order.delivered())return CancelDemandOrderResult.ALREADY_DELIVERED;
    if(!context.operator()&&!order.sellerId().equals(context.actorId()))return CancelDemandOrderResult.NOT_OWNER;
    if(order.totalPrice()<=0)return CancelDemandOrderResult.INVALID_PRICE;
    return CancelDemandOrderResult.SUCCESS;
  }
  private static CancelDemandOrderResult map(DemandOrderRemovalStatus status) { return switch(status) { case NOT_FOUND->CancelDemandOrderResult.NOT_FOUND; case WRONG_ORDER_TYPE->CancelDemandOrderResult.WRONG_ORDER_TYPE; case ALREADY_DELIVERED->CancelDemandOrderResult.ALREADY_DELIVERED; case ORDER_CHANGED->CancelDemandOrderResult.ORDER_CHANGED; case PERSIST_FAILED->CancelDemandOrderResult.ORDER_REMOVE_FAILED; case REMOVED->throw new IllegalArgumentException(); }; }
  private static CancelDemandOrderOutcome validation(Context c,UUID id,UUID requester,CancelDemandOrderResult result){report(c,failure(c,id,requester,"validation",result,MarketMutationState.UNCHANGED,null,null,null,null,null,null,null));return CancelDemandOrderOutcome.validationFailure(result);}
  private static CancelDemandOrderFailure failure(Context c,UUID id,UUID requester,String stage,CancelDemandOrderResult result,MarketMutationState state,DemandOrderRemovalStatus removal,BalanceMutationResult refund,MarketOrderRestoreResult restore,RuntimeException primary,RuntimeException repository,RuntimeException refundError,RuntimeException restoreError){return new CancelDemandOrderFailure(id,c.actorId(),requester,c.operator(),stage,result,state,removal,refund,restore,primary,repository,refundError,restoreError);}
  private static void report(Context c,CancelDemandOrderFailure failure){try{c.reporter().report(failure);}catch(RuntimeException ignored){}}
  public record Context(UUID actorId,boolean operator,Account account,Repository repository,FailureReporter reporter){public Context{Objects.requireNonNull(actorId);Objects.requireNonNull(account);Objects.requireNonNull(repository);Objects.requireNonNull(reporter);}}
  public interface Account{BalanceMutationResult previewCreditExact(UUID owner,int amount);BalanceMutationResult creditExact(UUID owner,int amount);}
  public interface Repository{MarketOrder find(UUID id);DemandOrderRemovalResult removeUndeliveredDemandIfUnchanged(UUID id,MarketOrder expected);}
  public interface FailureReporter{void report(CancelDemandOrderFailure failure);static FailureReporter noop(){return failure->{};}}
}
