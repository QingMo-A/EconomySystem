package com.mo.economy_system.ui.territory.confirm;

import java.util.UUID;

public interface TerritoryConfirmationPort {
  void removeTerritory(UUID territoryId);
  void removeMember(UUID territoryId, UUID memberId);
}
