package com.mo.economy_system.common.tpa;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Identity-scoped services prevent requests leaking between integrated/dedicated servers. */
public final class TpaServiceRegistry<S> {
  private final Map<S, TpaService> services = new IdentityHashMap<>();

  public synchronized TpaService get(S server, Function<S, TpaService> factory) {
    Objects.requireNonNull(server, "server");
    Objects.requireNonNull(factory, "factory");
    return services.computeIfAbsent(server, factory);
  }

  public synchronized void remove(S server) {
    TpaService service = services.remove(server);
    if (service != null) service.clear();
  }
}
