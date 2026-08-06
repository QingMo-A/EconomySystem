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

    /** Returns the creation-time channel positioned for exact reading. */
    default SeekableByteChannel exactReadChannel() throws IOException {
      SeekableByteChannel exact = channel();
      exact.position(0);
      return exact;
    }

    /** Deletes the owned path without following a replaced temp-root ancestor. */
    void delete() throws IOException;

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
      this(compatibilityOutput(path), size, sha256);
    }

    private Snapshot(TempOutput output, long size, String sha256) {
      this(output.path(), size, sha256, new SystemFileAccess(), Reservation.NONE, output);
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

    /** Opens the exact creation-time snapshot handle; callers must not close it. */
    public synchronized SeekableByteChannel openExactReadChannel() throws IOException {
      if (closed.get() || output == null) throw new IOException("snapshot closed");
      SeekableByteChannel channel = output.exactReadChannel();
      if (!channel.isOpen() || channel.size() != size) throw new IOException("snapshot changed");
      return channel;
    }

    @Override
    public synchronized void close() throws IOException {
      if (closed.get()) return;
      IOException deletionFailure = null;
      boolean deleted = false;
      try {
        if (output == null) throw new IOException("unmanaged snapshot");
        output.delete();
        deleted = true;
      } catch (IOException | RuntimeException failure) {
        deletionFailure = new IOException("snapshot cleanup failed");
      } finally {
        if (output != null) closeQuietly(output);
        if (deleted) {
          reservation.release();
          closed.set(true);
        }
      }
      if (deletionFailure != null) throw deletionFailure;
    }

    private static TempOutput compatibilityOutput(Path path) {
      Objects.requireNonNull(path, "snapshot path");
      Path absolute = path.toAbsolutePath().normalize();
      Path parent = absolute.getParent();
      Path name = absolute.getFileName();
      if (parent == null || name == null) throw new IllegalArgumentException("snapshot path");
      CheckedFileTransferTempDirectory directory = null;
      try {
        directory = CheckedFileTransferTempDirectory.openFixedRoot(parent);
        CheckedFileTransferTempDirectory.OwnedFile file = directory.adoptExisting(name);
        return new DirectoryTempOutput(file, directory, true);
      } catch (IOException failure) {
        if (directory != null) closeQuietly(directory);
        throw new IllegalArgumentException("secure snapshot path required");
      }
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

  /** Creates a snapshot using a caller-owned secure temp-directory handle. */
  public static Outcome create(
      Path gameDirectory,
      ClientFileCheckType type,
      String rawName,
      long expectedSize,
      String expectedHash,
      CheckedFileTransferTempDirectory tempDirectory,
      long deadlineNanos,
      CheckedFileTransferTempBudget tempBudget) {
    Objects.requireNonNull(tempDirectory, "temp directory");
    return new CheckedFileSnapshotter(SYSTEM_CLOCK, SYSTEM, tempBudget)
        .snapshot(
            gameDirectory,
            type,
            rawName,
            expectedSize,
            expectedHash,
            tempDirectory,
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
      outputOwned = true;
      outputPath = output.path();
      if (outputPath == null) return failure("SNAPSHOT_FAILED");
      if (!isPrivateTempFile(tempRoot, outputPath))
        return failure("DIRECTORY_PROVIDER_UNSAFE");
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
    } catch (CheckedFileTransferTempDirectory.ProviderUnsafeException unsafe) {
      return failure("TEMP_DIRECTORY_PROVIDER_UNSAFE");
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
      boolean cleaned = true;
      if (output != null) {
        if (outputOwned) cleaned = deleteQuietly(output);
        closeQuietly(output);
      }
      if (!completed && cleaned) reservation.release();
    }
  }

  /** Runs a snapshot while retaining the caller's shared temp-directory lifecycle. */
  public Outcome snapshot(
      Path gameDirectory,
      ClientFileCheckType type,
      String rawName,
      long expectedSize,
      String expectedHash,
      CheckedFileTransferTempDirectory tempDirectory,
      long deadlineNanos) {
    Objects.requireNonNull(tempDirectory, "temp directory");
    FileAccess sharedAccess =
        new FileAccess() {
          @Override
          public boolean existsNoFollow(Path root) {
            return access.existsNoFollow(root);
          }

          @Override
          public SnapshotDirectory open(Path game, Path root) throws IOException {
            return access.open(game, root);
          }

          @Override
          public TempOutput openTemp(Path ignoredRoot) throws IOException {
            return new DirectoryTempOutput(tempDirectory.createPart(), tempDirectory, false);
          }
        };
    return new CheckedFileSnapshotter(clock, sharedAccess, tempBudget)
        .snapshot(
            gameDirectory,
            type,
            rawName,
            expectedSize,
            expectedHash,
            tempDirectory.path(),
            deadlineNanos);
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

  private boolean deleteQuietly(TempOutput output) {
    try {
      output.delete();
      return true;
    } catch (IOException | RuntimeException ignored) {
      // Do not disclose path/provider details in a protocol error.
      return false;
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
      if (game == null) throw new CheckedFileTransferTempDirectory.ProviderUnsafeException();
      CheckedFileTransferTempDirectory directory =
          CheckedFileTransferTempDirectory.openFixedRoot(root);
      try {
        return new DirectoryTempOutput(directory.createPart(), directory, true);
      } catch (IOException | RuntimeException failure) {
        closeQuietly(directory);
        throw failure;
      }
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

  private static final class DirectoryTempOutput implements TempOutput {
    private final CheckedFileTransferTempDirectory.OwnedFile file;
    private final CheckedFileTransferTempDirectory directory;
    private final boolean closeDirectory;
    private final AtomicBoolean closed = new AtomicBoolean();

    private DirectoryTempOutput(
        CheckedFileTransferTempDirectory.OwnedFile file,
        CheckedFileTransferTempDirectory directory,
        boolean closeDirectory) {
      this.file = file;
      this.directory = directory;
      this.closeDirectory = closeDirectory;
    }

    @Override
    public Path path() {
      return file.path();
    }

    @Override
    public SeekableByteChannel channel() {
      try {
        return file.writeChannel();
      } catch (IOException failure) {
        throw new IllegalStateException("temporary channel unavailable", failure);
      }
    }

    @Override
    public SeekableByteChannel exactReadChannel() throws IOException {
      return file.exactReadChannel();
    }

    @Override
    public void delete() throws IOException {
      file.delete();
    }

    @Override
    public void close() throws IOException {
      if (!closed.compareAndSet(false, true)) return;
      IOException failure = null;
      try {
        file.closeWriteChannel();
      } catch (IOException closeFailure) {
        failure = closeFailure;
      }
      if (closeDirectory) {
        try {
          directory.close();
        } catch (IOException closeFailure) {
          if (failure == null) failure = closeFailure;
        }
      }
      if (failure != null) throw failure;
    }
  }
}
