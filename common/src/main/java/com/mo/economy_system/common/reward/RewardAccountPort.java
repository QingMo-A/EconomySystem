package com.mo.economy_system.common.reward;

import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import java.util.UUID;

@FunctionalInterface
public interface RewardAccountPort {
  BalanceMutationResult credit(UUID playerId, int amount, String category, String reason);
}
