package com.mo.economy_system.common.check;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.EconomyMessages;
import com.mo.economy_system.protocol.EconomyMessageDirection;
import com.mo.economy_system.protocol.EconomyProtocol;
import org.junit.jupiter.api.Test;

class ClientFileCheckProtocolTest {
  @Test
  void canonicalSpecsRemainStableAndAtomic() {
    assertEquals(23, EconomyMessages.CHECK.discriminator());
    assertEquals("economy_system:check_system/packet_check", EconomyMessages.CHECK.id());
    assertEquals(EconomyMessageDirection.SERVER_TO_CLIENT, EconomyMessages.CHECK.direction());
    assertEquals(24, EconomyMessages.CHECK_RESULT_REQUEST.discriminator());
    assertEquals(
        "economy_system:check_system/packet_check_result_request",
        EconomyMessages.CHECK_RESULT_REQUEST.id());
    assertEquals(
        EconomyMessageDirection.CLIENT_TO_SERVER, EconomyMessages.CHECK_RESULT_REQUEST.direction());
    assertEquals(25, EconomyMessages.CHECK_RESULT_RESPONSE.discriminator());
    assertEquals(
        "economy_system:check_system/packet_check_result_response",
        EconomyMessages.CHECK_RESULT_RESPONSE.id());
    assertEquals(
        EconomyMessageDirection.SERVER_TO_CLIENT,
        EconomyMessages.CHECK_RESULT_RESPONSE.direction());
    assertEquals(26, EconomyProtocol.GET.discriminator());
    assertEquals("economy_system:check_system/packet_get", EconomyProtocol.GET.id());
  }
}
