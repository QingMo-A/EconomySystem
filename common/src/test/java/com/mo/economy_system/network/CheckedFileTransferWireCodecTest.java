package com.mo.economy_system.network;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.check.ClientFileCheckType;
import com.mo.economy_system.common.network.CheckedFileTransferChunkRequestMessage;
import com.mo.economy_system.common.network.CheckedFileTransferControlRequestMessage;
import com.mo.economy_system.common.network.CheckedFileTransferRequestMessage;
import com.mo.economy_system.testsupport.TestWireBuffer;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CheckedFileTransferWireCodecTest {
  @Test
  void roundTripsLegacyShapesAndRejectsTrailing() {
    UUID targetId = UUID.randomUUID();
    UUID requesterId = UUID.randomUUID();
    UUID transferId = UUID.randomUUID();
    var request = new CheckedFileTransferRequestMessage(
        "target", targetId, "requester", requesterId, ClientFileCheckType.MODS, "a.jar");
    TestWireBuffer requestBuffer = new TestWireBuffer();
    CheckedFileTransferWireCodec.encodeRequest(request, requestBuffer);
    assertEquals(request, CheckedFileTransferWireCodec.decodeRequest(requestBuffer));

    var chunk = new CheckedFileTransferChunkRequestMessage(
        "target", targetId, "requester", requesterId, ClientFileCheckType.MODS,
        "a.jar", transferId, 0, 1, "YQ==");
    TestWireBuffer chunkBuffer = new TestWireBuffer();
    CheckedFileTransferWireCodec.encodeChunkRequest(chunk, chunkBuffer);
    chunkBuffer.writeByte(1);
    assertThrows(RuntimeException.class,
        () -> CheckedFileTransferWireCodec.decodeChunkRequest(chunkBuffer));
  }

  @Test
  void failedEncodeLeavesDestinationUntouched() {
    TestWireBuffer destination = new TestWireBuffer();
    destination.writeInt(7);
    int before = destination.writerIndex();
    assertThrows(RuntimeException.class, () -> CheckedFileTransferWireCodec.encodeControlRequest(
        new CheckedFileTransferControlRequestMessage(
            "target", UUID.randomUUID(), "requester", UUID.randomUUID(),
            ClientFileCheckType.MODS, "a", "{}".repeat(2000)), destination));
    assertEquals(before, destination.writerIndex());
  }
}
