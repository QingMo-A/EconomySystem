package com.mo.economy_system.common.check;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** Shared session and consent validation for every client protocol-24 send. */
public final class ClientFileCheckClientResultDispatcher {
  @FunctionalInterface
  public interface Sender {
    void send(ClientFileCheckResult result);
  }

  @FunctionalInterface
  public interface Diagnostics {
    void record(
        String stage, ClientFileCheckTaskCoordinator.RequestIdentity request, Throwable failure);
  }

  private ClientFileCheckClientResultDispatcher() {}

  public static boolean terminal(
      ClientFileCheckTaskCoordinator tasks,
      ClientFileCheckConsentCoordinator consent,
      ClientFileCheckTaskCoordinator.Session session,
      ClientFileCheckTaskCoordinator.RequestIdentity request,
      ClientFileCheckTaskCoordinator.TaskToken token,
      Supplier<Object> currentConnection,
      Supplier<UUID> currentPlayer,
      ClientFileCheckResult result,
      Sender sender,
      Diagnostics diagnostics) {
    Objects.requireNonNull(result, "result");
    if (!valid(tasks, consent, session, request, token, currentConnection, currentPlayer)
        || !consent.beginSending(request, session)) {
      consent.finish(request, session);
      diagnose(diagnostics, "stale_result", request, null);
      return false;
    }
    try {
      sender.send(result);
      return true;
    } catch (RuntimeException failure) {
      diagnose(diagnostics, "send_failed", request, failure);
      return false;
    } finally {
      consent.finish(request, session);
    }
  }

  public static boolean busy(
      ClientFileCheckTaskCoordinator tasks,
      ClientFileCheckConsentCoordinator consent,
      ClientFileCheckTaskCoordinator.Session session,
      ClientFileCheckTaskCoordinator.RequestIdentity request,
      Supplier<Object> currentConnection,
      Supplier<UUID> currentPlayer,
      ClientFileCheckResult result,
      Sender sender,
      Diagnostics diagnostics) {
    if (!baseValid(tasks, session, request, null, currentConnection, currentPlayer)
        || !consent.busyFor(request, session)) {
      diagnose(diagnostics, "stale_busy_result", request, null);
      return false;
    }
    try {
      sender.send(result);
      return true;
    } catch (RuntimeException failure) {
      diagnose(diagnostics, "busy_send_failed", request, failure);
      return false;
    }
  }

  private static boolean valid(
      ClientFileCheckTaskCoordinator tasks,
      ClientFileCheckConsentCoordinator consent,
      ClientFileCheckTaskCoordinator.Session session,
      ClientFileCheckTaskCoordinator.RequestIdentity request,
      ClientFileCheckTaskCoordinator.TaskToken token,
      Supplier<Object> currentConnection,
      Supplier<UUID> currentPlayer) {
    return baseValid(tasks, session, request, token, currentConnection, currentPlayer)
        && consent.held(request, session);
  }

  private static boolean baseValid(
      ClientFileCheckTaskCoordinator tasks,
      ClientFileCheckTaskCoordinator.Session session,
      ClientFileCheckTaskCoordinator.RequestIdentity request,
      ClientFileCheckTaskCoordinator.TaskToken token,
      Supplier<Object> currentConnection,
      Supplier<UUID> currentPlayer) {
    return session != null
        && tasks.isCurrent(session)
        && session.connectionIdentity() == currentConnection.get()
        && session.localPlayerId().equals(currentPlayer.get())
        && request.targetPlayerId().equals(session.localPlayerId())
        && (token == null
            || (!token.cancelled()
                && token.session().equals(session)
                && token.request().equals(request)));
  }

  private static void diagnose(
      Diagnostics diagnostics,
      String stage,
      ClientFileCheckTaskCoordinator.RequestIdentity request,
      Throwable failure) {
    try {
      diagnostics.record(stage, request, failure);
    } catch (RuntimeException ignored) {
      // Diagnostics cannot make a stale result cross a connection boundary.
    }
  }
}
