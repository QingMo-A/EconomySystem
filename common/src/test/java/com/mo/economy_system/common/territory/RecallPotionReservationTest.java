package com.mo.economy_system.common.territory;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecallPotionReservationTest {
  record Stack(String item,int count,int max) { boolean empty(){return count==0;} }
  @Test void restoresOriginalSlotWhenUnchanged() throws Exception {
    Fake slots=new Fake(new Stack("potion",1,1),new Stack("stone",1,64));
    var reservation=reservation(slots); reservation.rollback();
    assertEquals(new Stack("potion",1,1),slots.get(0));assertEquals(2,slots.changed);
    assertThrows(IllegalStateException.class,reservation::rollback);
  }
  @Test void conflictNeverOverwritesAndUsesEmptySlot() throws Exception {
    Fake slots=new Fake(new Stack("potion",1,1),new Stack("air",0,64));
    var reservation=reservation(slots);slots.set(0,new Stack("diamond",1,64));reservation.rollback();
    assertEquals("diamond",slots.get(0).item());assertEquals("potion",slots.get(1).item());
  }
  @Test void mergePrecedesEmptySlot() throws Exception {
    Fake slots=new Fake(new Stack("potion",1,1),new Stack("potion",1,2),new Stack("air",0,64));
    var reservation=reservation(slots);slots.set(0,new Stack("diamond",1,64));reservation.rollback();assertEquals(2,slots.get(1).count());assertTrue(slots.get(2).empty());
  }
  @Test void noSafeSlotFailsWithoutMutation() throws Exception {
    Fake slots=new Fake(new Stack("potion",1,1),new Stack("stone",64,64));var reservation=reservation(slots);slots.set(0,new Stack("diamond",64,64));List<Stack> before=List.copyOf(slots.values);
    assertThrows(IllegalStateException.class,reservation::rollback);
    assertEquals(before,slots.values);assertEquals(1,slots.changed);
  }
  @Test void commitIsIdempotentAndPreventsRollback() throws Exception {
    Fake slots=new Fake(new Stack("potion",1,1));var reservation=reservation(slots);
    reservation.commit();reservation.commit();assertThrows(IllegalStateException.class,reservation::rollback);
  }
  @Test void reserveWriteAndDirtyFailuresRestoreOriginal() {
    Fake write=new Fake(new Stack("potion",1,1));write.throwSetCalls.add(1);
    RecallPotionReserveException first=assertThrows(RecallPotionReserveException.class,()->reservation(write));assertFalse(first.rollbackFailed());assertEquals("potion",write.get(0).item());
    Fake dirty=new Fake(new Stack("potion",1,1));dirty.throwMarkCalls.add(1);
    RecallPotionReserveException second=assertThrows(RecallPotionReserveException.class,()->reservation(dirty));assertFalse(second.rollbackFailed());assertEquals("potion",dirty.get(0).item());
  }
  @Test void reserveCompensationFailureIsExplicitAndSuppressed() {
    Fake slots=new Fake(new Stack("potion",1,1));slots.throwMarkCalls.add(1);slots.throwSetCalls.add(2);
    RecallPotionReserveException error=assertThrows(RecallPotionReserveException.class,()->reservation(slots));assertTrue(error.rollbackFailed());assertEquals(1,error.getCause().getSuppressed().length);
  }
  @Test void setThrowsAfterWritingDoesNotRestoreTwice() throws Exception {
    Fake slots=new Fake(new Stack("potion",1,1),new Stack("air",0,64));var reservation=reservation(slots);slots.throwAfterSetCalls.add(2);reservation.rollback();
    assertEquals(1,slots.values.stream().mapToInt(v->v.item().equals("potion")?v.count():0).sum());
  }
  @Test void syncFailureIsBestEffortForReserveAndRollback() throws Exception {
    Fake slots=new Fake(new Stack("potion",1,1));slots.syncThrows=true;var reservation=reservation(slots);reservation.rollback();assertEquals(2,slots.warnings);
  }
  @Test void illegalRemovedOrReplacementCountsAreRejected() throws Exception {
    Fake removed=new Fake(new Stack("potion",1,1));removed.badRemoved=true;assertThrows(IllegalArgumentException.class,()->reservation(removed));
    Fake replacement=new Fake(new Stack("potion",1,1));var reservation=reservation(replacement);replacement.badAdd=true;assertThrows(IllegalArgumentException.class,reservation::rollback);
  }
  private static RecallPotionReservation<Stack> reservation(Fake slots) throws RecallPotionReserveException{return RecallPotionReservation.reserve(0,slots.get(0),slots);}
  private static final class Fake implements RecallPotionReservation.Slots<Stack> {
    final List<Stack> values;int changed,setCalls,markCalls,warnings;boolean syncThrows,badRemoved,badAdd;final List<Integer> throwSetCalls=new ArrayList<>(),throwAfterSetCalls=new ArrayList<>(),throwMarkCalls=new ArrayList<>();
    Fake(Stack... values){this.values=new ArrayList<>(List.of(values));}
    public int size(){return values.size();}public Stack get(int slot){return values.get(slot);}public void set(int slot,Stack value){setCalls++;if(throwSetCalls.contains(setCalls))throw new IllegalStateException("set");values.set(slot,value);if(throwAfterSetCalls.contains(setCalls))throw new IllegalStateException("set-after");}
    public Stack copy(Stack value){return value;}public Stack withCount(Stack value,int count){return new Stack(value.item(),badRemoved&&count==1?2:count,value.max());}public boolean equivalent(Stack a,Stack b){return a.item().equals(b.item())||a.empty()&&b.empty();}public boolean empty(Stack value){return value.empty();}
    public boolean canMerge(Stack a,Stack b){return !a.empty()&&a.item().equals(b.item());}public int count(Stack value){return value.count();}public int maximum(Stack value){return value.max();}
    public Stack withAddedOne(Stack existing,Stack removed){if(badAdd)return new Stack("potion",99,1);return existing.empty()?removed:new Stack(existing.item(),existing.count()+1,existing.max());}public void markChanged(){markCalls++;if(throwMarkCalls.contains(markCalls))throw new IllegalStateException("dirty");changed++;}public void synchronizeClient(){if(syncThrows)throw new IllegalStateException("sync");}public void warning(String stage,Exception error){warnings++;}
  }
}
