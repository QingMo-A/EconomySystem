package com.mo.economy_system.server.notice;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.screen.server_screen.tips.TipPushHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 公告系统事件处理器
 */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class NoticeEventHandler {

    /**
     * 玩家登录时检测新公告并发送提醒
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 获取最新公告ID
            int maxNoticeId = NoticeManager.getMaxNoticeId();

            // 没有公告，直接返回
            if (maxNoticeId <= 0) {
                return;
            }

            // 获取玩家已读公告ID
            var readNoticeIds = PlayerNoticeDataManager.getReadNoticeIds(player.getUUID());

            // 检查是否有未读的最新公告
            if (!readNoticeIds.contains(maxNoticeId)) {
                NoticeData latestNotice = NoticeManager.getLatestNotice();
                if (latestNotice != null) {
                    // 发送新公告提醒（左上角提示框，持续15秒）
                    TipPushHelper.sendTipToPlayer(player, "§b§l您有新的公告需要查看", 15000);
                    EconomySystem.LOGGER.info("玩家 {} 有新公告 #{} 待阅读",
                        player.getScoreboardName(), maxNoticeId);
                }
            }
        }
    }
}