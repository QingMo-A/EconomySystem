package com.mo.economy_system.core.storybook_system;

/**
 * 随记本中的单条残页/片段数据。
 * 这里只保存静态内容本身，不保存玩家的收集状态。
 */
public class FragmentData {
    private int id;
    private int stageId;
    private int chapterId;
    private String authorName;
    private String title;
    private String time;
    private String content;

    public FragmentData() {
    }

    public FragmentData(int id, int stageId, int chapterId, String authorName, String title, String time, String content) {
        this.id = id;
        this.stageId = stageId;
        this.chapterId = chapterId;
        this.authorName = authorName;
        this.title = title;
        this.time = time;
        this.content = content;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStageId() {
        return stageId;
    }

    public void setStageId(int stageId) {
        this.stageId = stageId;
    }

    public int getChapterId() {
        return chapterId;
    }

    public void setChapterId(int chapterId) {
        this.chapterId = chapterId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String[] getContentLines() {
        if (content == null || content.isEmpty()) {
            return new String[0];
        }
        return content.split("\\n");
    }

    @Override
    public String toString() {
        return "FragmentData{" +
                "id=" + id +
                ", stageId=" + stageId +
                ", chapterId=" + chapterId +
                ", authorName='" + authorName + '\'' +
                ", title='" + title + '\'' +
                ", time='" + time + '\'' +
                '}';
    }
}
