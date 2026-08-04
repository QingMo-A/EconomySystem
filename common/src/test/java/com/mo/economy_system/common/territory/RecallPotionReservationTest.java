package com.mo.economy_system.common.territory;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecallPotionReservationTest {
  record Stack(String item,int count,int max) { boolean empty(){return count==0;} }
  @Test void restoresOriginalSlotWhenUnchanged() throws Exception {
    Fake slots=new Fake(new Stack("air",0,64),new Stack("stone",1,64));
    var reservation=reservation(slots); reservation.rollback();
    assertEquals(new Stack("potion",1,1),slots.get(0));assertEquals(1,slots.changed);
    assertThrows(IllegalStateException.class,reservation::rollback);
  }
  @Test void conflictNeverOverwritesAndUsesEmptySlot() throws Exception {
    Fake slots=new Fake(new Stack("diamond",1,64),new Stack("air",0,64));
    var reservation=reservation(slots);reservation.rollback();
    assertEquals("diamond",slots.get(0).item());assertEquals("potion",slots.get(1).item());
  }
  @Test void mergePrecedesEmptySlot() throws Exception {
    Fake slots=new Fake(new Stack("diamond",1,64),new Stack("potion",1,2),new Stack("air",0,64));
    reservation(slots).rollback();assertEquals(2,slots.get(1).count());assertTrue(slots.get(2).empty());
  }
  @Test void noSafeSlotFailsWithoutMutation() {
    Fake slots=new Fake(new Stack("diamond",64,64),new Stack("stone",64,64));List<Stack> before=List.copyOf(slots.values);
    var reservation=reservation(slots);assertThrows(IllegalStateException.class,reservation::rollback);
    assertEquals(before,slots.values);assertEquals(0,slots.changed);
  }
  @Test void commitIsIdempotentAndPreventsRollback() throws Exception {
    Fake slots=new Fake(new Stack("air",0,64));var reservation=reservation(slots);
    reservation.commit();reservation.commit();assertThrows(IllegalStateException.class,reservation::rollback);
  }
  private static RecallPotionReservation<Stack> reservation(Fake slots){return new RecallPotionReservation<>(0,new Stack("potion",1,1),new Stack("air",0,64),slots);}
  private static final class Fake implements RecallPotionReservation.Slots<Stack> {
    final List<Stack> values;int changed;
    Fake(Stack... values){this.values=new ArrayList<>(List.of(values));}
    public int size(){return values.size();}public Stack get(int slot){return values.get(slot);}public void set(int slot,Stack value){values.set(slot,value);}
    public Stack copy(Stack value){return value;}public boolean equivalent(Stack a,Stack b){return a.equals(b);}public boolean empty(Stack value){return value.empty();}
    public boolean canMerge(Stack a,Stack b){return !a.empty()&&a.item().equals(b.item());}public int count(Stack value){return value.count();}public int maximum(Stack value){return value.max();}
    public Stack withAddedOne(Stack existing,Stack removed){return existing.empty()?removed:new Stack(existing.item(),existing.count()+1,existing.max());}public void changed(){changed++;}
  }
}
