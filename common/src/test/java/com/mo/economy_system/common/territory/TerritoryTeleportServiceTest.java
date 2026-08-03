package com.mo.economy_system.common.territory;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryTeleportServiceTest {
  private static final UUID OWNER=UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID MEMBER=UUID.fromString("00000000-0000-0000-0000-000000000002");
  private static final UUID ID=UUID.fromString("00000000-0000-0000-0000-000000000003");
  private final Fake fake=new Fake();
  private TerritoryTeleportService<String> service() {
    return new TerritoryTeleportService<>(id -> fake.exists ? Optional.of(new TerritoryTeleportTarget(ID,"Home",OWNER,
        Set.of(MEMBER),"minecraft:overworld",fake.backpoint?Optional.of(new Position(1,2,3)):Optional.empty())) : Optional.empty(),
        fake,fake,new TerritoryTeleportRateLimiter(),fake);
  }
  @Test void validatesBeforeInventory() {
    fake.exists=false; assertEquals(TerritoryTeleportResult.TERRITORY_NOT_FOUND,service().execute(OWNER,ID,1).result());
    fake.exists=true; assertEquals(TerritoryTeleportResult.NO_PERMISSION,service().execute(UUID.randomUUID(),ID,1).result());
    fake.backpoint=false; assertEquals(TerritoryTeleportResult.NO_BACKPOINT,service().execute(OWNER,ID,1).result());
    assertEquals(0,fake.reserves);
  }
  @Test void ownerAndMemberSucceedAndConsumeOne() {
    assertEquals(TerritoryTeleportResult.SUCCESS,service().execute(OWNER,ID,1).result());
    assertEquals(1,fake.reserves); assertEquals(0,fake.rollbacks);
    fake.reserves=0;
    assertEquals(TerritoryTeleportResult.SUCCESS,service().execute(MEMBER,ID,1).result());
  }
  @Test void rejectsDimensionUnsafePotionAndCooldown() {
    fake.dimension=false; assertEquals(TerritoryTeleportResult.DIMENSION_NOT_FOUND,service().execute(OWNER,ID,1).result());
    fake.dimension=true; fake.safe=false; assertEquals(TerritoryTeleportResult.UNSAFE_DESTINATION,service().execute(OWNER,ID,1).result());
    fake.safe=true; fake.potion=false; assertEquals(TerritoryTeleportResult.NO_RECALL_POTION,service().execute(OWNER,ID,1).result());
    fake.potion=true; TerritoryTeleportService<String> s=service(); assertEquals(TerritoryTeleportResult.SUCCESS,s.execute(OWNER,ID,20).result());
    assertEquals(TerritoryTeleportResult.COOLDOWN,s.execute(OWNER,ID,21).result());
  }
  @Test void teleportFailureRollsBack() {
    fake.arrived=false; assertEquals(TerritoryTeleportResult.TELEPORT_FAILED,service().execute(OWNER,ID,1).result()); assertEquals(1,fake.rollbacks);
  }
  @Test void thrownTeleportButArrivedCommits() {
    fake.teleportThrows=true; assertEquals(TerritoryTeleportResult.SUCCESS,service().execute(OWNER,ID,1).result()); assertEquals(0,fake.rollbacks);
  }
  @Test void rollbackFailureIsExplicitAndKeepsBothErrors() {
    fake.arrived=false; fake.teleportThrows=true; fake.rollbackThrows=true;
    assertEquals(TerritoryTeleportResult.ROLLBACK_FAILED,service().execute(OWNER,ID,1).result());
    assertNotNull(fake.primary); assertNotNull(fake.secondary);
  }
  @Test void effectsAreBestEffort() {
    fake.effectsThrow=true; assertEquals(TerritoryTeleportResult.SUCCESS,service().execute(OWNER,ID,1).result());
  }
  @Test void limiterIsBoundedAndCleans() {
    TerritoryTeleportRateLimiter limiter=new TerritoryTeleportRateLimiter(20,2);
    limiter.tryAcquire(UUID.randomUUID(),0); limiter.tryAcquire(UUID.randomUUID(),0); limiter.tryAcquire(UUID.randomUUID(),0);
    assertTrue(limiter.size()<=2); limiter.tryAcquire(UUID.randomUUID(),100); assertTrue(limiter.size()<=2);
  }
  private static final class Fake implements TerritoryTeleportService.DestinationAdapter<String>, TerritoryTeleportService.Inventory, TerritoryTeleportService.Diagnostics {
    boolean exists=true,backpoint=true,dimension=true,safe=true,potion=true,arrived=true,teleportThrows,rollbackThrows,effectsThrow;
    int reserves,rollbacks; Throwable primary,secondary;
    public Optional<String> resolve(String id){return dimension?Optional.of(id):Optional.empty();}
    public boolean prepareAndValidate(String d,Position p){return safe;}
    public void teleport(String d,Position p){if(teleportThrows)throw new IllegalStateException("teleport");}
    public boolean arrived(String d,Position p){return arrived;}
    public void particles(String d,Position p){if(effectsThrow)throw new IllegalStateException();}
    public void sound(String d,Position p){if(effectsThrow)throw new IllegalStateException();}
    public Optional<TerritoryTeleportService.Reservation> reserveRecallPotion(){reserves++; if(!potion)return Optional.empty(); return Optional.of(new TerritoryTeleportService.Reservation(){public int slot(){return 0;}public void rollback(){rollbacks++;if(rollbackThrows)throw new IllegalStateException("rollback");}});}
    public void warning(String s,UUID p,UUID t,int slot,Throwable a,Throwable b){primary=a;secondary=b;}
  }
}
