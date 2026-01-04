package com.mo.economy_system.server.playerdata;

import com.mo.economy_system.server.chattitle.Title;
import com.mo.economy_system.server.chattitle.TitleRegistry;
import com.mo.economy_system.server.rank.Rank;
import com.mo.economy_system.server.rank.RankRegistry;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class PlayerData {
    private UUID uuid;
    private String playerName;
    private Rank rank;
    private Title title;
    private int level;
    private long currentExperience;
    private long lastLoginTime;
    private long totalPlayTime;

    public PlayerData() {

    }

    public PlayerData(ServerPlayer player) {
        this.uuid = player.getUUID();
        this.playerName = player.getScoreboardName();
        this.rank = RankRegistry.NO_RANK;
        this.title = TitleRegistry.getDefaultTitle();
        this.level = 1;
        this.lastLoginTime = System.currentTimeMillis();  //登录时记录当前时间
        this.totalPlayTime = 0;
    }

    public PlayerData(UUID uuid, String playerName, Rank rank, Title title, int level) {
        this.uuid = uuid;
        this.playerName = playerName;
        this.rank = rank;
        this.title = title;
        this.level = level;
    }

    public void setRank(Rank rank) {
        this.rank = rank;
    }
    public void setTitle(Title title) {
        this.title = title;
    }
    public void setLevel(int level) {
        this.level = level;
    }
    public void setCurrentExperience(long currentExperience) {
        this.currentExperience = currentExperience;
    }

    public String getPlayerName() {
        return this.playerName;
    }
    public UUID getUUID() {
        return uuid;
    }
    public Rank getRank() {
        return this.rank;
    }
    public Title getTitle() {
        return this.title;
    }
    public int getLevel() {
        return this.level;
    }
    public long getCurrentExperience() {
        return currentExperience;
    }



    public long getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(long time) { this.lastLoginTime = time; }
    public long getTotalPlayTime() { return totalPlayTime; }
    public void addPlayTime(long time) { this.totalPlayTime += time; }
}
