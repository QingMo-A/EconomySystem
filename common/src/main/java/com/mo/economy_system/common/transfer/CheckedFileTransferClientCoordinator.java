package com.mo.economy_system.common.transfer;

import com.mo.economy_system.common.check.ClientFileCheckTaskCoordinator;
import com.mo.economy_system.common.check.ClientFileCheckType;
import com.mo.economy_system.common.check.ClientFileCheckValidation;
import com.mo.economy_system.common.network.CheckedFileTransferChunkResponseMessage;
import com.mo.economy_system.common.network.CheckedFileTransferControlResponseMessage;
import com.mo.economy_system.common.network.CheckedFileTransferRequestMessage;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.io.IOException;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.UUID;

/** Loader-neutral owner for one client's checked-file transfer lifecycle. */
public final class CheckedFileTransferClientCoordinator implements AutoCloseable {
  @FunctionalInterface
  interface TempDirectoryFactory {
    CheckedFileTransferTempDirectory open(Path gameDirectory) throws IOException;
  }
  public enum IncomingResult {
    OPEN,
    DUPLICATE,
    BUSY,
    ACCEPTED,
    COMPLETE,
    TERMINAL,
    ARTIFACT_PENDING,
    IGNORED_STALE_SESSION,
    IGNORED,
    INVALID
  }

  public enum RequestResult {
    OPEN,
    DUPLICATE,
    CONSENT_BUSY,
    IGNORED_STALE_SESSION,
    INVALID,
    CLOSED
  }

  /** Save results deliberately omit the absolute destination path. */
  public record SaveResult(CheckedFileTransferSaveService.ResultCode code) {
    public SaveResult {
      Objects.requireNonNull(code, "save result code");
    }

    public boolean success() {
      return code == CheckedFileTransferSaveService.ResultCode.SAVED;
    }
  }

  public record DiscardResult(CheckedFileTransferReceivedArtifact.DiscardResult code) {
    public DiscardResult {
      Objects.requireNonNull(code, "discard result code");
    }

    public boolean success() {
      return code == CheckedFileTransferReceivedArtifact.DiscardResult.DISCARDED;
    }
  }

  public record TerminalMetadata(
      String targetPlayerName,
      UUID targetPlayerId,
      String requesterPlayerName,
      UUID requesterPlayerId,
      ClientFileCheckType checkType,
      String fileName) {
    public TerminalMetadata {
      targetPlayerName = ClientFileCheckValidation.playerName(targetPlayerName);
      targetPlayerId = Objects.requireNonNull(targetPlayerId, "target player id");
      requesterPlayerName = ClientFileCheckValidation.playerName(requesterPlayerName);
      requesterPlayerId = Objects.requireNonNull(requesterPlayerId, "requester player id");
      checkType = CheckedFileTransferValidation.type(checkType);
      fileName = CheckedFileTransferValidation.fileName(fileName);
    }

    public static TerminalMetadata from(CheckedFileTransferControlResponseMessage message) {
      return new TerminalMetadata(
          message.targetPlayerName(),
          message.targetPlayerId(),
          message.requesterPlayerName(),
          message.requesterPlayerId(),
          message.checkType(),
          message.fileName());
    }

    boolean matches(CheckedFileTransferIncoming.Metadata metadata) {
      return metadata != null
          && targetPlayerName.equals(metadata.targetPlayerName())
          && targetPlayerId.equals(metadata.targetPlayerId())
          && requesterPlayerName.equals(metadata.requesterPlayerName())
          && requesterPlayerId.equals(metadata.requesterPlayerId())
          && checkType == metadata.checkType()
          && fileName.equals(metadata.fileName());
    }
  }

  public record TerminalResult(
      TerminalMetadata metadata,
      CheckedFileTransferControlStatus status,
      String errorCode,
      long createdNanos,
      long expiresNanos) {
    public TerminalResult {
      Objects.requireNonNull(metadata, "terminal metadata");
      Objects.requireNonNull(status, "terminal status");
      if (status == CheckedFileTransferControlStatus.READY
          || status == CheckedFileTransferControlStatus.COMPLETE) {
        throw new IllegalArgumentException("terminal status");
      }
      errorCode = ClientFileCheckValidation.errorCode(errorCode);
      if (expiresNanos <= createdNanos) throw new IllegalArgumentException("terminal expiry");
    }
  }

  private static final long DEFAULT_TTL_NANOS = 60_000_000_000L;

  private final CheckedFileTransferTempBudget tempBudget;
  private final CheckedFileTransferOutgoing outgoing;
  private final long lifecycleTtlNanos;
  private final TempDirectoryFactory tempDirectoryFactory;
  private final IdentityHashMap<Object, Boolean> arrivalConnectionIdentities =
      new IdentityHashMap<>();
  private long generation;
  private ClientFileCheckTaskCoordinator.Session session;
  private CheckedFileTransferTempDirectory tempDirectory;
  private CheckedFileTransferIncoming incoming;
  private CheckedFileTransferReceivedArtifact artifact;
  private long artifactDeadlineNanos;
  private TerminalResult terminalResult;
  private String lastErrorCode;
  private boolean closed;

  public CheckedFileTransferClientCoordinator() {
    this(new CheckedFileTransferTempBudget(), DEFAULT_TTL_NANOS);
  }

  public CheckedFileTransferClientCoordinator(
      CheckedFileTransferTempBudget tempBudget, long lifecycleTtlNanos) {
    this(tempBudget, lifecycleTtlNanos, CheckedFileTransferTempDirectory::open);
  }

  CheckedFileTransferClientCoordinator(
      CheckedFileTransferTempBudget tempBudget,
      long lifecycleTtlNanos,
      TempDirectoryFactory tempDirectoryFactory) {
    this.tempBudget = Objects.requireNonNull(tempBudget, "temp budget");
    if (lifecycleTtlNanos <= 0) throw new IllegalArgumentException("lifecycle ttl");
    this.lifecycleTtlNanos = lifecycleTtlNanos;
    this.tempDirectoryFactory =
        Objects.requireNonNull(tempDirectoryFactory, "temp directory factory");
    outgoing = new CheckedFileTransferOutgoing(tempBudget, lifecycleTtlNanos);
  }

  public synchronized ClientFileCheckTaskCoordinator.Session beginSession(
      Object connectionIdentity, UUID localPlayerId) {
    if (closed) throw new IllegalStateException("coordinator closed");
    invalidateSessionLocked();
    generation = generation == Long.MAX_VALUE ? 1 : generation + 1;
    session =
        new ClientFileCheckTaskCoordinator.Session(generation, connectionIdentity, localPlayerId);
    arrivalConnectionIdentities.put(connectionIdentity, Boolean.TRUE);
    outgoing.beginSession(session);
    return session;
  }

  /** Adds a loader network-connection object as an identity alias for the current session. */
  public synchronized void bindArrivalConnection(Object connectionIdentity) {
    if (closed || session == null || connectionIdentity == null) return;
    arrivalConnectionIdentities.put(connectionIdentity, Boolean.TRUE);
  }

  /** Captures the immutable session at packet arrival, before the main-thread callback is queued. */
  public synchronized ClientFileCheckTaskCoordinator.Session captureArrivalSession(
      Object connectionIdentity) {
    if (closed || session == null || connectionIdentity == null
        || !arrivalConnectionIdentities.containsKey(connectionIdentity)) {
      return null;
    }
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

  public synchronized TerminalResult terminalResult() {
    return terminalResult;
  }

  public synchronized String lastErrorCode() {
    return lastErrorCode;
  }

  /** Returns the coordinator-owned secure temp handle. Callers must not close it. */
  public synchronized CheckedFileTransferTempDirectory temporaryDirectory(Path gameDirectory)
      throws IOException {
    if (closed || session == null) throw new IOException("STALE_SESSION");
    Path game = CheckedFileTransferTempDirectory.normalizeGameDirectory(gameDirectory);
    if (tempDirectory == null) {
      tempDirectory = tempDirectoryFactory.open(game);
    } else if (!tempDirectory.gameDirectory().equals(game)) {
      throw new CheckedFileTransferTempDirectory.ProviderUnsafeException();
    }
    return tempDirectory;
  }

  /** Validates a protocol-26 request against the session captured at packet arrival. */
  public synchronized RequestResult receiveRequest(
      CheckedFileTransferRequestMessage message,
      ClientFileCheckTaskCoordinator.Session arrivalSession) {
    Objects.requireNonNull(message, "message");
    if (!isArrivalCurrentLocked(arrivalSession)) return RequestResult.IGNORED_STALE_SESSION;
    return switch (outgoing.receive(message, arrivalSession)) {
      case OPEN -> RequestResult.OPEN;
      case DUPLICATE -> RequestResult.DUPLICATE;
      case CONSENT_BUSY -> RequestResult.CONSENT_BUSY;
      case INVALID_SESSION -> RequestResult.INVALID;
      case CLOSED -> RequestResult.CLOSED;
    };
  }

  public synchronized boolean cancelRequest(
      CheckedFileTransferRequestMessage message,
      ClientFileCheckTaskCoordinator.Session arrivalSession) {
    return isArrivalCurrentLocked(arrivalSession) && outgoing.cancel(message, arrivalSession);
  }

  /** Compatibility entry point for adapters not yet passing an arrival session. */
  public IncomingResult control(
      CheckedFileTransferControlResponseMessage message,
      Path temporaryDirectory,
      long nowNanos) {
    Path root = Objects.requireNonNull(temporaryDirectory, "temporary directory")
        .toAbsolutePath().normalize();
    Path economy = root.getParent();
    Path game = economy == null ? null : economy.getParent();
    if (game == null || !root.equals(CheckedFileTransferTempDirectory.expectedPath(game))) {
      synchronized (this) {
        lastErrorCode = CheckedFileTransferTempDirectory.PROVIDER_UNSAFE;
      }
      return IncomingResult.INVALID;
    }
    return control(message, currentSession(), game, nowNanos);
  }

  /** Handles protocol 28 only when its arrival session is still current. */
  public IncomingResult control(
      CheckedFileTransferControlResponseMessage message,
      ClientFileCheckTaskCoordinator.Session arrivalSession,
      Path gameDirectory,
      long nowNanos) {
    Objects.requireNonNull(message, "message");
    Objects.requireNonNull(gameDirectory, "game directory");
    synchronized (this) {
      lastErrorCode = null;
      if (!isArrivalCurrentLocked(arrivalSession) || !requesterIsLocalLocked(message)) {
        return IncomingResult.IGNORED_STALE_SESSION;
      }
    }

    final CheckedFileTransferControl control;
    try {
      control = CheckedFileTransferControlJsonCodec.decode(message.controlPayload());
    } catch (RuntimeException malformed) {
      synchronized (this) {
        lastErrorCode = "INVALID_SERVER_RESPONSE";
        if (incoming != null && incomingMetadataMatches(message)) clearIncomingLocked();
        terminalResult = terminalFailure(message, "INVALID_SERVER_RESPONSE", nowNanos);
      }
      return IncomingResult.INVALID;
    }

    if (control.status() == CheckedFileTransferControlStatus.READY) {
      return receiveReady(message, control, arrivalSession, gameDirectory, nowNanos);
    }

    synchronized (this) {
      if (!isArrivalCurrentLocked(arrivalSession) || !requesterIsLocalLocked(message)) {
        return IncomingResult.IGNORED_STALE_SESSION;
      }
      if (control.status() == CheckedFileTransferControlStatus.COMPLETE) {
        if (incoming == null || !incomingMetadataMatches(message)
            || !incoming.transferId().equals(control.transferId())) {
          lastErrorCode = "INVALID_SERVER_RESPONSE";
          return IncomingResult.INVALID;
        }
        try {
          artifact = incoming.completeArtifact(message, control, arrivalSession);
          artifactDeadlineNanos = safeDeadline(nowNanos);
          incoming = null;
          terminalResult = null;
          return IncomingResult.COMPLETE;
        } catch (IOException | RuntimeException failure) {
          lastErrorCode = stableFailureCode(failure, "INVALID_SERVER_RESPONSE");
          clearIncomingLocked();
          terminalResult = terminalFailure(message, lastErrorCode, nowNanos);
          return IncomingResult.INVALID;
        }
      }

      TerminalMetadata metadata = TerminalMetadata.from(message);
      if (incoming != null && metadata.matches(incoming.metadata())) clearIncomingLocked();
      terminalResult =
          new TerminalResult(
              metadata, control.status(), control.errorCode(), nowNanos, safeDeadline(nowNanos));
      return IncomingResult.TERMINAL;
    }
  }

  /** Compatibility entry point for direct callers on the current session. */
  public IncomingResult chunk(CheckedFileTransferChunkResponseMessage message) {
    return chunk(message, currentSession());
  }

  /** Handles protocol 30 only when its arrival session is still current. */
  public IncomingResult chunk(
      CheckedFileTransferChunkResponseMessage message,
      ClientFileCheckTaskCoordinator.Session arrivalSession) {
    Objects.requireNonNull(message, "message");
    synchronized (this) {
      lastErrorCode = null;
      if (!isArrivalCurrentLocked(arrivalSession) || !requesterIsLocalLocked(message)) {
        return IncomingResult.IGNORED_STALE_SESSION;
      }
      if (incoming == null) return IncomingResult.IGNORED;
      if (!incomingMetadataMatches(message)) {
        lastErrorCode = "INVALID_SERVER_RESPONSE";
        clearIncomingLocked();
        return IncomingResult.INVALID;
      }
      try {
        incoming.chunk(message, arrivalSession);
        return IncomingResult.ACCEPTED;
      } catch (IOException | RuntimeException failure) {
        lastErrorCode = stableFailureCode(failure, "INVALID_SERVER_RESPONSE");
        clearIncomingLocked();
        return IncomingResult.INVALID;
      }
    }
  }

  /** Saves the managed artifact and clears it only after a successful secure move. */
  public synchronized SaveResult saveCompleted(Path gameDirectory) {
    if (artifact == null) {
      return new SaveResult(CheckedFileTransferSaveService.ResultCode.NOT_PENDING);
    }
    CheckedFileTransferSaveService.Result result =
        new CheckedFileTransferSaveService(gameDirectory).save(artifact);
    if (result.success()) {
      artifact = null;
      artifactDeadlineNanos = 0;
      closeUnusedTempDirectoryLocked();
    }
    return new SaveResult(result.code());
  }

  /** Discards the managed artifact and retains it when relative deletion fails. */
  public synchronized DiscardResult discardCompleted() {
    if (artifact == null) {
      return new DiscardResult(CheckedFileTransferReceivedArtifact.DiscardResult.NOT_PENDING);
    }
    CheckedFileTransferReceivedArtifact.DiscardResult result = artifact.discardResult();
    if (result == CheckedFileTransferReceivedArtifact.DiscardResult.DISCARDED
        || result == CheckedFileTransferReceivedArtifact.DiscardResult.NOT_PENDING) {
      artifact = null;
      artifactDeadlineNanos = 0;
      closeUnusedTempDirectoryLocked();
    }
    return new DiscardResult(result);
  }

  /** Expires streams, outgoing workers, artifacts, and bounded terminal results. */
  public synchronized boolean tick(long nowNanos) {
    boolean changed = false;
    if (incoming != null && incoming.expired(nowNanos)) {
      clearIncomingLocked();
      changed = true;
    } else if (incoming != null && !incoming.cleanupComplete()) {
      changed |= clearIncomingLocked();
    }
    if (artifact != null && artifact.isPendingDecision() && nowNanos >= artifactDeadlineNanos) {
      CheckedFileTransferReceivedArtifact.DiscardResult discarded = artifact.discardResult();
      if (discarded == CheckedFileTransferReceivedArtifact.DiscardResult.DISCARDED
          || discarded == CheckedFileTransferReceivedArtifact.DiscardResult.NOT_PENDING) {
        artifact = null;
        artifactDeadlineNanos = 0;
        closeUnusedTempDirectoryLocked();
      } else {
        artifactDeadlineNanos = safeDeadline(nowNanos);
      }
      changed = true;
    }
    if (terminalResult != null && nowNanos >= terminalResult.expiresNanos()) {
      terminalResult = null;
      changed = true;
    }
    changed |= outgoing.tick(nowNanos);
    closeUnusedTempDirectoryLocked();
    return changed;
  }

  private IncomingResult receiveReady(
      CheckedFileTransferControlResponseMessage message,
      CheckedFileTransferControl control,
      ClientFileCheckTaskCoordinator.Session arrivalSession,
      Path gameDirectory,
      long nowNanos) {
    synchronized (this) {
      if (!isArrivalCurrentLocked(arrivalSession) || !requesterIsLocalLocked(message)) {
        return IncomingResult.IGNORED_STALE_SESSION;
      }
      if (artifact != null && artifact.isPendingDecision()) {
        if (artifactMatches(message, control)) return IncomingResult.DUPLICATE;
        lastErrorCode = "ARTIFACT_PENDING";
        return IncomingResult.ARTIFACT_PENDING;
      }
      if (incoming != null) {
        if (incomingMetadataMatches(message)
            && incoming.transferId().equals(control.transferId())) return IncomingResult.DUPLICATE;
        return IncomingResult.BUSY;
      }
      if (control.byteLength() < 0
          || control.byteLength() > EconomyNetworkLimits.MAX_TRANSFER_FILE_BYTES) {
        lastErrorCode = "INVALID_SERVER_RESPONSE";
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
                arrivalSession,
                temporaryDirectory(gameDirectory),
                tempBudget,
                safeDeadline(nowNanos));
        terminalResult = null;
        return IncomingResult.OPEN;
      } catch (CheckedFileTransferTempDirectory.ProviderUnsafeException unsafe) {
        lastErrorCode = CheckedFileTransferTempDirectory.PROVIDER_UNSAFE;
        return IncomingResult.INVALID;
      } catch (IOException | RuntimeException failure) {
        lastErrorCode = stableFailureCode(failure, "INVALID_SERVER_RESPONSE");
        return IncomingResult.INVALID;
      }
    }
  }

  private boolean artifactMatches(
      CheckedFileTransferControlResponseMessage message, CheckedFileTransferControl control) {
    if (artifact == null) return false;
    CheckedFileTransferReceivedArtifact.Metadata metadata = artifact.metadata();
    return metadata.targetPlayerName().equals(message.targetPlayerName())
        && metadata.targetPlayerId().equals(message.targetPlayerId())
        && metadata.requesterPlayerName().equals(message.requesterPlayerName())
        && metadata.requesterPlayerId().equals(message.requesterPlayerId())
        && metadata.checkType() == message.checkType()
        && metadata.fileName().equals(message.fileName())
        && metadata.transferId().equals(control.transferId());
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
        && incoming.metadata().matches(message);
  }

  private boolean incomingMetadataMatches(CheckedFileTransferChunkResponseMessage message) {
    return incoming != null
        && incoming.metadata() != null
        && incoming.metadata().matches(message);
  }

  private boolean isArrivalCurrentLocked(ClientFileCheckTaskCoordinator.Session arrivalSession) {
    return !closed && sameSession(session, arrivalSession);
  }

  public synchronized void invalidateSession() {
    invalidateSessionLocked();
  }

  public synchronized void invalidateSession(ClientFileCheckTaskCoordinator.Session expected) {
    if (!sameSession(session, expected)) return;
    invalidateSessionLocked();
  }

  private void invalidateSessionLocked() {
    if (session != null) outgoing.invalidateSession(session);
    else outgoing.invalidateSession();
    clearIncomingLocked();
    if (artifact != null) {
      CheckedFileTransferReceivedArtifact.DiscardResult result = artifact.discardResult();
      if (result == CheckedFileTransferReceivedArtifact.DiscardResult.DISCARDED
          || result == CheckedFileTransferReceivedArtifact.DiscardResult.NOT_PENDING) {
        artifact = null;
        artifactDeadlineNanos = 0;
      }
    }
    session = null;
    arrivalConnectionIdentities.clear();
    terminalResult = null;
    lastErrorCode = null;
    closeUnusedTempDirectoryLocked();
  }

  private boolean clearIncomingLocked() {
    if (incoming == null) return true;
    incoming.close();
    if (!incoming.cleanupComplete()) return false;
    incoming = null;
    closeUnusedTempDirectoryLocked();
    return true;
  }

  private void closeUnusedTempDirectoryLocked() {
    if (tempDirectory == null) return;
    if (closed) {
      try {
        tempDirectory.close();
        if (incoming == null && artifact == null && !outgoing.hasPendingSnapshotCleanup()) {
          tempDirectory = null;
        }
      } catch (IOException ignored) {
        // Retain the handle; a later owned-file release can still close it safely.
      }
      return;
    }
    if (incoming != null || artifact != null || outgoing.hasPendingSnapshotCleanup()) return;
    try {
      tempDirectory.close();
      tempDirectory = null;
    } catch (IOException ignored) {
      // A retained lease keeps its secure handle alive and a later cleanup retries it.
    }
  }

  private TerminalResult terminalFailure(
      CheckedFileTransferControlResponseMessage message, String errorCode, long nowNanos) {
    return new TerminalResult(
        TerminalMetadata.from(message),
        CheckedFileTransferControlStatus.FAILED,
        errorCode,
        nowNanos,
        safeDeadline(nowNanos));
  }

  private static String stableFailureCode(Throwable failure, String fallback) {
    String message = failure == null ? null : failure.getMessage();
    if (message != null) {
      try {
        return ClientFileCheckValidation.errorCode(message);
      } catch (RuntimeException ignored) {
        // Never expose provider or exception text to a UI or protocol control.
      }
    }
    return fallback;
  }

  private long safeDeadline(long nowNanos) {
    return nowNanos > Long.MAX_VALUE - lifecycleTtlNanos
        ? Long.MAX_VALUE
        : nowNanos + lifecycleTtlNanos;
  }

  private static boolean sameSession(
      ClientFileCheckTaskCoordinator.Session left,
      ClientFileCheckTaskCoordinator.Session right) {
    return left != null
        && right != null
        && left.generation() == right.generation()
        && left.connectionIdentity() == right.connectionIdentity()
        && left.localPlayerId().equals(right.localPlayerId());
  }

  @Override
  public synchronized void close() {
    if (closed) return;
    closed = true;
    invalidateSessionLocked();
    outgoing.close();
    closeUnusedTempDirectoryLocked();
  }
}
