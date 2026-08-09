package com.mo.economy_system.network;

import com.mo.economy_system.common.check.ClientFileCheckType;
import com.mo.economy_system.common.network.CheckedFileTransferChunkRequestMessage;
import com.mo.economy_system.common.network.CheckedFileTransferChunkResponseMessage;
import com.mo.economy_system.common.network.CheckedFileTransferControlRequestMessage;
import com.mo.economy_system.common.network.CheckedFileTransferControlResponseMessage;
import com.mo.economy_system.common.network.CheckedFileTransferRequestMessage;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.transfer.CheckedFileTransferControlJsonCodec;
import com.mo.economy_system.common.transfer.CheckedFileTransferControlStatus;
import com.mo.economy_system.common.transfer.CheckedFileTransferValidation;
import com.mo.economy_system.platform.network.WireBuffer;
import com.mo.economy_system.platform.network.WireDecodeException;
import java.util.UUID;
import java.util.function.Function;

/** Legacy-compatible fields with bounded, atomic buffer operations. */
public final class CheckedFileTransferWireCodec {
  private CheckedFileTransferWireCodec() {}

  public static void encodeRequest(CheckedFileTransferRequestMessage message, WireBuffer out) {
    atomic(out, buffer -> base(buffer, message.targetPlayerName(), message.targetPlayerId(),
        message.requesterPlayerName(), message.requesterPlayerId(), message.checkType(), message.fileName()));
  }

  public static CheckedFileTransferRequestMessage decodeRequest(WireBuffer buffer) {
    return decode(buffer, value -> {
      Base base = base(value);
      return new CheckedFileTransferRequestMessage(base.targetName, base.targetId, base.requesterName,
          base.requesterId, base.type, base.fileName);
    });
  }

  public static void encodeControlRequest(CheckedFileTransferControlRequestMessage message, WireBuffer out) {
    atomic(out, buffer -> {
      base(buffer, message.targetPlayerName(), message.targetPlayerId(), message.requesterPlayerName(),
          message.requesterPlayerId(), message.checkType(), message.fileName());
      buffer.writeUtf(message.controlPayload(), EconomyNetworkLimits.MAX_TRANSFER_CONTROL_JSON_CHARS);
    });
  }

  public static CheckedFileTransferControlRequestMessage decodeControlRequest(WireBuffer buffer) {
    return decode(buffer, value -> {
      Base base = base(value);
      String payload = value.readUtf(EconomyNetworkLimits.MAX_TRANSFER_CONTROL_JSON_CHARS);
      if (CheckedFileTransferControlJsonCodec.decode(payload).status() == CheckedFileTransferControlStatus.COMPLETE) {
        throw new WireDecodeException("client COMPLETE is forbidden");
      }
      return new CheckedFileTransferControlRequestMessage(base.targetName, base.targetId, base.requesterName,
          base.requesterId, base.type, base.fileName, payload);
    });
  }

  public static void encodeControlResponse(CheckedFileTransferControlResponseMessage message, WireBuffer out) {
    atomic(out, buffer -> {
      base(buffer, message.targetPlayerName(), message.targetPlayerId(), message.requesterPlayerName(),
          message.requesterPlayerId(), message.checkType(), message.fileName());
      buffer.writeUtf(message.controlPayload(), EconomyNetworkLimits.MAX_TRANSFER_CONTROL_JSON_CHARS);
    });
  }

  public static CheckedFileTransferControlResponseMessage decodeControlResponse(WireBuffer buffer) {
    return decode(buffer, value -> {
      Base base = base(value);
      String payload = value.readUtf(EconomyNetworkLimits.MAX_TRANSFER_CONTROL_JSON_CHARS);
      CheckedFileTransferControlJsonCodec.decode(payload);
      return new CheckedFileTransferControlResponseMessage(base.targetName, base.targetId, base.requesterName,
          base.requesterId, base.type, base.fileName, payload);
    });
  }

  public static void encodeChunkRequest(CheckedFileTransferChunkRequestMessage message, WireBuffer out) {
    atomic(out, buffer -> {
      base(buffer, message.targetPlayerName(), message.targetPlayerId(), message.requesterPlayerName(),
          message.requesterPlayerId(), message.checkType(), message.fileName());
      chunk(buffer, message.transferId(), message.chunkIndex(), message.totalChunks(), message.chunkData());
    });
  }

  public static CheckedFileTransferChunkRequestMessage decodeChunkRequest(WireBuffer buffer) {
    return decode(buffer, value -> {
      Base base = base(value);
      Chunk chunk = chunk(value);
      return new CheckedFileTransferChunkRequestMessage(base.targetName, base.targetId, base.requesterName,
          base.requesterId, base.type, base.fileName, chunk.id, chunk.index, chunk.total, chunk.data);
    });
  }

  public static void encodeChunkResponse(CheckedFileTransferChunkResponseMessage message, WireBuffer out) {
    atomic(out, buffer -> {
      base(buffer, message.targetPlayerName(), message.targetPlayerId(), message.requesterPlayerName(),
          message.requesterPlayerId(), message.checkType(), message.fileName());
      chunk(buffer, message.transferId(), message.chunkIndex(), message.totalChunks(), message.chunkData());
    });
  }

  public static CheckedFileTransferChunkResponseMessage decodeChunkResponse(WireBuffer buffer) {
    return decode(buffer, value -> {
      Base base = base(value);
      Chunk chunk = chunk(value);
      return new CheckedFileTransferChunkResponseMessage(base.targetName, base.targetId, base.requesterName,
          base.requesterId, base.type, base.fileName, chunk.id, chunk.index, chunk.total, chunk.data);
    });
  }

  private static void base(WireBuffer buffer, String targetName, UUID targetId, String requesterName,
      UUID requesterId, ClientFileCheckType type, String fileName) {
    buffer.writeUtf(targetName, EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH);
    buffer.writeUtf(CheckedFileTransferValidation.canonicalUuid(targetId), 36);
    buffer.writeUtf(requesterName, EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH);
    buffer.writeUtf(CheckedFileTransferValidation.canonicalUuid(requesterId), 36);
    buffer.writeUtf(type.id(), 32);
    buffer.writeUtf(fileName, EconomyNetworkLimits.MAX_TRANSFER_FILE_NAME_CHARS);
  }

  private static Base base(WireBuffer buffer) {
    return new Base(buffer.readUtf(EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH),
        CheckedFileTransferValidation.canonicalUuid(buffer.readUtf(36)),
        buffer.readUtf(EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH),
        CheckedFileTransferValidation.canonicalUuid(buffer.readUtf(36)),
        ClientFileCheckType.fromId(buffer.readUtf(32)),
        buffer.readUtf(EconomyNetworkLimits.MAX_TRANSFER_FILE_NAME_CHARS));
  }

  private static void chunk(WireBuffer buffer, UUID id, int index, int total, String data) {
    buffer.writeUtf(CheckedFileTransferValidation.canonicalUuid(id), 36);
    buffer.writeInt(index);
    buffer.writeInt(total);
    buffer.writeUtf(data, EconomyNetworkLimits.MAX_TRANSFER_CHUNK_BASE64_CHARS);
  }

  private static Chunk chunk(WireBuffer buffer) {
    return new Chunk(CheckedFileTransferValidation.canonicalUuid(buffer.readUtf(36)), buffer.readInt(),
        buffer.readInt(), buffer.readUtf(EconomyNetworkLimits.MAX_TRANSFER_CHUNK_BASE64_CHARS));
  }

  private static void atomic(WireBuffer destination, Writer writer) {
    try (WireBuffer temporary = destination.temporary()) {
      writer.write(temporary);
      destination.writeRemaining(temporary);
    }
  }

  private static <T> T decode(WireBuffer buffer, Function<WireBuffer, T> decoder) {
    try {
      T value = decoder.apply(buffer);
      if (buffer.isReadable()) throw new WireDecodeException("trailing transfer bytes");
      return value;
    } catch (RuntimeException failure) {
      if (failure instanceof WireDecodeException) throw failure;
      throw new WireDecodeException("invalid transfer payload", failure);
    }
  }

  @FunctionalInterface
  private interface Writer {
    void write(WireBuffer buffer);
  }

  private record Base(String targetName, UUID targetId, String requesterName, UUID requesterId,
                      ClientFileCheckType type, String fileName) {}

  private record Chunk(UUID id, int index, int total, String data) {}
}
