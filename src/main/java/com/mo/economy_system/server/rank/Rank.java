package com.mo.economy_system.server.rank;

public class Rank {
    private final String rankName;
    private final int rankLevel;

    public Rank(String rankName, int rankLevel) {
        this.rankName = rankName;
        this.rankLevel = rankLevel;
    }

    public int getRankLevel() {
        return rankLevel;
    }
    public String getRankName() {
        return rankName;
    }

}