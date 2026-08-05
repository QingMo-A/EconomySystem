package com.mo.economy_system.common.check;

import java.util.Objects;
import java.util.UUID;

/** Connection-bound state machine for one consent/check transaction. */
public final class ClientFileCheckConsentCoordinator {
  public enum State {
    IDLE,
    CONSENT,
    SCANNING,
    SENDING
  }

  public enum Decision {
    OPEN,
    DUPLICATE,
    BUSY
  }

  public record Active(
      ClientFileCheckTaskCoordinator.RequestIdentity request,
      long sessionGeneration,
      Object connectionIdentity,
      UUID localPlayerId,
      State state) {
    public Active {
      Objects.requireNonNull(request, "request");
      if (sessionGeneration <= 0) throw new IllegalArgumentException("sessionGeneration");
      Objects.requireNonNull(connectionIdentity, "connectionIdentity");
      Objects.requireNonNull(localPlayerId, "localPlayerId");
      if (state == State.IDLE) throw new IllegalArgumentException("active state");
      Objects.requireNonNull(state, "state");
    }

    boolean sameSession(ClientFileCheckTaskCoordinator.Session session) {
      return session != null
          && sessionGeneration == session.generation()
          && connectionIdentity == session.connectionIdentity()
          && localPlayerId.equals(session.localPlayerId());
    }
  }

  private Active active;

  public synchronized Decision receive(
      ClientFileCheckTaskCoordinator.RequestIdentity request,
      ClientFileCheckTaskCoordinator.Session session) {
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(session, "session");
    if (active == null) {
      active =
          new Active(
              request,
              session.generation(),
              session.connectionIdentity(),
              session.localPlayerId(),
              State.CONSENT);
      return Decision.OPEN;
    }
    return active.request().equals(request) && active.sameSession(session)
        ? Decision.DUPLICATE
        : Decision.BUSY;
  }

  public synchronized boolean transition(
      ClientFileCheckTaskCoordinator.RequestIdentity request,
      ClientFileCheckTaskCoordinator.Session session,
      State expected,
      State next) {
    if (next == State.IDLE) throw new IllegalArgumentException("use finish");
    if (!matches(request, session) || active.state() != expected) return false;
    active =
        new Active(
            request,
            session.generation(),
            session.connectionIdentity(),
            session.localPlayerId(),
            next);
    return true;
  }

  public synchronized boolean held(
      ClientFileCheckTaskCoordinator.RequestIdentity request,
      ClientFileCheckTaskCoordinator.Session session) {
    return matches(request, session);
  }

  public synchronized boolean beginSending(
      ClientFileCheckTaskCoordinator.RequestIdentity request,
      ClientFileCheckTaskCoordinator.Session session) {
    if (!matches(request, session) || active.state() == State.SENDING) return false;
    active =
        new Active(
            request,
            session.generation(),
            session.connectionIdentity(),
            session.localPlayerId(),
            State.SENDING);
    return true;
  }

  public synchronized boolean busyFor(
      ClientFileCheckTaskCoordinator.RequestIdentity request,
      ClientFileCheckTaskCoordinator.Session session) {
    return active != null && active.sameSession(session) && !active.request().equals(request);
  }

  public synchronized boolean finish(
      ClientFileCheckTaskCoordinator.RequestIdentity request,
      ClientFileCheckTaskCoordinator.Session session) {
    if (!matches(request, session)) return false;
    active = null;
    return true;
  }

  public synchronized void invalidate() {
    active = null;
  }

  public synchronized State state() {
    return active == null ? State.IDLE : active.state();
  }

  public synchronized Active active() {
    return active;
  }

  private boolean matches(
      ClientFileCheckTaskCoordinator.RequestIdentity request,
      ClientFileCheckTaskCoordinator.Session session) {
    return active != null && active.request().equals(request) && active.sameSession(session);
  }
}
