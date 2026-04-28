package com.mo.economy_system.server.notice;

import com.google.gson.annotations.SerializedName;

/**
 * 公告数据类
 */
public class NoticeData {
    @SerializedName("noticeId")
    private int noticeId;

    @SerializedName("noticeTitle")
    private String noticeTitle;

    @SerializedName("noticeContent")
    private String noticeContent;

    @SerializedName("publishTime")
    private long publishTime;

    public NoticeData() {
    }

    public NoticeData(int noticeId, String noticeTitle, String noticeContent, long publishTime) {
        this.noticeId = noticeId;
        this.noticeTitle = noticeTitle;
        this.noticeContent = noticeContent;
        this.publishTime = publishTime;
    }

    public int getNoticeId() {
        return noticeId;
    }

    public void setNoticeId(int noticeId) {
        this.noticeId = noticeId;
    }

    public String getNoticeTitle() {
        return noticeTitle;
    }

    public void setNoticeTitle(String noticeTitle) {
        this.noticeTitle = noticeTitle;
    }

    public String getNoticeContent() {
        return noticeContent;
    }

    public void setNoticeContent(String noticeContent) {
        this.noticeContent = noticeContent;
    }

    public long getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(long publishTime) {
        this.publishTime = publishTime;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        NoticeData that = (NoticeData) obj;
        // 只比较 noticeId，因为 ID 相同就是同一公告
        return noticeId == that.noticeId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(noticeId);
    }
}
