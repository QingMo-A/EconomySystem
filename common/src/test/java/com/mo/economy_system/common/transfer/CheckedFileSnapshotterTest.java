package com.mo.economy_system.common.transfer;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.check.ClientFileCheckType;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CheckedFileSnapshotterTest {
  @TempDir Path game;

  @Test
  void streamsWithOneSourceChannelAndDeletesOwnedSnapshot() throws Exception {
    byte[] bytes = "snapshot-content".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    FakeAccess access = new FakeAccess(bytes);
    CheckedFileSnapshotter snapshotter = new CheckedFileSnapshotter(System::nanoTime, access);

    CheckedFileSnapshotter.Outcome outcome = snapshotter.snapshot(game, ClientFileCheckType.MODS,
        "mod.jar", bytes.length, hash(bytes), tempRoot(), System.nanoTime() + 5_000_000_000L);

    assertTrue(outcome.success(), outcome.errorCode());
    assertEquals(1, access.sourceOpens);
    assertTrue(access.sourceSizeCalls >= 2);
    Path path = outcome.snapshot().path();
    assertArrayEquals(bytes, readBytes(path));
    outcome.snapshot().close();
    outcome.snapshot().close();
    assertFalse(Files.exists(path));
  }

  @Test
  void rejectsManifestAndSourceSizeChangesWithoutLeakingTemp() throws Exception {
    byte[] bytes = "content".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    FakeAccess access = new FakeAccess(bytes);
    access.sourceReportedSize = bytes.length - 1;
    CheckedFileSnapshotter.Outcome outcome = snapshotter(access).snapshot(game,
        ClientFileCheckType.MODS, "mod.jar", bytes.length, hash(bytes), tempRoot(),
        System.nanoTime() + 5_000_000_000L);
    assertEquals("FILE_CHANGED_RECHECK_REQUIRED", outcome.errorCode());
    assertEquals(0, access.tempFiles.size());
  }

  @Test
  void rejectsManifestHashMismatchAndCleansSnapshot() throws Exception {
    byte[] bytes = "content".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    FakeAccess access = new FakeAccess(bytes);
    CheckedFileSnapshotter.Outcome outcome = snapshotter(access).snapshot(game,
        ClientFileCheckType.MODS, "mod.jar", bytes.length, hash(new byte[] {9}), tempRoot(),
        deadline());
    assertEquals("FILE_CHANGED_RECHECK_REQUIRED", outcome.errorCode());
    assertTrue(access.tempFiles.isEmpty());
  }

  @Test
  void rejectsSymlinkAndNonRegularEntriesBeforeOpeningSource() {
    FakeAccess symlink = new FakeAccess(new byte[] {1});
    symlink.symlink = true;
    assertEquals("SYMLINK", snapshotter(symlink).snapshot(game, ClientFileCheckType.MODS,
        "mod.jar", 1, hash(new byte[] {1}), tempRoot(), deadline()).errorCode());
    assertEquals(0, symlink.sourceOpens);

    FakeAccess directory = new FakeAccess(new byte[] {1});
    directory.directory = true;
    assertEquals("NOT_REGULAR_FILE", snapshotter(directory).snapshot(game,
        ClientFileCheckType.MODS, "mod.jar", 1, hash(new byte[] {1}), tempRoot(), deadline())
        .errorCode());
    assertEquals(0, directory.sourceOpens);
  }

  @Test
  void unsupportedProviderAndChangedRootFailClosed() {
    FakeAccess unsafe = new FakeAccess(new byte[] {1});
    unsafe.unsafe = true;
    assertEquals("DIRECTORY_PROVIDER_UNSAFE", snapshotter(unsafe).snapshot(game,
        ClientFileCheckType.MODS, "mod.jar", 1, hash(new byte[] {1}), tempRoot(), deadline())
        .errorCode());

    FakeAccess changed = new FakeAccess(new byte[] {1});
    changed.changed = true;
    assertEquals("DIRECTORY_CHANGED", snapshotter(changed).snapshot(game, ClientFileCheckType.MODS,
        "mod.jar", 1, hash(new byte[] {1}), tempRoot(), deadline()).errorCode());
  }

  @Test
  void deadlineAndInterruptHaveDistinctStableErrors() {
    FakeAccess access = new FakeAccess(new byte[] {1});
    MutableClock clock = new MutableClock();
    CheckedFileSnapshotter snapshotter = new CheckedFileSnapshotter(clock, access);
    assertEquals("SNAPSHOT_TIMEOUT", snapshotter.snapshot(game, ClientFileCheckType.MODS,
        "mod.jar", 1, hash(new byte[] {1}), tempRoot(), 0).errorCode());

    Thread.currentThread().interrupt();
    try {
      assertEquals("SNAPSHOT_CANCELLED", snapshotter.snapshot(game, ClientFileCheckType.MODS,
          "mod.jar", 1, hash(new byte[] {1}), tempRoot(), Long.MAX_VALUE).errorCode());
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void reservationIsReleasedExactlyOnceOnSuccessAndFailure() throws Exception {
    byte[] bytes = {1, 2, 3};
    CountingBudget budget = new CountingBudget();
    CheckedFileSnapshotter snapshotter = new CheckedFileSnapshotter(System::nanoTime,
        new FakeAccess(bytes), budget);
    CheckedFileSnapshotter.Outcome success = snapshotter.snapshot(game, ClientFileCheckType.MODS,
        "mod.jar", bytes.length, hash(bytes), tempRoot(), deadline());
    assertTrue(success.success(), success.errorCode());
    assertEquals(1, budget.reserves.get());
    success.snapshot().close();
    success.snapshot().close();
    assertEquals(1, budget.releases.get());

    CountingBudget rejected = new CountingBudget();
    rejected.reject = true;
    CheckedFileSnapshotter.Outcome failure = new CheckedFileSnapshotter(System::nanoTime,
        new FakeAccess(bytes), rejected).snapshot(game, ClientFileCheckType.MODS, "mod.jar",
        bytes.length, hash(bytes), tempRoot(), deadline());
    assertEquals("TEMP_STORAGE_LIMIT", failure.errorCode());
    assertEquals(1, rejected.reserves.get());
  }

  @Test
  void invalidNamesAndNonFixedTempRootsFailClosed() {
    FakeAccess access = new FakeAccess(new byte[] {1});
    assertEquals("FILE_NOT_IN_CHECK_RESULT", snapshotter(access).snapshot(game,
        ClientFileCheckType.MODS, "../mod.jar", 1, hash(new byte[] {1}), tempRoot(), deadline())
        .errorCode());
    assertEquals("DIRECTORY_PROVIDER_UNSAFE", snapshotter(access).snapshot(game,
        ClientFileCheckType.MODS, "mod.jar", 1, hash(new byte[] {1}), game.resolve("other"),
        deadline()).errorCode());
  }

  private CheckedFileSnapshotter snapshotter(FakeAccess access) {
    return new CheckedFileSnapshotter(System::nanoTime, access);
  }

  private Path tempRoot() {
    return game.toAbsolutePath().normalize().resolve("economy_system").resolve("transfer-temp");
  }

  private static long deadline() {
    return System.nanoTime() + 5_000_000_000L;
  }

  private static String hash(byte[] bytes) throws RuntimeException {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (Exception impossible) {
      throw new AssertionError(impossible);
    }
  }

  private static byte[] readBytes(Path path) throws IOException {
    try (InputStream input = Files.newInputStream(path)) {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      input.transferTo(output);
      return output.toByteArray();
    }
  }

  private static final class MutableClock implements CheckedFileSnapshotter.Clock {
    long now;

    @Override
    public long nanoTime() {
      return now;
    }
  }

  private static final class CountingBudget {
    final AtomicInteger reserves = new AtomicInteger();
    final AtomicInteger releases = new AtomicInteger();
    boolean reject;

    public Object reserve(long bytes) {
      reserves.incrementAndGet();
      if (reject) return null;
      return new Object() {
        public void release() {
          releases.incrementAndGet();
        }
      };
    }
  }

  private static final class FakeAccess implements CheckedFileSnapshotter.FileAccess {
    final byte[] bytes;
    final java.util.Set<Path> tempFiles = new java.util.HashSet<>();
    int sourceOpens;
    int sourceSizeCalls;
    boolean symlink;
    boolean directory;
    boolean unsafe;
    boolean changed;
    long sourceReportedSize = -1;

    FakeAccess(byte[] bytes) {
      this.bytes = bytes.clone();
    }

    @Override
    public boolean existsNoFollow(Path root) {
      return true;
    }

    @Override
    public CheckedFileSnapshotter.SnapshotDirectory open(Path gameDirectory, Path root)
        throws IOException {
      if (unsafe) throw new CheckedFileSnapshotter.UnsafeDirectoryProviderException();
      if (changed) throw new CheckedFileSnapshotter.RootChangedException();
      return new CheckedFileSnapshotter.SnapshotDirectory() {
        @Override
        public SeekableByteChannel openNoFollow(Path relativeName) {
          sourceOpens++;
          return new SourceChannel(bytes, FakeAccess.this);
        }

        @Override
        public BasicFileAttributes attributesNoFollow(Path relativeName) {
          return attributes(symlink, directory, !symlink && !directory, bytes.length);
        }

        @Override
        public void validate() {}

        @Override
        public void close() {}
      };
    }

    @Override
    public CheckedFileSnapshotter.TempOutput openTemp(Path root) throws IOException {
      Files.createDirectories(root);
      Path path = root.resolve("test-" + UUID.randomUUID() + ".part");
      SeekableByteChannel channel =
          Files.newByteChannel(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
      tempFiles.add(path);
      return new CheckedFileSnapshotter.TempOutput() {
        @Override
        public Path path() {
          return path;
        }

        @Override
        public SeekableByteChannel channel() {
          return channel;
        }

        @Override
        public void delete() throws IOException {
          channel.close();
          Files.deleteIfExists(path);
          tempFiles.remove(path);
        }

        @Override
        public void close() throws IOException {
          channel.close();
        }
      };
    }

  }

  private static BasicFileAttributes attributes(
      boolean symlink, boolean directory, boolean regular, long size) {
    return new BasicFileAttributes() {
      @Override
      public FileTime lastModifiedTime() {
        return FileTime.fromMillis(0);
      }

      @Override
      public FileTime lastAccessTime() {
        return FileTime.fromMillis(0);
      }

      @Override
      public FileTime creationTime() {
        return FileTime.fromMillis(0);
      }

      @Override
      public boolean isRegularFile() {
        return regular;
      }

      @Override
      public boolean isDirectory() {
        return directory;
      }

      @Override
      public boolean isSymbolicLink() {
        return symlink;
      }

      @Override
      public boolean isOther() {
        return false;
      }

      @Override
      public long size() {
        return size;
      }

      @Override
      public Object fileKey() {
        return "test-key";
      }
    };
  }

  private static final class SourceChannel implements SeekableByteChannel {
    private final byte[] bytes;
    private final FakeAccess owner;
    private int position;
    private boolean open = true;

    SourceChannel(byte[] bytes, FakeAccess owner) {
      this.bytes = bytes;
      this.owner = owner;
    }

    @Override
    public int read(ByteBuffer destination) {
      if (position >= bytes.length) return -1;
      int count = Math.min(destination.remaining(), bytes.length - position);
      destination.put(bytes, position, count);
      position += count;
      return count;
    }

    @Override
    public int write(ByteBuffer source) {
      throw new UnsupportedOperationException();
    }

    @Override
    public long position() {
      return position;
    }

    @Override
    public SeekableByteChannel position(long value) {
      position = Math.toIntExact(value);
      return this;
    }

    @Override
    public long size() {
      owner.sourceSizeCalls++;
      return owner.sourceReportedSize < 0 ? bytes.length : owner.sourceReportedSize;
    }

    @Override
    public SeekableByteChannel truncate(long size) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOpen() {
      return open;
    }

    @Override
    public void close() {
      open = false;
    }
  }
}
