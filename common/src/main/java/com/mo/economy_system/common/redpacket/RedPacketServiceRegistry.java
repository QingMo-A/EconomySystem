package com.mo.economy_system.common.redpacket;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Function;

/** Keeps server-session services isolated without making common depend on a loader API. */
public final class RedPacketServiceRegistry<S> {
  private final Map<S, RedPacketService> services =
      Collections.synchronizedMap(new WeakHashMap<>());

  public RedPacketService get(S server, Function<? super S, RedPacketService> factory) {
    Objects.requireNonNull(server, "server");
    Objects.requireNonNull(factory, "factory");
    synchronized (services) {
      return services.computeIfAbsent(server, factory);
    }
  }

  public void remove(S server) {
    if (server == null) return;
    services.remove(server);
  }

  public void clear() {
    services.clear();
  }
}
