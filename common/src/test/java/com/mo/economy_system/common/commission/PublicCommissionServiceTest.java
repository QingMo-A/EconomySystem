package com.mo.economy_system.common.commission;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PublicCommissionServiceTest {
  private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000011");
  private static final UUID COMMISSION = UUID.fromString("00000000-0000-0000-0000-000000000012");

  @Test
  void concurrentStyleSubmissionsAreCappedAndEachAcceptedSubmissionGetsOneReward() {
    InMemoryPublicCommissionRepository publicRepo = new InMemoryPublicCommissionRepository();
    InMemoryCommissionRepository rewardRepo = new InMemoryCommissionRepository();
    PublicCommission commission = PublicCommission.create(COMMISSION, "Stone Drive", "town", "Town",
        "minecraft:stone", 5, 4, 1, 100, "Deliver stone");
    publicRepo.save(commission);
    PublicCommissionService service = new PublicCommissionService(publicRepo, rewardRepo, new Delivery(rewardRepo));

    UUID firstSubmission = UUID.randomUUID();
    PublicCommissionService.SubmitResult first = service.submit(
        PLAYER, COMMISSION, firstSubmission, 3, 10);
    assertEquals(PublicCommissionService.SubmitOutcome.ACCEPTED, first.outcome());
    assertEquals(3, first.acceptedAmount());
    assertEquals(12, first.payout());
    PublicCommissionService.SubmitResult second = service.submit(
        PLAYER, COMMISSION, UUID.randomUUID(), 3, 11);
    assertEquals(PublicCommissionService.SubmitOutcome.COMPLETED, second.outcome());
    assertEquals(2, second.acceptedAmount());
    assertEquals(2, rewardRepo.listForPlayer(PLAYER).size());

    PublicCommissionService.SubmitResult duplicate = service.submit(
        PLAYER, COMMISSION, firstSubmission, 1, 12);
    assertEquals(PublicCommissionService.SubmitOutcome.DUPLICATE, duplicate.outcome());
  }

  @Test
  void expiryStopsSubmissionAndPreservesSharedState() {
    InMemoryPublicCommissionRepository publicRepo = new InMemoryPublicCommissionRepository();
    InMemoryCommissionRepository rewardRepo = new InMemoryCommissionRepository();
    publicRepo.save(PublicCommission.create(COMMISSION, "Stone Drive", "town", "Town",
        "minecraft:stone", 5, 4, 1, 10, "Deliver stone"));
    PublicCommissionService service = new PublicCommissionService(publicRepo, rewardRepo, new Delivery(rewardRepo));
    PublicCommissionService.SubmitResult result = service.submit(
        PLAYER, COMMISSION, UUID.randomUUID(), 1, 10);
    assertEquals(PublicCommissionService.SubmitOutcome.EXPIRED, result.outcome());
    assertEquals(PublicCommissionStatus.EXPIRED, publicRepo.find(COMMISSION).orElseThrow().status());
  }

  @Test
  void samePacketIdIsIndependentAcrossPlayers() {
    InMemoryPublicCommissionRepository publicRepo = new InMemoryPublicCommissionRepository();
    InMemoryCommissionRepository rewardRepo = new InMemoryCommissionRepository();
    publicRepo.save(PublicCommission.create(COMMISSION, "Stone Drive", "town", "Town",
        "minecraft:stone", 4, 2, 1, 100, "Deliver stone"));
    PublicCommissionService service = new PublicCommissionService(publicRepo, rewardRepo,
        new Delivery(rewardRepo));
    UUID sharedPacketId = UUID.fromString("00000000-0000-0000-0000-000000000099");
    UUID otherPlayer = UUID.fromString("00000000-0000-0000-0000-000000000013");

    assertEquals(PublicCommissionService.SubmitOutcome.ACCEPTED,
        service.submit(PLAYER, COMMISSION, sharedPacketId, 1, 10).outcome());
    assertEquals(PublicCommissionService.SubmitOutcome.ACCEPTED,
        service.submit(otherPlayer, COMMISSION, sharedPacketId, 1, 11).outcome());
    assertEquals(2, rewardRepo.listForPlayer(PLAYER).size()
        + rewardRepo.listForPlayer(otherPlayer).size());
  }

  @Test
  void retryPendingPublicRewardsIsScopedToPublicBatch() {
    InMemoryCommissionRepository repository = new InMemoryCommissionRepository();
    UUID player = UUID.randomUUID();
    UUID publicId = UUID.randomUUID();
    CommissionRewardRecord pending = new CommissionRewardRecord(UUID.randomUUID(), "public:key",
        player, publicId, "public", "requester", "requester", CommissionRewardSnapshot.coins(4),
        1, null, CommissionRewardStatus.PENDING_MAIL, 0);
    repository.createIfAbsent(pending);
    final boolean[] delivered = {false};
    PublicCommissionService service = new PublicCommissionService(
        new InMemoryPublicCommissionRepository(), repository, new CommissionRewardDeliveryPort() {
          @Override public DeliveryResult deliver(CommissionRewardRecord record) {
            delivered[0] = true;
            repository.save(record.mailCreated(UUID.randomUUID()));
            return DeliveryResult.CREATED;
          }
          @Override public ClaimResult claim(UUID rewardRecordId, UUID playerId, long nowMillis) {
            return ClaimResult.CLAIMED;
          }
        });
    assertEquals(1, service.retryPendingRewards(player));
    assertTrue(delivered[0]);
  }

  private record Delivery(InMemoryCommissionRepository rewards) implements CommissionRewardDeliveryPort {
    @Override public DeliveryResult deliver(CommissionRewardRecord record) {
      rewards.save(record.mailCreated(UUID.randomUUID()));
      return DeliveryResult.CREATED;
    }
    @Override public ClaimResult claim(UUID rewardRecordId, UUID playerId, long nowMillis) {
      return ClaimResult.CLAIMED;
    }
  }
}
