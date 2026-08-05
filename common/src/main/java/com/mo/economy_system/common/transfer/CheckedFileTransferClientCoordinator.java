package com.mo.economy_system.common.transfer;

import com.mo.economy_system.common.check.ClientFileCheckTaskCoordinator;
import com.mo.economy_system.common.network.CheckedFileTransferChunkResponseMessage;
import com.mo.economy_system.common.network.CheckedFileTransferControlResponseMessage;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

/**
 * Loader-neutral owner for one client's checked-file transfer session.
 *
 * <p>Both outgoing snapshots and incoming part files use the same bounded temporary budget. The
 * coordinator invalidates all state before replacing a connection, so a worker or packet from a
 * previous connection can never mutate the new session.
 */
public final class CheckedFileTransferClientCoordinator implements AutoCloseable {
  public enum IncomingResult {
    OPEN,
    DUPLICATE,
    BUSY,
    ACCEPTED,
    COMPLETE,
    TERMINAL,
    IGNORED,
    INVALID
  }

  private final CheckedFileTransferTempBudget tempBudget;
  private final CheckedFileTransferOutgoing outgoing;
  private final long incomingTtlNanos;
  private long generation;
  private ClientFileCheckTaskCoordinator.Session session;
  private CheckedFileTransferIncoming incoming;
  private CheckedFileTransferReceivedArtifact artifact;
  private boolean closed;

  public CheckedFileTransferClientCoordinator() {
    this(new CheckedFileTransferTempBudget(), 60_000_000_000L);
  }

  public CheckedFileTransferClientCoordinator(
      CheckedFileTransferTempBudget tempBudget, long incomingTtlNanos) {
    this.tempBudget = Objects.requireNonNull(tempBudget, "temp budget");
    if (incomingTtlNanos <= 0) throw new IllegalArgumentException("incoming ttl");
    this.incomingTtlNanos = incomingTtlNanos;
    outgoing = new CheckedFileTransferOutgoing(tempBudget);
  }

  public synchronized ClientFileCheckTaskCoordinator.Session beginSession(
      Object connectionIdentity, UUID localPlayerId) {
    if (closed) throw new IllegalStateException("coordinator closed");
    invalidateSessionLocked();
    generation = generation == Long.MAX_VALUE ? 1 : generation + 1;
    session = new ClientFileCheckTaskCoordinator.Session(generation, connectionIdentity, localPlayerId);
    return session;
  }

  public synchronized ClientFileCheckTaskCoordinator.Session currentSession() {
    return session;
  }

  public CheckedFileTransferOutgoing outgoing() {
    return outgoing;
  }

  public CheckedFileTransferTempBudget tempBudget() {
    return tempBudget;
  }

  public synchronized CheckedFileTransferIncoming incoming() {
    return incoming;
  }

  public synchronized CheckedFileTransferReceivedArtifact completedArtifact() {
    return artifact;
  }

  /**
   * Handles a control response. READY creates a part stream; terminal controls only clear the
   * exact matching incoming operation. Malformed responses never clear an unrelated operation.
   */
  public IncomingResult control(
      CheckedFileTransferControlResponseMessage message, Path temporaryDirectory, long nowNanos) {
    Objects.requireNonNull(message, "message");
    Objects.requireNonNull(temporaryDirectory, "temporary directory");
    final CheckedFileTransferControl control;
    try {
      control = CheckedFileTransferControlJsonCodec.decode(message.controlPayload());
    } catch (RuntimeException malformed) {
      synchronized (this) {
        if (incoming != null && incomingMetadataMatches(message)) clearIncomingLocked();
      }
      return IncomingResult.INVALID;
    }
    if (control.status() == CheckedFileTransferControlStatus.READY) {
      return receiveReady(message, control, temporaryDirectory, nowNanos);
    }
    synchronized (this) {
      if (!isCurrentSessionLocked() || !requesterIsLocalLocked(message)) return IncomingResult.IGNORED;
      if (incoming == null || !incomingMetadataMatches(message)) return IncomingResult.IGNORED;
      if (control.status() == CheckedFileTransferControlStatus.COMPLETE) {
        try {
          artifact = incoming.completeArtifact(message, control);
          artifactDeadlineNanos = safeDeadline(nowNanos);
          incoming = null;
          return IncomingResult.COMPLETE;
        } catch (IOException failure) {
          clearIncomingLocked();
          return IncomingResult.INVALID;
        }
      }
      clearIncomingLocked();
      return IncomingResult.TERMINAL;
    }
  }

  /** Handles a chunk only when all metadata and session identity match the active stream. */
  public IncomingResult chunk(CheckedFileTransferChunkResponseMessage message) {
    Objects.requireNonNull(message, "message");
    synchronized (this) {
      if (!isCurrentSessionLocked() || !requesterIsLocalLocked(message) || incoming == null) {
        return IncomingResult.IGNORED;
      }
      if (!incomingMetadataMatches(message)) {
        clearIncomingLocked();
        return IncomingResult.INVALID;
      }
      try {
        incoming.chunk(message, session);
        return IncomingResult.ACCEPTED;
      } catch (IOException failure) {
        clearIncomingLocked();
        return IncomingResult.INVALID;
      }
    }
  }

  /** Expires incomplete streams and stale completed artifacts without touching unrelated state. */
  public synchronized boolean tick(long nowNanos) {
    boolean changed = false;
    if (incoming != null && incoming.expire(nowNanos)) {
      incoming = null;
      changed = true;
    }
    if (artifact != null && artifact.isPendingDecision() && nowNanos >= artifactDeadlineNanos) {
      artifact.discard();
      artifact = null;
      artifactDeadlineNanos = 0;
      changed = true;
    }
    changed |= outgoing.tick(nowNanos);
    return changed;
  }

  private long artifactDeadlineNanos;

  private IncomingResult receiveReady(
      CheckedFileTransferControlResponseMessage message,
      CheckedFileTransferControl control,
      Path temporaryDirectory,
      long nowNanos) {
    synchronized (this) {
      if (!isCurrentSessionLocked() || !requesterIsLocalLocked(message)) return IncomingResult.IGNORED;
      if (incoming != null) {
        if (incomingMetadataMatches(message)
            && incoming.transferId().equals(control.transferId())) return IncomingResult.DUPLICATE;
        return IncomingResult.BUSY;
      }
      if (control.byteLength() < 0
          || control.byteLength() > EconomyNetworkLimits.MAX_TRANSFER_FILE_BYTES) {
        return IncomingResult.INVALID;
      }
      try {
        CheckedFileTransferIncoming.Metadata metadata =
            new CheckedFileTransferIncoming.Metadata(
                message.targetPlayerName(),
                message.targetPlayerId(),
                message.requesterPlayerName(),
                message.requesterPlayerId(),
                message.checkType(),
                message.fileName(),
                control.transferId(),
                control.byteLength(),
                control.sha256(),
                control.totalChunks());
        incoming =
            new CheckedFileTransferIncoming(
                metadata,
                session,
                temporaryDirectory,
                tempBudget,
                safeDeadline(nowNanos));
        return IncomingResult.OPEN;
      } catch (RuntimeException | IOException failure) {
        return IncomingResult.INVALID;
      }
    }
  }

  private boolean isCurrentSessionLocked() {
    return !closed && session != null;
  }

  private boolean requesterIsLocalLocked(CheckedFileTransferControlResponseMessage message) {
    return session != null && session.localPlayerId().equals(message.requesterPlayerId());
  }

  private boolean requesterIsLocalLocked(CheckedFileTransferChunkResponseMessage message) {
    return session != null && session.localPlayerId().equals(message.requesterPlayerId());
  }

  private boolean incomingMetadataMatches(CheckedFileTransferControlResponseMessage message) {
    return incoming != null
        && incoming.metadata() != null
        && incoming.metadata().targetPlayerName().equals(message.targetPlayerName())
        && incoming.metadata().targetPlayerId().equals(message.targetPlayerId())
        && incoming.metadata().requesterPlayerName().equals(message.requesterPlayerName())
        && incoming.metadata().requesterPlayerId().equals(message.requesterPlayerId())
        && incoming.metadata().checkType() == message.checkType()
        && incoming.metadata().fileName().equals(message.fileName());
  }

  private boolean incomingMetadataMatches(CheckedFileTransferChunkResponseMessage message) {
    return incoming != null
        && incoming.metadata() != null
        && incoming.metadata().matches(message);
  }

  public synchronized void invalidateSession() {
    invalidateSessionLocked();
  }

  private void invalidateSessionLocked() {
    outgoing.invalidateSession();
    clearIncomingLocked();
    if (artifact != null) {
      artifact.discard();
      artifact = null;
      artifactDeadlineNanos = 0;
    }
    session = null;
    tempBudget.clear();
  }

  private void clearIncomingLocked() {
    if (incoming != null) incoming.close();
    incoming = null;
  }

  private long safeDeadline(long nowNanos) {
    return nowNanos > Long.MAX_VALUE - incomingTtlNanos
        ? Long.MAX_VALUE
        : nowNanos + incomingTtlNanos;
  }

  @Override
  public synchronized void close() {
    if (closed) return;
    closed = true;
    invalidateSessionLocked();
    outgoing.close();
    tempBudget.clear();
  }
}
