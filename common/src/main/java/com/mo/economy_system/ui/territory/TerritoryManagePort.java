package com.mo.economy_system.ui.territory;

import java.util.UUID;

/** Target adapter for requests and common territory actions. */
public interface TerritoryManagePort {
    long nextRequestId();

    void requestMembers(UUID territoryId, long requestId);

    void submit(UUID territoryId, TerritoryManageAction action, UUID targetPlayerId);

    default void confirm(UUID territoryId, TerritoryManageAction action, UUID targetPlayerId) {
        submit(territoryId, action, targetPlayerId);
    }

    void open(UUID territoryId, TerritoryManageAction action);
}
