package com.mo.economy_system.server.serverui.tips;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TipDisplayManager {
    // 使用线程安全的列表存储信息
    private static final List<TipMessage> messages = new CopyOnWriteArrayList<>();

    // 服务器信息框的高度（Tips需要避开）
    private static int serverInfoHeight = 0;

    // 设置服务器信息框高度（由ServerInformationDisplay调用）
    public static void setServerInfoHeight(int height) {
        serverInfoHeight = height;
    }

    // 获取服务器信息框高度
    public static int getServerInfoHeight() {
        return serverInfoHeight;
    }

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

    /**
     * 清除包含指定文本的Tip（用于玩家按下U键后清除对应提示）
     * @param targetText 要清除的Tip包含的文本
     */
    public static void removeTipContainingText(String targetText) {
        // 第一步：收集所有需要删除的 Tip（普通for循环，不使用迭代器）
        List<TipMessage> tipsToRemove = new ArrayList<>();
        for (TipMessage msg : TipDisplayManager.messages) {
            if (msg.getText() != null && msg.getText().contains(targetText)) {
                tipsToRemove.add(msg);
            }
        }
        // 第二步：批量移除（CopyOnWriteArrayList 支持 removeAll，无异常）
        if (!tipsToRemove.isEmpty()) {
            TipDisplayManager.messages.removeAll(tipsToRemove);
        }
    }
}