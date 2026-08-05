package com.mo.economy_system.common.check;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Owns one bounded worker for one client connection generation. */
public final class ClientFileCheckTaskCoordinator implements AutoCloseable {
  public enum TaskState {
    RUNNING,
    CALLBACK_QUEUED,
    COMPLETED,
    FAILED,
    CANCELLED,
    DISPATCH_FAILED
  }
  public record Session(long generation, Object connectionIdentity, UUID localPlayerId) {
    public Session {
      if (generation <= 0) throw new IllegalArgumentException("generation");
      Objects.requireNonNull(connectionIdentity, "connectionIdentity");
      Objects.requireNonNull(localPlayerId, "localPlayerId");
    }
  }

  public record RequestIdentity(
      UUID targetPlayerId, UUID requesterPlayerId, ClientFileCheckType checkType) {
    public RequestIdentity {
      Objects.requireNonNull(targetPlayerId, "targetPlayerId");
      Objects.requireNonNull(requesterPlayerId, "requesterPlayerId");
      Objects.requireNonNull(checkType, "checkType");
    }
  }

  public final class TaskToken {
    private final Session session;
    private final RequestIdentity request;
    private final long controllerGeneration;
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private volatile TaskState state = TaskState.RUNNING;

    private TaskToken(Session session, RequestIdentity request, long controllerGeneration) {
      this.session = session;
      this.request = request;
      this.controllerGeneration = controllerGeneration;
    }

    public Session session() {
      return session;
    }

    public RequestIdentity request() {
      return request;
    }

    public long controllerGeneration() {
      return controllerGeneration;
    }

    public void cancel() {
      cancelled.set(true);
      if (!terminal(state)) state = TaskState.CANCELLED;
    }

    public boolean cancelled() {
      return cancelled.get();
    }

    public TaskState state() { return state; }

    private void state(TaskState next) { state = next; }
  }

  private long generation;
  private Session current;
  private ClientFileCheckExecutor executor;
  private boolean closed;

  public synchronized Session beginSession(Object connectionIdentity, UUID localPlayerId) {
    if (closed) throw new IllegalStateException("coordinator closed");
    invalidateSession();
    generation = generation == Long.MAX_VALUE ? 1 : generation + 1;
    executor = new ClientFileCheckExecutor();
    current = new Session(generation, connectionIdentity, localPlayerId);
    return current;
  }

  public synchronized Session currentSession() {
    return current;
  }

  public synchronized void invalidateSession() {
    current = null;
    if (executor != null) {
      executor.close();
      executor = null;
    }
  }

  public synchronized <T> TaskToken submit(
      Session session,
      RequestIdentity request,
      long controllerGeneration,
      Supplier<T> task,
      Consumer<Runnable> mainThread,
      Predicate<TaskToken> acceptance,
      BiConsumer<TaskToken, T> completion,
      BiConsumer<TaskToken, RuntimeException> failure,
      BiConsumer<TaskToken, Throwable> abandonment) {
    Objects.requireNonNull(task, "task");
    Objects.requireNonNull(mainThread, "mainThread");
    Objects.requireNonNull(acceptance, "acceptance");
    Objects.requireNonNull(completion, "completion");
    Objects.requireNonNull(failure, "failure");
    Objects.requireNonNull(abandonment, "abandonment");
    if (!isCurrent(session) || executor == null) return null;
    TaskToken token = new TaskToken(session, request, controllerGeneration);
    boolean accepted =
        executor.submit(
            () -> {
              try {
                T value = task.get();
                schedule(
                    token,
                    acceptance,
                    mainThread,
                    () -> completion.accept(token, value),
                    TaskState.COMPLETED,
                    abandonment);
              } catch (RuntimeException taskFailure) {
                schedule(
                    token,
                    acceptance,
                    mainThread,
                    () -> failure.accept(token, taskFailure),
                    TaskState.FAILED,
                    abandonment);
              } catch (Error fatal) {
                if (!terminal(token.state())) {
                  token.state(TaskState.FAILED);
                  abandon(token, fatal, abandonment);
                }
                throw fatal;
              }
            });
    if (!accepted) token.cancel();
    return accepted ? token : null;
  }

  private void schedule(
      TaskToken token,
      Predicate<TaskToken> acceptance,
      Consumer<Runnable> mainThread,
      Runnable callback,
      TaskState terminalState,
      BiConsumer<TaskToken, Throwable> abandonment) {
    if (!isAccepted(token, acceptance)) {
      token.cancel();
      return;
    }
    token.state(TaskState.CALLBACK_QUEUED);
    try {
      mainThread.accept(() -> runCallback(
          token, acceptance, callback, terminalState, abandonment));
    } catch (RuntimeException schedulingFailure) {
      token.state(TaskState.DISPATCH_FAILED);
      abandon(token, schedulingFailure, abandonment);
    }
  }

  private void runCallback(
      TaskToken token,
      Predicate<TaskToken> acceptance,
      Runnable callback,
      TaskState terminalState,
      BiConsumer<TaskToken, Throwable> abandonment) {
    if (!isAccepted(token, acceptance)) {
      token.cancel();
      return;
    }
    try {
      callback.run();
      token.state(terminalState);
    } catch (RuntimeException callbackFailure) {
      token.state(TaskState.FAILED);
      abandon(token, callbackFailure, abandonment);
    } catch (Error fatal) {
      token.state(TaskState.FAILED);
      abandon(token, fatal, abandonment);
      throw fatal;
    }
  }

  private void abandon(
      TaskToken token,
      Throwable failure,
      BiConsumer<TaskToken, Throwable> abandonment) {
    try {
      abandonment.accept(token, failure);
    } catch (RuntimeException ignored) {
      // Abandonment is restricted to thread-safe common state.
    }
  }

  private static boolean terminal(TaskState state) {
    return state == TaskState.COMPLETED
        || state == TaskState.FAILED
        || state == TaskState.CANCELLED
        || state == TaskState.DISPATCH_FAILED;
  }

  public synchronized boolean isCurrent(Session session) {
    return !closed && current != null && current.equals(session);
  }

  private boolean isAccepted(TaskToken token, Predicate<TaskToken> acceptance) {
    return !token.cancelled() && isCurrent(token.session()) && acceptance.test(token);
  }

  @Override
  public synchronized void close() {
    if (closed) return;
    invalidateSession();
    closed = true;
  }
}
