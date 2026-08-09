package com.mo.economy_system.ui.territory.invite;

import java.util.UUID;

/** Platform operations required by the common invite controller. */
public interface TerritoryInvitePort {
  long nextRequestId();
  void requestPlayers(long requestId);
  void submitInvite(UUID territoryId, UUID playerId);
}
