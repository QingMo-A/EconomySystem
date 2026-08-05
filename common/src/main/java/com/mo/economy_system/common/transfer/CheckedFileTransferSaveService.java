package com.mo.economy_system.common.transfer;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
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
  private static final String ROOT_DIRECTORY = "economy_system";
  private static final String RECEIVED_DIRECTORY = "received-check-files";

  public enum ResultCode {
    SAVED,
    NOT_PENDING,
    INVALID_FILE_NAME,
    SAVE_PARENT_UNSAFE,
    SOURCE_MISSING,
    SAVE_NAME_EXHAUSTED,
    MOVE_FAILED
  }

  public record Result(ResultCode code, Path savedPath) {
    public Result {
      Objects.requireNonNull(code, "code");
      if ((code == ResultCode.SAVED) != (savedPath != null)) {
        throw new IllegalArgumentException("save result");
      }
    }

    public boolean success() {
      return code == ResultCode.SAVED;
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

    Path targetDirectory;
    try {
      targetDirectory = prepareTargetDirectory(targetId);
    } catch (UnsafeParentException | IOException | SecurityException failure) {
      return new Result(ResultCode.SAVE_PARENT_UNSAFE, null);
    }

    if (!sourceIsRegular(artifact.path())) {
      return new Result(ResultCode.SOURCE_MISSING, null);
    }

    for (int attempt = 0; attempt < maxNameAttempts; attempt++) {
      String candidate = candidateName(fileName, attempt);
      if (candidate == null) {
        return new Result(ResultCode.SAVE_NAME_EXHAUSTED, null);
      }
      Path destination = targetDirectory.resolve(candidate).normalize();
      if (!destination.startsWith(targetDirectory)) {
        return new Result(ResultCode.SAVE_PARENT_UNSAFE, null);
      }
      // Recheck parent identity before every move. This catches a pre-existing or newly swapped
      // symlink in normal providers; the move itself remains no-replace and race-safe for names.
      try {
        verifyDirectoryTree(targetDirectory);
      } catch (UnsafeParentException | IOException | SecurityException failure) {
        return new Result(ResultCode.SAVE_PARENT_UNSAFE, null);
      }
      CheckedFileTransferReceivedArtifact.MoveResult moved = artifact.moveTo(destination);
      if (moved == CheckedFileTransferReceivedArtifact.MoveResult.MOVED) {
        return new Result(ResultCode.SAVED, destination);
      }
      if (moved == CheckedFileTransferReceivedArtifact.MoveResult.TARGET_EXISTS) {
        continue;
      }
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
    try {
      return prepareTargetDirectory(targetId);
    } catch (UnsafeParentException unsafe) {
      throw new IOException("unsafe destination");
    }
  }

  private Path prepareTargetDirectory(UUID targetId)
      throws IOException, UnsafeParentException {
    verifyDirectoryTree(gameDirectory);
    Path economy = gameDirectory.resolve(ROOT_DIRECTORY);
    Path received = economy.resolve(RECEIVED_DIRECTORY);
    Path target = received.resolve(targetId.toString());
    ensureDirectory(gameDirectory, economy);
    ensureDirectory(economy, received);
    ensureDirectory(received, target);
    verifyDirectoryTree(target);
    if (!target.startsWith(gameDirectory)) {
      throw new UnsafeParentException();
    }
    return target;
  }

  private static void ensureDirectory(Path parent, Path directory)
      throws IOException, UnsafeParentException {
    if (!directory.startsWith(parent)) {
      throw new UnsafeParentException();
    }
    try {
      BasicFileAttributes attributes =
          Files.readAttributes(directory, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      if (attributes.isSymbolicLink() || !attributes.isDirectory()) {
        throw new UnsafeParentException();
      }
      return;
    } catch (NoSuchFileException missing) {
      // CREATE one segment at a time. If another thread creates it first, inspect it again.
      try {
        Files.createDirectory(directory);
      } catch (FileAlreadyExistsException race) {
        BasicFileAttributes attributes =
            Files.readAttributes(directory, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (attributes.isSymbolicLink() || !attributes.isDirectory()) {
          throw new UnsafeParentException();
        }
      }
    }
  }

  private static void verifyDirectoryTree(Path directory)
      throws IOException, UnsafeParentException {
    Path absolute = directory.toAbsolutePath().normalize();
    Path root = absolute.getRoot();
    if (root == null) {
      throw new UnsafeParentException();
    }
    Path current = root;
    for (Path part : root.relativize(absolute)) {
      current = current.resolve(part);
      BasicFileAttributes attributes =
          Files.readAttributes(current, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      if (attributes.isSymbolicLink() || !attributes.isDirectory()) {
        throw new UnsafeParentException();
      }
    }
  }

  private static boolean sourceIsRegular(Path source) {
    try {
      BasicFileAttributes attributes =
          Files.readAttributes(source, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      return attributes.isRegularFile() && !attributes.isSymbolicLink();
    } catch (IOException | SecurityException failure) {
      return false;
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

  private static final class UnsafeParentException extends Exception {
    private UnsafeParentException() {}
  }
}
