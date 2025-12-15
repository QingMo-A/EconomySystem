//package com.mo.economy_system.playerlevel.overalllevel.capability;
//
//import com.mo.economy_system.EconomySystem;
//import com.mo.economy_system.network.EconomySystem_NetworkManager;
//import com.mo.economy_system.network.packets.level_system.Packet_SyncLevel;
//import net.minecraft.core.Direction;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.server.level.ServerPlayer;
//import net.minecraft.world.entity.Entity;
//import net.minecraft.world.entity.player.Player;
//import net.minecraftforge.common.capabilities.*;
//import net.minecraftforge.common.util.INBTSerializable;
//import net.minecraftforge.common.util.LazyOptional;
//import net.minecraftforge.event.AttachCapabilitiesEvent;
//import net.minecraftforge.event.entity.player.PlayerEvent;
//import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.common.Mod;
//import net.minecraftforge.network.PacketDistributor;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
//public class OverAllLevelCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
//    // 唯一ID
//    public static final ResourceLocation LEVEL_CAP_ID = new ResourceLocation(EconomySystem.MODID, "player_overall_level");
//
//    // 能力标识
//    public static Capability<IOverAllLevelCapability> LEVEL_CAPABILITY;
//
//    // 每个玩家的等级实例
//    private final IOverAllLevelCapability levelCapability = new OverAllLevelCapability();
//
//    // 懒加载容器
//    private final LazyOptional<IOverAllLevelCapability> lazyCapability = LazyOptional.of(() -> levelCapability);
//
//    // 注册能力
//    @SubscribeEvent
//    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
//        event.register(IOverAllLevelCapability.class);
//        LEVEL_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
//    }
//
//    @Override
//    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
//        if (cap == LEVEL_CAPABILITY) {
//            return lazyCapability.cast();
//        }
//        return LazyOptional.empty();
//    }
//
//    @Override
//    public CompoundTag serializeNBT() {
//        return levelCapability.serializeNBT();
//    }
//
//    @Override
//    public void deserializeNBT(CompoundTag nbt) {
//        levelCapability.deserializeNBT(nbt);
//    }
//
//    // 绑定能力到玩家实体
//    @Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
//    public static class ForgeEvents {
//        @SubscribeEvent
//        public static void attachCapability(AttachCapabilitiesEvent<Entity> event) {
//            if (event.getObject() instanceof Player player) {
//                OverAllLevelCapabilityProvider provider = new OverAllLevelCapabilityProvider();
//                event.addCapability(LEVEL_CAP_ID, provider);
//                // 玩家实体销毁时释放资源
//                event.addListener(() -> provider.lazyCapability.invalidate());
//
//                // 玩家加载时同步等级（仅服务端）
//                if (player instanceof ServerPlayer serverPlayer) {
//                    int initialLevel = OverAllLevelCapabilityProvider.getPlayerLevel(serverPlayer);
//                    EconomySystem_NetworkManager.INSTANCE.send(
//                            PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> serverPlayer),
//                            new Packet_SyncLevel(serverPlayer.getUUID(), initialLevel)
//                    );
//                }
//            }
//        }
//        @SubscribeEvent
//        public static void onPlayerClone(PlayerEvent.Clone event) {
//            Player oldPlayer = event.getOriginal();
//            Player newPlayer = event.getEntity();
//            // 仅在“死亡重生”时复制数据（从末地返回不处理）
//            if (!event.isWasDeath()) {
//                return;
//            }
//            // 从旧玩家获取旧Rank数据，然后复制到重生后的玩家
//            oldPlayer.reviveCaps();
//            int old_level = OverAllLevelCapabilityProvider.getPlayerLevel(oldPlayer);
//            OverAllLevelCapabilityProvider.setPlayerLevel(oldPlayer, old_level);
//            EconomySystem.LOGGER.info("玩家死亡重生 - 已复制LEVEL: {} 到新实体", old_level);
//        }
//    }
//
//    // 工具方法：获取玩家的总等级
//    public static int getPlayerLevel(Player player) {
//        if (LEVEL_CAPABILITY == null) {
//            return 0; // 能力未初始化时返回默认值
//        }
//        LazyOptional<IOverAllLevelCapability> capOptional = player.getCapability(LEVEL_CAPABILITY);
//        return capOptional.map(IOverAllLevelCapability::getLevel).orElse(0);
//    }
//
//    // 工具方法：设置玩家的总等级
//    public static void setPlayerLevel(Player player, int level) {
//        if (LEVEL_CAPABILITY == null) {
//            return;
//        }
//        LazyOptional<IOverAllLevelCapability> capOptional = player.getCapability(LEVEL_CAPABILITY);
//        capOptional.ifPresent(cap -> cap.setLevel(level));
//
//        // 服务器端同步到客户端
//        if (player instanceof ServerPlayer serverPlayer) {
//            // 发送总等级同步包给所有跟踪该玩家的客户端（包括自己）
//            EconomySystem_NetworkManager.INSTANCE.send(
//                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> serverPlayer),
//                    new Packet_SyncLevel(serverPlayer.getUUID(), level)
//            );
//        }
//    }
//}