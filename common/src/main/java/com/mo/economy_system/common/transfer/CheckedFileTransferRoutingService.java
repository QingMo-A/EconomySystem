package com.mo.economy_system.common.transfer;

import com.mo.economy_system.common.check.ClientFileCheckType;
import com.mo.economy_system.common.network.CheckedFileTransferChunkRequestMessage;
import com.mo.economy_system.common.network.CheckedFileTransferChunkResponseMessage;
import com.mo.economy_system.common.network.CheckedFileTransferControlRequestMessage;
import com.mo.economy_system.common.network.CheckedFileTransferControlResponseMessage;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/** Authenticated transactional server routing for protocols 27 and 29. */
public final class CheckedFileTransferRoutingService {
  public interface PlayerLookup { Object find(UUID id); }
  public interface Sender { void send(Object player, Object message); }
  public interface Diagnostics { void record(String stage, UUID targetId, UUID requesterId, Throwable failure); }
  private static final Diagnostics NO_DIAGNOSTICS = (stage, target, requester, failure) -> {};

  private CheckedFileTransferRoutingService() {}

  public static CheckedFileTransferStore.Result control(
      CheckedFileTransferControlRequestMessage message, UUID authenticatedTarget, long tick,
      CheckedFileTransferStore store, PlayerLookup lookup, Sender sender) {
    return control(message, authenticatedTarget, tick, store, lookup, sender, NO_DIAGNOSTICS);
  }

  public static CheckedFileTransferStore.Result control(
      CheckedFileTransferControlRequestMessage message, UUID authenticatedTarget, long tick,
      CheckedFileTransferStore store, PlayerLookup lookup, Sender sender, Diagnostics diagnostics) {
    Objects.requireNonNull(message); Objects.requireNonNull(authenticatedTarget);
    Objects.requireNonNull(store); Objects.requireNonNull(lookup); Objects.requireNonNull(sender);
    Objects.requireNonNull(diagnostics);
    CheckedFileTransferStore.Key key = key(message.targetPlayerId(), message.requesterPlayerId(),
        message.checkType(), message.fileName());
    if (!authenticatedTarget.equals(message.targetPlayerId())) {
      return CheckedFileTransferStore.Result.WRONG_TARGET;
    }
    if (!store.contains(key, tick)) return CheckedFileTransferStore.Result.NOT_FOUND;
    if (!store.metadataMatches(key, message.targetPlayerName(), message.requesterPlayerName(), tick)) {
      store.discard(key, tick);
      return CheckedFileTransferStore.Result.INVALID_METADATA;
    }
    CheckedFileTransferControl control;
    try { control = CheckedFileTransferControlJsonCodec.decode(message.controlPayload()); }
    catch (RuntimeException invalid) {
      store.discard(key, tick);
      return CheckedFileTransferStore.Result.INVALID_METADATA;
    }
    if (control.status() == CheckedFileTransferControlStatus.COMPLETE) {
      store.discard(key, tick);
      return CheckedFileTransferStore.Result.INVALID_METADATA;
    }
    Object requester;
    try { requester = lookup.find(message.requesterPlayerId()); }
    catch (RuntimeException failure) {
      store.discard(key, tick); diagnose(diagnostics, "requester_lookup", message, failure);
      return CheckedFileTransferStore.Result.REQUESTER_OFFLINE;
    } catch (Error failure) {
      store.discard(key, tick); diagnose(diagnostics, "requester_lookup", message, failure);
      throw failure;
    }
    if (requester == null) {
      store.discard(key, tick);
      return CheckedFileTransferStore.Result.REQUESTER_OFFLINE;
    }
    if (control.status() != CheckedFileTransferControlStatus.READY) {
      store.discard(key, tick);
      return sendTerminal(requester, response(message, control), message, sender, diagnostics);
    }
    CheckedFileTransferStore.PrepareReady prepared =
        store.prepareReady(key, authenticatedTarget, control, tick);
    if (prepared.claim() == null) return prepared.result();
    try {
      sender.send(requester, response(message, control));
    } catch (RuntimeException failure) {
      store.consumeClaim(prepared.claim());
      diagnose(diagnostics, "ready_forward", message, failure);
      return CheckedFileTransferStore.Result.REQUESTER_OFFLINE;
    } catch (Error failure) {
      store.consumeClaim(prepared.claim());
      diagnose(diagnostics, "ready_forward", message, failure);
      throw failure;
    }
    CheckedFileTransferStore.Result committed = store.commitReady(prepared.claim(), tick);
    if (committed != CheckedFileTransferStore.Result.COMPLETE) return committed;
    CheckedFileTransferControl complete = CheckedFileTransferControl.complete(
        control.transferId(), control.byteLength(), control.sha256());
    store.complete(key, tick);
    try { sender.send(requester, response(message, complete)); }
    catch (RuntimeException failure) { diagnose(diagnostics, "complete_forward", message, failure); }
    catch (Error failure) { diagnose(diagnostics, "complete_forward", message, failure); throw failure; }
    return CheckedFileTransferStore.Result.COMPLETE;
  }

  public static CheckedFileTransferStore.Result chunk(
      CheckedFileTransferChunkRequestMessage message, UUID authenticatedTarget, long tick,
      CheckedFileTransferStore store, PlayerLookup lookup, Sender sender) {
    return chunk(message, authenticatedTarget, tick, store, lookup, sender, NO_DIAGNOSTICS);
  }

  public static CheckedFileTransferStore.Result chunk(
      CheckedFileTransferChunkRequestMessage message, UUID authenticatedTarget, long tick,
      CheckedFileTransferStore store, PlayerLookup lookup, Sender sender, Diagnostics diagnostics) {
    Objects.requireNonNull(message); Objects.requireNonNull(authenticatedTarget);
    CheckedFileTransferStore.Key key = key(message.targetPlayerId(), message.requesterPlayerId(),
        message.checkType(), message.fileName());
    // Authentication is intentionally the first state-affecting check.
    if (!authenticatedTarget.equals(message.targetPlayerId())) {
      return CheckedFileTransferStore.Result.WRONG_TARGET;
    }
    if (!store.contains(key, tick)) return CheckedFileTransferStore.Result.NOT_FOUND;
    if (!store.metadataMatches(key, message.targetPlayerName(), message.requesterPlayerName(), tick)) {
      store.discard(key, tick);
      return CheckedFileTransferStore.Result.INVALID_METADATA;
    }
    byte[] raw;
    try { raw = decodeCanonical(message.chunkData()); }
    catch (RuntimeException invalid) {
      store.discard(key, tick);
      return CheckedFileTransferStore.Result.INVALID_CHUNK;
    }
    CheckedFileTransferStore.PrepareChunk prepared = store.prepareChunk(key, authenticatedTarget,
        message.transferId(), message.chunkIndex(), message.totalChunks(), raw, tick);
    if (prepared.claim() == null) return prepared.result();
    Object requester;
    try { requester = lookup.find(message.requesterPlayerId()); }
    catch (RuntimeException failure) {
      store.consumeClaim(prepared.claim()); diagnose(diagnostics, "requester_lookup", message, failure);
      return CheckedFileTransferStore.Result.REQUESTER_OFFLINE;
    } catch (Error failure) {
      store.consumeClaim(prepared.claim()); diagnose(diagnostics, "requester_lookup", message, failure);
      throw failure;
    }
    if (requester == null) {
      store.consumeClaim(prepared.claim());
      return CheckedFileTransferStore.Result.REQUESTER_OFFLINE;
    }
    try { sender.send(requester, chunkResponse(message)); }
    catch (RuntimeException failure) {
      store.consumeClaim(prepared.claim()); diagnose(diagnostics, "chunk_forward", message, failure);
      return CheckedFileTransferStore.Result.REQUESTER_OFFLINE;
    } catch (Error failure) {
      store.consumeClaim(prepared.claim()); diagnose(diagnostics, "chunk_forward", message, failure);
      throw failure;
    }
    CheckedFileTransferStore.Result committed = store.commitChunk(prepared.claim(), tick);
    if (committed != CheckedFileTransferStore.Result.COMPLETE) return committed;
    store.complete(key, tick);
    CheckedFileTransferControl complete = CheckedFileTransferControl.complete(
        message.transferId(), prepared.claim().byteLength(), prepared.claim().sha256());
    try { sender.send(requester, response(message, complete)); }
    catch (RuntimeException failure) { diagnose(diagnostics, "complete_forward", message, failure); }
    catch (Error failure) { diagnose(diagnostics, "complete_forward", message, failure); throw failure; }
    return CheckedFileTransferStore.Result.COMPLETE;
  }

  public static byte[] decodeCanonical(String encoded) {
    if (encoded == null || encoded.length() > EconomyNetworkLimits.MAX_TRANSFER_CHUNK_BASE64_CHARS
        || encoded.chars().anyMatch(Character::isWhitespace)) throw new IllegalArgumentException("base64");
    byte[] raw = Base64.getDecoder().decode(encoded);
    if (!Arrays.equals(Base64.getEncoder().encode(raw), encoded.getBytes(StandardCharsets.US_ASCII))) {
      throw new IllegalArgumentException("canonical base64");
    }
    return raw;
  }

  private static CheckedFileTransferStore.Result sendTerminal(
      Object requester, CheckedFileTransferControlResponseMessage response, Object source,
      Sender sender, Diagnostics diagnostics) {
    try { sender.send(requester, response); return CheckedFileTransferStore.Result.COMPLETE; }
    catch (RuntimeException failure) { diagnose(diagnostics, "terminal_forward", source, failure); return CheckedFileTransferStore.Result.REQUESTER_OFFLINE; }
    catch (Error failure) { diagnose(diagnostics, "terminal_forward", source, failure); throw failure; }
  }
  private static CheckedFileTransferStore.Key key(UUID target, UUID requester,
                                                   ClientFileCheckType type, String file) {
    return new CheckedFileTransferStore.Key(target, requester, type, file);
  }
  private static CheckedFileTransferChunkResponseMessage chunkResponse(CheckedFileTransferChunkRequestMessage m) {
    return new CheckedFileTransferChunkResponseMessage(m.targetPlayerName(), m.targetPlayerId(),
        m.requesterPlayerName(), m.requesterPlayerId(), m.checkType(), m.fileName(),
        m.transferId(), m.chunkIndex(), m.totalChunks(), m.chunkData());
  }
  private static CheckedFileTransferControlResponseMessage response(
      CheckedFileTransferControlRequestMessage m, CheckedFileTransferControl c) {
    return new CheckedFileTransferControlResponseMessage(m.targetPlayerName(), m.targetPlayerId(),
        m.requesterPlayerName(), m.requesterPlayerId(), m.checkType(), m.fileName(),
        CheckedFileTransferControlJsonCodec.encode(c));
  }
  private static CheckedFileTransferControlResponseMessage response(
      CheckedFileTransferChunkRequestMessage m, CheckedFileTransferControl c) {
    return new CheckedFileTransferControlResponseMessage(m.targetPlayerName(), m.targetPlayerId(),
        m.requesterPlayerName(), m.requesterPlayerId(), m.checkType(), m.fileName(),
        CheckedFileTransferControlJsonCodec.encode(c));
  }
  private static void diagnose(Diagnostics diagnostics, String stage, Object source, Throwable failure) {
    UUID target; UUID requester;
    if (source instanceof CheckedFileTransferControlRequestMessage m) {
      target = m.targetPlayerId(); requester = m.requesterPlayerId();
    } else { CheckedFileTransferChunkRequestMessage m = (CheckedFileTransferChunkRequestMessage) source;
      target = m.targetPlayerId(); requester = m.requesterPlayerId(); }
    try { diagnostics.record(stage, target, requester, failure); } catch (RuntimeException | Error ignored) {}
  }
}
