package com.mo.economy_system.common.transfer;

import com.mo.economy_system.common.check.ClientFileCheckTaskCoordinator;
import com.mo.economy_system.common.check.ClientFileCheckType;
import com.mo.economy_system.common.network.CheckedFileTransferChunkResponseMessage;
import com.mo.economy_system.common.network.CheckedFileTransferControlResponseMessage;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

/**
 * One requester-side incoming stream.
 *
 * <p>The stream is bound to an exact session and metadata tuple. Any mismatch is terminal for
 * this stream only: its private part file is removed and its temporary reservation is released.
 * The caller may then continue handling unrelated transfers.
 */
public final class CheckedFileTransferIncoming implements AutoCloseable {
  public enum State {
    RECEIVING,
    COMPLETED,
    ABORTED,
    CLOSED
  }

  /** Exact identity captured from the READY response. */
  public record Metadata(
      String targetPlayerName,
      UUID targetPlayerId,
      String requesterPlayerName,
      UUID requesterPlayerId,
      ClientFileCheckType checkType,
      String fileName,
      UUID transferId,
      long byteLength,
      String sha256,
      int totalChunks) {
    public Metadata {
      targetPlayerName = com.mo.economy_system.common.check.ClientFileCheckValidation.playerName(targetPlayerName);
      targetPlayerId = Objects.requireNonNull(targetPlayerId, "target player id");
      requesterPlayerName = com.mo.economy_system.common.check.ClientFileCheckValidation.playerName(requesterPlayerName);
      requesterPlayerId = Objects.requireNonNull(requesterPlayerId, "requester player id");
      checkType = CheckedFileTransferValidation.type(checkType);
      fileName = CheckedFileTransferValidation.fileName(fileName);
      transferId = Objects.requireNonNull(transferId, "transfer id");
      if (byteLength < 0 || byteLength > EconomyNetworkLimits.MAX_TRANSFER_FILE_BYTES) {
        throw new IllegalArgumentException("byte length");
      }
      sha256 = CheckedFileTransferValidation.sha256(sha256);
      if (totalChunks
          != CheckedFileTransferValidation.totalChunks(
              byteLength, EconomyNetworkLimits.TRANSFER_RAW_CHUNK_BYTES)) {
        throw new IllegalArgumentException("total chunks");
      }
    }

    public boolean matches(CheckedFileTransferChunkResponseMessage message) {
      return message != null
          && targetPlayerName.equals(message.targetPlayerName())
          && targetPlayerId.equals(message.targetPlayerId())
          && requesterPlayerName.equals(message.requesterPlayerName())
          && requesterPlayerId.equals(message.requesterPlayerId())
          && checkType == message.checkType()
          && fileName.equals(message.fileName())
          && transferId.equals(message.transferId())
          && totalChunks == message.totalChunks();
    }

    public boolean matches(CheckedFileTransferControlResponseMessage message) {
      return message != null
          && targetPlayerName.equals(message.targetPlayerName())
          && targetPlayerId.equals(message.targetPlayerId())
          && requesterPlayerName.equals(message.requesterPlayerName())
          && requesterPlayerId.equals(message.requesterPlayerId())
          && checkType == message.checkType()
          && fileName.equals(message.fileName());
    }
  }

  private final UUID transferId;
  private final long expectedSize;
  private final String expectedHash;
  private final int total;
  private final CheckedFileTransferTempDirectory tempDirectory;
  private final boolean closeTempDirectory;
  private final CheckedFileTransferTempDirectory.OwnedFile part;
  private final CheckedFileTransferTempBudget.Reservation reservation;
  private final ClientFileCheckTaskCoordinator.Session session;
  private final Metadata metadata;
  private final long deadlineNanos;
  private final MessageDigest digest;
  private int next;
  private long received;
  private boolean ownershipTransferred;
  private State state = State.RECEIVING;

  /** Compatibility constructor retained for existing loader adapters. */
  public CheckedFileTransferIncoming(
      UUID transferId, long size, String hash, int totalChunks, Path directory) throws IOException {
    this(
        null,
        transferId,
        size,
        hash,
        totalChunks,
        CheckedFileTransferTempDirectory.openFixedRoot(directory),
        true,
        null,
        Long.MAX_VALUE,
        null);
  }

  /** Compatibility overload for callers that already track session identity separately. */
  public CheckedFileTransferIncoming(
      UUID transferId,
      long size,
      String hash,
      int totalChunks,
      Path directory,
      ClientFileCheckTaskCoordinator.Session session,
      CheckedFileTransferTempBudget budget,
      long deadlineNanos)
      throws IOException {
    this(
        null,
        transferId,
        size,
        hash,
        totalChunks,
        CheckedFileTransferTempDirectory.openFixedRoot(directory),
        true,
        Objects.requireNonNull(budget, "temp budget"),
        deadlineNanos,
        session);
  }

  /** Creates a session-bound stream and atomically reserves its part-file budget. */
  public CheckedFileTransferIncoming(
      Metadata metadata,
      ClientFileCheckTaskCoordinator.Session session,
      Path directory,
      CheckedFileTransferTempBudget budget,
      long deadlineNanos)
      throws IOException {
    this(
        metadata,
        metadata.transferId(),
        metadata.byteLength(),
        metadata.sha256(),
        metadata.totalChunks(),
        CheckedFileTransferTempDirectory.openFixedRoot(directory),
        true,
        Objects.requireNonNull(budget, "temp budget"),
        deadlineNanos,
        session);
  }

  /** Creates a session-bound stream in a caller-owned secure temp directory. */
  public CheckedFileTransferIncoming(
      Metadata metadata,
      ClientFileCheckTaskCoordinator.Session session,
      CheckedFileTransferTempDirectory tempDirectory,
      CheckedFileTransferTempBudget budget,
      long deadlineNanos)
      throws IOException {
    this(
        metadata,
        metadata.transferId(),
        metadata.byteLength(),
        metadata.sha256(),
        metadata.totalChunks(),
        Objects.requireNonNull(tempDirectory, "temp directory"),
        false,
        Objects.requireNonNull(budget, "temp budget"),
        deadlineNanos,
        session);
  }

  private CheckedFileTransferIncoming(
      Metadata metadata,
      UUID transferId,
      long size,
      String hash,
      int totalChunks,
      CheckedFileTransferTempDirectory tempDirectory,
      boolean closeTempDirectory,
      CheckedFileTransferTempBudget budget,
      long deadlineNanos,
      ClientFileCheckTaskCoordinator.Session session)
      throws IOException {
    this.transferId = Objects.requireNonNull(transferId, "transfer id");
    if (size < 0 || size > EconomyNetworkLimits.MAX_TRANSFER_FILE_BYTES) {
      throw new IllegalArgumentException("size");
    }
    this.expectedSize = size;
    this.expectedHash = CheckedFileTransferValidation.sha256(hash);
    int expectedTotal =
        CheckedFileTransferValidation.totalChunks(
            size, EconomyNetworkLimits.TRANSFER_RAW_CHUNK_BYTES);
    if (totalChunks != expectedTotal) throw new IllegalArgumentException("chunks");
    this.total = totalChunks;
    this.metadata = metadata;
    this.session = session;
    this.tempDirectory = Objects.requireNonNull(tempDirectory, "temp directory");
    this.closeTempDirectory = closeTempDirectory;
    if (deadlineNanos <= 0) throw new IllegalArgumentException("deadline");
    this.deadlineNanos = deadlineNanos;
    CheckedFileTransferTempBudget.Reservation held = null;
    if (budget != null) {
      CheckedFileTransferTempBudget.ReservationResult result = budget.tryReserve(size);
      if (!result.success()) throw new IOException("TEMP_STORAGE_LIMIT");
      held = result.reservation();
    }
    MessageDigest createdDigest;
    try {
      createdDigest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      if (held != null) held.release();
      closeOwnedDirectoryQuietly();
      throw new IllegalStateException(impossible);
    }
    CheckedFileTransferTempDirectory.OwnedFile createdPart;
    try {
      createdPart = tempDirectory.createPart();
    } catch (IOException | RuntimeException failure) {
      if (held != null) held.release();
      closeOwnedDirectoryQuietly();
      throw failure;
    }
    this.reservation = held;
    this.part = createdPart;
    this.digest = createdDigest;
  }

  public synchronized UUID transferId() {
    return transferId;
  }

  public synchronized Metadata metadata() {
    return metadata;
  }

  public synchronized ClientFileCheckTaskCoordinator.Session session() {
    return session;
  }

  public synchronized long deadlineNanos() {
    return deadlineNanos;
  }

  public synchronized State state() {
    return state;
  }

  public synchronized int nextChunkIndex() {
    return next;
  }

  public synchronized long receivedBytes() {
    return received;
  }

  public synchronized boolean isComplete() {
    return state == State.COMPLETED;
  }

  public synchronized CheckedFileTransferTempBudget.Reservation reservation() {
    return reservation;
  }

  public synchronized Path path() {
    return part.path();
  }

  public synchronized Path relativeName() {
    return part.relativeName();
  }

  public synchronized boolean expired(long nowNanos) {
    return state == State.RECEIVING && nowNanos >= deadlineNanos;
  }

  public synchronized boolean expire(long nowNanos) {
    if (!expired(nowNanos)) return false;
    abortLocked();
    return true;
  }

  /** Indicates whether the part and its reservation have been cleaned up exactly. */
  public synchronized boolean cleanupComplete() {
    if (ownershipTransferred || !part.isOwned()) return true;
    try {
      part.delete();
      if (reservation != null) reservation.release();
      closeOwnedDirectoryQuietly();
      return true;
    } catch (IOException ignored) {
      return false;
    }
  }

  /** Legacy field-level append API. */
  public synchronized void chunk(UUID transfer, int index, int declaredTotal, String encoded)
      throws IOException {
    ensureReceiving();
    append(transfer, index, declaredTotal, encoded);
  }

  /** Exact metadata/session append API used by the common coordinator. */
  public synchronized void chunk(
      CheckedFileTransferChunkResponseMessage message,
      ClientFileCheckTaskCoordinator.Session currentSession)
      throws IOException {
    ensureReceiving();
    if (session != null && !sameSession(session, currentSession)) {
      fail("INVALID_SESSION");
    }
    if (metadata == null || !metadata.matches(message)) fail("INVALID_METADATA");
    append(message.transferId(), message.chunkIndex(), message.totalChunks(), message.chunkData());
  }

  private void append(UUID transfer, int index, int declaredTotal, String encoded)
      throws IOException {
    if (!transferId.equals(transfer) || index != next || declaredTotal != total) {
      fail("INVALID_CHUNK");
    }
    byte[] raw;
    try {
      raw = CheckedFileTransferRoutingService.decodeCanonical(encoded);
    } catch (RuntimeException malformed) {
      fail("INVALID_CHUNK");
      return;
    }
    long remaining = expectedSize - received;
    if (remaining < 0 || raw.length != (int) Math.min(EconomyNetworkLimits.TRANSFER_RAW_CHUNK_BYTES, remaining)) {
      fail("INVALID_CHUNK");
    }
    ByteBuffer buffer = ByteBuffer.wrap(raw);
    while (buffer.hasRemaining()) {
      int written = part.writeChannel().write(buffer);
      if (written <= 0) fail("WRITE_FAILED");
    }
    digest.update(raw);
    received += raw.length;
    next++;
  }

  /** Compatibility completion API. The path is retained for the caller's result screen. */
  public synchronized Path complete(CheckedFileTransferControl control) throws IOException {
    completeInternal(control);
    state = State.COMPLETED;
    part.closeWriteChannel();
    return part.path();
  }

  /** Completes and retains the reservation inside a managed result artifact. */
  public synchronized CheckedFileTransferReceivedArtifact completeArtifact(
      CheckedFileTransferControlResponseMessage message,
      CheckedFileTransferControl control)
      throws IOException {
    if (metadata == null || !metadata.matches(message)) fail("INVALID_METADATA");
    completeInternal(control);
    part.closeWriteChannel();
    CheckedFileTransferTempBudget.Reservation artifactReservation = reservation;
    if (artifactReservation == null) {
      CheckedFileTransferTempBudget budget = new CheckedFileTransferTempBudget(1, expectedSize);
      artifactReservation = budget.reserve(expectedSize);
    }
    CheckedFileTransferReceivedArtifact artifact =
        new CheckedFileTransferReceivedArtifact(
            part,
            artifactReservation,
            new CheckedFileTransferReceivedArtifact.Metadata(
                metadata.targetPlayerName(),
                metadata.targetPlayerId(),
                metadata.requesterPlayerName(),
                metadata.requesterPlayerId(),
                metadata.checkType(),
                metadata.fileName(),
                metadata.transferId(),
                metadata.byteLength(),
                metadata.sha256(),
                metadata.totalChunks()));
    ownershipTransferred = true;
    state = State.COMPLETED;
    closeOwnedDirectoryQuietly();
    return artifact;
  }

  /** Exact session-aware completion helper. */
  public synchronized CheckedFileTransferReceivedArtifact completeArtifact(
      CheckedFileTransferControlResponseMessage message,
      CheckedFileTransferControl control,
      ClientFileCheckTaskCoordinator.Session currentSession)
      throws IOException {
    if (session != null && !sameSession(session, currentSession)) fail("INVALID_SESSION");
    return completeArtifact(message, control);
  }

  private void completeInternal(CheckedFileTransferControl control) throws IOException {
    if (state != State.RECEIVING) throw new IOException("INVALID_STATE");
    if (control == null
        || control.status() != CheckedFileTransferControlStatus.COMPLETE
        || !transferId.equals(control.transferId())
        || control.byteLength() != expectedSize
        || !expectedHash.equals(control.sha256())
        || next != total
        || received != expectedSize) {
      fail("SIZE_MISMATCH");
    }
    String actual = HexFormat.of().formatHex(digest.digest());
    if (!actual.equals(expectedHash)) fail("HASH_MISMATCH");
  }

  private void ensureReceiving() throws IOException {
    if (state != State.RECEIVING) throw new IOException("INVALID_STATE");
  }

  private void fail(String code) throws IOException {
    abortLocked();
    throw new IOException(code);
  }

  private void abortLocked() {
    try {
      if (!ownershipTransferred && part.delete() && reservation != null) reservation.release();
    } catch (IOException ignored) {
      // Keep both the relative-file ownership and reservation so cleanup can be retried.
    }
    state = State.ABORTED;
    closeOwnedDirectoryQuietly();
  }

  private void closeOwnedDirectoryQuietly() {
    if (!closeTempDirectory) return;
    try {
      tempDirectory.close();
    } catch (IOException ignored) {
      // An owned file lease keeps the handle alive until exact cleanup completes.
    }
  }

  private static boolean sameSession(
      ClientFileCheckTaskCoordinator.Session left,
      ClientFileCheckTaskCoordinator.Session right) {
    return right != null
        && left.generation() == right.generation()
        && left.connectionIdentity() == right.connectionIdentity()
        && left.localPlayerId().equals(right.localPlayerId());
  }

  @Override
  public synchronized void close() {
    if (!ownershipTransferred && part.isOwned()) {
      abortLocked();
    } else if (state == State.COMPLETED) {
      state = State.CLOSED;
    }
    closeOwnedDirectoryQuietly();
  }
}
