package com.mo.economy_system.server.rank;

import net.minecraft.world.effect.MobEffects;

public class RankRegistry {
    //5个等级
    public static final Rank NO_RANK = new Rank(
            "NO RANK", 0
    );
    public static final Rank FISH = new Rank(
            "FISH", 1
    );
    public static final Rank FISH_PLUS = new Rank(
            "FISH+", 2
    );
    public static final Rank FISH_PLUS_PLUS = new Rank(
            "FISH++", 3
    );
    public static final Rank OPERATOR = new Rank(
            "OPERATOR", 4
    );
}