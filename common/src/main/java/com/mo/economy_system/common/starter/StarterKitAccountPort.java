package com.mo.economy_system.common.starter;

import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import java.util.UUID;

/** Exact account mutation adapter for the starter-kit transaction. */
public interface StarterKitAccountPort {
  BalanceMutationResult credit(UUID playerId, int amount, String category, String reason);

  BalanceMutationResult debit(UUID playerId, int amount, String category, String reason);
}
