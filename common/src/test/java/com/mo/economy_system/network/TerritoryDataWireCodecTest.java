package com.mo.economy_system.network;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import com.mo.economy_system.common.territory.TerritoryResponseBudget;
import com.mo.economy_system.common.territory.TerritoryTestFixtures;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import com.mo.economy_system.common.territory.TerritorySnapshots.Summary;
import com.mo.economy_system.testsupport.TestWireBuffer;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryDataWireCodecTest {
  @Test void errorHasStableGoldenBytes() {
    TestWireBuffer buffer = new TestWireBuffer();
    TerritoryDataWireCodec.encodeResponse(TerritoryDataResponseMessage.error(7), buffer);
    byte[] bytes = new byte[buffer.readableBytes()];
    buffer.getBytes(buffer.readerIndex(), bytes);
    assertEquals("056572726f720000000000000007", hex(bytes));
    assertEquals(TerritoryDataResponseMessage.error(7), TerritoryDataWireCodec.decodeResponse(buffer));
  }

  @Test void dataHasStableGoldenDigest() throws Exception {
    TestWireBuffer buffer = new TestWireBuffer();
    TerritoryDataWireCodec.encodeResponse(TerritoryTestFixtures.response(7), buffer);
    byte[] bytes = new byte[buffer.readableBytes()];
    buffer.getBytes(buffer.readerIndex(), bytes);
    assertEquals("0c2b14801c1e76d58c0fda7470283083152d718eecd8df06f586bd779769dbdc",
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
  }

  @Test void estimatedBudgetCoversActualDataAndErrorBytes() {
    assertCovered(TerritoryTestFixtures.response(7));
    assertCovered(TerritoryDataResponseMessage.data(7, List.of(), List.of()));
    Summary unicode = new Summary(UUID.fromString("50000000-0000-0000-0000-000000000001"),
        UUID.fromString("60000000-0000-0000-0000-000000000001"), "领".repeat(64),
        "地".repeat(128), new Position(0, 0, 0), new Position(1, 1, 1), "example:moon");
    Summary second = TerritoryTestFixtures.summary(
        UUID.fromString("50000000-0000-0000-0000-000000000002"),
        UUID.fromString("60000000-0000-0000-0000-000000000002"), "Second");
    assertCovered(TerritoryDataResponseMessage.data(7, List.of(), List.of(unicode, second)));
    assertCovered(TerritoryDataResponseMessage.error(7));
  }

  @Test void failedEncodeLeavesDestinationWriterIndexAndBytesUnchanged() {
    TestWireBuffer destination = new TestWireBuffer();
    destination.writeInt(0x12345678);
    int writerIndex = destination.writerIndex();
    assertThrows(IllegalArgumentException.class, () -> TerritoryDataWireCodec.encodeResponse(
        TerritoryTestFixtures.response(7), destination, 1));
    assertEquals(writerIndex, destination.writerIndex());
    assertEquals(0x12345678, destination.getInt(0));
  }

  private static void assertCovered(TerritoryDataResponseMessage message) {
    TestWireBuffer buffer = new TestWireBuffer();
    TerritoryDataWireCodec.encodeResponse(message, buffer);
    assertTrue(TerritoryResponseBudget.estimate(message.owned(), message.authorized())
        >= buffer.readableBytes());
  }

  private static String hex(byte[] bytes) {
    StringBuilder value = new StringBuilder(bytes.length * 2);
    for (byte current : bytes) value.append(String.format("%02x", current & 0xff));
    return value.toString();
  }
}
