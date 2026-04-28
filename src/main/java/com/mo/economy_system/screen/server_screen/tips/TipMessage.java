package com.mo.economy_system.screen.server_screen.tips;

public class TipMessage {
    private final String text;
    private final long timestamp;
    private final int displayDuration; // 显示时长(毫秒)

    public TipMessage(String text, int displayDuration) {
        this.text = text;
        this.timestamp = System.currentTimeMillis();
        this.displayDuration = displayDuration;
    }

    public String getText() {
        return text;
    }

    // 判断信息是否已过期
    public boolean isExpired() {
        return displayDuration >= 0 && System.currentTimeMillis() - timestamp > displayDuration;
    }
}
