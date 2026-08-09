package com.mo.economy_system.ui.territory;

import java.util.UUID;

/** Target adapter for requests and common territory actions. */
public interface TerritoryManagePort {
    long nextRequestId();

    void requestMembers(UUID territoryId, long requestId);

    void submit(UUID territoryId, TerritoryManageAction action, UUID targetPlayerId);

    /** Explicit confirmation intent; implementations must not fail open into submit(). */
    void confirm(UUID territoryId, TerritoryManageAction action, UUID targetPlayerId);

    void open(UUID territoryId, TerritoryManageAction action);
}
