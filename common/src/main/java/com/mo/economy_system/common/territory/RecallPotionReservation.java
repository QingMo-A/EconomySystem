package com.mo.economy_system.common.territory;

import java.util.Objects;

/** Owns removal and one-shot, conflict-safe restoration of exactly one recall potion. */
public final class RecallPotionReservation<T> implements TerritoryTeleportService.Reservation {
  public interface Slots<T> {
    int size(); T get(int slot); void set(int slot, T value) throws Exception;
    T copy(T value); T withCount(T value, int count); boolean equivalent(T left, T right); boolean empty(T value);
    boolean canMerge(T existing, T removed); int count(T value); int maximum(T value);
    T withAddedOne(T existing, T removed); void markChanged() throws Exception;
    void synchronizeClient() throws Exception; void warning(String stage, Exception error);
  }
  private enum State { RESERVED, COMMITTED, ROLLED_BACK, ROLLBACK_FAILED }
  private final int slot; private final T removed; private final T expectedRemaining; private final Slots<T> slots;
  private State state = State.RESERVED;

  private RecallPotionReservation(int slot, T removed, T expectedRemaining, Slots<T> slots) {
    this.slot=slot;this.removed=removed;this.expectedRemaining=expectedRemaining;this.slots=slots;
  }

  public static <T> RecallPotionReservation<T> reserve(int slot, T originalStack, Slots<T> slots)
      throws RecallPotionReserveException {
    Objects.requireNonNull(originalStack,"originalStack");Objects.requireNonNull(slots,"slots");
    if(slot<0||slot>=slots.size())throw new IllegalArgumentException("invalid slot");
    T original=checkedCopy(slots,originalStack,"original");int originalCount=slots.count(original);
    if(slots.empty(original)||originalCount<1)throw new IllegalArgumentException("original stack is empty");
    T removed=checkedCount(slots,slots.withCount(slots.copy(original),1),1,"removedPotion");
    T remaining=checkedCount(slots,slots.withCount(slots.copy(original),originalCount-1),originalCount-1,"expectedRemaining");
    Exception primary;
    try {
      slots.set(slot,slots.copy(remaining)); requireExact(slots,slots.get(slot),remaining,"reserve write");
      slots.markChanged();
    } catch(Exception error) {
      primary=error;
      try {
        slots.set(slot,slots.copy(original));requireExact(slots,slots.get(slot),original,"reserve compensation");slots.markChanged();
        sync(slots);throw new RecallPotionReserveException(slot,false,primary);
      } catch(RecallPotionReserveException finished){throw finished;}
      catch(Exception rollbackError){if(rollbackError!=primary)primary.addSuppressed(rollbackError);throw new RecallPotionReserveException(slot,true,primary);}
    }
    sync(slots);
    return new RecallPotionReservation<>(slot,removed,remaining,slots);
  }

  public int slot(){return slot;}
  public synchronized void commit(){if(state==State.COMMITTED)return;if(state!=State.RESERVED)throw new IllegalStateException("reservation already completed: "+state);state=State.COMMITTED;}
  public synchronized void rollback() throws Exception {
    if(state!=State.RESERVED)throw new IllegalStateException("reservation already completed: "+state);
    int destination=-1;T replacement=null;T current=slots.get(slot);
    if(slots.equivalent(current,expectedRemaining)&&slots.count(current)==slots.count(expectedRemaining)){
      destination=slot;replacement=validatedReplacement(current);
    }else{
      for(int i=0;i<slots.size();i++){T candidate=slots.get(i);if(slots.canMerge(candidate,removed)&&slots.equivalent(slots.withCount(slots.copy(candidate),1),removed)&&slots.count(candidate)<slots.maximum(candidate)){destination=i;replacement=validatedReplacement(candidate);break;}}
      if(destination<0)for(int i=0;i<slots.size();i++)if(slots.empty(slots.get(i))){destination=i;replacement=checkedCount(slots,slots.copy(removed),1,"empty-slot replacement");break;}
    }
    if(destination<0){state=State.ROLLBACK_FAILED;throw new IllegalStateException("no safe recall-potion rollback slot");}
    Exception writeError=null;
    try{slots.set(destination,replacement);}catch(Exception error){writeError=error;}
    try{requireExact(slots,slots.get(destination),replacement,"rollback write");}
    catch(Exception mismatch){state=State.ROLLBACK_FAILED;if(writeError!=null&&writeError!=mismatch)mismatch.addSuppressed(writeError);throw mismatch;}
    if(writeError!=null)warn(slots,"rollback-write",writeError);
    try{slots.markChanged();}catch(Exception error){warn(slots,"rollback-dirty",error);}
    state=State.ROLLED_BACK;sync(slots);
  }
  private T validatedReplacement(T previous){int before=slots.count(previous);T value=slots.withAddedOne(slots.copy(previous),slots.copy(removed));
    if(slots.empty(value)||slots.count(value)!=before+1||slots.count(value)>slots.maximum(value))throw new IllegalArgumentException("invalid rollback replacement");return value;}
  private static <T>T checkedCopy(Slots<T>s,T value,String name){T copy=Objects.requireNonNull(s.copy(value),name);if(s.count(copy)<0)throw new IllegalArgumentException(name+" has negative count");return copy;}
  private static <T>T checkedCount(Slots<T>s,T value,int count,String name){Objects.requireNonNull(value,name);if(s.count(value)!=count||count<0||(count>0&&s.empty(value)))throw new IllegalArgumentException("invalid "+name);return value;}
  private static <T>void requireExact(Slots<T>s,T actual,T expected,String stage){if(!s.equivalent(actual,expected)||s.count(actual)!=s.count(expected))throw new IllegalStateException(stage+" was not applied");}
  private static <T>void sync(Slots<T>s){try{s.synchronizeClient();}catch(Exception e){warn(s,"inventory-sync",e);}}
  private static <T>void warn(Slots<T>s,String stage,Exception e){try{s.warning(stage,e);}catch(Exception ignored){}}
}
