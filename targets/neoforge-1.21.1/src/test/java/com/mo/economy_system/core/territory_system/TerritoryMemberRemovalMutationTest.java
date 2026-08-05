package com.mo.economy_system.core.territory_system;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.territory.TerritoryMemberRemovalService;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

class TerritoryMemberRemovalMutationTest {
  private final UUID owner = UUID.randomUUID(),
      target = UUID.randomUUID(),
      other = UUID.randomUUID();

  private Territory territory() {
    Territory t =
        new Territory(
            UUID.randomUUID(),
            "land",
            owner,
            "owner",
            0,
            64,
            0,
            1,
            64,
            1,
            new BlockPos(0, 64, 0),
            Level.OVERWORLD);
    t.addAuthorizedPlayer(target, " target ");
    t.addAuthorizedPlayer(other, "other");
    return t;
  }

  @Test
  void removesExactMemberAndPreservesName() {
    Territory t = territory();
    var o = TerritoryMemberRemovalMutation.remove(t, owner, target, () -> {});
    assertEquals(TerritoryMemberRemovalService.RepositoryResult.REMOVED, o.result());
    assertEquals("target", o.removedMember().targetPlayerName());
    assertFalse(t.hasPermission(target));
    assertTrue(t.hasPermission(other));
  }

  @Test
  void rejectsOwnerAndMissing() {
    Territory t = territory();
    assertEquals(
        TerritoryMemberRemovalService.RepositoryResult.OWNER_TARGET,
        TerritoryMemberRemovalMutation.remove(t, owner, owner, () -> {}).result());
    assertEquals(
        TerritoryMemberRemovalService.RepositoryResult.TARGET_NOT_MEMBER,
        TerritoryMemberRemovalMutation.remove(t, owner, UUID.randomUUID(), () -> {}).result());
  }

  @Test
  void firstDirtyFailureFullyRollsBack() {
    Territory t = territory();
    int[] calls = {0};
    var o =
        TerritoryMemberRemovalMutation.remove(
            t,
            owner,
            target,
            () -> {
              if (calls[0]++ == 0) throw new IllegalStateException("dirty");
            });
    assertEquals(TerritoryMemberRemovalService.RepositoryResult.PERSIST_FAILED, o.result());
    assertTrue(t.hasPermission(target));
    assertEquals("target", TerritoryMemberRemovalMutation.snapshot(t).get(target));
    assertTrue(t.hasPermission(other));
  }

  @Test
  void secondDirtyFailureIsUnknown() {
    Territory t = territory();
    var o =
        TerritoryMemberRemovalMutation.remove(
            t,
            owner,
            target,
            () -> {
              throw new IllegalStateException("dirty");
            });
    assertEquals(TerritoryMemberRemovalService.RepositoryResult.STATE_UNKNOWN, o.result());
  }
}
