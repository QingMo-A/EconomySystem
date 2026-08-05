package com.mo.economy_system.common.check;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ClientFileCheckResultJsonCodecTest {
  @Test
  void roundTripsDeterministically() {
    var value =
        new ClientFileCheckResult(
            1,
            ClientFileCheckStatus.SUCCESS,
            ClientFileCheckType.MODS,
            List.of(new ClientFileCheckEntry("a.jar", 3, "0".repeat(64))),
            List.of(),
            null);
    String json = ClientFileCheckResultJsonCodec.encode(value);
    assertEquals(value, ClientFileCheckResultJsonCodec.decode(json));
    assertEquals(
        json, ClientFileCheckResultJsonCodec.encode(ClientFileCheckResultJsonCodec.decode(json)));
    assertFalse(json.contains("\n"));
  }

  @Test
  void supportsTerminalStatuses() {
    assertEquals(
        ClientFileCheckStatus.DECLINED,
        ClientFileCheckResultJsonCodec.decode(
                ClientFileCheckResultJsonCodec.encode(
                    ClientFileCheckResult.declined(ClientFileCheckType.MODS)))
            .status());
    assertEquals(
        ClientFileCheckStatus.FAILED,
        ClientFileCheckResultJsonCodec.decode(
                ClientFileCheckResultJsonCodec.encode(
                    ClientFileCheckResult.failed(ClientFileCheckType.MODS, "SCAN_FAILED")))
            .status());
    var truncated =
        new ClientFileCheckResult(
            1,
            ClientFileCheckStatus.TRUNCATED,
            ClientFileCheckType.MODS,
            List.of(),
            List.of(),
            "FILE_LIMIT");
    assertEquals(
        ClientFileCheckStatus.TRUNCATED,
        ClientFileCheckResultJsonCodec.decode(ClientFileCheckResultJsonCodec.encode(truncated))
            .status());
  }

  @Test
  void rejectsMalformedOrAmbiguousJson() {
    String valid =
        ClientFileCheckResultJsonCodec.encode(
            ClientFileCheckResult.declined(ClientFileCheckType.MODS));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ClientFileCheckResultJsonCodec.decode(
                valid.replaceFirst("\\{", "{\"status\":\"DECLINED\",")));
    assertThrows(
        IllegalArgumentException.class,
        () -> ClientFileCheckResultJsonCodec.decode(valid.replace("\"errorCode\"", "\"unknown\"")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            ClientFileCheckResultJsonCodec.decode(
                valid.replace("\"schemaVersion\":1", "\"schemaVersion\":2")));
    assertThrows(
        IllegalArgumentException.class,
        () -> ClientFileCheckResultJsonCodec.decode(valid.replace("\"mods\"", "\"MODS\"")));
  }

  @Test
  void rejectsUnsafeEntries() {
    assertThrows(
        IllegalArgumentException.class, () -> new ClientFileCheckEntry("../x", 1, "0".repeat(64)));
    assertThrows(
        IllegalArgumentException.class, () -> new ClientFileCheckEntry("x", -1, "0".repeat(64)));
    assertThrows(
        IllegalArgumentException.class, () -> new ClientFileCheckEntry("x", 1, "A".repeat(64)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ClientFileCheckResult(
                1,
                ClientFileCheckStatus.DECLINED,
                ClientFileCheckType.MODS,
                List.of(new ClientFileCheckEntry("x", 1, "0".repeat(64))),
                List.of(),
                null));
  }
}
