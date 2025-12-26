package com.mo.economy_system.server.serverui.tips;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TipDisplayManager {
    // 使用线程安全的列表存储信息
    private static final List<TipMessage> messages = new CopyOnWriteArrayList<>();

    // 添加信息
    public static void addMessage(String text) {
        addMessage(text, 5000);
    }

    // 添加信息（自定义显示时长）
    public static void addMessage(String text, int displayDuration) {
        messages.add(new TipMessage(text, displayDuration));
    }

    // 清理过期信息
    public static void cleanExpiredMessages() {
        messages.removeIf(TipMessage::isExpired);
    }

    // 获取当前需要显示的信息
    public static List<TipMessage> getActiveMessages() {
        cleanExpiredMessages();
        return new ArrayList<>(messages);
    }
}