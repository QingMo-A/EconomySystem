package com.mo.economy_system.common.redpacket;

import java.util.List;

/** Persistence port for active red packets. Implementations must copy the supplied list. */
public interface RedPacketRepository {
  List<RedPacket> load();

  void save(List<RedPacket> packets);

  static RedPacketRepository empty() {
    return new RedPacketRepository() {
      @Override
      public List<RedPacket> load() {
        return List.of();
      }

      @Override
      public void save(List<RedPacket> packets) {
        // Deliberately a no-op for tests or targets that use another durable store.
      }
    };
  }
}
