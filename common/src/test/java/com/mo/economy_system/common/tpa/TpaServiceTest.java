package com.mo.economy_system.common.tpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TpaServiceTest {
  private static final UUID SENDER = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID TARGET = UUID.fromString("00000000-0000-0000-0000-000000000002");

  @Test
  void sendAcceptConsumesPotionOnlyAfterRequestAndSuccessfulArrival() {
    FakePort port = new FakePort();
    TpaService service = new TpaService(new TpaRequestStore(10, 10), port);

    assertEquals(TpaService.SendResult.SUCCESS, service.send(SENDER, TARGET, 0));
    assertEquals(0, port.reserves);
    assertEquals(TpaService.AcceptResult.SUCCESS, service.accept(TARGET, 1).result());
    assertEquals(1, port.reserves);
    assertEquals(1, port.commits);
    assertEquals(0, port.rollbacks);
    assertEquals(List.of(SENDER + "->" + TARGET), port.teleports);
  }

  @Test
  void selfNoPotionAndBusyRequestsAreRejected() {
    FakePort port = new FakePort();
    TpaService service = new TpaService(new TpaRequestStore(), port);
    assertEquals(TpaService.SendResult.SELF, service.send(SENDER, SENDER, 0));
    port.potion = false;
    assertEquals(TpaService.SendResult.NO_POTION, service.send(SENDER, TARGET, 0));
    port.potion = true;
    assertEquals(TpaService.SendResult.SUCCESS, service.send(SENDER, TARGET, 0));
    assertEquals(TpaService.SendResult.TARGET_BUSY, service.send(UUID.randomUUID(), TARGET, 1));
    assertEquals(TpaService.SendResult.SENDER_BUSY, service.send(SENDER, UUID.randomUUID(), 1));
  }

  @Test
  void offlineSenderCanRetryBeforeExpiry() {
    FakePort port = new FakePort();
    TpaService service = new TpaService(new TpaRequestStore(), port);
    assertEquals(TpaService.SendResult.SUCCESS, service.send(SENDER, TARGET, 0));
    port.online = false;
    assertEquals(TpaService.AcceptResult.SENDER_OFFLINE, service.accept(TARGET, 1).result());
    assertEquals(1, service.pendingCount());
    port.online = true;
    assertEquals(TpaService.AcceptResult.SUCCESS, service.accept(TARGET, 2).result());
  }

  @Test
  void senderWithoutPotionKeepsRequestForRetry() {
    FakePort port = new FakePort();
    TpaService service = new TpaService(new TpaRequestStore(), port);
    assertEquals(TpaService.SendResult.SUCCESS, service.send(SENDER, TARGET, 0));

    port.potion = false;
    assertEquals(TpaService.AcceptResult.SENDER_NO_POTION, service.accept(TARGET, 1).result());
    assertEquals(1, service.pendingCount());

    port.potion = true;
    assertEquals(TpaService.AcceptResult.SUCCESS, service.accept(TARGET, 2).result());
    assertEquals(0, service.pendingCount());
  }

  @Test
  void failedRequestRestoreReturnsStateUnknown() {
    TpaRequestStore requests = new TpaRequestStore();
    FakePort port = new FakePort();
    port.beforeReserve = () -> requests.create(UUID.randomUUID(), TARGET, 1);
    TpaService service = new TpaService(requests, port);
    service.send(SENDER, TARGET, 0);

    port.potion = false;
    assertEquals(
        TpaService.AcceptResult.TELEPORT_STATE_UNKNOWN,
        service.accept(TARGET, 1).result());
  }

  @Test
  void failedArrivalRollsBackPotionAndConsumesRequest() {
    FakePort port = new FakePort();
    port.arrival = TpaPort.TeleportArrival.NOT_ARRIVED;
    TpaService service = new TpaService(new TpaRequestStore(), port);
    service.send(SENDER, TARGET, 0);

    assertEquals(TpaService.AcceptResult.TELEPORT_FAILED, service.accept(TARGET, 1).result());
    assertEquals(1, port.rollbacks);
    assertEquals(0, service.pendingCount());
  }

  @Test
  void unknownArrivalCommitsWithoutAutomaticRefund() {
    FakePort port = new FakePort();
    port.arrival = TpaPort.TeleportArrival.UNKNOWN;
    TpaService service = new TpaService(new TpaRequestStore(), port);
    service.send(SENDER, TARGET, 0);

    assertEquals(
        TpaService.AcceptResult.TELEPORT_STATE_UNKNOWN,
        service.accept(TARGET, 1).result());
    assertEquals(1, port.commits);
    assertEquals(0, port.rollbacks);
  }

  @Test
  void denyAndTimeoutReturnTheStoredTargetIdentity() {
    FakePort port = new FakePort();
    TpaService service = new TpaService(new TpaRequestStore(10, 10), port);
    service.send(SENDER, TARGET, 0);
    TpaRequest request = service.expire(10).get(0);
    assertEquals(TARGET, request.targetId());
    service.send(SENDER, TARGET, 20);
    assertEquals(TpaService.DenyResult.SUCCESS, service.deny(TARGET, 21).result());
    assertEquals(TpaService.DenyResult.NO_REQUEST, service.deny(TARGET, 22).result());
  }

  @Test
  void reservationFailureWithFailedRollbackIsNotRetried() {
    FakePort port = new FakePort();
    port.reservationFailure = new TpaReservationException(3, true, new IllegalStateException("dirty"));
    List<String> diagnostics = new ArrayList<>();
    TpaService service = new TpaService(new TpaRequestStore(), port,
        (stage, request, slot, primary, secondary) -> diagnostics.add(stage));
    service.send(SENDER, TARGET, 0);

    assertEquals(TpaService.AcceptResult.ROLLBACK_FAILED, service.accept(TARGET, 1).result());
    assertEquals(0, service.pendingCount());
    assertEquals(List.of("reserve"), diagnostics);
  }

  private static final class FakePort implements TpaPort {
    boolean online = true;
    boolean potion = true;
    TeleportArrival arrival = TeleportArrival.ARRIVED;
    int reserves;
    int commits;
    int rollbacks;
    TpaReservationException reservationFailure;
    Runnable beforeReserve;
    final List<String> teleports = new ArrayList<>();

    public boolean isOnline(UUID id) { return online; }
    public boolean hasWormholePotion(UUID id) { return potion; }
    public Optional<PotionReservation> reserveWormholePotion(UUID id) throws Exception {
      if (beforeReserve != null) beforeReserve.run();
      if (reservationFailure != null) throw reservationFailure;
      if (!potion) return Optional.empty();
      reserves++;
      return Optional.of(new PotionReservation() {
        public int slot() { return 0; }
        public void commit() { commits++; }
        public void rollback() { rollbacks++; }
      });
    }
    public void teleport(UUID senderId, UUID targetId) { teleports.add(senderId + "->" + targetId); }
    public TeleportArrival arrival(UUID senderId, UUID targetId) { return arrival; }
    public void effects(UUID senderId, UUID targetId) {}
  }
}
