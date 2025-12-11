package com.mo.economy_system.server.rank.capability;

import com.mo.economy_system.server.rank.Rank;
import com.mo.economy_system.server.rank.RankRegistry;

public interface IRankCapability {
    Rank getRank();
    void setRank(Rank rank);
    default void clearRank() {
        setRank(RankRegistry.NO_RANK);
    }
}