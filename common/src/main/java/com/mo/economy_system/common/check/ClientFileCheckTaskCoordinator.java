package com.mo.economy_system.common.check;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Owns one bounded worker for one client connection generation. */
public final class ClientFileCheckTaskCoordinator implements AutoCloseable {
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
    }

    public boolean cancelled() {
      return cancelled.get();
    }
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
      Consumer<T> completion) {
    Objects.requireNonNull(task, "task");
    Objects.requireNonNull(mainThread, "mainThread");
    Objects.requireNonNull(acceptance, "acceptance");
    Objects.requireNonNull(completion, "completion");
    if (!isCurrent(session) || executor == null) return null;
    TaskToken token = new TaskToken(session, request, controllerGeneration);
    boolean accepted =
        executor.submit(
            () -> {
              T value = task.get();
              if (!isAccepted(token, acceptance)) return;
              mainThread.accept(
                  () -> {
                    if (isAccepted(token, acceptance)) completion.accept(value);
                  });
            });
    return accepted ? token : null;
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
