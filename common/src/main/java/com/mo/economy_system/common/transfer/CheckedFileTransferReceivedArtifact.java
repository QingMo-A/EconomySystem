package com.mo.economy_system.common.transfer;

import com.mo.economy_system.common.check.ClientFileCheckType;
import com.mo.economy_system.common.check.ClientFileCheckValidation;
import com.mo.economy_system.common.network.CheckedFileTransferControlResponseMessage;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.UUID;

/**
 * Runtime-owned file produced by an incoming checked-file transfer.
 *
 * <p>An incoming transfer must not hand a raw path to a screen. The artifact retains the exact
 * request metadata and its temporary-storage reservation until the caller explicitly saves or
 * discards it. All state transitions are one-shot and synchronized.
 */
public final class CheckedFileTransferReceivedArtifact implements AutoCloseable {
  public enum State {
    PENDING_DECISION,
    SAVED,
    DISCARDED
  }

  public enum MoveResult {
    MOVED,
    NOT_PENDING,
    TARGET_EXISTS,
    MOVE_FAILED
  }

  public enum DiscardResult {
    DISCARDED,
    NOT_PENDING,
    DELETE_FAILED
  }

  /** Immutable metadata copied from the authenticated transfer response. */
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
      targetPlayerName = ClientFileCheckValidation.playerName(targetPlayerName);
      targetPlayerId = Objects.requireNonNull(targetPlayerId, "target player id");
      requesterPlayerName = ClientFileCheckValidation.playerName(requesterPlayerName);
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

    public static Metadata from(
        CheckedFileTransferControlResponseMessage message,
        CheckedFileTransferControl control) {
      Objects.requireNonNull(message, "message");
      Objects.requireNonNull(control, "control");
      if (control.status() != CheckedFileTransferControlStatus.COMPLETE) {
        throw new IllegalArgumentException("completed control required");
      }
      return new Metadata(
          message.targetPlayerName(),
          message.targetPlayerId(),
          message.requesterPlayerName(),
          message.requesterPlayerId(),
          message.checkType(),
          message.fileName(),
          control.transferId(),
          control.byteLength(),
          control.sha256(),
          CheckedFileTransferValidation.totalChunks(
              control.byteLength(), EconomyNetworkLimits.TRANSFER_RAW_CHUNK_BYTES));
    }
  }

  private final CheckedFileTransferTempDirectory.OwnedFile temporaryFile;
  private final CheckedFileTransferTempBudget.Reservation reservation;
  private final Metadata metadata;
  private Path savedPath;
  private State state = State.PENDING_DECISION;

  public CheckedFileTransferReceivedArtifact(
      CheckedFileTransferTempDirectory.OwnedFile temporaryFile,
      CheckedFileTransferTempBudget.Reservation reservation,
      Metadata metadata) {
    this.temporaryFile = Objects.requireNonNull(temporaryFile, "temporary file");
    this.reservation = Objects.requireNonNull(reservation, "reservation");
    this.metadata = Objects.requireNonNull(metadata, "metadata");
    if (reservation.bytes() != metadata.byteLength()) {
      try {
        if (temporaryFile.delete()) reservation.release();
      } catch (IOException ignored) {
        // Keep the reservation held when exact relative cleanup could not complete.
      }
      throw new IllegalArgumentException("reservation size");
    }
  }

  public CheckedFileTransferReceivedArtifact(
      CheckedFileTransferTempDirectory.OwnedFile temporaryFile,
      CheckedFileTransferTempBudget.Reservation reservation,
      CheckedFileTransferControlResponseMessage message,
      CheckedFileTransferControl control) {
    this(temporaryFile, reservation, Metadata.from(message, control));
  }

  /** Compatibility-only display path; ownership remains relative to the secure source handle. */
  public synchronized Path path() {
    return state == State.SAVED ? savedPath : temporaryFile.path();
  }

  /** Alias used by callers that want to emphasize that the path is currently managed. */
  public synchronized Path currentPath() {
    return path();
  }

  public Metadata metadata() {
    return metadata;
  }

  public synchronized State state() {
    return state;
  }

  public synchronized boolean isPendingDecision() {
    return state == State.PENDING_DECISION;
  }

  public synchronized boolean isTerminal() {
    return state != State.PENDING_DECISION;
  }

  /**
   * Moves this artifact to an already validated destination without replacement.
   *
   * <p>This method is package-private so only {@link CheckedFileTransferSaveService} can perform
   * the move. The synchronized block covers the move and state transition, preventing a concurrent
   * discard or second save from releasing the reservation twice.
   */
  synchronized MoveResult moveTo(
      CheckedFileTransferTempDirectory.DirectoryHandle target,
      Path destinationName,
      Path destinationDisplayPath)
      throws CheckedFileTransferTempDirectory.ProviderUnsafeException {
    if (state != State.PENDING_DECISION) {
      return MoveResult.NOT_PENDING;
    }
    try {
      temporaryFile.moveTo(target, destinationName);
    } catch (FileAlreadyExistsException alreadyExists) {
      return MoveResult.TARGET_EXISTS;
    } catch (CheckedFileTransferTempDirectory.ProviderUnsafeException unsafe) {
      throw unsafe;
    } catch (IOException | SecurityException failure) {
      return MoveResult.MOVE_FAILED;
    }
    savedPath = Objects.requireNonNull(destinationDisplayPath, "destination display path")
        .toAbsolutePath()
        .normalize();
    state = State.SAVED;
    reservation.release();
    return MoveResult.MOVED;
  }

  synchronized BasicFileAttributes sourceAttributesNoFollow() throws IOException {
    if (state != State.PENDING_DECISION) throw new IOException("NOT_PENDING");
    return temporaryFile.attributesNoFollow();
  }

  synchronized CheckedFileTransferTempDirectory sourceDirectory() {
    return temporaryFile.directory();
  }

  /** Deletes the temporary artifact and releases its reservation. */
  public synchronized boolean discard() {
    return discardResult() == DiscardResult.DISCARDED;
  }

  public synchronized DiscardResult discardResult() {
    if (state != State.PENDING_DECISION) {
      return DiscardResult.NOT_PENDING;
    }
    try {
      temporaryFile.delete();
    } catch (IOException | SecurityException failure) {
      return DiscardResult.DELETE_FAILED;
    }
    state = State.DISCARDED;
    reservation.release();
    return DiscardResult.DISCARDED;
  }

  /** Expiry uses the same exact cleanup transition as an explicit discard. */
  public synchronized boolean expire() {
    return discard();
  }

  @Override
  public void close() {
    discard();
  }
}
