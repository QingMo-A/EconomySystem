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
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ClientFileCheckScanner {
  public interface Clock {
    long nanoTime();
  }

  interface FileAccess {
    boolean existsNoFollow(Path root);

    ScanDirectory open(Path gameDirectory, Path root) throws IOException;
  }

  interface ScanDirectory extends AutoCloseable {
    Iterator<Path> entries();

    SeekableByteChannel openNoFollow(Path name) throws IOException;

    BasicFileAttributes attributesNoFollow(Path name) throws IOException;

    void validate() throws IOException;

    @Override
    void close() throws IOException;
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

  private static final Set<OpenOption> READ_NOFOLLOW =
      Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
  private static final LinkOption[] NOFOLLOW = {LinkOption.NOFOLLOW_LINKS};
  private static final FileAccess SYSTEM = new SystemFileAccess();

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
    this.limits = Objects.requireNonNull(limits);
    this.clock = Objects.requireNonNull(clock);
    this.access = Objects.requireNonNull(access);
  }

  public Path directory(Path gameDirectory, ClientFileCheckType type) {
    Path game = gameDirectory.toAbsolutePath().normalize();
    Path selected = game.resolve(type.id()).normalize();
    if (selected.equals(game) || !selected.getParent().equals(game))
      throw new IllegalStateException("unsafe scan directory");
    return selected;
  }

  public ClientFileCheckResult scan(Path gameDirectory, ClientFileCheckType type) {
    long deadline = saturatedAdd(clock.nanoTime(), limits.maxNanos());
    String stopped = stopReason(deadline);
    if (stopped != null) return truncated(type, List.of(), List.of(), stopped);
    Path game = gameDirectory.toAbsolutePath().normalize();
    Path root = directory(game, type);
    stopped = stopReason(deadline);
    if (stopped != null) return truncated(type, List.of(), List.of(), stopped);
    if (!access.existsNoFollow(root)) return success(type, List.of(), List.of());

    List<Path> candidates = new ArrayList<>(Math.min(limits.maxCandidates(), 256));
    List<ClientFileCheckEntry> files = new ArrayList<>();
    List<ClientFileCheckSkippedEntry> skipped = new ArrayList<>();
    String truncation = null;
    try (ScanDirectory directory = access.open(game, root)) {
      stopped = stopReason(deadline);
      if (stopped != null) return truncated(type, files, skipped, stopped);
      directory.validate();
      Iterator<Path> iterator = directory.entries();
      while (iterator.hasNext()) {
        stopped = stopReason(deadline);
        if (stopped != null) {
          truncation = stopped;
          break;
        }
        if (candidates.size() >= limits.maxCandidates()) {
          truncation = "DIRECTORY_ENTRY_LIMIT";
          break;
        }
        Path name = iterator.next().getFileName();
        if (name == null || name.getNameCount() != 1) name = Path.of("INVALID");
        candidates.add(name);
      }
      directory.validate();
      if (truncation == null) truncation = sort(candidates, deadline);
      long total = 0;
      if (truncation == null) {
        for (Path namePath : candidates) {
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
            name = ClientFileCheckValidation.fileName(namePath.toString());
          } catch (RuntimeException invalid) {
            if (!addSkipped(skipped, "INVALID", "INVALID_FILE_NAME")) truncation = "SKIPPED_LIMIT";
            if (truncation != null) break;
            continue;
          }
          directory.validate();
          stopped = stopReason(deadline);
          if (stopped != null) {
            truncation = stopped;
            break;
          }
          try (SeekableByteChannel channel = directory.openNoFollow(namePath)) {
            directory.validate();
            stopped = stopReason(deadline);
            if (stopped != null) {
              truncation = stopped;
              break;
            }
            long size = channel.size();
            if (size < 0) throw new IOException("negative size");
            if (size > limits.maxSingleBytes()) {
              if (!addSkipped(skipped, name, "FILE_TOO_LARGE")) truncation = "SKIPPED_LIMIT";
              if (truncation != null) break;
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
              if (!addSkipped(skipped, name, "FILE_CHANGED")) truncation = "SKIPPED_LIMIT";
              if (truncation != null) break;
              continue;
            }
            files.add(new ClientFileCheckEntry(name, size, outcome.sha256()));
            total += size;
          } catch (RootChangedException changed) {
            truncation = "DIRECTORY_CHANGED";
            break;
          } catch (IOException | SecurityException failure) {
            stopped = stopReason(deadline);
            if (stopped != null) {
              truncation = stopped;
              break;
            }
            String reason = classify(directory, namePath);
            if (!addSkipped(skipped, name, reason)) truncation = "SKIPPED_LIMIT";
            if (truncation != null) break;
          }
        }
      }
    } catch (RootChangedException changed) {
      truncation = "DIRECTORY_CHANGED";
    } catch (UnsafeDirectoryProviderException unsafe) {
      return ClientFileCheckResult.failed(type, "DIRECTORY_PROVIDER_UNSAFE");
    } catch (IOException | SecurityException failure) {
      if (files.isEmpty() && skipped.isEmpty())
        return ClientFileCheckResult.failed(type, "DIRECTORY_UNREADABLE");
      truncation = "DIRECTORY_CHANGED";
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

  private String sort(List<Path> candidates, long deadline) {
    try {
      candidates.sort(
          Comparator.comparing(
              path -> {
                String stopped = stopReason(deadline);
                if (stopped != null) throw new ScanStopped(stopped);
                return path.toString();
              }));
      return null;
    } catch (ScanStopped stopped) {
      return stopped.reason;
    }
  }

  private String classify(ScanDirectory directory, Path name) {
    try {
      BasicFileAttributes attributes = directory.attributesNoFollow(name);
      if (attributes.isSymbolicLink()) return "SYMLINK";
      if (attributes.isDirectory() || !attributes.isRegularFile()) return "NOT_REGULAR_FILE";
    } catch (IOException | SecurityException ignored) {
      // Do not expose provider details.
    }
    return "READ_FAILED";
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
      int extra = channel.read(buffer);
      boolean changed = extra >= 0 || readTotal != expectedSize || channel.size() != expectedSize;
      return new HashOutcome(
          changed ? null : java.util.HexFormat.of().formatHex(digest.digest()), changed, null);
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 unavailable", impossible);
    }
  }

  private String stopReason(long deadline) {
    if (Thread.currentThread().isInterrupted()) return "SCAN_CANCELLED";
    return clock.nanoTime() >= deadline ? "TIME_LIMIT" : null;
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

  static final class RootChangedException extends IOException {
    RootChangedException() {
      super("scan directory changed");
    }
  }

  static final class UnsafeDirectoryProviderException extends IOException {
    UnsafeDirectoryProviderException() {
      super("directory provider cannot bind an opened handle identity");
    }
  }

  private static final class SystemFileAccess implements FileAccess {
    public boolean existsNoFollow(Path root) {
      return Files.exists(root, NOFOLLOW);
    }

    public ScanDirectory open(Path gameDirectory, Path root) throws IOException {
      BasicFileAttributes attributes = readRoot(root);
      Object fileKey = attributes.fileKey();
      if (fileKey == null) throw new UnsafeDirectoryProviderException();
      DirectoryStream<Path> stream = Files.newDirectoryStream(root);
      try {
        if (!(stream instanceof SecureDirectoryStream<Path> secure))
          throw new UnsafeDirectoryProviderException();
        BasicFileAttributeView view = secure.getFileAttributeView(BasicFileAttributeView.class);
        if (view == null) throw new UnsafeDirectoryProviderException();
        BasicFileAttributes opened = view.readAttributes();
        if (!opened.isDirectory()
            || opened.isSymbolicLink()
            || opened.fileKey() == null
            || !fileKey.equals(opened.fileKey())) throw new RootChangedException();
        return new SystemScanDirectory(fileKey, stream, secure, view);
      } catch (RuntimeException | Error failure) {
        stream.close();
        throw failure;
      } catch (IOException failure) {
        stream.close();
        throw failure;
      }
    }

    private static BasicFileAttributes readRoot(Path root) throws IOException {
      BasicFileAttributes value = Files.readAttributes(root, BasicFileAttributes.class, NOFOLLOW);
      if (!value.isDirectory() || value.isSymbolicLink()) throw new RootChangedException();
      return value;
    }

  }

  private static final class SystemScanDirectory implements ScanDirectory {
    private final Object fileKey;
    private final DirectoryStream<Path> stream;
    private final SecureDirectoryStream<Path> secure;
    private final BasicFileAttributeView rootView;

    private SystemScanDirectory(
        Object fileKey,
        DirectoryStream<Path> stream,
        SecureDirectoryStream<Path> secure,
        BasicFileAttributeView rootView) {
      this.fileKey = fileKey;
      this.stream = stream;
      this.secure = secure;
      this.rootView = rootView;
    }

    public Iterator<Path> entries() {
      return stream.iterator();
    }

    public SeekableByteChannel openNoFollow(Path name) throws IOException {
      validate();
      SeekableByteChannel channel = secure.newByteChannel(name, READ_NOFOLLOW);
      try {
        validate();
        return channel;
      } catch (IOException failure) {
        channel.close();
        throw failure;
      }
    }

    public BasicFileAttributes attributesNoFollow(Path name) throws IOException {
      BasicFileAttributeView view =
          secure.getFileAttributeView(name, BasicFileAttributeView.class, NOFOLLOW);
      if (view == null) throw new IOException("attributes unavailable");
      return view.readAttributes();
    }

    public void validate() throws IOException {
      BasicFileAttributes current = rootView.readAttributes();
      if (!current.isDirectory()
          || current.isSymbolicLink()
          || current.fileKey() == null
          || !fileKey.equals(current.fileKey())) throw new RootChangedException();
    }

    public void close() throws IOException {
      stream.close();
    }
  }
}
