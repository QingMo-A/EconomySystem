package com.mo.economy_system.common.check;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

public final class ClientFileCheckRequestStoreRegistry<S> {
  private final Map<S, ClientFileCheckRequestStore> stores = new WeakHashMap<>();

  public synchronized ClientFileCheckRequestStore get(S server) {
    return stores.computeIfAbsent(
        Objects.requireNonNull(server), ignored -> new ClientFileCheckRequestStore());
  }

  public synchronized int serverCount() {
    return stores.size();
  }
}
