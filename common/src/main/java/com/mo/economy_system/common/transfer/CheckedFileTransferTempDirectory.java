package com.mo.economy_system.common.transfer;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Secure handle for the fixed checked-file transfer temporary directory.
 *
 * <p>Files owned by this class are addressed by a single relative internal name. Creation,
 * deletion, reads, and moves never resolve that name through a replaceable absolute parent path.
 * The default provider fails closed unless every opened directory exposes a stable
 * {@link SecureDirectoryStream} identity.
 */
public final class CheckedFileTransferTempDirectory implements AutoCloseable {
  public static final String PROVIDER_UNSAFE = "TEMP_DIRECTORY_PROVIDER_UNSAFE";
  public static final String ROOT_DIRECTORY = "economy_system";
  public static final String TEMP_DIRECTORY = "transfer-temp";
  private static final int MAX_RANDOM_NAME_ATTEMPTS = 16;
  private static final Set<OpenOption> CREATE_NEW_READ_WRITE =
      Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.READ, StandardOpenOption.WRITE);
  private static final Set<OpenOption> READ_NOFOLLOW =
      Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
  private static final DirectoryProvider SYSTEM_PROVIDER = new SystemDirectoryProvider();

  private final Path gameDirectory;
  private final DirectoryProvider provider;
  private final DirectoryHandle handle;
  private int leases;
  private boolean closeRequested;
  private boolean handleClosed;

  private CheckedFileTransferTempDirectory(
      Path gameDirectory, DirectoryProvider provider, DirectoryHandle handle) {
    this.gameDirectory = gameDirectory;
    this.provider = provider;
    this.handle = handle;
  }

  /** Opens {@code <game>/economy_system/transfer-temp} using secure directory handles. */
  public static CheckedFileTransferTempDirectory open(Path gameDirectory) throws IOException {
    return open(gameDirectory, SYSTEM_PROVIDER);
  }

  static CheckedFileTransferTempDirectory open(
      Path gameDirectory, DirectoryProvider provider) throws IOException {
    Path game = normalizeGameDirectory(gameDirectory);
    DirectoryProvider checkedProvider = Objects.requireNonNull(provider, "directory provider");
    DirectoryHandle handle =
        checkedProvider.open(game, List.of(ROOT_DIRECTORY, TEMP_DIRECTORY));
    boolean accepted = false;
    try {
      handle.validate();
      Path expected = expectedPath(game);
      if (!handle.absolutePath().toAbsolutePath().normalize().equals(expected)) {
        throw new ProviderUnsafeException();
      }
      accepted = true;
      return new CheckedFileTransferTempDirectory(game, checkedProvider, handle);
    } finally {
      if (!accepted) handle.close();
    }
  }

  /** Opens a fixed temp root supplied by legacy callers after deriving its game directory. */
  public static CheckedFileTransferTempDirectory openFixedRoot(Path temporaryDirectory)
      throws IOException {
    Objects.requireNonNull(temporaryDirectory, "temporary directory");
    Path root = temporaryDirectory.toAbsolutePath().normalize();
    Path economy = root.getParent();
    Path game = economy == null ? null : economy.getParent();
    if (game == null || !root.equals(expectedPath(game))) {
      throw new ProviderUnsafeException();
    }
    return open(game);
  }

  public static Path expectedPath(Path gameDirectory) {
    Path game = normalizeGameDirectory(gameDirectory);
    return game.resolve(ROOT_DIRECTORY).resolve(TEMP_DIRECTORY).normalize();
  }

  public Path gameDirectory() {
    return gameDirectory;
  }

  /** Compatibility-only display path. It is never used to mutate an owned part. */
  public Path path() {
    return handle.absolutePath();
  }

  /** Creates a random UUID {@code .part} through the opened directory handle. */
  public synchronized OwnedFile createPart() throws IOException {
    ensureUsable();
    handle.validate();
    for (int attempt = 0; attempt < MAX_RANDOM_NAME_ATTEMPTS; attempt++) {
      Path relativeName = Path.of(UUID.randomUUID() + ".part");
      try {
        SeekableByteChannel channel = handle.newByteChannel(relativeName, CREATE_NEW_READ_WRITE);
        leases++;
        return new OwnedFile(this, relativeName, channel);
      } catch (FileAlreadyExistsException collision) {
        // An internal UUID collision is harmless; generate another name.
      }
    }
    throw new IOException("TEMP_NAME_EXHAUSTED");
  }

  synchronized OwnedFile adoptExisting(Path relativeName) throws IOException {
    ensureUsable();
    Path checkedName = validateRelative(relativeName);
    handle.validate();
    BasicFileAttributes attributes = handle.attributesNoFollow(checkedName);
    if (attributes.isSymbolicLink() || !attributes.isRegularFile()) {
      throw new IOException("TEMP_SOURCE_MISSING");
    }
    leases++;
    return new OwnedFile(this, checkedName, null);
  }

  synchronized SeekableByteChannel openRead(Path relativeName) throws IOException {
    ensureUsableForLease();
    handle.validate();
    return handle.newByteChannel(validateRelative(relativeName), READ_NOFOLLOW);
  }

  synchronized BasicFileAttributes attributesNoFollow(Path relativeName) throws IOException {
    ensureUsableForLease();
    handle.validate();
    return handle.attributesNoFollow(validateRelative(relativeName));
  }

  synchronized void delete(Path relativeName) throws IOException {
    ensureUsableForLease();
    handle.validate();
    try {
      handle.deleteFile(validateRelative(relativeName));
    } catch (NoSuchFileException ignored) {
      // Idempotent cleanup of an already removed owned file.
    }
    handle.validate();
  }

  synchronized void move(Path relativeName, DirectoryHandle target, Path targetName)
      throws IOException {
    ensureUsableForLease();
    handle.validate();
    target.validate();
    handle.move(validateRelative(relativeName), target, validateRelative(targetName));
    handle.validate();
    target.validate();
  }

  synchronized DirectoryHandle openTargetDirectory(UUID targetId) throws IOException {
    ensureUsableForLease();
    Objects.requireNonNull(targetId, "target id");
    return provider.open(
        gameDirectory,
        List.of(ROOT_DIRECTORY, "received-check-files", targetId.toString()));
  }

  private synchronized void releaseLease() throws IOException {
    if (leases <= 0) throw new IllegalStateException("temporary directory lease underflow");
    leases--;
    try {
      closeHandleIfUnused();
    } catch (IOException ignored) {
      // The file lease has been released; a later explicit close retries the directory handle.
    }
  }

  private void ensureUsable() throws IOException {
    if (closeRequested || handleClosed) throw new IOException("TEMP_DIRECTORY_CLOSED");
  }

  private void ensureUsableForLease() throws IOException {
    if (handleClosed) throw new IOException("TEMP_DIRECTORY_CLOSED");
  }

  private void closeHandleIfUnused() throws IOException {
    if (!handleClosed && closeRequested && leases == 0) {
      handle.close();
      handleClosed = true;
    }
  }

  @Override
  public synchronized void close() throws IOException {
    closeRequested = true;
    closeHandleIfUnused();
  }

  static Path normalizeGameDirectory(Path gameDirectory) {
    return Objects.requireNonNull(gameDirectory, "game directory").toAbsolutePath().normalize();
  }

  static Path validateRelative(Path relativeName) throws IOException {
    if (relativeName == null || relativeName.isAbsolute() || relativeName.getNameCount() != 1) {
      throw new IOException("INVALID_INTERNAL_NAME");
    }
    String text = relativeName.toString();
    if (text.isEmpty()
        || text.equals(".")
        || text.equals("..")
        || text.indexOf('/') >= 0
        || text.indexOf('\\') >= 0) {
      throw new IOException("INVALID_INTERNAL_NAME");
    }
    return relativeName;
  }

  /** One relative part owned by this directory until it is deleted or securely moved. */
  public static final class OwnedFile {
    private final CheckedFileTransferTempDirectory directory;
    private final Path relativeName;
    private SeekableByteChannel exactChannel;
    private boolean ownershipReleased;

    private OwnedFile(
        CheckedFileTransferTempDirectory directory,
        Path relativeName,
        SeekableByteChannel writeChannel) {
      this.directory = directory;
      this.relativeName = relativeName;
      this.exactChannel = writeChannel;
    }

    public Path relativeName() {
      return relativeName;
    }

    /** Compatibility-only display path. Mutations always use {@link #relativeName()}. */
    public Path path() {
      return directory.path().resolve(relativeName);
    }

    synchronized SeekableByteChannel writeChannel() throws IOException {
      ensureOwned();
      if (exactChannel == null || !exactChannel.isOpen()) {
        throw new IOException("TEMP_CHANNEL_CLOSED");
      }
      return exactChannel;
    }

    /** Returns the creation-time channel positioned at zero without resolving the pathname. */
    public synchronized SeekableByteChannel exactReadChannel() throws IOException {
      SeekableByteChannel channel = writeChannel();
      channel.position(0);
      return channel;
    }

    public synchronized void closeWriteChannel() throws IOException {
      if (exactChannel == null) return;
      SeekableByteChannel channel = exactChannel;
      exactChannel = null;
      channel.close();
    }

    public synchronized SeekableByteChannel openReadChannel() throws IOException {
      ensureOwned();
      return exactReadChannel();
    }

    synchronized BasicFileAttributes attributesNoFollow() throws IOException {
      ensureOwned();
      return directory.attributesNoFollow(relativeName);
    }

    /** Deletes by relative handle. A failure retains ownership so cleanup can be retried. */
    public synchronized boolean delete() throws IOException {
      if (ownershipReleased) return true;
      closeWriteChannel();
      directory.delete(relativeName);
      ownershipReleased = true;
      directory.releaseLease();
      return true;
    }

    synchronized void moveTo(DirectoryHandle target, Path targetName) throws IOException {
      ensureOwned();
      closeWriteChannel();
      directory.move(relativeName, target, targetName);
      ownershipReleased = true;
      directory.releaseLease();
    }

    synchronized boolean isOwned() {
      return !ownershipReleased;
    }

    CheckedFileTransferTempDirectory directory() {
      return directory;
    }

    private void ensureOwned() throws IOException {
      if (ownershipReleased) throw new IOException("TEMP_FILE_RELEASED");
    }
  }

  /** Stable protocol-facing failure for unsafe or unsupported directory providers. */
  public static class ProviderUnsafeException extends IOException {
    public ProviderUnsafeException() {
      super(PROVIDER_UNSAFE);
    }

    public ProviderUnsafeException(Throwable cause) {
      super(PROVIDER_UNSAFE, cause);
    }

    public String errorCode() {
      return PROVIDER_UNSAFE;
    }
  }

  interface DirectoryProvider {
    DirectoryHandle open(Path gameDirectory, List<String> children) throws IOException;
  }

  interface DirectoryHandle extends AutoCloseable {
    Path absolutePath();

    void validate() throws IOException;

    SeekableByteChannel newByteChannel(Path relativeName, Set<? extends OpenOption> options)
        throws IOException;

    BasicFileAttributes attributesNoFollow(Path relativeName) throws IOException;

    void deleteFile(Path relativeName) throws IOException;

    void move(Path sourceName, DirectoryHandle target, Path targetName) throws IOException;

    @Override
    void close() throws IOException;
  }

  private static final class SystemDirectoryProvider implements DirectoryProvider {
    private static final LinkOption[] NOFOLLOW = {LinkOption.NOFOLLOW_LINKS};

    @Override
    public DirectoryHandle open(Path gameDirectory, List<String> children) throws IOException {
      Path game = normalizeGameDirectory(gameDirectory);
      SystemDirectoryHandle current = openRoot(game);
      try {
        Path currentPath = game;
        for (String child : children) {
          Path relative = validateRelative(Path.of(Objects.requireNonNull(child, "child")));
          currentPath = currentPath.resolve(relative).normalize();
          if (!currentPath.startsWith(game)) throw new ProviderUnsafeException();
          ensureDirectory(currentPath);
          BasicFileAttributes before = safeDirectoryAttributes(currentPath);
          current.validate();
          SystemDirectoryHandle next = current.openChild(relative, currentPath, before.fileKey());
          current.close();
          current = next;
        }
        current.validate();
        return current;
      } catch (IOException | RuntimeException | Error failure) {
        try {
          current.close();
        } catch (IOException ignored) {
          // Preserve the operation failure.
        }
        throw failure;
      }
    }

    private static SystemDirectoryHandle openRoot(Path path) throws IOException {
      BasicFileAttributes before = safeDirectoryAttributes(path);
      DirectoryStream<Path> stream = Files.newDirectoryStream(path);
      try {
        if (!(stream instanceof SecureDirectoryStream<Path> secure)) {
          throw new ProviderUnsafeException();
        }
        return checkedHandle(path, stream, secure, before.fileKey());
      } catch (IOException | RuntimeException | Error failure) {
        stream.close();
        throw failure;
      }
    }

    private static SystemDirectoryHandle checkedHandle(
        Path path,
        DirectoryStream<Path> stream,
        SecureDirectoryStream<Path> secure,
        Object expectedIdentity)
        throws IOException {
      BasicFileAttributeView view = secure.getFileAttributeView(BasicFileAttributeView.class);
      if (view == null) throw new ProviderUnsafeException();
      BasicFileAttributes opened = view.readAttributes();
      requireSafeDirectory(opened);
      if (!expectedIdentity.equals(opened.fileKey())) throw new ProviderUnsafeException();
      return new SystemDirectoryHandle(path, stream, secure, view, expectedIdentity);
    }

    private static void ensureDirectory(Path path) throws IOException {
      try {
        safeDirectoryAttributes(path);
      } catch (NoSuchFileException missing) {
        try {
          Files.createDirectory(path);
        } catch (FileAlreadyExistsException raced) {
          // Inspect the entry created by the racing process below.
        }
        safeDirectoryAttributes(path);
      }
    }

    private static BasicFileAttributes safeDirectoryAttributes(Path path) throws IOException {
      BasicFileAttributes attributes =
          Files.readAttributes(path, BasicFileAttributes.class, NOFOLLOW);
      requireSafeDirectory(attributes);
      return attributes;
    }

    private static void requireSafeDirectory(BasicFileAttributes attributes) throws IOException {
      if (attributes == null
          || attributes.isSymbolicLink()
          || !attributes.isDirectory()
          || attributes.fileKey() == null) {
        throw new ProviderUnsafeException();
      }
    }
  }

  private static final class SystemDirectoryHandle implements DirectoryHandle {
    private final Path absolutePath;
    private final DirectoryStream<Path> stream;
    private final SecureDirectoryStream<Path> secure;
    private final BasicFileAttributeView selfView;
    private final Object identity;
    private final AtomicBoolean closed = new AtomicBoolean();

    private SystemDirectoryHandle(
        Path absolutePath,
        DirectoryStream<Path> stream,
        SecureDirectoryStream<Path> secure,
        BasicFileAttributeView selfView,
        Object identity) {
      this.absolutePath = absolutePath;
      this.stream = stream;
      this.secure = secure;
      this.selfView = selfView;
      this.identity = identity;
    }

    private SystemDirectoryHandle openChild(
        Path relativeName, Path childPath, Object expectedIdentity) throws IOException {
      validateRelative(relativeName);
      SecureDirectoryStream<Path> child =
          secure.newDirectoryStream(relativeName, LinkOption.NOFOLLOW_LINKS);
      try {
        return SystemDirectoryProvider.checkedHandle(
            childPath, child, child, expectedIdentity);
      } catch (IOException | RuntimeException | Error failure) {
        child.close();
        throw failure;
      }
    }

    @Override
    public Path absolutePath() {
      return absolutePath;
    }

    @Override
    public void validate() throws IOException {
      if (closed.get()) throw new IOException("DIRECTORY_HANDLE_CLOSED");
      BasicFileAttributes current = selfView.readAttributes();
      SystemDirectoryProvider.requireSafeDirectory(current);
      if (!identity.equals(current.fileKey())) throw new ProviderUnsafeException();
    }

    @Override
    public SeekableByteChannel newByteChannel(
        Path relativeName, Set<? extends OpenOption> options) throws IOException {
      validateRelative(relativeName);
      validate();
      try {
        return secure.newByteChannel(relativeName, options);
      } catch (UnsupportedOperationException unsupported) {
        throw new ProviderUnsafeException(unsupported);
      }
    }

    @Override
    public BasicFileAttributes attributesNoFollow(Path relativeName) throws IOException {
      validateRelative(relativeName);
      validate();
      BasicFileAttributeView view =
          secure.getFileAttributeView(
              relativeName, BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
      if (view == null) throw new ProviderUnsafeException();
      return view.readAttributes();
    }

    @Override
    public void deleteFile(Path relativeName) throws IOException {
      validateRelative(relativeName);
      validate();
      try {
        secure.deleteFile(relativeName);
      } catch (UnsupportedOperationException unsupported) {
        throw new ProviderUnsafeException(unsupported);
      }
    }

    @Override
    public void move(Path sourceName, DirectoryHandle target, Path targetName)
        throws IOException {
      validateRelative(sourceName);
      validateRelative(targetName);
      validate();
      if (!(target instanceof SystemDirectoryHandle systemTarget)) {
        throw new ProviderUnsafeException();
      }
      systemTarget.validate();
      try {
        secure.move(sourceName, systemTarget.secure, targetName);
      } catch (UnsupportedOperationException unsupported) {
        throw new ProviderUnsafeException(unsupported);
      }
    }

    @Override
    public void close() throws IOException {
      if (closed.compareAndSet(false, true)) stream.close();
    }
  }
}
