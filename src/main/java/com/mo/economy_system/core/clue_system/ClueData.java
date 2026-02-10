package com.mo.economy_system.core.clue_system;

/**
 * 线索数据类
 * 用于配置文件中定义单个线索的信息
 */
public class ClueData {
    private int clueId;
    private int clueStage;
    private String clueTitle;
    private String clueTime;
    private String clueAuthor;
    private String clueContent;

    /**
     * 默认构造函数（用于Gson反序列化）
     */
    public ClueData() {
    }

    /**
     * 完整构造函数
     */
    public ClueData(int clueId, int clueStage, String clueTitle, String clueTime, String clueAuthor, String clueContent) {
        this.clueId = clueId;
        this.clueStage = clueStage;
        this.clueTitle = clueTitle;
        this.clueTime = clueTime;
        this.clueAuthor = clueAuthor;
        this.clueContent = clueContent;
    }

    // ==================== Getters and Setters ====================

    public int getClueId() {
        return clueId;
    }

    public void setClueId(int clueId) {
        this.clueId = clueId;
    }

    public int getClueStage() {
        return clueStage;
    }

    public void setClueStage(int clueStage) {
        this.clueStage = clueStage;
    }

    public String getClueTitle() {
        return clueTitle;
    }

    public void setClueTitle(String clueTitle) {
        this.clueTitle = clueTitle;
    }

    public String getClueTime() {
        return clueTime;
    }

    public void setClueTime(String clueTime) {
        this.clueTime = clueTime;
    }

    public String getClueAuthor() {
        return clueAuthor;
    }

    public void setClueAuthor(String clueAuthor) {
        this.clueAuthor = clueAuthor;
    }

    public String getClueContent() {
        return clueContent;
    }

    public void setClueContent(String clueContent) {
        this.clueContent = clueContent;
    }

    /**
     * 获取内容行数组（支持多行显示）
     */
    public String[] getContentLines() {
        if (clueContent == null || clueContent.isEmpty()) {
            return new String[0];
        }
        return clueContent.split("\\n");
    }

    @Override
    public String toString() {
        return "ClueData{" +
                "clueId=" + clueId +
                ", clueStage=" + clueStage +
                ", clueTitle='" + clueTitle + '\'' +
                ", clueTime='" + clueTime + '\'' +
                ", clueAuthor='" + clueAuthor + '\'' +
                ", clueContent='" + clueContent + '\'' +
                '}';
    }
}
