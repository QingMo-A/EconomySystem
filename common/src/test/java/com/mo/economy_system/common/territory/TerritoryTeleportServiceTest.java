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
    return new TerritoryTeleportService<>(id -> { if(fake.repositoryJvmError) throw new AssertionError("jvm"); if(fake.repositoryError) throw new IllegalStateException("repository"); if(fake.nullOptional)return null; return fake.exists ? Optional.of(new TerritoryTeleportTarget(fake.mismatch?UUID.randomUUID():ID,"Home",OWNER,
        Set.of(MEMBER),"minecraft:overworld",fake.backpoint?Optional.of(new Position(1,2,3)):Optional.empty())) : Optional.empty(); },
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
    fake.teleportThrows=true; assertEquals(TerritoryTeleportResult.SUCCESS,service().execute(OWNER,ID,1).result()); assertEquals(0,fake.rollbacks); assertEquals(1,fake.commits);
  }
  @Test void rollbackFailureIsExplicitAndKeepsBothErrors() {
    fake.arrived=false; fake.teleportThrows=true; fake.rollbackThrows=true;
    assertEquals(TerritoryTeleportResult.ROLLBACK_FAILED,service().execute(OWNER,ID,1).result());
    assertNotNull(fake.primary); assertNotNull(fake.secondary);
    assertSame(fake.secondary,fake.primary.getSuppressed()[0]);
  }
  @Test void effectsAreBestEffort() {
    fake.effectsThrow=true; assertEquals(TerritoryTeleportResult.SUCCESS,service().execute(OWNER,ID,1).result());
  }
  @Test void limiterIsBoundedAndCleans() {
    TerritoryTeleportRateLimiter limiter=new TerritoryTeleportRateLimiter(20,2);
    limiter.tryAcquire(UUID.randomUUID(),0); limiter.tryAcquire(UUID.randomUUID(),0); limiter.tryAcquire(UUID.randomUUID(),0);
    assertTrue(limiter.size()<=2); limiter.tryAcquire(UUID.randomUUID(),100); assertTrue(limiter.size()<=2);
  }
  @Test void limiterCleansExpiredEntriesBehindActiveOnesAndDefinesBoundaries() {
    TerritoryTeleportRateLimiter limiter=new TerritoryTeleportRateLimiter(20,10);
    UUID a=UUID.randomUUID(),b=UUID.randomUUID(),c=UUID.randomUUID();
    assertTrue(limiter.tryAcquire(a,0));assertTrue(limiter.tryAcquire(b,10));assertTrue(limiter.tryAcquire(a,20));
    assertTrue(limiter.tryAcquire(c,91));assertEquals(2,limiter.size());
    assertFalse(limiter.tryAcquire(c,92));assertTrue(limiter.tryAcquire(c,111));
    assertTrue(limiter.tryAcquire(c,110));assertEquals(1,limiter.size());
  }
  @Test void validatesInputsAndDependencies() {
    assertThrows(NullPointerException.class,()->new TerritoryTeleportService<>(null,fake,fake,new TerritoryTeleportRateLimiter(),fake));
    assertThrows(NullPointerException.class,()->new TerritoryTeleportService<>(id->Optional.empty(),null,fake,new TerritoryTeleportRateLimiter(),fake));
    assertThrows(NullPointerException.class,()->new TerritoryTeleportService<>(id->Optional.empty(),fake,null,new TerritoryTeleportRateLimiter(),fake));
    assertThrows(NullPointerException.class,()->new TerritoryTeleportService<>(id->Optional.empty(),fake,fake,null,fake));
    assertThrows(NullPointerException.class,()->new TerritoryTeleportService<>(id->Optional.empty(),fake,fake,new TerritoryTeleportRateLimiter(),null));
    assertThrows(NullPointerException.class,()->service().execute(null,ID,1));
    assertThrows(NullPointerException.class,()->service().execute(OWNER,null,1));
    assertThrows(IllegalArgumentException.class,()->service().execute(OWNER,ID,-1));
    assertThrows(IllegalArgumentException.class,()->new TerritoryTeleportTarget(ID," ",OWNER,Set.of(),"minecraft:overworld",Optional.empty()));
    assertThrows(IllegalArgumentException.class,()->new TerritoryTeleportTarget(ID,"Home",OWNER,Set.of()," ",Optional.empty()));
  }
  @Test void infrastructureExceptionsAreGenericFailures() {
    fake.repositoryError=true; assertEquals(TerritoryTeleportResult.TELEPORT_FAILED,service().execute(OWNER,ID,1).result());
    fake.repositoryError=false;fake.nullOptional=true;assertEquals(TerritoryTeleportResult.TELEPORT_FAILED,service().execute(OWNER,ID,1).result());
    fake.nullOptional=false;fake.mismatch=true;assertEquals(TerritoryTeleportResult.TELEPORT_FAILED,service().execute(OWNER,ID,1).result());assertEquals(0,fake.reserves);
    fake.mismatch=false;fake.resolveError=true;assertEquals(TerritoryTeleportResult.TELEPORT_FAILED,service().execute(OWNER,ID,1).result());
    fake.resolveError=false;fake.prepareError=true;assertEquals(TerritoryTeleportResult.TELEPORT_FAILED,service().execute(OWNER,ID,1).result());
    fake.prepareError=false;fake.inventoryError=true;assertEquals(TerritoryTeleportResult.TELEPORT_FAILED,service().execute(OWNER,ID,1).result());
  }
  @Test void jvmErrorsAreNotCaught() { fake.repositoryJvmError=true; assertThrows(AssertionError.class,()->service().execute(OWNER,ID,1)); }
  @Test void unknownArrivalCommitsWithoutRefund() {
    fake.arrivalError=true;assertEquals(TerritoryTeleportResult.TELEPORT_STATE_UNKNOWN,service().execute(OWNER,ID,1).result());assertEquals(1,fake.commits);assertEquals(0,fake.rollbacks);
  }
  @Test void commitRuntimeAfterArrivalDoesNotChangeSuccessFact() {
    fake.commitError=true;assertEquals(TerritoryTeleportResult.SUCCESS,service().execute(OWNER,ID,1).result());assertEquals(0,fake.rollbacks);
  }
  private static final class Fake implements TerritoryTeleportService.DestinationAdapter<String>, TerritoryTeleportService.Inventory, TerritoryTeleportService.Diagnostics {
    boolean exists=true,backpoint=true,dimension=true,safe=true,potion=true,arrived=true,teleportThrows,rollbackThrows,effectsThrow;
    boolean repositoryError,nullOptional,mismatch,resolveError,prepareError,inventoryError,repositoryJvmError,arrivalError,unknown,commitError;
    int reserves,rollbacks,commits; Throwable primary,secondary;
    public Optional<String> resolve(String id){if(resolveError)throw new IllegalStateException();return dimension?Optional.of(id):Optional.empty();}
    public boolean prepareAndValidate(String d,Position p){if(prepareError)throw new IllegalStateException();return safe;}
    public void teleport(String d,Position p){if(teleportThrows)throw new IllegalStateException("teleport");}
    public TerritoryTeleportArrival arrival(String d,Position p){if(arrivalError)throw new IllegalStateException("arrival");return unknown?TerritoryTeleportArrival.UNKNOWN:(arrived?TerritoryTeleportArrival.ARRIVED:TerritoryTeleportArrival.NOT_ARRIVED);}
    public void particles(String d,Position p){if(effectsThrow)throw new IllegalStateException();}
    public void sound(String d,Position p){if(effectsThrow)throw new IllegalStateException();}
    public Optional<TerritoryTeleportService.Reservation> reserveRecallPotion(){if(inventoryError)throw new IllegalStateException();reserves++; if(!potion)return Optional.empty(); return Optional.of(new TerritoryTeleportService.Reservation(){public int slot(){return 0;}public void commit(){commits++;if(commitError)throw new IllegalStateException("commit");}public void rollback(){rollbacks++;if(rollbackThrows)throw new IllegalStateException("rollback");}});}
    public void warning(String s,UUID p,UUID t,int slot,Throwable a,Throwable b){primary=a;secondary=b;}
  }
}
