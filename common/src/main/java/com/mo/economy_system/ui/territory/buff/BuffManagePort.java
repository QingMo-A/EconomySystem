package com.mo.economy_system.ui.territory.buff;

import com.mo.economy_system.common.territory.TerritoryBuffCost;
import java.util.UUID;

public interface BuffManagePort {
    long nextRequestId();
    void request(UUID territoryId, long requestId);
    void submit(UUID territoryId, BuffAction action, String buffId);

    BuffResourceSnapshot inspect(TerritoryBuffCost cost);

    /** Client-only feedback for a disabled or informational action. */
    default void feedback(String translationKey) {}
}
