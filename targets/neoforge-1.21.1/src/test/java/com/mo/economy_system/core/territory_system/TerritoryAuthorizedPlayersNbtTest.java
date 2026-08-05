package com.mo.economy_system.core.territory_system;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

class TerritoryAuthorizedPlayersNbtTest {
  private final UUID owner = UUID.randomUUID();

  @Test
  void missingAndEmptyListsLoadAsNoMembers() {
    CompoundTag missing = base().toNBT();
    missing.remove("AuthorizedPlayers");
    assertTrue(Territory.fromNBT(missing).getAuthorizedPlayers().isEmpty());
    assertTrue(Territory.fromNBT(base().toNBT()).getAuthorizedPlayers().isEmpty());
  }

  @Test
  void roundTripPreservesUuidAndTrimmedName() {
    Territory territory = base();
    UUID member = UUID.randomUUID();
    territory.addAuthorizedPlayer(member, " member ");
    Territory loaded = Territory.fromNBT(territory.toNBT());
    assertEquals(1, loaded.getAuthorizedPlayers().size());
    PlayerInfo info = loaded.getAuthorizedPlayers().iterator().next();
    assertEquals(member, info.getUuid());
    assertEquals("member", info.getName());
  }

  @Test
  void wrongContainerAndElementTypesAreRejected() {
    CompoundTag wrong = base().toNBT();
    wrong.putString("AuthorizedPlayers", "bad");
    assertThrows(IllegalArgumentException.class, () -> Territory.fromNBT(wrong));

    CompoundTag element = base().toNBT();
    ListTag list = new ListTag();
    list.add(StringTag.valueOf("bad"));
    element.put("AuthorizedPlayers", list);
    assertThrows(IllegalArgumentException.class, () -> Territory.fromNBT(element));
  }

  @Test
  void missingFieldsAndInvalidNamesAreRejected() {
    assertInvalid(member(null, "name"));
    assertInvalid(member(UUID.randomUUID(), null));
    assertInvalid(member(UUID.randomUUID(), "   "));
    assertInvalid(member(UUID.randomUUID(), "x".repeat(EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH + 1)));
  }

  @Test
  void ownerAndDuplicateUuidAreRejectedWithoutFolding() {
    assertInvalid(member(owner, "owner"));
    UUID duplicate = UUID.randomUUID();
    CompoundTag tag = base().toNBT();
    ListTag list = new ListTag();
    list.add(member(duplicate, "first"));
    list.add(member(duplicate, "second"));
    tag.put("AuthorizedPlayers", list);
    assertThrows(IllegalArgumentException.class, () -> Territory.fromNBT(tag));
  }

  private void assertInvalid(CompoundTag member) {
    CompoundTag tag = base().toNBT();
    ListTag list = new ListTag();
    list.add(member);
    tag.put("AuthorizedPlayers", list);
    assertThrows(IllegalArgumentException.class, () -> Territory.fromNBT(tag));
  }

  private CompoundTag member(UUID uuid, String name) {
    CompoundTag tag = new CompoundTag();
    if (uuid != null) tag.putUUID("PlayerUUID", uuid);
    if (name != null) tag.putString("PlayerName", name);
    return tag;
  }

  private Territory base() {
    return new Territory(UUID.randomUUID(), "land", owner, "owner", 0, 64, 0, 1, 64, 1, new BlockPos(0, 64, 0), Level.OVERWORLD);
  }
}
