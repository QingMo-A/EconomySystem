package com.mo.economy_system.target.forge1201.network;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import com.mo.economy_system.common.territory.TerritoryTeleportTarget;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

class Forge1201TerritorySnapshotStoreTest {
  @Test void permissionCompatibilityUsesMembersForMissingAndUnknownValues() {
    assertEquals(RuleLevel.OWNER_ONLY, Forge1201TerritorySnapshotStore.permission("OWNER_ONLY"));
    assertEquals(RuleLevel.MEMBERS, Forge1201TerritorySnapshotStore.permission("MEMBERS"));
    assertEquals(RuleLevel.EVERYONE, Forge1201TerritorySnapshotStore.permission("EVERYONE"));
    assertEquals(RuleLevel.MEMBERS, Forge1201TerritorySnapshotStore.permission(""));
    assertEquals(RuleLevel.MEMBERS, Forge1201TerritorySnapshotStore.permission("FUTURE"));
  }

  @Test void dimensionsMustBeCanonicalAndBounded() {
    assertEquals("minecraft:overworld",
        Forge1201TerritorySnapshotStore.canonicalDimension("minecraft:overworld"));
    assertEquals("example:moon", Forge1201TerritorySnapshotStore.canonicalDimension("example:moon"));
    assertThrows(IllegalArgumentException.class,
        () -> Forge1201TerritorySnapshotStore.canonicalDimension("overworld"));
    for (String invalid : new String[] {"", "bad id", "Minecraft:overworld", "a:b:c", "x".repeat(257)}) {
      assertThrows(IllegalArgumentException.class,
          () -> Forge1201TerritorySnapshotStore.canonicalDimension(invalid));
    }
  }

  @Test void captureFallsBackUnknownPermissionsWithoutChangingOtherData() {
    CompoundTag tag = validTerritory();
    CompoundTag permissions = new CompoundTag();
    permissions.putString(RuleAction.PLACE_BLOCK.name(), "OWNER_ONLY");
    permissions.putString(RuleAction.BREAK_BLOCK.name(), "FUTURE");
    tag.put("Permissions", permissions);
    var snapshot = Forge1201TerritorySnapshotStore.capture(tag);
    assertEquals(RuleLevel.OWNER_ONLY, snapshot.rules().stream()
        .filter(rule -> rule.action() == RuleAction.PLACE_BLOCK).findFirst().orElseThrow().level());
    assertEquals(RuleLevel.MEMBERS, snapshot.rules().stream()
        .filter(rule -> rule.action() == RuleAction.BREAK_BLOCK).findFirst().orElseThrow().level());
  }

  @Test void invalidBuffValuesFailClosedInsteadOfBeingClamped() {
    CompoundTag buff = new CompoundTag();
    buff.putString("id", "economy:speed");
    buff.putString("displayText", "Speed");
    buff.putString("effectId", "minecraft:speed");
    buff.putInt("initialLevel", 4);
    buff.putInt("single_Upgrade_Level", 0);
    buff.putInt("max_Level", 3);
    buff.putInt("level", 4);
    assertThrows(IllegalArgumentException.class, () -> Forge1201TerritorySnapshotStore.buff(buff));
  }

  @Test void findBuildsOwnerAndAuthorizedTeleportTarget() {
    CompoundTag tag = validTerritory();
    CompoundTag backpoint = new CompoundTag();
    backpoint.putInt("BackX", 2);
    backpoint.putInt("BackY", 64);
    backpoint.putInt("BackZ", 3);
    tag.put("Backpoint", backpoint);
    UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    CompoundTag member = new CompoundTag();
    member.putUUID("PlayerUUID", memberId);
    member.putString("PlayerName", "Member");
    ListTag members = new ListTag();
    members.add(member);
    tag.put("AuthorizedPlayers", members);

    var owned = Forge1201TerritorySnapshotStore.capture(tag);
    var store = new Forge1201TerritorySnapshotStore(List.of(owned));
    TerritoryTeleportTarget target = store.find(owned.summary().territoryId()).orElseThrow();
    assertEquals(owned.summary().name(), target.territoryName());
    assertEquals(owned.summary().ownerId(), target.ownerId());
    assertTrue(target.permits(owned.summary().ownerId()));
    assertTrue(target.permits(memberId));
    assertTrue(target.backpoint().isPresent());
    assertTrue(store.find(UUID.randomUUID()).isEmpty());
  }

  private static CompoundTag validTerritory() {
    CompoundTag tag = new CompoundTag();
    tag.putUUID("TerritoryID", UUID.fromString("10000000-0000-0000-0000-000000000001"));
    tag.putUUID("OwnerUUID", UUID.fromString("00000000-0000-0000-0000-000000000001"));
    tag.putString("OwnerName", "Owner");
    tag.putString("Name", "Home");
    tag.putString("Dimension", "minecraft:overworld");
    tag.putInt("X1", 0); tag.putInt("Y1", 64); tag.putInt("Z1", 0);
    tag.putInt("X2", 10); tag.putInt("Y2", 80); tag.putInt("Z2", 10);
    return tag;
  }
}
