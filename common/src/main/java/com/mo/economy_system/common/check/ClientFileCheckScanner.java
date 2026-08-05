package com.mo.economy_system.common.check;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ClientFileCheckScanner {
  public interface Clock {
    long nanoTime();
  }

  public record Limits(
      int maxFiles, int maxSkipped, long maxSingleBytes, long maxTotalBytes, long maxNanos) {
    public Limits {
      if (maxFiles < 1 || maxSkipped < 0 || maxSingleBytes < 0 || maxTotalBytes < 0 || maxNanos < 1)
        throw new IllegalArgumentException("scan limits");
    }

    public static Limits defaults() {
      return new Limits(
          EconomyNetworkLimits.MAX_CHECK_FILES,
          EconomyNetworkLimits.MAX_CHECK_SKIPPED_FILES,
          EconomyNetworkLimits.MAX_CHECK_SINGLE_FILE_BYTES,
          EconomyNetworkLimits.MAX_CHECK_TOTAL_HASHED_BYTES,
          EconomyNetworkLimits.MAX_CHECK_SCAN_SECONDS * 1_000_000_000L);
    }
  }

  private final Limits limits;
  private final Clock clock;

  public ClientFileCheckScanner() {
    this(Limits.defaults(), System::nanoTime);
  }

  public ClientFileCheckScanner(Limits limits, Clock clock) {
    this.limits = limits;
    this.clock = clock;
  }

  public Path directory(Path gameDirectory, ClientFileCheckType type) {
    Path root = gameDirectory.toAbsolutePath().normalize();
    Path selected = root.resolve(type.id()).normalize();
    if (selected.equals(root) || !selected.getParent().equals(root))
      throw new IllegalStateException("unsafe scan directory");
    return selected;
  }

  public ClientFileCheckResult scan(Path gameDirectory, ClientFileCheckType type) {
    Path root = directory(gameDirectory, type);
    if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS))
      return new ClientFileCheckResult(
          1, ClientFileCheckStatus.SUCCESS, type, List.of(), List.of(), null);
    if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(root))
      return ClientFileCheckResult.failed(type, "DIRECTORY_UNREADABLE");

    List<Path> candidates = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
      for (Path path : stream) candidates.add(path);
    } catch (IOException | SecurityException failure) {
      return ClientFileCheckResult.failed(type, "DIRECTORY_UNREADABLE");
    }
    candidates.sort(Comparator.comparing(path -> path.getFileName().toString()));
    List<ClientFileCheckEntry> files = new ArrayList<>();
    List<ClientFileCheckSkippedEntry> skipped = new ArrayList<>();
    long total = 0;
    long deadline = saturatedAdd(clock.nanoTime(), limits.maxNanos());
    String truncation = null;
    for (Path path : candidates) {
      if (Thread.currentThread().isInterrupted()) {
        truncation = "SCAN_CANCELLED";
        break;
      }
      if (clock.nanoTime() > deadline) {
        truncation = "TIME_LIMIT";
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
        addSkipped(skipped, safeDisplayName(path), "INVALID_FILE_NAME");
        continue;
      }
      if (Files.isSymbolicLink(path)) {
        addSkipped(skipped, name, "SYMLINK");
        continue;
      }
      if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
        addSkipped(skipped, name, "NOT_REGULAR_FILE");
        continue;
      }
      try {
        long size = Files.size(path);
        if (size > limits.maxSingleBytes()) {
          addSkipped(skipped, name, "FILE_TOO_LARGE");
          continue;
        }
        if (size > limits.maxTotalBytes() - total) {
          truncation = "TOTAL_BYTE_LIMIT";
          break;
        }
        HashOutcome outcome = sha256(path, size, deadline);
        if (outcome.timedOut()) {
          truncation = "TIME_LIMIT";
          break;
        }
        if (outcome.changed()) {
          addSkipped(skipped, name, "FILE_CHANGED");
          continue;
        }
        files.add(new ClientFileCheckEntry(name, size, outcome.sha256()));
        total += size;
      } catch (IOException | SecurityException failure) {
        if (Thread.currentThread().isInterrupted()) {
          truncation = "SCAN_CANCELLED";
          break;
        }
        addSkipped(skipped, name, "READ_FAILED");
      }
    }
    ClientFileCheckStatus status =
        truncation == null ? ClientFileCheckStatus.SUCCESS : ClientFileCheckStatus.TRUNCATED;
    ClientFileCheckResult result =
        new ClientFileCheckResult(1, status, type, files, skipped, truncation);
    while (true) {
      try {
        ClientFileCheckResultJsonCodec.encode(result);
        return result;
      } catch (IllegalArgumentException tooLarge) {
        if (files.isEmpty()) return ClientFileCheckResult.failed(type, "RESULT_TOO_LARGE");
        files.remove(files.size() - 1);
        result =
            new ClientFileCheckResult(
                1, ClientFileCheckStatus.TRUNCATED, type, files, skipped, "JSON_LIMIT");
      }
    }
  }

  private void addSkipped(List<ClientFileCheckSkippedEntry> skipped, String name, String reason) {
    if (skipped.size() < limits.maxSkipped())
      skipped.add(new ClientFileCheckSkippedEntry(name, reason));
  }

  private static String safeDisplayName(Path path) {
    String name = path.getFileName() == null ? "INVALID" : path.getFileName().toString();
    if (name.isEmpty()
        || name.length() > EconomyNetworkLimits.MAX_CHECK_FILE_NAME_LENGTH
        || name.contains("/")
        || name.contains("\\")
        || name.contains("\0")
        || name.contains("..")
        || name.contains(":")) return "INVALID";
    return name;
  }

  private HashOutcome sha256(Path path, long expectedSize, long deadline) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] buffer = new byte[8192];
      try (InputStream input = Files.newInputStream(path)) {
        long remaining = expectedSize;
        while (remaining > 0) {
          if (Thread.currentThread().isInterrupted()) throw new IOException("scan cancelled");
          if (clock.nanoTime() > deadline) return new HashOutcome(null, true, false);
          int count = input.read(buffer, 0, (int) Math.min(buffer.length, remaining));
          if (count < 0) return new HashOutcome(null, false, true);
          if (count > 0) {
            digest.update(buffer, 0, count);
            remaining -= count;
          }
        }
        if (input.read() >= 0) return new HashOutcome(null, false, true);
      }
      return new HashOutcome(java.util.HexFormat.of().formatHex(digest.digest()), false, false);
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 unavailable", impossible);
    }
  }

  private record HashOutcome(String sha256, boolean timedOut, boolean changed) {}

  private static long saturatedAdd(long left, long right) {
    long value = left + right;
    return value < left ? Long.MAX_VALUE : value;
  }
}
