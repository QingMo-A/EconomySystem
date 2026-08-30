package com.mo.economy_system.common.commission;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CommissionServiceTest {
  private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID COMMISSION = UUID.fromString("00000000-0000-0000-0000-000000000002");

  @Test
  void refreshKeepsOldWorkAndAddsOnlyWhenDue() {
    InMemoryCommissionRepository repository = new InMemoryCommissionRepository();
    CommissionTemplate template = template(2, 10);
    CommissionGenerator generator = new CommissionGenerator(
        catalog(template), fixedRandom(), CommissionRewardPolicy.defaultPolicy(), () -> COMMISSION);
    repository.save(new CommissionPlayerState(
        PLAYER, List.of(), PersonalCommissionSchedule.initial(PLAYER, 1, 10)));
    CommissionService service = new CommissionService(generator, repository, repository, delivery(repository));

    CommissionService.RefreshView notDue = service.refresh(PLAYER, 5);
    assertEquals(CommissionGenerator.RefreshOutcome.NOT_DUE, notDue.generation().outcome());
    CommissionService.RefreshView due = service.refresh(PLAYER, 11);
    assertEquals(CommissionGenerator.RefreshOutcome.GENERATED, due.generation().outcome());
    assertEquals(1, due.state().commissions().size());
    assertEquals(11, due.state().commissions().get(0).generatedAt());
  }

  @Test
  void firstRefreshGeneratesImmediatelyEvenWhenClockIsNearZero() {
    InMemoryCommissionRepository repository = new InMemoryCommissionRepository();
    CommissionTemplate template = template(1, 10);
    CommissionGenerator generator = new CommissionGenerator(
        catalog(template), fixedRandom(), CommissionRewardPolicy.defaultPolicy(), () -> COMMISSION);
    CommissionService service = new CommissionService(generator, repository, repository,
        delivery(repository));

    CommissionService.RefreshView first = service.refresh(PLAYER, 1);

    assertEquals(CommissionGenerator.RefreshOutcome.GENERATED, first.generation().outcome());
    assertEquals(1, first.state().commissions().size());
    assertTrue(first.state().schedule().nextRefreshAt() > 1);
  }

  @Test
  void builtInCatalogProducesBothServerSafeWorkKinds() {
    CommissionCatalog catalog = CommissionCatalogDefaults.create();
    assertTrue(catalog.template("vanilla_material_delivery").isPresent());
    assertTrue(catalog.template("vanilla_hunt").isPresent());
    assertEquals(2, catalog.legalTemplates().size());
  }

  @Test
  void adminGenerationCountsOnlyLiveWorkAgainstCapacity() {
    InMemoryCommissionRepository repository = new InMemoryCommissionRepository();
    CommissionTemplate template = template(1, 10);
    CommissionGenerator generator = new CommissionGenerator(
        catalog(template), fixedRandom(), CommissionRewardPolicy.defaultPolicy(), () -> COMMISSION);
    CommissionService service = new CommissionService(generator, repository, repository,
        delivery(repository));
    List<CommissionInstance> expired = java.util.stream.IntStream.rangeClosed(3, 8)
        .mapToObj(value -> new CommissionInstance(
            UUID.fromString("00000000-0000-0000-0000-00000000000" + value), PLAYER, "deliver",
            CommissionType.ITEM_DELIVERY, "town", "Town", "minecraft:stone", 1,
            CommissionRewardSnapshot.coins(10), 1, 2).expireIfDue(2))
        .toList();
    repository.save(new CommissionPlayerState(PLAYER, expired, null));

    List<CommissionInstance> generated = service.generate(PLAYER, 10, 1);

    assertEquals(1, generated.size());
  }

  @Test
  void completionCreatesOnePendingRewardAndDuplicateSubmitIsIdempotent() {
    InMemoryCommissionRepository repository = new InMemoryCommissionRepository();
    CommissionTemplate template = template(2, 10);
    CommissionGenerator generator = new CommissionGenerator(
        catalog(template), fixedRandom(), CommissionRewardPolicy.defaultPolicy(), () -> COMMISSION);
    CommissionService service = new CommissionService(generator, repository, repository, delivery(repository));
    CommissionInstance instance = new CommissionInstance(
        COMMISSION, PLAYER, "deliver", CommissionType.ITEM_DELIVERY, "town", "Town",
        "minecraft:stone", 2, CommissionRewardSnapshot.coins(20), 1, 100);
    repository.save(new CommissionPlayerState(PLAYER, List.of(instance), null));

    CommissionService.SubmitResult first = service.submitProgress(PLAYER, COMMISSION, 2, 10);
    assertEquals(CommissionService.SubmitOutcome.REWARD_PENDING_MAIL, first.outcome());
    assertEquals(1, repository.listForPlayer(PLAYER).size());
    CommissionService.SubmitResult duplicate = service.submitProgress(PLAYER, COMMISSION, 1, 11);
    assertEquals(CommissionService.SubmitOutcome.ALREADY_COMPLETED, duplicate.outcome());
    assertEquals(1, repository.listForPlayer(PLAYER).size());
  }

  @Test
  void expiredCommissionCannotGainProgress() {
    InMemoryCommissionRepository repository = new InMemoryCommissionRepository();
    CommissionTemplate template = template(2, 10);
    CommissionGenerator generator = new CommissionGenerator(
        catalog(template), fixedRandom(), CommissionRewardPolicy.defaultPolicy(), () -> COMMISSION);
    CommissionService service = new CommissionService(generator, repository, repository, delivery(repository));
    CommissionInstance instance = new CommissionInstance(
        COMMISSION, PLAYER, "deliver", CommissionType.ITEM_DELIVERY, "town", "Town",
        "minecraft:stone", 2, CommissionRewardSnapshot.coins(20), 1, 10);
    repository.save(new CommissionPlayerState(PLAYER, List.of(instance), null));

    CommissionService.SubmitResult result = service.submitProgress(PLAYER, COMMISSION, 1, 10);
    assertEquals(CommissionService.SubmitOutcome.EXPIRED, result.outcome());
    assertTrue(repository.load(PLAYER).commissions().get(0).status() == CommissionStatus.EXPIRED);
    assertTrue(repository.listForPlayer(PLAYER).isEmpty());
  }

  @Test
  void completedCommissionRetriesPendingMailOnLaterSubmit() {
    InMemoryCommissionRepository repository = new InMemoryCommissionRepository();
    CommissionGenerator generator = new CommissionGenerator(
        catalog(template(1, 10)), fixedRandom(), CommissionRewardPolicy.defaultPolicy(), () -> COMMISSION);
    final boolean[] firstAttempt = {true};
    CommissionRewardDeliveryPort flaky = new CommissionRewardDeliveryPort() {
      @Override public DeliveryResult deliver(CommissionRewardRecord record) {
        if (firstAttempt[0]) { firstAttempt[0] = false; return DeliveryResult.RETRYABLE_FAILURE; }
        repository.save(record.mailCreated(UUID.nameUUIDFromBytes(record.rewardRecordId().toString().getBytes())));
        return DeliveryResult.CREATED;
      }
      @Override public ClaimResult claim(UUID rewardRecordId, UUID playerId, long nowMillis) {
        return ClaimResult.CLAIMED;
      }
    };
    CommissionService service = new CommissionService(generator, repository, repository, flaky);
    repository.save(new CommissionPlayerState(PLAYER, List.of(new CommissionInstance(
        COMMISSION, PLAYER, "deliver", CommissionType.ITEM_DELIVERY, "town", "Town",
        "minecraft:stone", 1, CommissionRewardSnapshot.coins(10), 1, 100)), null));
    assertEquals(CommissionService.SubmitOutcome.REWARD_DELIVERY_RETRY,
        service.submitProgress(PLAYER, COMMISSION, 1, 10).outcome());
    assertEquals(CommissionService.SubmitOutcome.REWARD_PENDING_MAIL,
        service.submitProgress(PLAYER, COMMISSION, 1, 11).outcome());
  }

  @Test
  void duplicatePartialSubmissionIdDoesNotDoubleProgress() {
    InMemoryCommissionRepository repository = new InMemoryCommissionRepository();
    CommissionGenerator generator = new CommissionGenerator(
        catalog(template(4, 10)), fixedRandom(), CommissionRewardPolicy.defaultPolicy(), () -> COMMISSION);
    CommissionService service = new CommissionService(generator, repository, repository, delivery(repository));
    repository.save(new CommissionPlayerState(PLAYER, List.of(new CommissionInstance(
        COMMISSION, PLAYER, "deliver", CommissionType.ITEM_DELIVERY, "town", "Town",
        "minecraft:stone", 4, CommissionRewardSnapshot.coins(40), 1, 100)), null));
    UUID submission = UUID.randomUUID();

    assertEquals(CommissionService.SubmitOutcome.PROGRESSED,
        service.submitProgress(PLAYER, COMMISSION, submission, 2, 10).outcome());
    CommissionService.SubmitResult duplicate = service.submitProgress(
        PLAYER, COMMISSION, submission, 2, 11);
    assertEquals(CommissionService.SubmitOutcome.DUPLICATE_SUBMISSION, duplicate.outcome());
    assertEquals(2, repository.load(PLAYER).commissions().get(0).progress());
  }

  private static CommissionCatalog catalog(CommissionTemplate template) {
    return new CommissionCatalog(
        List.of(template),
        Map.of("requesters", List.of(new CommissionRequester("town", "Town"))),
        Map.of("targets", CommissionTargetPool.unweighted("targets", List.of("minecraft:stone"))),
        new PersonalCommissionSettings(10, 0, 1, 1, 6, 10, 20));
  }

  private static CommissionTemplate template(int min, int reward) {
    return new CommissionTemplate("deliver", CommissionType.ITEM_DELIVERY, "requesters", "targets",
        min, min, 1, CommissionRewardMode.PER_UNIT, reward, 1, "material", "common", 10, 20);
  }

  private static CommissionRandom fixedRandom() {
    return new CommissionRandom() {
      @Override public double nextDouble() { return 0.5D; }
      @Override public int nextInt(int bound) { return 0; }
    };
  }

  private static CommissionRewardPolicy idPolicy() {
    return (template, requester, target, quantity, random) -> CommissionRewardSnapshot.coins(20);
  }

  private static CommissionRewardDeliveryPort delivery(InMemoryCommissionRepository repository) {
    return new CommissionRewardDeliveryPort() {
      @Override public DeliveryResult deliver(CommissionRewardRecord record) {
        repository.save(record.mailCreated(UUID.nameUUIDFromBytes(
            record.rewardRecordId().toString().getBytes())));
        return DeliveryResult.CREATED;
      }
      @Override public ClaimResult claim(UUID rewardRecordId, UUID playerId, long nowMillis) {
        return ClaimResult.CLAIMED;
      }
    };
  }
}
