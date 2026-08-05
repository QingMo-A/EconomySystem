package com.mo.economy_system.network;

import com.mo.economy_system.common.check.ClientFileCheckType;
import com.mo.economy_system.common.network.ClientFileCheckRequestMessage;
import com.mo.economy_system.common.network.ClientFileCheckResultRequestMessage;
import com.mo.economy_system.common.network.ClientFileCheckResultResponseMessage;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;

public final class ClientFileCheckWireCodec {
  private static final int UUID_LENGTH = 36;
  private static final int TYPE_LENGTH = 16;

  private ClientFileCheckWireCodec() {}

  public static void encodeRequest(ClientFileCheckRequestMessage message, FriendlyByteBuf buffer) {
    encodeAtomically(
        buffer,
        temporary ->
            writeMetadata(
                message.targetPlayerName(),
                message.targetPlayerId(),
                message.requesterPlayerName(),
                message.requesterPlayerId(),
                message.checkType(),
                temporary));
  }

  public static ClientFileCheckRequestMessage decodeRequest(FriendlyByteBuf buffer) {
    Metadata metadata = readMetadata(buffer);
    requireEnd(buffer);
    return new ClientFileCheckRequestMessage(
        metadata.targetName,
        metadata.targetId,
        metadata.requesterName,
        metadata.requesterId,
        metadata.type);
  }

  public static void encodeResultRequest(
      ClientFileCheckResultRequestMessage message, FriendlyByteBuf buffer) {
    encodeAtomically(
        buffer,
        temporary -> {
          writeMetadata(
              message.targetPlayerName(),
              message.targetPlayerId(),
              message.requesterPlayerName(),
              message.requesterPlayerId(),
              message.checkType(),
              temporary);
          temporary.writeUtf(
              message.resultJson(), EconomyNetworkLimits.MAX_CHECK_RESULT_JSON_LENGTH);
        });
  }

  public static ClientFileCheckResultRequestMessage decodeResultRequest(FriendlyByteBuf buffer) {
    Metadata metadata = readMetadata(buffer);
    String result =
        readUtf(buffer, EconomyNetworkLimits.MAX_CHECK_RESULT_JSON_LENGTH, "result JSON");
    requireEnd(buffer);
    return new ClientFileCheckResultRequestMessage(
        metadata.targetName,
        metadata.targetId,
        metadata.requesterName,
        metadata.requesterId,
        metadata.type,
        result);
  }

  public static void encodeResultResponse(
      ClientFileCheckResultResponseMessage message, FriendlyByteBuf buffer) {
    encodeAtomically(
        buffer,
        temporary -> {
          writeMetadata(
              message.targetPlayerName(),
              message.targetPlayerId(),
              message.requesterPlayerName(),
              message.requesterPlayerId(),
              message.checkType(),
              temporary);
          temporary.writeUtf(
              message.resultJson(), EconomyNetworkLimits.MAX_CHECK_RESULT_JSON_LENGTH);
        });
  }

  public static ClientFileCheckResultResponseMessage decodeResultResponse(FriendlyByteBuf buffer) {
    Metadata metadata = readMetadata(buffer);
    String result =
        readUtf(buffer, EconomyNetworkLimits.MAX_CHECK_RESULT_JSON_LENGTH, "result JSON");
    requireEnd(buffer);
    return new ClientFileCheckResultResponseMessage(
        metadata.targetName,
        metadata.targetId,
        metadata.requesterName,
        metadata.requesterId,
        metadata.type,
        result);
  }

  private static void writeMetadata(
      String targetName,
      UUID targetId,
      String requesterName,
      UUID requesterId,
      ClientFileCheckType type,
      FriendlyByteBuf buffer) {
    buffer.writeUtf(targetName, EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH);
    buffer.writeUtf(targetId.toString(), UUID_LENGTH);
    buffer.writeUtf(requesterName, EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH);
    buffer.writeUtf(requesterId.toString(), UUID_LENGTH);
    buffer.writeUtf(type.id(), TYPE_LENGTH);
  }

  private static Metadata readMetadata(FriendlyByteBuf buffer) {
    String targetName = readUtf(buffer, EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH, "target name");
    UUID targetId = readCanonicalUuid(buffer);
    String requesterName =
        readUtf(buffer, EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH, "requester name");
    UUID requesterId = readCanonicalUuid(buffer);
    ClientFileCheckType type;
    try {
      type = ClientFileCheckType.fromId(readUtf(buffer, TYPE_LENGTH, "check type"));
    } catch (IllegalArgumentException failure) {
      throw new DecoderException("invalid check type", failure);
    }
    return new Metadata(targetName, targetId, requesterName, requesterId, type);
  }

  private static UUID readCanonicalUuid(FriendlyByteBuf buffer) {
    String encoded = readUtf(buffer, UUID_LENGTH, "UUID");
    try {
      UUID uuid = UUID.fromString(encoded);
      if (!uuid.toString().equals(encoded)) throw new IllegalArgumentException("non-canonical");
      return uuid;
    } catch (IllegalArgumentException failure) {
      throw new DecoderException("invalid canonical UUID", failure);
    }
  }

  private static String readUtf(FriendlyByteBuf buffer, int max, String field) {
    try {
      return buffer.readUtf(max);
    } catch (RuntimeException failure) {
      throw new DecoderException("invalid " + field, failure);
    }
  }

  private static void requireEnd(FriendlyByteBuf buffer) {
    if (buffer.isReadable()) throw new DecoderException("trailing client file check payload data");
  }

  private static void encodeAtomically(FriendlyByteBuf destination, Encoder encoder) {
    FriendlyByteBuf temporary = new FriendlyByteBuf(Unpooled.buffer());
    try {
      encoder.encode(temporary);
      destination.writeBytes(temporary, temporary.readerIndex(), temporary.readableBytes());
    } finally {
      temporary.release();
    }
  }

  @FunctionalInterface
  private interface Encoder {
    void encode(FriendlyByteBuf buffer);
  }

  private record Metadata(
      String targetName,
      UUID targetId,
      String requesterName,
      UUID requesterId,
      ClientFileCheckType type) {}
}
