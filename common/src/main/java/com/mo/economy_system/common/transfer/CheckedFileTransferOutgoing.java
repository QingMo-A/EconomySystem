package com.mo.economy_system.common.transfer;

import com.mo.economy_system.common.check.ClientFileCheckTaskCoordinator;
import com.mo.economy_system.common.network.CheckedFileTransferChunkRequestMessage;
import com.mo.economy_system.common.network.CheckedFileTransferControlRequestMessage;
import com.mo.economy_system.common.network.CheckedFileTransferRequestMessage;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Connection-bound outgoing checked-file transfer state.
 *
 * <p>The loader layer owns consent widgets and supplies a snapshot operation and a
 * session-aware sender. This class owns all worker, token, timeout and terminal cleanup
 * decisions. A legacy static {@link #send} entry point is retained for old adapters while
 * those adapters are migrated to {@link #allow}.
 */
public final class CheckedFileTransferOutgoing implements AutoCloseable {
  public enum State {
    IDLE,
    CONSENT,
    SNAPSHOTTING,
    SENDING_READY,
    STREAMING,
    FINISHED
  }

  public enum BeginResult {
    OPEN,
    DUPLICATE,
    CONSENT_BUSY,
    INVALID_SESSION,
    CLOSED
  }

  /** A worker callback that creates one private snapshot. */
  @FunctionalInterface
  public interface SnapshotTask {
    CheckedFileSnapshotter.Outcome snapshot(long deadlineNanos) throws Exception;
  }

  /** A sender that is only called after this class has validated its session and token. */
  @FunctionalInterface
  public interface SessionSender {
    void send(
        ClientFileCheckTaskCoordinator.Session session, WorkerToken token, Object message)
        throws Exception;
  }

  /** Immutable identity passed to every worker callback and dispatch operation. */
  public record WorkerToken(long generation, UUID requestId) {
    public WorkerToken {
      if (generation <= 0) throw new IllegalArgumentException("generation");
      Objects.requireNonNull(requestId, "request id");
    }
  }

  public record Active(
      CheckedFileTransferRequestMessage request,
      ClientFileCheckTaskCoordinator.Session session,
      WorkerToken token,
      State state,
      long deadlineNanos) {
    public Active {
      Objects.requireNonNull(request, "request");
      Objects.requireNonNull(session, "session");
      Objects.requireNonNull(token, "token");
      Objects.requireNonNull(state, "state");
      if (state == State.IDLE) {
        throw new IllegalArgumentException("active state");
      }
    }
  }

  private static final long DEFAULT_TIMEOUT_NANOS = 60_000_000_000L;

  private final CheckedFileTransferTempBudget tempBudget;
  private final long timeoutNanos;
  private ThreadPoolExecutor worker;
  private long tokenSequence;
  private Active active;
  private CheckedFileSnapshotter.Snapshot retainedSnapshot;
  private boolean closed;

  public CheckedFileTransferOutgoing() {
    this(new CheckedFileTransferTempBudget(), DEFAULT_TIMEOUT_NANOS);
  }

  public CheckedFileTransferOutgoing(CheckedFileTransferTempBudget tempBudget) {
    this(tempBudget, DEFAULT_TIMEOUT_NANOS);
  }

  public CheckedFileTransferOutgoing(CheckedFileTransferTempBudget tempBudget, long timeoutNanos) {
    this.tempBudget = Objects.requireNonNull(tempBudget, "temp budget");
    if (timeoutNanos <= 0) throw new IllegalArgumentException("timeout");
    this.timeoutNanos = timeoutNanos;
    worker = newWorker();
  }

  private static ThreadPoolExecutor newWorker() {
    return new ThreadPoolExecutor(
            1,
            1,
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(1),
            runnable -> {
              Thread thread = new Thread(runnable, "economy-file-transfer-outgoing");
              thread.setDaemon(true);
              return thread;
            },
            new ThreadPoolExecutor.AbortPolicy());
  }

  /** Opens consent for a request. Exact duplicate requests are deliberately idempotent. */
  public synchronized BeginResult receive(
      CheckedFileTransferRequestMessage request,
      ClientFileCheckTaskCoordinator.Session session) {
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(session, "session");
    if (closed) return BeginResult.CLOSED;
    if (active == null) {
      active =
          new Active(
              request,
              session,
              new WorkerToken(session.generation(), nextRequestId()),
              State.CONSENT,
              safeDeadline(System.nanoTime()));
      return BeginResult.OPEN;
    }
    if (active.request().equals(request) && sameSession(active.session(), session)) {
      return BeginResult.DUPLICATE;
    }
    return BeginResult.CONSENT_BUSY;
  }

  /** Alias that makes the consent operation read naturally in loader adapters. */
  public BeginResult beginConsent(
      CheckedFileTransferRequestMessage request,
      ClientFileCheckTaskCoordinator.Session session) {
    return receive(request, session);
  }

  /** Returns the current active operation, or {@code null} after terminal cleanup. */
  public synchronized Active active() {
    return active;
  }

  public synchronized State state() {
    return active == null ? State.IDLE : active.state();
  }

  /** Shared reservation ledger used by snapshot tasks supplied by the loader adapter. */
  public CheckedFileTransferTempBudget tempBudget() {
    return tempBudget;
  }

  /**
   * Accepts consent and queues exactly one snapshot worker. The token is created before queueing
   * and is passed to every callback, preventing a late callback from crossing a reconnect.
   */
  public boolean allow(
      CheckedFileTransferRequestMessage request,
      ClientFileCheckTaskCoordinator.Session session,
      SnapshotTask snapshotTask,
      SessionSender sender) {
    return allow(request, session, snapshotTask, sender, System.nanoTime());
  }

  public synchronized boolean allow(
      CheckedFileTransferRequestMessage request,
      ClientFileCheckTaskCoordinator.Session session,
      SnapshotTask snapshotTask,
      SessionSender sender,
      long nowNanos) {
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(session, "session");
    Objects.requireNonNull(snapshotTask, "snapshot task");
    Objects.requireNonNull(sender, "sender");
    if (closed || active == null || !active.request().equals(request)
        || !sameSession(active.session(), session) || active.state() != State.CONSENT) {
      return false;
    }
    Active operation = replaceState(State.SNAPSHOTTING, safeDeadline(nowNanos));
    try {
      worker.execute(() -> run(operation, snapshotTask, sender));
      return true;
    } catch (RejectedExecutionException rejected) {
      finish(operation.token());
      return false;
    }
  }

  /** Sends a one-shot DECLINED control through the same session-aware validation gate. */
  public boolean decline(
      CheckedFileTransferRequestMessage request,
      ClientFileCheckTaskCoordinator.Session session,
      SessionSender sender) {
    Objects.requireNonNull(sender, "sender");
    Active operation;
    synchronized (this) {
      if (closed || active == null || !active.request().equals(request)
          || !sameSession(active.session(), session) || active.state() != State.CONSENT) {
        return false;
      }
      operation = active;
    }
    boolean delivered =
        dispatch(
            operation,
            sender,
            control(operation.request(), CheckedFileTransferControl.error(
                CheckedFileTransferControlStatus.DECLINED, "DECLINED")));
    finish(operation.token());
    return delivered;
  }

  /** Cancels all state for a reconnect/logout. No callback may dispatch to the old session. */
  public synchronized void invalidateSession() {
    if (active != null) finishLocked(active.token());
    worker.getQueue().clear();
    worker.purge();
    worker.shutdownNow();
    if (!closed) worker = newWorker();
  }

  /** Expires consent or an in-flight operation after the configured local deadline. */
  public synchronized boolean tick(long nowNanos) {
    if (active == null || nowNanos < active.deadlineNanos()) return false;
    finishLocked(active.token());
    worker.getQueue().clear();
    worker.purge();
    worker.shutdownNow();
    if (!closed) worker = newWorker();
    return true;
  }

  private void run(Active operation, SnapshotTask task, SessionSender sender) {
    CheckedFileSnapshotter.Snapshot snapshot = null;
    try {
      CheckedFileSnapshotter.Outcome outcome = task.snapshot(operation.deadlineNanos());
      if (outcome == null || !outcome.success()) {
        sendFailure(operation, sender, outcome == null ? "SNAPSHOT_FAILED" : outcome.errorCode());
        return;
      }
      snapshot = outcome.snapshot();
      synchronized (this) {
        if (!isCurrentLocked(operation.token())) return;
        retainedSnapshot = snapshot;
        active =
            new Active(
                operation.request(),
                operation.session(),
                operation.token(),
                State.SENDING_READY,
                operation.deadlineNanos());
      }
      CheckedFileTransferOutgoing.send(
          operation.request(),
          snapshot,
          () -> valid(operation.token()),
          message -> dispatchOrThrow(operation, sender, message));
      synchronized (this) {
        if (isCurrentLocked(operation.token())) {
          active =
              new Active(
                  operation.request(),
                  operation.session(),
                  operation.token(),
                  State.FINISHED,
                  operation.deadlineNanos());
        }
      }
    } catch (Exception failure) {
      sendFailure(operation, sender, "TRANSFER_INTERRUPTED");
    } catch (Error fatal) {
      finish(operation.token());
      throw fatal;
    } finally {
      if (snapshot != null) {
        try {
          snapshot.close();
        } catch (IOException | RuntimeException ignored) {
          // A terminal transfer must not keep worker state alive because cleanup failed.
        }
      }
      synchronized (this) {
        if (retainedSnapshot == snapshot) retainedSnapshot = null;
        if (active != null && active.token().equals(operation.token())) finishLocked(operation.token());
      }
    }
  }

  private void sendFailure(Active operation, SessionSender sender, String code) {
    try {
      dispatch(
          operation,
          sender,
          control(operation.request(), CheckedFileTransferControl.error(
              CheckedFileTransferControlStatus.FAILED,
              code == null ? "TRANSFER_INTERRUPTED" : code)));
    } finally {
      finish(operation.token());
    }
  }

  private void dispatchOrThrow(Active operation, SessionSender sender, Object message) {
    if (!dispatch(operation, sender, message)) throw new IllegalStateException("session invalid");
  }

  private boolean dispatch(Active operation, SessionSender sender, Object message) {
    if (!valid(operation.token())) return false;
    try {
      synchronized (this) {
        if (active != null && active.token().equals(operation.token())
            && active.state() == State.SENDING_READY) {
          active =
              new Active(operation.request(), operation.session(), operation.token(), State.STREAMING,
                  operation.deadlineNanos());
        }
      }
      if (!valid(operation.token())) return false;
      sender.send(operation.session(), operation.token(), message);
      return true;
    } catch (RuntimeException | Error failure) {
      finish(operation.token());
      if (failure instanceof Error error) throw error;
      return false;
    } catch (Exception failure) {
      finish(operation.token());
      return false;
    }
  }

  private synchronized Active replaceState(State next, long deadline) {
    Active previous = active;
    active = new Active(previous.request(), previous.session(), previous.token(), next, deadline);
    return active;
  }

  private boolean valid(WorkerToken token) {
    synchronized (this) {
      return !closed && isCurrentLocked(token);
    }
  }

  private boolean isCurrentLocked(WorkerToken token) {
    return active != null && active.token().equals(token)
        && sameSession(active.session(), sessionOf(active));
  }

  private static ClientFileCheckTaskCoordinator.Session sessionOf(Active value) {
    return value.session();
  }

  private synchronized void finish(WorkerToken token) {
    finishLocked(token);
  }

  private void finishLocked(WorkerToken token) {
    if (active == null || !active.token().equals(token)) return;
    active = null;
    retainedSnapshot = null;
  }

  private long safeDeadline(long now) {
    return now > Long.MAX_VALUE - timeoutNanos ? Long.MAX_VALUE : now + timeoutNanos;
  }

  private UUID nextRequestId() {
    tokenSequence = tokenSequence == Long.MAX_VALUE ? 1 : tokenSequence + 1;
    return new UUID(0L, tokenSequence);
  }

  private static boolean sameSession(
      ClientFileCheckTaskCoordinator.Session left,
      ClientFileCheckTaskCoordinator.Session right) {
    return left.generation() == right.generation()
        && left.connectionIdentity() == right.connectionIdentity()
        && left.localPlayerId().equals(right.localPlayerId());
  }

  /** Compatibility helper retained for the old loader screens. */
  public static void send(
      CheckedFileTransferRequestMessage request,
      CheckedFileSnapshotter.Snapshot snapshot,
      BooleanSupplier valid,
      java.util.function.Consumer<Object> sender)
      throws IOException {
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(snapshot, "snapshot");
    Objects.requireNonNull(valid, "valid");
    Objects.requireNonNull(sender, "sender");
    UUID transferId = UUID.randomUUID();
    int total =
        CheckedFileTransferValidation.totalChunks(
            snapshot.size(), EconomyNetworkLimits.TRANSFER_RAW_CHUNK_BYTES);
    // Check before READY. A late screen callback must never emit a new transfer.
    if (!valid.getAsBoolean()) return;
    sender.accept(control(request, CheckedFileTransferControl.ready(transferId, snapshot.size(), snapshot.sha256())));
    try (InputStream input = Files.newInputStream(snapshot.path(), StandardOpenOption.READ)) {
      byte[] buffer = new byte[EconomyNetworkLimits.TRANSFER_RAW_CHUNK_BYTES];
      for (int index = 0; index < total; index++) {
        int expected =
            (int)
                Math.min(buffer.length, snapshot.size() - (long) index * buffer.length);
        int offset = 0;
        while (offset < expected) {
          int read = input.read(buffer, offset, expected - offset);
          if (read < 0) throw new EOFException("snapshot ended early");
          offset += read;
        }
        if (!valid.getAsBoolean()) return;
        byte[] raw = expected == buffer.length ? buffer : Arrays.copyOf(buffer, expected);
        String encoded =
            new String(Base64.getEncoder().encode(raw), StandardCharsets.US_ASCII);
        if (!valid.getAsBoolean()) return;
        sender.accept(
            new CheckedFileTransferChunkRequestMessage(
                request.targetPlayerName(),
                request.targetPlayerId(),
                request.requesterPlayerName(),
                request.requesterPlayerId(),
                request.checkType(),
                request.fileName(),
                transferId,
                index,
                total,
                encoded));
      }
      if (input.read() != -1) throw new IOException("snapshot changed");
    }
  }

  public static CheckedFileTransferControlRequestMessage control(
      CheckedFileTransferRequestMessage request, CheckedFileTransferControl control) {
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(control, "control");
    return new CheckedFileTransferControlRequestMessage(
        request.targetPlayerName(),
        request.targetPlayerId(),
        request.requesterPlayerName(),
        request.requesterPlayerId(),
        request.checkType(),
        request.fileName(),
        CheckedFileTransferControlJsonCodec.encode(control));
  }

  @Override
  public synchronized void close() {
    if (closed) return;
    closed = true;
    if (active != null) finishLocked(active.token());
    worker.shutdownNow();
  }
}
