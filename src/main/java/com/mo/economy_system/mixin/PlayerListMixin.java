package com.mo.economy_system.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PlayerList Mixin - 拦截原版系统消息（进服、离服、死亡消息）
 */
@Mixin(PlayerList.class)
public class PlayerListMixin {

    /**
     * 拦截 broadcastSystemMessage 方法，过滤掉原版的进服、离服、死亡消息
     */
    @Inject(
            method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0  // 如果方法不存在也不会报错（兼容性）
    )
    private void economy_system$filterSystemMessages(Component message, boolean toOps, CallbackInfo ci) {
        if (shouldFilterMessage(message)) {
            ci.cancel(); // 取消消息发送
        }
    }

    /**
     * 判断是否应该过滤这条消息
     */
    private boolean shouldFilterMessage(Component message) {
        String messageStr = message.getString();

        // 检查是否为原版进服/离服消息（英文）
        if (messageStr.contains("joined the game") || messageStr.contains("left the game")) {
            return true;
        }

        // 检查是否为原版进服/离服消息（中文）
        if (messageStr.contains("加入了游戏") || messageStr.contains("离开了游戏") || messageStr.contains("退出了游戏")) {
            return true;
        }

        // 检查是否为死亡消息
        // 死亡消息通常包含 "was slain by", "fell from", "drowned", "burned", "blew up" 等关键词
        // 我们通过检查消息是否包含常见的死亡模式来识别
        String lowerMessage = messageStr.toLowerCase();

        // 常见的死亡模式关键词
        String[] deathKeywords = {
                "was slain by",      // 被杀死
                "was shot by",       // 被射杀
                "was fireballed by", // 被火球杀死
                "was killed by",     // 被杀死（通用）
                "fell from",         // 摔落
                "fell out of",       // 掉出
                "was doomed to fall",// 注定摔落
                "was impaled on",    // 被刺穿
                "drowned",           // 溺水
                "suffocated in",     // 窒息
                "squished too much", // 被挤压
                "was squashed by",   // 被压扁
                "was pricked to",    // 被刺死
                "walked into",       // 走进（仙人掌等）
                "was burnt to",      // 被烧死
                "was struck by",     // 被击中（闪电）
                "froze to",          // 冻死
                "was stung to",      // 被蛰死（蜜蜂）
                "was blown up by",   // 被炸死
                "was killed by magic",// 被魔法杀死
                "withered away",     // 凋零死亡
                "starved to",        // 饿死
                "died",              // 死亡（通用）
                "hit the ground",    // 撞击地面
                // 中文死亡关键词
                "被",
                "摔",
                "溺",
                "烧",
                "炸",
                "饿",
                "冻",
                "凋零",
                "死于"
        };

        for (String keyword : deathKeywords) {
            if (lowerMessage.contains(keyword.toLowerCase())) {
                return true;
            }
        }

        return false;
    }
}
