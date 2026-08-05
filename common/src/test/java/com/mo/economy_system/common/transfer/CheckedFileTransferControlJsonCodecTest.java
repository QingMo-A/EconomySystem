package com.mo.economy_system.common.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CheckedFileTransferControlJsonCodecTest {
  @Test
  void roundTripsAllStatuses() {
    UUID id = UUID.randomUUID();
    for (var value : new CheckedFileTransferControl[] {
        CheckedFileTransferControl.ready(id, 18001, "0".repeat(64)),
        CheckedFileTransferControl.complete(id, 18001, "0".repeat(64)),
        CheckedFileTransferControl.error(CheckedFileTransferControlStatus.DECLINED, "DECLINED"),
        CheckedFileTransferControl.error(CheckedFileTransferControlStatus.NOT_FOUND, "NOT_FOUND"),
        CheckedFileTransferControl.error(CheckedFileTransferControlStatus.FAILED, "SNAPSHOT_FAILED")}) {
      assertEquals(value, CheckedFileTransferControlJsonCodec.decode(
          CheckedFileTransferControlJsonCodec.encode(value)));
    }
  }

  @Test
  void rejectsNonCanonicalJsonAndWrongTypes() {
    rejects("{\"schema\":1,\"schema\":1,\"status\":\"FAILED\",\"errorCode\":\"X\"}");
    rejects("{\"schema\":1,\"status\":\"FAILED\",\"errorCode\":\"X\",\"extra\":1}");
    rejects("{\"schema\":\"1\",\"status\":\"FAILED\",\"errorCode\":\"X\"}");
    rejects("{\"schema\":1.0,\"status\":\"FAILED\",\"errorCode\":\"X\"}");
    rejects("{\"schema\":1e0,\"status\":\"FAILED\",\"errorCode\":\"X\"}");
    rejects("{\"schema\":01,\"status\":\"FAILED\",\"errorCode\":\"X\"}");
    rejects("{\"schema\":1,\"status\":null,\"errorCode\":\"X\"}");
  }

  @Test
  void rejectsLongToIntNarrowingAndInvalidCanonicalBounds() {
    String base = "{\"schema\":1,\"status\":\"READY\",\"transferId\":\"%s\"," +
        "\"byteLength\":0,\"sha256\":\"%s\",\"rawChunkBytes\":%s,\"totalChunks\":%s}";
    String id = UUID.randomUUID().toString();
    rejects(base.formatted(id, "0".repeat(64), "4294985296", "0"));
    rejects(base.formatted(id, "0".repeat(64), "18000", "2147483648"));
    rejects(base.formatted(id, "0".repeat(64), "-18000", "0"));
    assertThrows(IllegalArgumentException.class,
        () -> CheckedFileTransferControl.ready(UUID.randomUUID(), -1, "0".repeat(64)));
  }

  private static void rejects(String json) {
    assertThrows(IllegalArgumentException.class,
        () -> CheckedFileTransferControlJsonCodec.decode(json));
  }
}
