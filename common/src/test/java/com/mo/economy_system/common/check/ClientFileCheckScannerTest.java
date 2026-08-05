package com.mo.economy_system.common.check;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClientFileCheckScannerTest {
  @TempDir Path game;

  @Test
  void scansOnlyDirectRegularFilesInStableOrder() throws Exception {
    Path mods = Files.createDirectory(game.resolve("mods"));
    Files.writeString(mods.resolve("b.jar"), "b");
    Files.writeString(mods.resolve("a.jar"), "abc");
    Files.createDirectories(mods.resolve("nested"));
    Files.writeString(mods.resolve("nested/x.jar"), "x");
    ClientFileCheckResult result =
        new ClientFileCheckScanner().scan(game, ClientFileCheckType.MODS);
    assertEquals(ClientFileCheckStatus.SUCCESS, result.status(), result.toString());
    assertEquals(
        List.of("a.jar", "b.jar"),
        result.files().stream().map(ClientFileCheckEntry::fileName).toList());
    assertEquals(
        "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
        result.files().get(0).sha256());
    assertTrue(result.skipped().stream().anyMatch(value -> value.fileName().equals("nested")));
    assertFalse(ClientFileCheckResultJsonCodec.encode(result).contains(game.toString()));
  }

  @Test
  void missingRootIsEmptyAndRootsAreFixed() {
    var scanner = new ClientFileCheckScanner();
    assertTrue(scanner.scan(game, ClientFileCheckType.RESOURCEPACKS).files().isEmpty());
    assertEquals(
        game.toAbsolutePath().normalize().resolve("mods"),
        scanner.directory(game, ClientFileCheckType.MODS));
    assertEquals(
        game.toAbsolutePath().normalize().resolve("shaderpacks"),
        scanner.directory(game, ClientFileCheckType.SHADERPACKS));
  }

  @Test
  void reportsFileAndTotalLimits() throws Exception {
    Path mods = Files.createDirectory(game.resolve("mods"));
    Files.writeString(mods.resolve("a"), "1234");
    Files.writeString(mods.resolve("b"), "12");
    var perFile =
        new ClientFileCheckScanner(
            new ClientFileCheckScanner.Limits(10, 10, 10, 2, 10, 1_000_000_000), System::nanoTime);
    assertEquals(
        "FILE_TOO_LARGE", perFile.scan(game, ClientFileCheckType.MODS).skipped().get(0).reason());
    var total =
        new ClientFileCheckScanner(
            new ClientFileCheckScanner.Limits(10, 10, 10, 10, 4, 1_000_000_000), System::nanoTime);
    assertEquals(
        ClientFileCheckStatus.TRUNCATED, total.scan(game, ClientFileCheckType.MODS).status());
    var count =
        new ClientFileCheckScanner(
            new ClientFileCheckScanner.Limits(10, 1, 10, 10, 10, 1_000_000_000), System::nanoTime);
    assertEquals("FILE_LIMIT", count.scan(game, ClientFileCheckType.MODS).errorCode());
  }

  @Test
  void reportsTimeLimit() throws Exception {
    Path mods = Files.createDirectory(game.resolve("mods"));
    Files.writeString(mods.resolve("a"), "a");
    long[] now = {0};
    var scanner =
        new ClientFileCheckScanner(
            new ClientFileCheckScanner.Limits(10, 10, 10, 10, 10, 1), () -> now[0] += 2);
    ClientFileCheckResult result = scanner.scan(game, ClientFileCheckType.MODS);
    assertEquals(ClientFileCheckStatus.TRUNCATED, result.status());
    assertEquals("TIME_LIMIT", result.errorCode());
  }

  @Test
  void boundsDirectoryCandidatesBeforeSortingOrOpening() throws Exception {
    Path mods = Files.createDirectory(game.resolve("mods"));
    Files.writeString(mods.resolve("a"), "a");
    Files.writeString(mods.resolve("b"), "b");
    var scanner =
        new ClientFileCheckScanner(
            new ClientFileCheckScanner.Limits(1, 10, 10, 10, 10, 1_000_000_000), System::nanoTime);
    ClientFileCheckResult result = scanner.scan(game, ClientFileCheckType.MODS);
    assertEquals(ClientFileCheckStatus.TRUNCATED, result.status());
    assertEquals("DIRECTORY_ENTRY_LIMIT", result.errorCode());
    assertTrue(result.files().isEmpty());
  }

  @Test
  void rootReplacementFailsClosedAsDirectoryChanged() {
    var access = new FakeAccess(List.of(Path.of("a.jar")), "abc".getBytes());
    access.failOpenRoot = true;
    ClientFileCheckResult result = scanner(access).scan(game, ClientFileCheckType.MODS);
    assertEquals("DIRECTORY_CHANGED", result.errorCode());
  }

  @Test
  void rootFileKeyChangeAfterEnumerationFailsClosed() {
    var access = new FakeAccess(List.of(Path.of("a.jar")), "abc".getBytes());
    access.failValidationAt = 2;
    ClientFileCheckResult result = scanner(access).scan(game, ClientFileCheckType.MODS);
    assertEquals("DIRECTORY_CHANGED", result.errorCode());
    assertEquals(0, access.opens);
  }

  @Test
  void entrySymlinkAtOpenIsSkippedWithoutOpeningTarget() {
    var access = new FakeAccess(List.of(Path.of("secret-name.jar")), "secret-content".getBytes());
    access.entrySymlink = true;
    ClientFileCheckResult result = scanner(access).scan(game, ClientFileCheckType.MODS);
    assertEquals("SYMLINK", result.skipped().get(0).reason());
    assertTrue(result.files().isEmpty());
    assertEquals(1, access.opens);
    assertFalse(ClientFileCheckResultJsonCodec.encode(result).contains("secret-content"));
  }

  @Test
  void oneNoFollowOpenSuppliesSizeAndHash() {
    var access = new FakeAccess(List.of(Path.of("a.jar")), "abc".getBytes());
    ClientFileCheckResult result = scanner(access).scan(game, ClientFileCheckType.MODS);
    assertEquals(ClientFileCheckStatus.SUCCESS, result.status());
    assertEquals(1, access.opens);
    assertTrue(access.noFollowOpen);
    assertEquals(2, access.channel.sizeCalls);
    assertEquals(1, result.files().size());
  }

  @Test
  void fileShrinkAndGrowthAreFileChanged() {
    var shrink = new FakeAccess(List.of(Path.of("a.jar")), "abc".getBytes());
    shrink.channel.reportedSize = 5;
    assertEquals(
        "FILE_CHANGED",
        scanner(shrink).scan(game, ClientFileCheckType.MODS).skipped().get(0).reason());
    var growth = new FakeAccess(List.of(Path.of("a.jar")), "abc".getBytes());
    growth.channel.reportedSize = 2;
    assertEquals(
        "FILE_CHANGED",
        scanner(growth).scan(game, ClientFileCheckType.MODS).skipped().get(0).reason());
  }

  @Test
  void interruptDuringEnumerationCancelsWithoutOpening() {
    var access = new FakeAccess(List.of(Path.of("a.jar")), "abc".getBytes());
    Thread.currentThread().interrupt();
    try {
      assertEquals(
          "SCAN_CANCELLED", scanner(access).scan(game, ClientFileCheckType.MODS).errorCode());
      assertEquals(0, access.opens);
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void deadlineDuringSortingStopsBeforeOpen() {
    var access =
        new FakeAccess(
            List.of(Path.of("c.jar"), Path.of("b.jar"), Path.of("a.jar")), "abc".getBytes());
    int[] calls = {0};
    var scanner =
        new ClientFileCheckScanner(
            new ClientFileCheckScanner.Limits(10, 10, 10, 100, 100, 10),
            () -> ++calls[0] >= 7 ? 100 : 0,
            access);
    ClientFileCheckResult result = scanner.scan(game, ClientFileCheckType.MODS);
    assertEquals("TIME_LIMIT", result.errorCode());
    assertEquals(0, access.opens);
  }

  @Test
  void interruptionDuringEnumerationAndHashingReturnsScanCancelled() {
    var enumeration = new FakeAccess(List.of(Path.of("a.jar")), "abc".getBytes());
    enumeration.interruptOnNext = true;
    try {
      assertEquals(
          "SCAN_CANCELLED", scanner(enumeration).scan(game, ClientFileCheckType.MODS).errorCode());
    } finally {
      Thread.interrupted();
    }
    var hashing = new FakeAccess(List.of(Path.of("a.jar")), new byte[9000]);
    hashing.channel.interruptAfterRead = true;
    try {
      assertEquals(
          "SCAN_CANCELLED",
          new ClientFileCheckScanner(
                  new ClientFileCheckScanner.Limits(10, 10, 10, 10_000, 10_000, 1_000_000_000),
                  System::nanoTime,
                  hashing)
              .scan(game, ClientFileCheckType.MODS)
              .errorCode());
    } finally {
      Thread.interrupted();
    }
  }

  private ClientFileCheckScanner scanner(FakeAccess access) {
    return new ClientFileCheckScanner(
        new ClientFileCheckScanner.Limits(10, 10, 10, 100, 100, 1_000_000_000),
        System::nanoTime,
        access);
  }

  private static final class FakeAccess implements ClientFileCheckScanner.FileAccess {
    final List<Path> entries;
    final TrackingChannel channel;
    int validations;
    int failValidationAt = -1;
    int opens;
    boolean failOpenRoot;
    boolean entrySymlink;
    boolean noFollowOpen;
    boolean interruptOnNext;

    FakeAccess(List<Path> entries, byte[] bytes) {
      this.entries = entries;
      this.channel = new TrackingChannel(bytes);
    }

    public boolean existsNoFollow(Path root) {
      return true;
    }

    public ClientFileCheckScanner.ScanDirectory open(Path game, Path root) throws IOException {
      if (failOpenRoot) throw new ClientFileCheckScanner.RootChangedException();
      return new ClientFileCheckScanner.ScanDirectory() {
        public Iterator<Path> entries() {
          Iterator<Path> delegate = entries.iterator();
          return new Iterator<>() {
            public boolean hasNext() {
              return delegate.hasNext();
            }

            public Path next() {
              Path value = delegate.next();
              if (interruptOnNext) Thread.currentThread().interrupt();
              return value;
            }
          };
        }

        public SeekableByteChannel openNoFollow(Path name) throws IOException {
          opens++;
          noFollowOpen = true;
          if (entrySymlink) throw new IOException("symlink");
          channel.position(0);
          return channel;
        }

        public BasicFileAttributes attributesNoFollow(Path name) {
          return attributes(entrySymlink, false, !entrySymlink);
        }

        public void validate() throws IOException {
          validations++;
          if (validations == failValidationAt)
            throw new ClientFileCheckScanner.RootChangedException();
        }

        public void close() {}
      };
    }
  }

  private static BasicFileAttributes attributes(
      boolean symlink, boolean directory, boolean regular) {
    return new BasicFileAttributes() {
      public FileTime lastModifiedTime() {
        return FileTime.fromMillis(0);
      }

      public FileTime lastAccessTime() {
        return FileTime.fromMillis(0);
      }

      public FileTime creationTime() {
        return FileTime.fromMillis(0);
      }

      public boolean isRegularFile() {
        return regular;
      }

      public boolean isDirectory() {
        return directory;
      }

      public boolean isSymbolicLink() {
        return symlink;
      }

      public boolean isOther() {
        return false;
      }

      public long size() {
        return 0;
      }

      public Object fileKey() {
        return "key";
      }
    };
  }

  private static final class TrackingChannel implements SeekableByteChannel {
    final byte[] bytes;
    int position;
    long reportedSize;
    int sizeCalls;
    boolean open = true;
    boolean interruptAfterRead;

    TrackingChannel(byte[] bytes) {
      this.bytes = bytes;
      this.reportedSize = bytes.length;
    }

    public int read(ByteBuffer destination) {
      if (position >= bytes.length) return -1;
      int count = Math.min(destination.remaining(), bytes.length - position);
      destination.put(bytes, position, count);
      position += count;
      if (interruptAfterRead) Thread.currentThread().interrupt();
      return count;
    }

    public int write(ByteBuffer source) {
      throw new UnsupportedOperationException();
    }

    public long position() {
      return position;
    }

    public SeekableByteChannel position(long value) {
      position = (int) value;
      open = true;
      return this;
    }

    public long size() {
      sizeCalls++;
      return reportedSize;
    }

    public SeekableByteChannel truncate(long size) {
      throw new UnsupportedOperationException();
    }

    public boolean isOpen() {
      return open;
    }

    public void close() {
      open = false;
    }
  }
}
