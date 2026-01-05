package com.mo.economy_system.server.rank;

import net.minecraft.world.effect.MobEffects;

public class RankRegistry {
    //5个等级
    public static final Rank NULL = new Rank(
            "NULL", -1
    );
    public static final Rank NO_RANK = new Rank(
            "NO_RANK", 0
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

    // 按名称查找
    public static Rank getRankByName(String name) {
        return switch (name) {
            case "NO_RANK" -> NO_RANK;
            case "FISH" -> FISH;
            case "FISH+" -> FISH_PLUS;
            case "FISH++" -> FISH_PLUS_PLUS;
            case "OPERATOR" -> OPERATOR;
            default -> NO_RANK;
        };
    }

    // 按等级查找
    public static Rank getRankByLevel(int level) {
        return switch (level) {
            case 0 -> NO_RANK;
            case 1 -> FISH;
            case 2 -> FISH_PLUS;
            case 3 -> FISH_PLUS_PLUS;
            case 4 -> OPERATOR;
            default -> NO_RANK;
        };
    }
}