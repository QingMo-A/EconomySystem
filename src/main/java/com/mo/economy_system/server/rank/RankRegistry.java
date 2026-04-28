package com.mo.economy_system.server.rank;

import net.minecraft.world.effect.MobEffects;

public class RankRegistry {
    //5个等级
    // 颜色定义：
    // NO_RANK: 灰色 (0xAAAAAA)
    // FISH: 绿色 (0x55FF55 - §a)
    // FISH+: 蓝色 (0x55FFFF - §b)
    // FISH++: 金色 (0xFFAA00 - §6)
    // OPERATOR: 红色 (0xFF5555 - §c)

    public static final Rank NULL = new Rank(
            "NULL", -1, 0xAAAAAA
    );
    public static final Rank NO_RANK = new Rank(
            "NO_RANK", 0, 0xAAAAAA
    );
    public static final Rank FISH = new Rank(
            "FISH", 1, 0x55FF55
    );
    public static final Rank FISH_PLUS = new Rank(
            "FISH+", 2, 0x55FFFF
    );
    public static final Rank FISH_PLUS_PLUS = new Rank(
            "FISH++", 3, 0xFFAA00
    );
    public static final Rank OPERATOR = new Rank(
            "OPERATOR", 4, 0xFF5555
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
