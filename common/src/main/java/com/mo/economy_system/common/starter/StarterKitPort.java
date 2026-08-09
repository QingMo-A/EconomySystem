package com.mo.economy_system.common.starter;

import java.util.UUID;

/** Target persistence hook for the one-time starter-kit marker. */
public interface StarterKitPort {
  boolean claimed(UUID playerId) throws Exception;

  void markClaimed(UUID playerId) throws Exception;

  void unmarkClaimed(UUID playerId) throws Exception;
}
