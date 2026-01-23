package com.mo.economy_system.core.task_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.story_system.StoryStageManager;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.task_system.Packet_SyncFullTaskData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus =  Mod.EventBusSubscriber.Bus.FORGE)
public class TaskSyncEvent {
    @SubscribeEvent
    public static void onPlayerLogging(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var playerUUID = player.getUUID();
            //从缓存里面获取全量任务数据
            var storyStages = StoryStageManager.getAllStages();
            var playerTasks = TaskDataManager.TASK_PLAYER_DATA_CACHE;

            //构建同步数据包
            Packet_SyncFullTaskData syncPacket = new Packet_SyncFullTaskData(
                    playerUUID,
                    playerTasks,
                    storyStages
            );

            //向当前登录玩家发送数据包
            EconomySystem_NetworkManager.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> (net.minecraft.server.level.ServerPlayer) player),
                    syncPacket
            );

            EconomySystem.LOGGER.info("已向玩家 {}({}) 同步全量任务数据",
                    player.getDisplayName().getString(),
                    playerUUID);
        }
    }
}
