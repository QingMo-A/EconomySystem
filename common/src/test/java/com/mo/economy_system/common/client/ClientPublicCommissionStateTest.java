package com.mo.economy_system.common.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.commission.PublicCommission;
import com.mo.economy_system.common.network.commission_public.PublicCommissionActionResponseMessage;
import com.mo.economy_system.common.network.commission_public.PublicCommissionDataResponseMessage;
import com.mo.economy_system.common.network.commission_public.PublicCommissionSubmitStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ClientPublicCommissionStateTest {
  private static final UUID ID = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff");

  @AfterEach
  void reset() {
    ClientPublicCommissionState.reset();
  }

  @Test
  void requestAndResponseStateIsStaleSafe() {
    long request = ClientPublicCommissionState.nextRequestId();
    assertTrue(ClientPublicCommissionState.snapshot().loading());
    PublicCommission commission = PublicCommission.create(ID, "City", "town", "Town",
        "minecraft:stone", 10, 3, 1_000L, 10_000L, "description");
    assertTrue(ClientPublicCommissionState.apply(PublicCommissionDataResponseMessage.data(
        request, 2_000L, List.of(commission))));
    assertFalse(ClientPublicCommissionState.snapshot().loading());
    assertEquals(List.of(commission), ClientPublicCommissionState.snapshot().commissions());
    assertFalse(ClientPublicCommissionState.apply(PublicCommissionDataResponseMessage.data(
        request - 1, 1_000L, List.of())));
    assertEquals(List.of(commission), ClientPublicCommissionState.snapshot().commissions());
  }

  @Test
  void actionStatusAndErrorsAreRetained() {
    long request = ClientPublicCommissionState.nextRequestId();
    ClientPublicCommissionState.apply(PublicCommissionDataResponseMessage.error(
        request, 2_000L, "screen.commissions.public.failed"));
    assertEquals("screen.commissions.public.failed", ClientPublicCommissionState.snapshot().errorKey());
    ClientPublicCommissionState.applyAction(new PublicCommissionActionResponseMessage(
        request, PublicCommissionSubmitStatus.ACCEPTED, 4, 12, "奖励已发送至邮箱"));
    assertEquals(PublicCommissionSubmitStatus.ACCEPTED,
        ClientPublicCommissionState.snapshot().lastSubmitStatus());
    assertEquals("奖励已发送至邮箱", ClientPublicCommissionState.snapshot().actionMessage());
  }
}
