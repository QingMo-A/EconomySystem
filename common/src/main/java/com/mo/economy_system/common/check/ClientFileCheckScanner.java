package com.mo.economy_system.common.check;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class ClientFileCheckScanner {
  public interface Clock {
    long nanoTime();
  }

  interface FileAccess {
    boolean exists(Path path);

    boolean directoryNoFollow(Path path);

    DirectoryStream<Path> list(Path path) throws IOException;

    SeekableByteChannel openNoFollow(Path path) throws IOException;

    boolean symbolicLink(Path path);

    boolean directoryEntry(Path path);
  }

  public record Limits(
      int maxCandidates,
      int maxFiles,
      int maxSkipped,
      long maxSingleBytes,
      long maxTotalBytes,
      long maxNanos) {
    public Limits {
      if (maxCandidates < 1
          || maxFiles < 1
          || maxSkipped < 0
          || maxSingleBytes < 0
          || maxTotalBytes < 0
          || maxNanos < 1) throw new IllegalArgumentException("scan limits");
    }

    public static Limits defaults() {
      return new Limits(
          EconomyNetworkLimits.MAX_CHECK_DIRECTORY_ENTRIES,
          EconomyNetworkLimits.MAX_CHECK_FILES,
          EconomyNetworkLimits.MAX_CHECK_SKIPPED_FILES,
          EconomyNetworkLimits.MAX_CHECK_SINGLE_FILE_BYTES,
          EconomyNetworkLimits.MAX_CHECK_TOTAL_HASHED_BYTES,
          EconomyNetworkLimits.MAX_CHECK_SCAN_SECONDS * 1_000_000_000L);
    }
  }

  private static final FileAccess SYSTEM =
      new FileAccess() {
        public boolean exists(Path path) {
          return Files.exists(path, LinkOption.NOFOLLOW_LINKS);
        }

        public boolean directoryNoFollow(Path path) {
          return Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS) && !Files.isSymbolicLink(path);
        }

        public DirectoryStream<Path> list(Path path) throws IOException {
          return Files.newDirectoryStream(path);
        }

        public SeekableByteChannel openNoFollow(Path path) throws IOException {
          Set<OpenOption> options = Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
          return Files.newByteChannel(path, options);
        }

        public boolean symbolicLink(Path path) {
          return Files.isSymbolicLink(path);
        }

        public boolean directoryEntry(Path path) {
          return Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS);
        }
      };

  private final Limits limits;
  private final Clock clock;
  private final FileAccess access;

  public ClientFileCheckScanner() {
    this(Limits.defaults(), System::nanoTime, SYSTEM);
  }

  public ClientFileCheckScanner(Limits limits, Clock clock) {
    this(limits, clock, SYSTEM);
  }

  ClientFileCheckScanner(Limits limits, Clock clock, FileAccess access) {
    this.limits = limits;
    this.clock = clock;
    this.access = access;
  }

  public Path directory(Path gameDirectory, ClientFileCheckType type) {
    Path root = gameDirectory.toAbsolutePath().normalize();
    Path selected = root.resolve(type.id()).normalize();
    if (selected.equals(root) || !selected.getParent().equals(root))
      throw new IllegalStateException("unsafe scan directory");
    return selected;
  }

  public ClientFileCheckResult scan(Path gameDirectory, ClientFileCheckType type) {
    long deadline = saturatedAdd(clock.nanoTime(), limits.maxNanos());
    String stopped = stopReason(deadline);
    if (stopped != null) return truncated(type, List.of(), List.of(), stopped);
    Path root = directory(gameDirectory, type);
    stopped = stopReason(deadline);
    if (stopped != null) return truncated(type, List.of(), List.of(), stopped);
    if (!access.exists(root)) return success(type, List.of(), List.of());
    if (!access.directoryNoFollow(root))
      return ClientFileCheckResult.failed(type, "DIRECTORY_UNREADABLE");

    List<Path> candidates = new ArrayList<>(Math.min(limits.maxCandidates(), 256));
    String truncation = null;
    try (DirectoryStream<Path> stream = access.list(root)) {
      for (Path path : stream) {
        stopped = stopReason(deadline);
        if (stopped != null) {
          truncation = stopped;
          break;
        }
        if (candidates.size() >= limits.maxCandidates()) {
          truncation = "DIRECTORY_ENTRY_LIMIT";
          break;
        }
        candidates.add(path);
      }
    } catch (IOException | SecurityException failure) {
      return ClientFileCheckResult.failed(type, "DIRECTORY_UNREADABLE");
    }
    stopped = stopReason(deadline);
    if (stopped != null) truncation = stopped;
    if (truncation == null) {
      try {
        candidates.sort(
            Comparator.comparing(
                path -> {
                  String reason = stopReason(deadline);
                  if (reason != null) throw new ScanStopped(reason);
                  return path.getFileName().toString();
                }));
      } catch (ScanStopped stoppedSort) {
        truncation = stoppedSort.reason;
      }
    }

    List<ClientFileCheckEntry> files = new ArrayList<>();
    List<ClientFileCheckSkippedEntry> skipped = new ArrayList<>();
    long total = 0;
    if (truncation == null) {
      for (Path path : candidates) {
        stopped = stopReason(deadline);
        if (stopped != null) {
          truncation = stopped;
          break;
        }
        if (files.size() >= limits.maxFiles()) {
          truncation = "FILE_LIMIT";
          break;
        }
        String name;
        try {
          name = ClientFileCheckValidation.fileName(path.getFileName().toString());
        } catch (RuntimeException invalid) {
          if (!addSkipped(skipped, safeDisplayName(path), "INVALID_FILE_NAME")) {
            truncation = "SKIPPED_LIMIT";
            break;
          }
          continue;
        }
        stopped = stopReason(deadline);
        if (stopped != null) {
          truncation = stopped;
          break;
        }
        try (SeekableByteChannel channel = access.openNoFollow(path)) {
          stopped = stopReason(deadline);
          if (stopped != null) {
            truncation = stopped;
            break;
          }
          long size = channel.size();
          if (size < 0) throw new IOException("negative size");
          if (size > limits.maxSingleBytes()) {
            if (!addSkipped(skipped, name, "FILE_TOO_LARGE")) {
              truncation = "SKIPPED_LIMIT";
              break;
            }
            continue;
          }
          if (size > limits.maxTotalBytes() - total) {
            truncation = "TOTAL_BYTE_LIMIT";
            break;
          }
          HashOutcome outcome = sha256(channel, size, deadline);
          if (outcome.stopReason() != null) {
            truncation = outcome.stopReason();
            break;
          }
          if (outcome.changed()) {
            if (!addSkipped(skipped, name, "FILE_CHANGED")) {
              truncation = "SKIPPED_LIMIT";
              break;
            }
            continue;
          }
          files.add(new ClientFileCheckEntry(name, size, outcome.sha256()));
          total += size;
        } catch (IOException | SecurityException failure) {
          stopped = stopReason(deadline);
          if (stopped != null) {
            truncation = stopped;
            break;
          }
          String reason =
              access.symbolicLink(path)
                  ? "SYMLINK"
                  : access.directoryEntry(path) ? "NOT_REGULAR_FILE" : "READ_FAILED";
          if (!addSkipped(skipped, name, reason)) {
            truncation = "SKIPPED_LIMIT";
            break;
          }
        }
      }
    }
    ClientFileCheckResult result =
        truncation == null
            ? success(type, files, skipped)
            : truncated(type, files, skipped, truncation);
    while (true) {
      stopped = stopReason(deadline);
      if (stopped != null) return truncated(type, files, skipped, stopped);
      try {
        ClientFileCheckResultJsonCodec.encode(result);
        return result;
      } catch (IllegalArgumentException tooLarge) {
        if (files.isEmpty()) return ClientFileCheckResult.failed(type, "RESULT_TOO_LARGE");
        files.remove(files.size() - 1);
        result = truncated(type, files, skipped, "JSON_LIMIT");
      }
    }
  }

  private HashOutcome sha256(SeekableByteChannel channel, long expectedSize, long deadline)
      throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      ByteBuffer buffer = ByteBuffer.allocate(8192);
      long readTotal = 0;
      while (readTotal < expectedSize) {
        String stopped = stopReason(deadline);
        if (stopped != null) return new HashOutcome(null, false, stopped);
        buffer.clear();
        buffer.limit((int) Math.min(buffer.capacity(), expectedSize - readTotal));
        int count = channel.read(buffer);
        if (count < 0) return new HashOutcome(null, true, null);
        if (count == 0) continue;
        digest.update(buffer.array(), 0, count);
        readTotal += count;
      }
      buffer.clear();
      buffer.limit(1);
      boolean extra = channel.read(buffer) >= 0;
      boolean changed = extra || readTotal != expectedSize || channel.size() != expectedSize;
      return new HashOutcome(
          changed ? null : java.util.HexFormat.of().formatHex(digest.digest()), changed, null);
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 unavailable", impossible);
    }
  }

  private String stopReason(long deadline) {
    if (Thread.currentThread().isInterrupted()) return "SCAN_CANCELLED";
    return clock.nanoTime() > deadline ? "TIME_LIMIT" : null;
  }

  private boolean addSkipped(
      List<ClientFileCheckSkippedEntry> skipped, String name, String reason) {
    if (skipped.size() >= limits.maxSkipped()) return false;
    skipped.add(new ClientFileCheckSkippedEntry(name, reason));
    return true;
  }

  private static ClientFileCheckResult success(
      ClientFileCheckType type,
      List<ClientFileCheckEntry> files,
      List<ClientFileCheckSkippedEntry> skipped) {
    return new ClientFileCheckResult(1, ClientFileCheckStatus.SUCCESS, type, files, skipped, null);
  }

  private static ClientFileCheckResult truncated(
      ClientFileCheckType type,
      List<ClientFileCheckEntry> files,
      List<ClientFileCheckSkippedEntry> skipped,
      String reason) {
    return new ClientFileCheckResult(
        1, ClientFileCheckStatus.TRUNCATED, type, files, skipped, reason);
  }

  private static String safeDisplayName(Path path) {
    String name = path.getFileName() == null ? "INVALID" : path.getFileName().toString();
    try {
      return ClientFileCheckValidation.fileName(name);
    } catch (RuntimeException invalid) {
      return "INVALID";
    }
  }

  private static long saturatedAdd(long left, long right) {
    return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
  }

  private record HashOutcome(String sha256, boolean changed, String stopReason) {}

  private static final class ScanStopped extends RuntimeException {
    private final String reason;

    private ScanStopped(String reason) {
      this.reason = reason;
    }
  }
}
