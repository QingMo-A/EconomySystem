package com.mo.economy_system.ui.territory.confirm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.core.UiNavigation;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryConfirmationControllerTest {
  private static final UUID TERRITORY_ID = new UUID(1, 1);
  private static final UUID MEMBER_ID = new UUID(2, 2);

  @Test
  void territoryRemovalIsSubmittedExactlyOnce() {
    FakePort port = new FakePort();
    TerritoryConfirmationController controller = new TerritoryConfirmationController(
        TerritoryConfirmationKind.REMOVE_TERRITORY, TERRITORY_ID, "spawn", null, "", port);

    controller.handle(new TerritoryConfirmationEvent.ActionClicked(TerritoryConfirmationAction.CONFIRM));
    controller.handle(new TerritoryConfirmationEvent.ActionClicked(TerritoryConfirmationAction.CONFIRM));

    assertEquals(1, port.territoryRemovals);
    assertEquals(TERRITORY_ID, port.territoryId);
    assertTrue(controller.state().actions().isEmpty());
    assertInstanceOf(UiNavigation.Back.class, controller.pollNavigation().orElseThrow());
  }

  @Test
  void memberRemovalAndCancellationUseDistinctCommonDecisions() {
    FakePort removalPort = new FakePort();
    TerritoryConfirmationController removal = new TerritoryConfirmationController(
        TerritoryConfirmationKind.REMOVE_MEMBER, TERRITORY_ID, "spawn", MEMBER_ID, "alice", removalPort);
    removal.handle(new TerritoryConfirmationEvent.ActionClicked(TerritoryConfirmationAction.CONFIRM));
    assertEquals(1, removalPort.memberRemovals);
    assertEquals(TERRITORY_ID, removalPort.territoryId);
    assertEquals(MEMBER_ID, removalPort.memberId);

    FakePort cancelPort = new FakePort();
    TerritoryConfirmationController cancel = new TerritoryConfirmationController(
        TerritoryConfirmationKind.REMOVE_MEMBER, TERRITORY_ID, "spawn", MEMBER_ID, "alice", cancelPort);
    cancel.handle(new TerritoryConfirmationEvent.ActionClicked(TerritoryConfirmationAction.CANCEL));
    assertEquals(0, cancelPort.memberRemovals);
    assertEquals(0, cancelPort.territoryRemovals);
    assertInstanceOf(UiNavigation.Back.class, cancel.pollNavigation().orElseThrow());
  }

  private static final class FakePort implements TerritoryConfirmationPort {
    private int territoryRemovals;
    private int memberRemovals;
    private UUID territoryId;
    private UUID memberId;

    @Override public void removeTerritory(UUID territoryId) {
      territoryRemovals++;
      this.territoryId = territoryId;
    }

    @Override public void removeMember(UUID territoryId, UUID memberId) {
      memberRemovals++;
      this.territoryId = territoryId;
      this.memberId = memberId;
    }
  }
}
