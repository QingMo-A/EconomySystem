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
    assertEquals(26, EconomyMessages.GET.discriminator());
    assertEquals("economy_system:check_system/packet_get", EconomyMessages.GET.id());
    assertEquals(EconomyMessageDirection.SERVER_TO_CLIENT, EconomyMessages.GET.direction());
    assertEquals(27, EconomyMessages.GET_RESULT_REQUEST.discriminator());
    assertEquals("economy_system:check_system/packet_get_result_request", EconomyMessages.GET_RESULT_REQUEST.id());
    assertEquals(EconomyMessageDirection.CLIENT_TO_SERVER, EconomyMessages.GET_RESULT_REQUEST.direction());
    assertEquals(28, EconomyMessages.GET_RESULT_RESPONSE.discriminator());
    assertEquals(EconomyMessageDirection.SERVER_TO_CLIENT, EconomyMessages.GET_RESULT_RESPONSE.direction());
    assertEquals(29, EconomyMessages.CHUNK.discriminator());
    assertEquals(EconomyMessageDirection.CLIENT_TO_SERVER, EconomyMessages.CHUNK.direction());
    assertEquals(30, EconomyMessages.CHUNK_RESPONSE.discriminator());
    assertEquals(EconomyMessageDirection.SERVER_TO_CLIENT, EconomyMessages.CHUNK_RESPONSE.direction());
    assertEquals(31, EconomyProtocol.DELIVERY_BOX_DATA_REQUEST.discriminator());
  }
}
