package com.mo.economy_system.common.redpacket;

import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import java.util.UUID;

/** Exact account mutations supplied by a target persistence adapter. */
public interface RedPacketAccountPort {
  BalanceMutationResult debit(UUID playerId, int amount, String category, String reason);

  BalanceMutationResult credit(UUID playerId, int amount, String category, String reason);
}
