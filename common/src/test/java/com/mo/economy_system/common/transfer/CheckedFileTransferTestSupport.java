package com.mo.economy_system.common.transfer;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/** Small provider-backed fixture used to exercise the handle APIs on providers without SDS. */
final class CheckedFileTransferTestSupport {
  private CheckedFileTransferTestSupport() {}

  static CheckedFileTransferTempDirectory open(Path game) throws IOException {
    return open(game, new AtomicBoolean());
  }

  static CheckedFileTransferTempDirectory open(Path game, AtomicBoolean failDelete)
      throws IOException {
    return CheckedFileTransferTempDirectory.open(game, new FixtureProvider(failDelete));
  }

  static CheckedFileTransferTempDirectory.OwnedFile part(
      CheckedFileTransferTempDirectory directory, byte[] bytes) throws IOException {
    CheckedFileTransferTempDirectory.OwnedFile part = directory.createPart();
    SeekableByteChannel channel = part.writeChannel();
    java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(bytes);
    while (buffer.hasRemaining()) channel.write(buffer);
    part.closeWriteChannel();
    return part;
  }

  static CheckedFileSnapshotter.Outcome snapshot(
      Path game, byte[] bytes, CheckedFileTransferTempBudget budget) throws Exception {
    Path root = Files.createDirectories(game.resolve("mods"));
    Path source = root.resolve("mod.jar");
    Files.write(source, bytes);
    Path temp = Files.createDirectories(game.resolve("economy_system").resolve("transfer-temp"));
    String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    CheckedFileSnapshotter.FileAccess access = new CheckedFileSnapshotter.FileAccess() {
      @Override
      public boolean existsNoFollow(Path ignored) {
        return true;
      }

      @Override
      public CheckedFileSnapshotter.SnapshotDirectory open(Path ignoredGame, Path directory) {
        return new CheckedFileSnapshotter.SnapshotDirectory() {
          @Override
          public SeekableByteChannel openNoFollow(Path relativeName) throws IOException {
            return Files.newByteChannel(directory.resolve(relativeName), StandardOpenOption.READ);
          }

          @Override
          public BasicFileAttributes attributesNoFollow(Path relativeName) throws IOException {
            return Files.readAttributes(
                directory.resolve(relativeName),
                BasicFileAttributes.class,
                LinkOption.NOFOLLOW_LINKS);
          }

          @Override public void validate() {}
          @Override public void close() {}
        };
      }

      @Override
      public CheckedFileSnapshotter.TempOutput openTemp(Path ignored) throws IOException {
        Path path = temp.resolve("snapshot-" + java.util.UUID.randomUUID() + ".part");
        SeekableByteChannel channel =
            Files.newByteChannel(
                path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        return new CheckedFileSnapshotter.TempOutput() {
          @Override public Path path() { return path; }
          @Override public SeekableByteChannel channel() { return channel; }
          @Override public void delete() throws IOException {
            channel.close();
            Files.deleteIfExists(path);
          }
          @Override public void close() throws IOException { channel.close(); }
        };
      }
    };
    return new CheckedFileSnapshotter(System::nanoTime, access, budget)
        .snapshot(
            game,
            com.mo.economy_system.common.check.ClientFileCheckType.MODS,
            "mod.jar",
            bytes.length,
            hash,
            temp,
            System.nanoTime() + 5_000_000_000L);
  }

  private static final class FixtureProvider implements CheckedFileTransferTempDirectory.DirectoryProvider {
    private final AtomicBoolean failDelete;

    private FixtureProvider(AtomicBoolean failDelete) {
      this.failDelete = failDelete;
    }

    @Override
    public CheckedFileTransferTempDirectory.DirectoryHandle open(Path game, List<String> children)
        throws IOException {
      Path current = game;
      requireDirectory(current);
      for (String child : children) {
        current = current.resolve(child);
        try {
          requireDirectory(current);
        } catch (NoSuchFileException missing) {
          Files.createDirectory(current);
          requireDirectory(current);
        }
      }
      return new FixtureHandle(current, failDelete);
    }

    private static void requireDirectory(Path path) throws IOException {
      BasicFileAttributes attributes =
          Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      if (!attributes.isDirectory() || attributes.isSymbolicLink()) {
        throw new CheckedFileTransferTempDirectory.ProviderUnsafeException();
      }
    }
  }

  private static final class FixtureHandle
      implements CheckedFileTransferTempDirectory.DirectoryHandle {
    private final Path path;
    private final AtomicBoolean failDelete;
    private boolean closed;

    private FixtureHandle(Path path, AtomicBoolean failDelete) {
      this.path = path.toAbsolutePath().normalize();
      this.failDelete = failDelete;
    }

    @Override
    public Path absolutePath() {
      return path;
    }

    @Override
    public void validate() throws IOException {
      if (closed) throw new IOException("closed");
      BasicFileAttributes attributes =
          Files.readAttributes(path, BasicFileAttributes.class, java.nio.file.LinkOption.NOFOLLOW_LINKS);
      if (!attributes.isDirectory() || attributes.isSymbolicLink()) throw new IOException("unsafe");
    }

    @Override
    public SeekableByteChannel newByteChannel(
        Path relativeName, Set<? extends java.nio.file.OpenOption> options) throws IOException {
      validate();
      return Files.newByteChannel(path.resolve(relativeName), options);
    }

    @Override
    public BasicFileAttributes attributesNoFollow(Path relativeName) throws IOException {
      validate();
      return Files.readAttributes(
          path.resolve(relativeName),
          BasicFileAttributes.class,
          java.nio.file.LinkOption.NOFOLLOW_LINKS);
    }

    @Override
    public void deleteFile(Path relativeName) throws IOException {
      validate();
      if (failDelete.get()) throw new IOException("injected delete failure");
      Files.deleteIfExists(path.resolve(relativeName));
    }

    @Override
    public void move(Path sourceName,
        CheckedFileTransferTempDirectory.DirectoryHandle target, Path targetName)
        throws IOException {
      validate();
      if (!(target instanceof FixtureHandle fixtureTarget)) throw new IOException("provider");
      fixtureTarget.validate();
      Files.move(path.resolve(sourceName), fixtureTarget.path.resolve(targetName));
    }

    @Override
    public void close() {
      closed = true;
    }
  }
}
