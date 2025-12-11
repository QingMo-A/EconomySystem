package com.mo.economy_system.server.rank.capability;

import com.mo.economy_system.server.rank.Rank;
import com.mo.economy_system.server.rank.RankRegistry;

public class RankCapability implements IRankCapability {
    // 默认无Rank
    private Rank currentRank = RankRegistry.NO_RANK;

    @Override
    public Rank getRank() {
        return currentRank;
    }

    @Override
    public void setRank(Rank rank) {
        this.currentRank = rank;
    }
}