package com.mo.economy_system.common.client;

import com.mo.economy_system.common.network.TerritoryDataResponseKind;
import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Summary;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Restores a complete protocol-18 response before making one page-state commit. */
public final class TerritoryDataClientApplier {
  private TerritoryDataClientApplier() {}

  public static <O, A> boolean apply(
      TerritoryDataResponseMessage message,
      TerritoryScreenTarget<O, A> target,
      Restorer<Owned, O> ownedRestorer,
      Restorer<Summary, A> authorizedRestorer,
      RestoreFailure failure) {
    Objects.requireNonNull(message, "message");
    Objects.requireNonNull(target, "target");
    Objects.requireNonNull(ownedRestorer, "ownedRestorer");
    Objects.requireNonNull(authorizedRestorer, "authorizedRestorer");
    Objects.requireNonNull(failure, "failure");
    if (!target.acceptsRequest(message.requestId())) return false;
    if (message.kind() == TerritoryDataResponseKind.ERROR) {
      target.territorySyncFailed(message.requestId());
      return true;
    }
    try {
      List<O> owned = new ArrayList<>(message.owned().size());
      for (Owned value : message.owned()) owned.add(Objects.requireNonNull(
          ownedRestorer.restore(value), "restored owned territory"));
      List<A> authorized = new ArrayList<>(message.authorized().size());
      for (Summary value : message.authorized()) authorized.add(Objects.requireNonNull(
          authorizedRestorer.restore(value), "restored authorized territory"));
      target.commitTerritoryData(message.requestId(), List.copyOf(owned), List.copyOf(authorized));
      return true;
    } catch (RuntimeException error) {
      failure.onFailure(message.requestId(), message.owned().size(), message.authorized().size(), error);
      target.territorySyncFailed(message.requestId());
      return true;
    }
  }

  public interface TerritoryScreenTarget<O, A> {
    boolean acceptsRequest(long requestId);
    void commitTerritoryData(long requestId, List<O> owned, List<A> authorized);
    void territorySyncFailed(long requestId);
  }

  @FunctionalInterface public interface Restorer<S, T> { T restore(S snapshot); }
  @FunctionalInterface public interface RestoreFailure {
    void onFailure(long requestId, int ownedCount, int authorizedCount, RuntimeException error);
  }
}
