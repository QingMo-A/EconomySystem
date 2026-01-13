package com.mo.economy_system.mixin;

import com.mo.economy_system.EconomySystem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SystemMessage Mixin - 拦截聊天栏中的系统消息
 * 注意：右上角显示由 SystemMessageEventHandler 通过事件系统处理
 * 此 Mixin 只负责拦截聊天栏显示
 */
@Mixin(PlayerList.class)
public class SystemMessageMixin {

    @Inject(
            method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void economy_system$filterSystemMessages(Component message, boolean toOps, CallbackInfo ci) {
        String messageStr = message.getString();
        EconomySystem.LOGGER.info("SystemMessageMixin intercepted: " + messageStr);

        // 拦截聊天栏中的系统消息（右上角由事件处理器处理）
        if (shouldFilterFromChat(messageStr)) {
            ci.cancel();
        }
    }

    /**
     * 判断是否应该从聊天栏中过滤
     * 使用宽松匹配，与事件处理器保持一致
     */
    private boolean shouldFilterFromChat(String message) {
        // === 进度/成就/挑战/目标消息 ===
        if (message.contains("[") && (message.contains("进度") || message.contains("挑战") || message.contains("目标") || message.contains("任务") || message.contains("成就"))) {
            return true;
        }
        if ((message.contains("获得") || message.contains("完成") || message.contains("达成")) &&
            (message.contains("任务") || message.contains("目标") || message.contains("挑战"))) {
            return true;
        }

        // === 进服/离服消息 ===
        if (message.contains("加入") && message.contains("游戏")) return true;
        if (message.contains("离开") || message.contains("退出")) {
            if (message.contains("游戏") || message.contains("服务器")) return true;
        }

        // === 死亡消息 ===
        String lowerMessage = message.toLowerCase();
        String[] deathKeywords = {
                "was slain by", "was shot by", "was fireballed by", "was killed by",
                "fell from", "fell out of", "was doomed to fall", "was impaled on",
                "drowned", "suffocated in", "squished too much", "was squashed by",
                "was pricked to", "walked into", "was burnt to", "was struck by",
                "froze to", "was stung to", "was blown up by", "was killed by magic",
                "withered away", "starved to", "hit the ground", "died",
                "被", "摔", "溺", "烧", "炸", "饿", "冻", "凋零", "死于"
        };

        for (String keyword : deathKeywords) {
            if (lowerMessage.contains(keyword.toLowerCase())) {
                return true;
            }
        }

        return false;
    }
}
