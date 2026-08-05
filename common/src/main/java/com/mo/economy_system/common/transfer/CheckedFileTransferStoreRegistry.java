package com.mo.economy_system.common.transfer;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

public final class CheckedFileTransferStoreRegistry<S> {
  public record Stores(ClientFileCheckManifestAuthorizationStore authorizations,
                       CheckedFileTransferStore transfers) {}
  private final Map<S, Stores> stores = new WeakHashMap<>();

  public synchronized Stores get(S server) {
    return stores.computeIfAbsent(Objects.requireNonNull(server), ignored ->
        new Stores(new ClientFileCheckManifestAuthorizationStore(), new CheckedFileTransferStore()));
  }

  public synchronized void remove(S server) {
    Stores removed = stores.remove(server);
    if (removed != null) {
      removed.authorizations().clear();
      removed.transfers().clear();
    }
  }

  public synchronized int size() { return stores.size(); }
}
