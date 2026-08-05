package com.mo.economy_system.common.check;

import com.mo.economy_system.common.network.ClientFileCheckResultRequestMessage;
import com.mo.economy_system.common.network.ClientFileCheckResultResponseMessage;
import java.util.Objects;
import java.util.UUID;
import com.mo.economy_system.common.transfer.ClientFileCheckManifestAuthorizationStore;

public final class ClientFileCheckResultRoutingService {
  public enum Outcome {
    WRONG_TARGET,
    NOT_FOUND,
    BUSY,
    METADATA_MISMATCH,
    REQUESTER_OFFLINE,
    DELIVERED,
    DELIVERY_FAILED
  }

  @FunctionalInterface
  public interface RequesterLookup {
    Object find(UUID requesterId);
  }

  @FunctionalInterface
  public interface ResponseSender {
    void send(Object requester, ClientFileCheckResultResponseMessage response);
  }

  @FunctionalInterface
  public interface Diagnostics {
    void record(String stage, UUID targetId, UUID requesterId, Throwable failure);
  }

  private ClientFileCheckResultRoutingService() {}

  public static Outcome route(
      ClientFileCheckResultRequestMessage message,
      UUID authenticatedTarget,
      long tick,
      ClientFileCheckRequestStore store,
      RequesterLookup lookup,
      ResponseSender sender,
      Diagnostics diagnostics) {
    return route(message, authenticatedTarget, tick, store, null, lookup, sender, diagnostics);
  }

  public static Outcome route(
      ClientFileCheckResultRequestMessage message,
      UUID authenticatedTarget,
      long tick,
      ClientFileCheckRequestStore store,
      ClientFileCheckManifestAuthorizationStore authorizations,
      RequesterLookup lookup,
      ResponseSender sender,
      Diagnostics diagnostics) {
    Objects.requireNonNull(message);
    Objects.requireNonNull(authenticatedTarget);
    Objects.requireNonNull(store);
    Objects.requireNonNull(lookup);
    Objects.requireNonNull(sender);
    Objects.requireNonNull(diagnostics);
    if (!authenticatedTarget.equals(message.targetPlayerId())) return Outcome.WRONG_TARGET;
    var key =
        new ClientFileCheckRequestStore.Key(
            authenticatedTarget, message.requesterPlayerId(), message.checkType());
    var claimed = store.claim(key, tick);
    if (claimed.status() == ClientFileCheckRequestStore.ClaimStatus.NOT_FOUND)
      return Outcome.NOT_FOUND;
    if (claimed.status() == ClientFileCheckRequestStore.ClaimStatus.BUSY) return Outcome.BUSY;
    var claim = claimed.claim();
    try {
      var pending = claim.pending();
      if (!metadataMatches(message, pending)) return Outcome.METADATA_MISMATCH;
      String json;
      ClientFileCheckResult parsed;
      try {
        parsed = ClientFileCheckResultJsonCodec.decode(message.resultJson());
        if (parsed.checkType() != pending.checkType())
          throw new IllegalArgumentException("check type");
        json = ClientFileCheckResultJsonCodec.encode(parsed);
      } catch (RuntimeException invalid) {
        parsed = ClientFileCheckResult.failed(pending.checkType(), "INVALID_RESULT");
        json =
            ClientFileCheckResultJsonCodec.encode(
                parsed);
      }
      Object requester = lookup.find(pending.requesterPlayerId());
      if (requester == null) {
        diagnose(diagnostics, "requester_offline", pending, null);
        return Outcome.REQUESTER_OFFLINE;
      }
      var response =
          new ClientFileCheckResultResponseMessage(
              pending.targetPlayerName(),
              pending.targetPlayerId(),
              pending.requesterPlayerName(),
              pending.requesterPlayerId(),
              pending.checkType(),
              json);
      try {
        sender.send(requester, response);
        if (authorizations != null)
          authorizations.replace(
              pending.targetPlayerId(), pending.requesterPlayerId(), parsed, tick);
        return Outcome.DELIVERED;
      } catch (RuntimeException failure) {
        if (authorizations != null)
          authorizations.removeScope(
              new ClientFileCheckManifestAuthorizationStore.Scope(
                  pending.targetPlayerId(), pending.requesterPlayerId(), pending.checkType()),
              tick);
        diagnose(diagnostics, "delivery_failed", pending, failure);
        return Outcome.DELIVERY_FAILED;
      }
    } finally {
      store.complete(claim);
    }
  }

  private static boolean metadataMatches(
      ClientFileCheckResultRequestMessage message, ClientFileCheckRequestStore.Pending pending) {
    return message.targetPlayerId().equals(pending.targetPlayerId())
        && message.targetPlayerName().equals(pending.targetPlayerName())
        && message.requesterPlayerId().equals(pending.requesterPlayerId())
        && message.requesterPlayerName().equals(pending.requesterPlayerName())
        && message.checkType() == pending.checkType();
  }

  private static void diagnose(
      Diagnostics diagnostics,
      String stage,
      ClientFileCheckRequestStore.Pending pending,
      Throwable failure) {
    try {
      diagnostics.record(stage, pending.targetPlayerId(), pending.requesterPlayerId(), failure);
    } catch (RuntimeException ignored) {
      // Diagnostics must not create a replay window.
    }
  }
}
