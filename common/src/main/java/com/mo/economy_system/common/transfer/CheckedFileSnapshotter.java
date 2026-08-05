package com.mo.economy_system.common.transfer;

import com.mo.economy_system.common.check.ClientFileCheckType;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Creates a private, immutable snapshot of one file authorized by a file-check result.
 *
 * <p>The source is opened relative to an authenticated directory handle and is copied through a
 * single channel.  The source channel is checked before and after the copy, while the digest is
 * calculated incrementally.  A snapshot is an owned resource: callers must close it when the
 * transfer is complete or abandoned.
 */
public final class CheckedFileSnapshotter {
  /** A monotonic clock used by the snapshot operation. */
  public interface Clock {
    long nanoTime();
  }

  /**
   * File-system operations required by the snapshotter.
   *
   * <p>The interface is deliberately small so tests can fault-inject provider changes without
   * touching the host file system.  The default implementation is a secure-directory provider
   * implementation and fails closed when the provider cannot supply one.
   */
  public interface FileAccess {
    boolean existsNoFollow(Path root);

    SnapshotDirectory open(Path gameDirectory, Path root) throws IOException;

    /** Opens a random CREATE_NEW output in the fixed transfer-temp directory. */
    default TempOutput openTemp(Path root) throws IOException {
      return SystemFileAccess.openTempInternal(root);
    }

    default void deleteIfExists(Path path) throws IOException {
      Files.deleteIfExists(path);
    }
  }

  /** A directory handle bound to one directory identity. */
  public interface SnapshotDirectory extends AutoCloseable {
    SeekableByteChannel openNoFollow(Path relativeName) throws IOException;

    BasicFileAttributes attributesNoFollow(Path relativeName) throws IOException;

    /** Re-checks that the opened directory still has its original identity. */
    void validate() throws IOException;

    @Override
    void close() throws IOException;
  }

  /** A temporary output channel and its private path. */
  public interface TempOutput extends AutoCloseable {
    Path path();

    SeekableByteChannel channel();

    /** Deletes the owned path without following a replaced temp-root ancestor. */
    default void delete() throws IOException {
      channel().close();
      Files.deleteIfExists(path());
    }

    @Override
    void close() throws IOException;
  }

  /** A completed snapshot.  {@link #close()} is idempotent and releases its reservation once. */
  public static final class Snapshot implements AutoCloseable {
    private final Path path;
    private final long size;
    private final String sha256;
    private final FileAccess access;
    private final Reservation reservation;
    private final TempOutput output;
    private final AtomicBoolean closed = new AtomicBoolean();

    /** Compatibility constructor for callers that already own an unreserved snapshot. */
    public Snapshot(Path path, long size, String sha256) {
      this(path, size, sha256, new SystemFileAccess(), Reservation.NONE, null);
    }

    private Snapshot(
        Path path,
        long size,
        String sha256,
        FileAccess access,
        Reservation reservation,
        TempOutput output) {
      this.path = Objects.requireNonNull(path);
      this.size = size;
      this.sha256 = Objects.requireNonNull(sha256);
      this.access = Objects.requireNonNull(access);
      this.reservation = Objects.requireNonNull(reservation);
      this.output = output;
    }

    public Path path() {
      return path;
    }

    public long size() {
      return size;
    }

    public String sha256() {
      return sha256;
    }

    @Override
    public void close() throws IOException {
      if (!closed.compareAndSet(false, true)) return;
      IOException deletionFailure = null;
      try {
        if (output == null) access.deleteIfExists(path);
        else output.delete();
      } catch (IOException | RuntimeException failure) {
        deletionFailure = new IOException("snapshot cleanup failed");
      } finally {
        if (output != null) closeQuietly(output);
        reservation.release();
      }
      if (deletionFailure != null) throw new IOException("snapshot cleanup failed");
    }
  }

  public record Outcome(Snapshot snapshot, String errorCode) {
    public boolean success() {
      return snapshot != null;
    }
  }

  private static final Set<OpenOption> READ_NOFOLLOW =
      Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
  private static final FileAccess SYSTEM = new SystemFileAccess();
  private static final Clock SYSTEM_CLOCK = System::nanoTime;

  private final Clock clock;
  private final FileAccess access;
  private final Object tempBudget;

  public CheckedFileSnapshotter() {
    this(SYSTEM_CLOCK, SYSTEM, null);
  }

  public CheckedFileSnapshotter(Clock clock, FileAccess access) {
    this(clock, access, null);
  }

  /**
   * Creates an injectable snapshotter.  The budget is intentionally an object so this class can
   * remain source-compatible while the common temp-budget implementation evolves.  It must expose
   * {@code reserve(long)} returning a reservation with either {@code close()} or {@code release()}.
   */
  public CheckedFileSnapshotter(Clock clock, FileAccess access, Object tempBudget) {
    this.clock = Objects.requireNonNull(clock);
    this.access = Objects.requireNonNull(access);
    this.tempBudget = tempBudget;
  }

  public CheckedFileSnapshotter(
      Clock clock, FileAccess access, CheckedFileTransferTempBudget tempBudget) {
    this(clock, access, (Object) tempBudget);
  }

  /**
   * Compatibility entry point used by the loader adapters.  The private root must be the fixed
   * {@code economy_system/transfer-temp} child of {@code gameDirectory}.
   */
  public static Outcome create(
      Path gameDirectory,
      ClientFileCheckType type,
      String rawName,
      long expectedSize,
      String expectedHash,
      Path privateTemp,
      long deadlineNanos) {
    return new CheckedFileSnapshotter()
        .snapshot(
            gameDirectory,
            type,
            rawName,
            expectedSize,
            expectedHash,
            privateTemp,
            deadlineNanos);
  }

  /** Injectable variant without a temporary-budget reservation. */
  public static Outcome create(
      Path gameDirectory,
      ClientFileCheckType type,
      String rawName,
      long expectedSize,
      String expectedHash,
      Path privateTemp,
      long deadlineNanos,
      Clock clock,
      FileAccess access) {
    return create(
        gameDirectory,
        type,
        rawName,
        expectedSize,
        expectedHash,
        privateTemp,
        deadlineNanos,
        clock,
        access,
        null);
  }

  public static Outcome create(
      Path gameDirectory,
      ClientFileCheckType type,
      String rawName,
      long expectedSize,
      String expectedHash,
      Path privateTemp,
      long deadlineNanos,
      CheckedFileTransferTempBudget tempBudget) {
    return create(
        gameDirectory,
        type,
        rawName,
        expectedSize,
        expectedHash,
        privateTemp,
        deadlineNanos,
        (Object) tempBudget);
  }

  /** Compatibility variant with the shared budget before the deadline. */
  public static Outcome create(
      Path gameDirectory,
      ClientFileCheckType type,
      String rawName,
      long expectedSize,
      String expectedHash,
      Path privateTemp,
      Object tempBudget,
      long deadlineNanos) {
    return create(
        gameDirectory,
        type,
        rawName,
        expectedSize,
        expectedHash,
        privateTemp,
        deadlineNanos,
        tempBudget);
  }

  /** Compatibility variant that supplies the shared temp budget while using system file access. */
  public static Outcome create(
      Path gameDirectory,
      ClientFileCheckType type,
      String rawName,
      long expectedSize,
      String expectedHash,
      Path privateTemp,
      long deadlineNanos,
      Object tempBudget) {
    return new CheckedFileSnapshotter(SYSTEM_CLOCK, SYSTEM, tempBudget)
        .snapshot(
            gameDirectory,
            type,
            rawName,
            expectedSize,
            expectedHash,
            privateTemp,
            deadlineNanos);
  }

  /** Injectable variant with a caller-supplied clock, access and temp budget. */
  public static Outcome create(
      Path gameDirectory,
      ClientFileCheckType type,
      String rawName,
      long expectedSize,
      String expectedHash,
      Path privateTemp,
      long deadlineNanos,
      Clock clock,
      FileAccess access,
      Object tempBudget) {
    return new CheckedFileSnapshotter(clock, access, tempBudget)
        .snapshot(
            gameDirectory,
            type,
            rawName,
            expectedSize,
            expectedHash,
            privateTemp,
            deadlineNanos);
  }

  /** Same operation with the budget placed before the deadline for adapter convenience. */
  public static Outcome create(
      Path gameDirectory,
      ClientFileCheckType type,
      String rawName,
      long expectedSize,
      String expectedHash,
      Path privateTemp,
      Object tempBudget,
      long deadlineNanos,
      Clock clock,
      FileAccess access) {
    return create(
        gameDirectory,
        type,
        rawName,
        expectedSize,
        expectedHash,
        privateTemp,
        deadlineNanos,
        clock,
        access,
        tempBudget);
  }

  /** Runs an injectable snapshot operation. */
  public Outcome snapshot(
      Path gameDirectory,
      ClientFileCheckType type,
      String rawName,
      long expectedSize,
      String expectedHash,
      Path privateTemp,
      long deadlineNanos) {
    String name;
    try {
      name = CheckedFileTransferValidation.fileName(rawName);
      CheckedFileTransferValidation.sha256(expectedHash);
      if (expectedSize < 0 || expectedSize > EconomyNetworkLimits.MAX_TRANSFER_FILE_BYTES)
        return failure("FILE_TOO_LARGE");
    } catch (RuntimeException invalid) {
      return failure("FILE_NOT_IN_CHECK_RESULT");
    }

    try {
      String initialStop = stopped(deadlineNanos);
      if (initialStop != null) return failure(initialStop);
    } catch (RuntimeException clockFailure) {
      return failure("SNAPSHOT_FAILED");
    }
    Path game;
    Path root;
    Path tempRoot;
    try {
      game = gameDirectory.toAbsolutePath().normalize();
      root = game.resolve(type.id()).normalize();
      tempRoot = game.resolve("economy_system").resolve("transfer-temp").normalize();
      if (!isDirectChild(game, root) || !isFixedTempRoot(game, privateTemp, tempRoot))
        return failure("DIRECTORY_PROVIDER_UNSAFE");
    } catch (RuntimeException invalidPath) {
      return failure("DIRECTORY_PROVIDER_UNSAFE");
    }

    try {
      if (!access.existsNoFollow(root)) return failure("SNAPSHOT_FAILED");
    } catch (RuntimeException failure) {
      return failure("SNAPSHOT_FAILED");
    }
    ReservationAttempt reservationAttempt;
    try {
      reservationAttempt = reserve(tempBudget, expectedSize);
    } catch (RuntimeException failure) {
      return failure("TEMP_STORAGE_LIMIT");
    }
    if (!reservationAttempt.success()) return failure("TEMP_STORAGE_LIMIT");
    Reservation reservation = reservationAttempt.reservation();
    TempOutput output = null;
    Path outputPath = null;
    boolean outputOwned = false;
    boolean completed = false;
    try (SnapshotDirectory directory = access.open(game, root)) {
      String stopped = stopped(deadlineNanos);
      if (stopped != null) return failure(stopped);
      directory.validate();
      Path relativeName = Path.of(name);
      BasicFileAttributes attributes = directory.attributesNoFollow(relativeName);
      String entryError = validateEntry(attributes);
      if (entryError != null) return failure(entryError);

      output = access.openTemp(tempRoot);
      if (output == null) return failure("SNAPSHOT_FAILED");
      outputPath = output.path();
      if (outputPath == null) return failure("SNAPSHOT_FAILED");
      if (!isPrivateTempFile(tempRoot, outputPath))
        return failure("DIRECTORY_PROVIDER_UNSAFE");
      outputOwned = true;
      if (output.channel() == null) return failure("SNAPSHOT_FAILED");
      try (SeekableByteChannel source = directory.openNoFollow(relativeName)) {
        directory.validate();
        stopped = stopped(deadlineNanos);
        if (stopped != null) return failure(stopped);
        long initialSize = source.size();
        if (initialSize < 0 || initialSize > EconomyNetworkLimits.MAX_TRANSFER_FILE_BYTES)
          return failure("FILE_TOO_LARGE");
        if (initialSize != expectedSize) return failure("FILE_CHANGED_RECHECK_REQUIRED");

        MessageDigest digest = sha256Digest();
        long copied = copyAndHash(source, output.channel(), digest, initialSize, deadlineNanos);
        if (copied < 0) return failure(stopped(deadlineNanos));
        if (copied != initialSize) return failure("FILE_CHANGED_RECHECK_REQUIRED");
        ByteBuffer oneByte = ByteBuffer.allocate(1);
        oneByte.limit(1);
        int extra = source.read(oneByte);
        if (extra >= 0 || source.size() != initialSize)
          return failure("FILE_CHANGED_RECHECK_REQUIRED");
        stopped = stopped(deadlineNanos);
        if (stopped != null) return failure(stopped);
        String digestHex = java.util.HexFormat.of().formatHex(digest.digest());
        if (!digestHex.equals(expectedHash)) return failure("FILE_CHANGED_RECHECK_REQUIRED");
      }
      directory.validate();
      Snapshot snapshot =
          new Snapshot(outputPath, expectedSize, expectedHash, access, reservation, output);
      output = null;
      completed = true;
      return new Outcome(snapshot, null);
    } catch (RootChangedException changed) {
      return failure("DIRECTORY_CHANGED");
    } catch (UnsafeDirectoryProviderException unsafe) {
      return failure("DIRECTORY_PROVIDER_UNSAFE");
    } catch (EntrySymlinkException symlink) {
      return failure("SYMLINK");
    } catch (EntryNotRegularException notRegular) {
      return failure("NOT_REGULAR_FILE");
    } catch (java.nio.channels.ClosedByInterruptException interrupted) {
      Thread.currentThread().interrupt();
      return failure("SNAPSHOT_CANCELLED");
    } catch (RuntimeException | IOException failure) {
      String stopped = stopped(deadlineNanos);
      return failure(stopped == null ? "SNAPSHOT_FAILED" : stopped);
    } finally {
      if (output != null) {
        if (outputOwned) deleteQuietly(output);
        closeQuietly(output);
      }
      if (!completed) reservation.release();
    }
  }

  private static Outcome failure(String code) {
    return new Outcome(null, code == null ? "SNAPSHOT_FAILED" : code);
  }

  private String stopped(long deadlineNanos) {
    if (Thread.currentThread().isInterrupted()) return "SNAPSHOT_CANCELLED";
    return clock.nanoTime() >= deadlineNanos ? "SNAPSHOT_TIMEOUT" : null;
  }

  private static boolean isDirectChild(Path parent, Path child) {
    return child.getParent() != null && child.getParent().equals(parent) && !child.equals(parent);
  }

  private static boolean isFixedTempRoot(Path game, Path supplied, Path expected) {
    if (supplied == null) return false;
    try {
      return supplied.toAbsolutePath().normalize().equals(expected);
    } catch (RuntimeException invalid) {
      return false;
    }
  }

  private static boolean isPrivateTempFile(Path root, Path path) {
    try {
      Path normalizedRoot = root.toAbsolutePath().normalize();
      Path normalizedPath = path.toAbsolutePath().normalize();
      return normalizedPath.getParent() != null
          && normalizedPath.getParent().equals(normalizedRoot)
          && normalizedPath.getFileName() != null
          && normalizedPath.getFileName().toString().indexOf('/') < 0
          && normalizedPath.getFileName().toString().indexOf('\\') < 0;
    } catch (RuntimeException invalid) {
      return false;
    }
  }

  private static String validateEntry(BasicFileAttributes attributes) throws IOException {
    if (attributes == null) return "SNAPSHOT_FAILED";
    if (attributes.isSymbolicLink()) throw new EntrySymlinkException();
    if (!attributes.isRegularFile()) throw new EntryNotRegularException();
    if (attributes.size() < 0) return "SNAPSHOT_FAILED";
    if (attributes.size() > EconomyNetworkLimits.MAX_TRANSFER_FILE_BYTES)
      return "FILE_TOO_LARGE";
    return null;
  }

  private static MessageDigest sha256Digest() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 unavailable", impossible);
    }
  }

  private long copyAndHash(
      SeekableByteChannel source,
      SeekableByteChannel destination,
      MessageDigest digest,
      long expectedSize,
      long deadlineNanos)
      throws IOException {
    ByteBuffer buffer = ByteBuffer.allocate(8192);
    long copied = 0;
    while (copied < expectedSize) {
      String stopped = stopped(deadlineNanos);
      if (stopped != null) return -1;
      buffer.clear();
      buffer.limit((int) Math.min(buffer.capacity(), expectedSize - copied));
      int count = source.read(buffer);
      if (count < 0) return copied;
      if (count == 0) continue;
      if (count > buffer.limit()) return copied;
      digest.update(buffer.array(), 0, count);
      buffer.flip();
      while (buffer.hasRemaining()) {
        stopped = stopped(deadlineNanos);
        if (stopped != null) return -1;
        int before = buffer.remaining();
        int written = destination.write(buffer);
        if (written < 0 || written > before) return copied;
        if (written == 0) continue;
      }
      copied += count;
    }
    return copied;
  }

  private ReservationAttempt reserve(Object budget, long bytes) {
    if (budget == null) return ReservationAttempt.success(Reservation.NONE);
    if (budget instanceof CheckedFileTransferTempBudget typedBudget) {
      CheckedFileTransferTempBudget.Reservation typedReservation = typedBudget.reserve(bytes);
      if (typedReservation == null) return ReservationAttempt.failure();
      return ReservationAttempt.success(typedReservation::release);
    }
    try {
      Method reserveMethod = findMethod(budget.getClass(), "reserve", long.class);
      if (reserveMethod == null) return ReservationAttempt.failure();
      Object value = invoke(reserveMethod, budget, bytes);
      if (value == null || Boolean.FALSE.equals(value)) return ReservationAttempt.failure();
      if (value instanceof Boolean) return ReservationAttempt.success(Reservation.NONE);
      Method releaseMethod = findMethod(value.getClass(), "release");
      if (releaseMethod == null) releaseMethod = findMethod(value.getClass(), "close");
      if (releaseMethod == null) return ReservationAttempt.failure();
      Method finalReleaseMethod = releaseMethod;
      return ReservationAttempt.success(
          new Reservation() {
            private final AtomicBoolean released = new AtomicBoolean();

            @Override
            public void release() {
              if (!released.compareAndSet(false, true)) return;
              try {
                invoke(finalReleaseMethod, value);
              } catch (ReflectiveOperationException | RuntimeException ignored) {
                // A terminal snapshot failure must not leak provider details.
              }
            }
          });
    } catch (InvocationTargetException failure) {
      Throwable cause = failure.getCause();
      if (cause instanceof Error error) throw error;
      return ReservationAttempt.failure();
    } catch (ReflectiveOperationException | RuntimeException failure) {
      return ReservationAttempt.failure();
    }
  }

  private static Method findMethod(Class<?> type, String name, Class<?>... parameters) {
    try {
      Method method = type.getMethod(name, parameters);
      method.trySetAccessible();
      return method;
    } catch (ReflectiveOperationException | SecurityException ignored) {
      try {
        Method method = type.getDeclaredMethod(name, parameters);
        method.trySetAccessible();
        return method;
      } catch (ReflectiveOperationException | SecurityException ignoredAgain) {
        return null;
      }
    }
  }

  private static Object invoke(Method method, Object receiver, Object... arguments)
      throws ReflectiveOperationException {
    return method.invoke(receiver, arguments);
  }

  private void deleteQuietly(TempOutput output) {
    try {
      output.delete();
    } catch (IOException | RuntimeException ignored) {
      // Do not disclose path/provider details in a protocol error.
    }
  }

  private static void closeQuietly(AutoCloseable closeable) {
    try {
      closeable.close();
    } catch (Exception ignored) {
      // Cleanup is best effort; the caller receives only a stable error code.
    }
  }

  private interface Reservation {
    Reservation NONE = () -> {};

    void release();
  }

  private record ReservationAttempt(boolean success, Reservation reservation) {
    static ReservationAttempt success(Reservation reservation) {
      return new ReservationAttempt(true, reservation);
    }

    static ReservationAttempt failure() {
      return new ReservationAttempt(false, Reservation.NONE);
    }
  }

  static final class RootChangedException extends IOException {
    RootChangedException() {
      super("directory changed");
    }
  }

  static final class UnsafeDirectoryProviderException extends IOException {
    UnsafeDirectoryProviderException() {
      super("unsafe directory provider");
    }
  }

  static final class EntrySymlinkException extends IOException {
    EntrySymlinkException() {
      super("symbolic link");
    }
  }

  static final class EntryNotRegularException extends IOException {
    EntryNotRegularException() {
      super("not regular");
    }
  }

  private static final class SystemFileAccess implements FileAccess {
    private static final LinkOption[] NOFOLLOW = {LinkOption.NOFOLLOW_LINKS};

    @Override
    public boolean existsNoFollow(Path root) {
      return Files.exists(root, NOFOLLOW);
    }

    @Override
    public SnapshotDirectory open(Path gameDirectory, Path root) throws IOException {
      BasicFileAttributes before = Files.readAttributes(root, BasicFileAttributes.class, NOFOLLOW);
      if (!before.isDirectory() || before.isSymbolicLink()) throw new RootChangedException();
      if (before.fileKey() == null) throw new UnsafeDirectoryProviderException();
      DirectoryStream<Path> stream = Files.newDirectoryStream(root);
      try {
        if (!(stream instanceof SecureDirectoryStream<Path> secure))
          throw new UnsafeDirectoryProviderException();
        BasicFileAttributeView view = secure.getFileAttributeView(BasicFileAttributeView.class);
        if (view == null) throw new UnsafeDirectoryProviderException();
        BasicFileAttributes opened = view.readAttributes();
        if (!opened.isDirectory()
            || opened.isSymbolicLink()
            || opened.fileKey() == null) throw new UnsafeDirectoryProviderException();
        if (!before.fileKey().equals(opened.fileKey())) throw new RootChangedException();
        return new SystemSnapshotDirectory(stream, secure, view, before.fileKey());
      } catch (RuntimeException | Error failure) {
        stream.close();
        throw failure;
      } catch (IOException failure) {
        stream.close();
        throw failure;
      }
    }

    private static TempOutput openTempInternal(Path root) throws IOException {
      Path game = root.getParent() == null ? null : root.getParent().getParent();
      if (game == null) throw new UnsafeDirectoryProviderException();
      ensureDirectory(game);
      ensureDirectory(game.resolve("economy_system"));
      ensureDirectory(root);
      BasicFileAttributes before = Files.readAttributes(root, BasicFileAttributes.class, NOFOLLOW);
      if (!before.isDirectory() || before.isSymbolicLink() || before.fileKey() == null)
        throw new UnsafeDirectoryProviderException();
      DirectoryStream<Path> stream = Files.newDirectoryStream(root);
      if (!(stream instanceof SecureDirectoryStream<Path> secure)) {
        stream.close();
        throw new UnsafeDirectoryProviderException();
      }
      try {
        BasicFileAttributeView view = secure.getFileAttributeView(BasicFileAttributeView.class);
        if (view == null) throw new UnsafeDirectoryProviderException();
        BasicFileAttributes opened = view.readAttributes();
        if (opened.fileKey() == null || !before.fileKey().equals(opened.fileKey()))
          throw new RootChangedException();
        for (int attempt = 0; attempt < 8; attempt++) {
          String basename = UUID.randomUUID() + ".part";
          Path relative = Path.of(basename);
          try {
            SeekableByteChannel channel =
                secure.newByteChannel(
                    relative, Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE));
            return new SystemTempOutput(root.resolve(basename), channel, stream, secure);
          } catch (java.nio.file.FileAlreadyExistsException collision) {
            // Generate another random internal basename.
          }
        }
        throw new IOException("temporary name unavailable");
      } catch (RuntimeException | Error failure) {
        stream.close();
        throw failure;
      } catch (IOException failure) {
        stream.close();
        throw failure;
      }
    }

    private static void ensureDirectory(Path path) throws IOException {
      BasicFileAttributes attributes;
      try {
        attributes = Files.readAttributes(path, BasicFileAttributes.class, NOFOLLOW);
      } catch (java.nio.file.NoSuchFileException missing) {
        Files.createDirectory(path);
        attributes = Files.readAttributes(path, BasicFileAttributes.class, NOFOLLOW);
      }
      if (!attributes.isDirectory() || attributes.isSymbolicLink())
        throw new UnsafeDirectoryProviderException();
    }
  }

  private static final class SystemSnapshotDirectory implements SnapshotDirectory {
    private final DirectoryStream<Path> stream;
    private final SecureDirectoryStream<Path> secure;
    private final BasicFileAttributeView view;
    private final Object identity;

    private SystemSnapshotDirectory(
        DirectoryStream<Path> stream,
        SecureDirectoryStream<Path> secure,
        BasicFileAttributeView view,
        Object identity) {
      this.stream = stream;
      this.secure = secure;
      this.view = view;
      this.identity = identity;
    }

    @Override
    public SeekableByteChannel openNoFollow(Path relativeName) throws IOException {
      validateRelative(relativeName);
      validate();
      SeekableByteChannel channel = secure.newByteChannel(relativeName, READ_NOFOLLOW);
      try {
        validate();
        return channel;
      } catch (IOException failure) {
        channel.close();
        throw failure;
      }
    }

    @Override
    public BasicFileAttributes attributesNoFollow(Path relativeName) throws IOException {
      validateRelative(relativeName);
      validate();
      BasicFileAttributeView entryView =
          secure.getFileAttributeView(relativeName, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
      if (entryView == null) throw new IOException("attributes unavailable");
      return entryView.readAttributes();
    }

    @Override
    public void validate() throws IOException {
      BasicFileAttributes current = view.readAttributes();
      if (!current.isDirectory()
          || current.isSymbolicLink()) throw new RootChangedException();
      if (current.fileKey() == null) throw new UnsafeDirectoryProviderException();
      if (!identity.equals(current.fileKey())) throw new RootChangedException();
    }

    @Override
    public void close() throws IOException {
      stream.close();
    }

    private static void validateRelative(Path relativeName) throws IOException {
      if (relativeName == null || relativeName.isAbsolute() || relativeName.getNameCount() != 1)
        throw new IOException("invalid relative name");
      String text = relativeName.toString();
      if (text.equals(".") || text.equals("..") || text.contains("/") || text.contains("\\"))
        throw new IOException("invalid relative name");
    }
  }

  private static final class SystemTempOutput implements TempOutput {
    private final Path path;
    private final SeekableByteChannel channel;
    private final DirectoryStream<Path> stream;
    private final SecureDirectoryStream<Path> secure;
    private final AtomicBoolean closed = new AtomicBoolean();

    private SystemTempOutput(
        Path path,
        SeekableByteChannel channel,
        DirectoryStream<Path> stream,
        SecureDirectoryStream<Path> secure) {
      this.path = path;
      this.channel = channel;
      this.stream = stream;
      this.secure = secure;
    }

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
      try {
        secure.deleteFile(Path.of(path.getFileName().toString()));
      } catch (java.nio.file.NoSuchFileException ignored) {
        // Closing an already discarded snapshot is idempotent.
      }
    }

    @Override
    public void close() throws IOException {
      if (!closed.compareAndSet(false, true)) return;
      IOException failure = null;
      try {
        channel.close();
      } catch (IOException closeFailure) {
        failure = closeFailure;
      }
      try {
        stream.close();
      } catch (IOException closeFailure) {
        if (failure == null) failure = closeFailure;
      }
      if (failure != null) throw failure;
    }
  }
}
