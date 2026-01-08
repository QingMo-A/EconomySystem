package com.mo.economy_system.server.rank;

public class Rank {
    private String rankName;
    private int rankLevel;

    // 无参构造函数（Gson反序列化需要）
    public Rank() {
    }

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