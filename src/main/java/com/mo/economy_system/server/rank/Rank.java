package com.mo.economy_system.server.rank;

public class Rank {
    private final String rankId;
    private final int rankLevel;

    public Rank(String rankId, int rankLevel) {
        this.rankId = rankId;
        this.rankLevel = rankLevel;
    }

    public int getRankLevel() {
        return rankLevel;
    }
    public String getRankId() {
        return rankId;
    }

}