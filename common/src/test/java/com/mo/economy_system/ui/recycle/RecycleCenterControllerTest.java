package com.mo.economy_system.ui.recycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.network.RecycleDataResponseMessage;
import com.mo.economy_system.common.network.RecycleOfferSnapshot;
import com.mo.economy_system.ui.core.ScreenState;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RecycleCenterControllerTest {
  @Test
  void amountIsClampedToOwnedAndProtocolLimit() {
    Port port = new Port(); RecycleCenterController controller = new RecycleCenterController(port);
    controller.handle(new RecycleCenterEvent.Initialize(1));
    RecycleOfferSnapshot offer = new RecycleOfferSnapshot("minecraft:kelp", 1, 2, 3, 8_000, 64, true);
    controller.handle(new RecycleCenterEvent.DataLoaded(RecycleDataResponseMessage.data(port.requestId, 10, 20, List.of(offer))));
    controller.handle(new RecycleCenterEvent.AmountChanged(9_999));
    assertEquals(2_304, controller.state().amount());
    controller.handle(new RecycleCenterEvent.AmountChanged(-1));
    assertEquals(1, controller.state().amount());
    assertEquals(ScreenState.READY, controller.state().screenState());
  }

  private static final class Port implements RecycleCenterPort {
    long requestId;
    public long nextRequestId() { return ++requestId; }
    public void requestData(long id) { requestId = id; }
    public void submit(long id, UUID submissionId, String itemId, int amount) { }
  }
}
