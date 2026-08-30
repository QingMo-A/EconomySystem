package com.mo.economy_system.ui.commission;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mo.economy_system.common.commission.CommissionInstance;
import com.mo.economy_system.common.commission.CommissionRewardSnapshot;
import com.mo.economy_system.common.commission.CommissionType;
import com.mo.economy_system.common.network.CommissionDataResponseMessage;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CommissionCenterControllerTest {
  private static final UUID COMMISSION_ID = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");

  @Test
  void serverClockAdvancesBetweenSnapshotsForLiveCountdowns() {
    FakePort port = new FakePort();
    CommissionCenterController controller = new CommissionCenterController(port);
    controller.handle(new CommissionCenterEvent.Initialize(1L));
    controller.handle(new CommissionCenterEvent.DataLoaded(CommissionDataResponseMessage.data(
        1L, 1_000L, 10_000L, 6, List.of(commission()))));

    controller.handle(new CommissionCenterEvent.Tick(1_000_000_000L));
    controller.handle(new CommissionCenterEvent.Tick(2_500_000_000L));

    assertEquals(2_500L, controller.state().serverNowMillis());
  }

  private static CommissionInstance commission() {
    return new CommissionInstance(COMMISSION_ID, UUID.randomUUID(), "deliver",
        CommissionType.ITEM_DELIVERY, "town", "Town", "minecraft:stone", 2,
        CommissionRewardSnapshot.coins(20), 1L, 10_000L);
  }

  private static final class FakePort implements CommissionCenterPort {
    @Override public long nextRequestId() { return 1L; }
    @Override public void requestData(long requestId) { }
    @Override public void submit(long requestId, UUID commissionId, UUID submissionId, int amount) { }
  }
}
