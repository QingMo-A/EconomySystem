package com.mo.economy_system.core.storybook_system;

public class StoryBookEntryViewData {
    private final int fragmentId;
    private final int stageId;
    private final int chapterId;
    private final String title;
    private final String content;
    private final String time;
    private final String authorName;
    private final boolean read;

    public StoryBookEntryViewData(int fragmentId, int stageId, int chapterId, String title, String content, String time, String authorName, boolean read) {
        this.fragmentId = fragmentId;
        this.stageId = stageId;
        this.chapterId = chapterId;
        this.title = title;
        this.content = content;
        this.time = time;
        this.authorName = authorName;
        this.read = read;
    }

    public int getFragmentId() {
        return fragmentId;
    }

    public int getStageId() {
        return stageId;
    }

    public int getChapterId() {
        return chapterId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getTime() {
        return time;
    }

    public String getAuthorName() {
        return authorName;
    }

    public boolean isRead() {
        return read;
    }
}
