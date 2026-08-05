package com.mo.economy_system.common.transfer;

import com.mo.economy_system.common.network.*;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Streams a private snapshot as independent canonical Base64 chunks. */
public final class CheckedFileTransferOutgoing {
  private CheckedFileTransferOutgoing() {}

  public static void send(
      CheckedFileTransferRequestMessage request,
      CheckedFileSnapshotter.Snapshot snapshot,
      BooleanSupplier valid,
      Consumer<Object> sender)
      throws IOException {
    UUID id = UUID.randomUUID();
    int total =
        CheckedFileTransferValidation.totalChunks(
            snapshot.size(), EconomyNetworkLimits.TRANSFER_RAW_CHUNK_BYTES);
    sender.accept(
        control(
            request,
            CheckedFileTransferControl.ready(id, snapshot.size(), snapshot.sha256())));
    if (!valid.getAsBoolean()) return;
    try (InputStream input = Files.newInputStream(snapshot.path(), StandardOpenOption.READ)) {
      byte[] buffer = new byte[EconomyNetworkLimits.TRANSFER_RAW_CHUNK_BYTES];
      for (int index = 0; index < total; index++) {
        int expected =
            (int)
                Math.min(
                    buffer.length, snapshot.size() - (long) index * buffer.length);
        int offset = 0;
        while (offset < expected) {
          int read = input.read(buffer, offset, expected - offset);
          if (read < 0) throw new EOFException();
          offset += read;
        }
        if (!valid.getAsBoolean()) return;
        byte[] raw = expected == buffer.length ? buffer : Arrays.copyOf(buffer, expected);
        String encoded =
            new String(Base64.getEncoder().encode(raw), StandardCharsets.US_ASCII);
        sender.accept(
            new CheckedFileTransferChunkRequestMessage(
                request.targetPlayerName(),
                request.targetPlayerId(),
                request.requesterPlayerName(),
                request.requesterPlayerId(),
                request.checkType(),
                request.fileName(),
                id,
                index,
                total,
                encoded));
      }
      if (input.read() != -1) throw new IOException("snapshot changed");
    }
  }

  public static CheckedFileTransferControlRequestMessage control(
      CheckedFileTransferRequestMessage request, CheckedFileTransferControl control) {
    return new CheckedFileTransferControlRequestMessage(
        request.targetPlayerName(),
        request.targetPlayerId(),
        request.requesterPlayerName(),
        request.requesterPlayerId(),
        request.checkType(),
        request.fileName(),
        CheckedFileTransferControlJsonCodec.encode(control));
  }
}
