package com.mo.economy_system.common.reward;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RewardServiceTest {
  private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Test
  void successfulRewardUsesExactCreditAndCanonicalLogFields() {
    List<Object> credit = new ArrayList<>();
    RewardService service = service((player, amount, category, reason) -> {
      credit.addAll(List.of(player, amount, category, reason));
      return BalanceMutationResult.SUCCESS;
    });

    RewardService.Outcome outcome =
        service.award(PLAYER, "minecraft:zombie", "Zombie", 0, 0);

    assertEquals(new RewardService.Outcome(RewardService.Result.SUCCESS, 3), outcome);
    assertEquals(List.of(PLAYER, 3, "系统", "击杀奖励: Zombie"), credit);
  }

  @Test
  void exactCreditFailuresNeverReportSuccess() {
    for (BalanceMutationResult mutation : List.of(
        BalanceMutationResult.BALANCE_LIMIT,
        BalanceMutationResult.PERSIST_FAILED,
        BalanceMutationResult.INVALID_AMOUNT,
        BalanceMutationResult.INSUFFICIENT_FUNDS)) {
      RewardService.Outcome outcome =
          service((player, amount, category, reason) -> mutation)
              .award(PLAYER, "minecraft:zombie", "Zombie", 0, 0);

      RewardService.Result expected = switch (mutation) {
        case BALANCE_LIMIT -> RewardService.Result.BALANCE_LIMIT;
        case PERSIST_FAILED -> RewardService.Result.PERSIST_FAILED;
        default -> RewardService.Result.STATE_UNKNOWN;
      };
      assertEquals(new RewardService.Outcome(expected, 0), outcome);
    }
  }

  @Test
  void missingEntryAndFailedRollDoNotTouchAccounts() {
    int[] credits = {0};
    RewardAccountPort accounts = (player, amount, category, reason) -> {
      credits[0]++;
      return BalanceMutationResult.SUCCESS;
    };
    RewardService missing = service(accounts);
    RewardService noDrop =
        new RewardService(
            new RewardCatalog(List.of(new RewardEntry("minecraft:zombie", 0.1D, 1, 5))),
            new RewardCalculator(new FixedRandom(0.5D, 0)),
            accounts);

    assertEquals(
        RewardService.Result.UNCONFIGURED,
        missing.award(PLAYER, "minecraft:skeleton", "Skeleton", 0, 0).result());
    assertEquals(
        RewardService.Result.NO_DROP,
        noDrop.award(PLAYER, "minecraft:zombie", "Zombie", 0, 0).result());
    assertEquals(0, credits[0]);
  }

  @Test
  void accountExceptionFailsClosedAndIsDiagnosed() {
    List<String> operations = new ArrayList<>();
    RewardService service =
        new RewardService(
            catalog(),
            new RewardCalculator(new FixedRandom(0.0D, 2)),
            (player, amount, category, reason) -> {
              throw new IllegalStateException("disk");
            },
            (operation, player, entity, error) -> operations.add(operation));

    assertEquals(
        RewardService.Result.STATE_UNKNOWN,
        service.award(PLAYER, "minecraft:zombie", "Zombie", 0, 0).result());
    assertEquals(List.of("credit"), operations);
  }

  private static RewardService service(RewardAccountPort accounts) {
    return new RewardService(
        catalog(), new RewardCalculator(new FixedRandom(0.0D, 2)), accounts);
  }

  private static RewardCatalog catalog() {
    return new RewardCatalog(List.of(new RewardEntry("minecraft:zombie", 1.0D, 1, 5)));
  }

  private record FixedRandom(double value, int integer) implements RewardRandom {
    @Override
    public double nextDouble() {
      return value;
    }

    @Override
    public int nextInt(int bound) {
      return integer;
    }
  }
}
