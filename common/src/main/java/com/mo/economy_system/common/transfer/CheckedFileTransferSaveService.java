package com.mo.economy_system.common.transfer;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

/**
 * Saves a completed checked-file artifact below the fixed managed receive directory.
 *
 * <p>The service owns path construction and collision handling so a screen never needs to join
 * user-controlled strings or call {@code Files.move} directly. Every move omits replacement and
 * atomic-move options; a collision is handled by trying the next bounded candidate.
 */
public final class CheckedFileTransferSaveService {
  public static final int DEFAULT_MAX_NAME_ATTEMPTS = 100;
  private static final String RECEIVED_DIRECTORY = "received-check-files";

  public enum ResultCode {
    SAVED,
    SAVED_CLEANUP_PENDING,
    NOT_PENDING,
    INVALID_FILE_NAME,
    SAVE_PARENT_UNSAFE,
    SOURCE_MISSING,
    SOURCE_CHANGED,
    SAVE_NAME_EXHAUSTED,
    MOVE_FAILED
  }

  public record Result(ResultCode code, Path savedPath) {
    public Result {
      Objects.requireNonNull(code, "code");
      if ((code == ResultCode.SAVED || code == ResultCode.SAVED_CLEANUP_PENDING)
          != (savedPath != null)) {
        throw new IllegalArgumentException("save result");
      }
    }

    public boolean success() {
      return code == ResultCode.SAVED || code == ResultCode.SAVED_CLEANUP_PENDING;
    }
  }

  private final Path gameDirectory;
  private final int maxNameAttempts;

  public CheckedFileTransferSaveService(Path gameDirectory) {
    this(gameDirectory, DEFAULT_MAX_NAME_ATTEMPTS);
  }

  public CheckedFileTransferSaveService(Path gameDirectory, int maxNameAttempts) {
    Objects.requireNonNull(gameDirectory, "game directory");
    if (maxNameAttempts < 1) {
      throw new IllegalArgumentException("max name attempts");
    }
    this.gameDirectory = gameDirectory.toAbsolutePath().normalize();
    this.maxNameAttempts = maxNameAttempts;
  }

  public Path gameDirectory() {
    return gameDirectory;
  }

  public int maxNameAttempts() {
    return maxNameAttempts;
  }

  /**
   * Saves an artifact under {@code economy_system/received-check-files/<target-uuid>}. A failed
   * save leaves the artifact pending and its reservation held, allowing a later retry or discard.
   */
  public Result save(CheckedFileTransferReceivedArtifact artifact) {
    Objects.requireNonNull(artifact, "artifact");
    if (!artifact.isPendingDecision()) {
      return new Result(ResultCode.NOT_PENDING, null);
    }

    String fileName;
    UUID targetId = artifact.metadata().targetPlayerId();
    try {
      fileName = CheckedFileTransferValidation.fileName(artifact.metadata().fileName());
    } catch (RuntimeException invalidName) {
      return new Result(ResultCode.INVALID_FILE_NAME, null);
    }

    CheckedFileTransferTempDirectory sourceDirectory = artifact.sourceDirectory();
    if (!sourceDirectory.gameDirectory().equals(gameDirectory)) {
      return new Result(ResultCode.SAVE_PARENT_UNSAFE, null);
    }

    CheckedFileTransferTempDirectory.DirectoryHandle targetDirectory;
    try {
      targetDirectory = sourceDirectory.openTargetDirectory(targetId);
    } catch (CheckedFileTransferTempDirectory.ProviderUnsafeException unsafe) {
      return new Result(ResultCode.SAVE_PARENT_UNSAFE, null);
    } catch (IOException | SecurityException failure) {
      return new Result(ResultCode.SAVE_PARENT_UNSAFE, null);
    }
    Result outcome;
    try {
      outcome = saveWithTarget(artifact, targetDirectory, targetId, fileName);
    } catch (CheckedFileTransferTempDirectory.ProviderUnsafeException unsafe) {
      outcome = new Result(ResultCode.SAVE_PARENT_UNSAFE, null);
    } catch (IOException | SecurityException failure) {
      outcome = new Result(ResultCode.SAVE_PARENT_UNSAFE, null);
    }
    try {
      targetDirectory.close();
    } catch (IOException closeFailure) {
      // A completed move remains a successful transaction; failed transactions stay pending.
      if (!outcome.success()) outcome = new Result(ResultCode.SAVE_PARENT_UNSAFE, null);
    }
    return outcome;
  }

  private Result saveWithTarget(
      CheckedFileTransferReceivedArtifact artifact,
      CheckedFileTransferTempDirectory.DirectoryHandle targetDirectory,
      UUID targetId,
      String fileName)
      throws IOException {
    targetDirectory.validate();
    Path expectedTarget =
        gameDirectory
            .resolve(CheckedFileTransferTempDirectory.ROOT_DIRECTORY)
            .resolve(RECEIVED_DIRECTORY)
            .resolve(targetId.toString())
            .normalize();
    if (!targetDirectory.absolutePath().toAbsolutePath().normalize().equals(expectedTarget)) {
      return new Result(ResultCode.SAVE_PARENT_UNSAFE, null);
    }
    for (int attempt = 0; attempt < maxNameAttempts; attempt++) {
      String candidate = candidateName(fileName, attempt);
      if (candidate == null) return new Result(ResultCode.SAVE_NAME_EXHAUSTED, null);
      Path relativeName = Path.of(candidate);
      CheckedFileTransferReceivedArtifact.MoveResult moved =
          artifact.copyVerifiedTo(
              targetDirectory,
              relativeName,
              targetDirectory.absolutePath().resolve(relativeName));
      if (moved == CheckedFileTransferReceivedArtifact.MoveResult.MOVED) {
        return new Result(ResultCode.SAVED, targetDirectory.absolutePath().resolve(relativeName));
      }
      if (moved == CheckedFileTransferReceivedArtifact.MoveResult.CLEANUP_PENDING) {
        return new Result(ResultCode.SAVED_CLEANUP_PENDING,
            targetDirectory.absolutePath().resolve(relativeName));
      }
      if (moved == CheckedFileTransferReceivedArtifact.MoveResult.SOURCE_CHANGED) {
        return new Result(ResultCode.SOURCE_CHANGED, null);
      }
      if (moved == CheckedFileTransferReceivedArtifact.MoveResult.TARGET_EXISTS) continue;
      if (moved == CheckedFileTransferReceivedArtifact.MoveResult.NOT_PENDING) {
        return new Result(ResultCode.NOT_PENDING, null);
      }
      return new Result(ResultCode.MOVE_FAILED, null);
    }
    return new Result(ResultCode.SAVE_NAME_EXHAUSTED, null);
  }

  /** Returns the fixed destination directory after validating its complete parent chain. */
  public Path targetDirectory(UUID targetId) throws IOException {
    Objects.requireNonNull(targetId, "target id");
    try (CheckedFileTransferTempDirectory temporaryDirectory =
            CheckedFileTransferTempDirectory.open(gameDirectory);
        CheckedFileTransferTempDirectory.DirectoryHandle target =
            temporaryDirectory.openTargetDirectory(targetId)) {
      target.validate();
      return target.absolutePath();
    }
  }

  private static String candidateName(String original, int attempt) {
    if (attempt == 0) {
      return original.length() <= EconomyNetworkLimits.MAX_TRANSFER_FILE_NAME_CHARS
          ? original
          : null;
    }
    String suffix = "-" + attempt;
    int dot = original.lastIndexOf('.');
    String candidate;
    if (dot > 0 && dot < original.length() - 1) {
      candidate = original.substring(0, dot) + suffix + original.substring(dot);
    } else {
      candidate = original + suffix;
    }
    return candidate.length() <= EconomyNetworkLimits.MAX_TRANSFER_FILE_NAME_CHARS
        ? candidate
        : null;
  }
}
