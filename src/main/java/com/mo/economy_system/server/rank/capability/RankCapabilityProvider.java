//package com.mo.economy_system.server.rank.capability;
//
//import com.mo.economy_system.EconomySystem;
//import com.mo.economy_system.network.EconomySystem_NetworkManager;
//import com.mo.economy_system.network.packets.ranktitle_system.Packet_SyncRankTitle;
//import com.mo.economy_system.server.rank.Rank;
//import com.mo.economy_system.server.rank.RankRegistry;
//import net.minecraft.core.Direction;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.server.level.ServerPlayer;
//import net.minecraft.world.entity.Entity;
//import net.minecraft.world.entity.player.Player;
//import net.minecraftforge.common.capabilities.*;
//import net.minecraftforge.common.util.LazyOptional;
//import net.minecraftforge.event.AttachCapabilitiesEvent;
//import net.minecraftforge.event.entity.player.PlayerEvent;
//import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.common.Mod;
//import net.minecraftforge.network.PacketDistributor;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//import net.minecraftforge.common.util.INBTSerializable;
//
//// 注册到模组事件总线
//@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
//public class RankCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
//    // 设置唯一ID,避免冲突
//    public static final ResourceLocation RANK_CAP_ID = new ResourceLocation(EconomySystem.MODID, "player_rank");
//
//    // 能力标识，后面靠这个匹配是什么模组的什么能力
//    public static Capability<IRankCapability> RANK_CAPABILITY;
//
//    // rankCapability可以使用之前在接口里面定义的方法，后面是每一个玩家的一个实例
//    private final IRankCapability rankCapability = new RankCapability();
//
//    // 懒加载容器，forge官方要求这么写，节省资源+防空指针
//    private final LazyOptional<IRankCapability> lazyCapability = LazyOptional.of(() -> rankCapability);
//
//    //注册rank这个能力
//    @SubscribeEvent
//    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
//        // 注册IRankCapability接口、
//        event.register(IRankCapability.class);
//        RANK_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
//    }
//
//
//    // 重写getCapability，forge要求，用于匹配哪个模组什么能力
//    @Override
//    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
//        // 只有是rank这个能力才行
//        if (cap == RANK_CAPABILITY) {
//            // cast()：LazyOptional源码提供的安全类型转换方法，将LazyOptional<IRankCapability>转为LazyOptional<T>
//            return lazyCapability.cast();
//        }
//        // 返回LazyOptional空实例（非null），符合接口规范，避免空指针
//        return LazyOptional.empty();
//    }
//
//    // 实现INBTSerializable的序列化方法（Forge自动调用）
//    @Override
//    public CompoundTag serializeNBT() {
//        return rankCapability.serializeNBT();
//    }
//
//    // 实现INBTSerializable的反序列化方法（Forge自动调用）
//    @Override
//    public void deserializeNBT(CompoundTag nbt) {
//        rankCapability.deserializeNBT(nbt);
//    }
//
//    // 事件监听方法：实体创建时触发，给玩家附加Rank能力
//    @Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
//    public static class ForgeEvents {
//        @SubscribeEvent
//        public static void attachCapability(AttachCapabilitiesEvent<Entity> event) {
//            // 只给玩家实体附加Rank能力（其他实体如怪物不处理）
//            if (event.getObject() instanceof Player) {
//                Player player = (Player) event.getObject();
//                RankCapabilityProvider provider = new RankCapabilityProvider();
//                // 创建RankCapabilityProvider这个大类的一个实例（对象），把他塞给玩家
//                event.addCapability(RANK_CAP_ID, provider);
//                // 玩家实体销毁时，释放LazyOptional
//                event.addListener(() -> provider.lazyCapability.invalidate());
//            }
//        }
//
//        // 修复核心：统一处理所有玩家克隆事件，不再区分死亡/维度传送
//        @SubscribeEvent
//        public static void onPlayerClone(PlayerEvent.Clone event) {
//            Player originalPlayer = event.getOriginal();
//            Player newPlayer = event.getEntity();
//
//            // 重要修复：不再区分死亡克隆和非死亡克隆，统一处理所有情况
//            boolean isDeath = event.isWasDeath();
//            String logType = isDeath ? "死亡克隆" : "维度传送/其他克隆";
//
//            EconomySystem.LOGGER.info("{}，复制Rank数据: {} -> {}",
//                    logType,
//                    originalPlayer.getScoreboardName(),
//                    newPlayer.getScoreboardName());
//
//            // 关键修复：无论什么类型的克隆，都先尝试从原玩家复制数据
//            originalPlayer.reviveCaps();
//
//            // 方法1：直接从原玩家的Capability复制
//            originalPlayer.getCapability(RANK_CAPABILITY).ifPresent(oldCap -> {
//                newPlayer.getCapability(RANK_CAPABILITY).ifPresent(newCap -> {
//                    // 复制Rank数据
//                    Rank oldRank = oldCap.getRank();
//                    if (oldRank != null && oldRank != RankRegistry.NO_RANK) {
//                        newCap.setRank(oldRank);
//                        EconomySystem.LOGGER.info("从原玩家Capability复制成功: Player={}, Rank={}",
//                                newPlayer.getScoreboardName(), oldRank.getRankName());
//
//                        // 修复：立即保存到持久化数据
//                        if (newPlayer instanceof ServerPlayer serverPlayer) {
//                            saveToPersistentData(serverPlayer, newCap);
//                        }
//                    } else {
//                        // 如果原玩家数据为空，尝试从持久化数据恢复
//                        restoreFromPersistentData(newPlayer, "原玩家Capability数据为空");
//                    }
//                });
//            });
//
//            originalPlayer.invalidateCaps();
//
//            // 方法2：确保从持久化数据恢复（双重保障）
//            if (newPlayer instanceof ServerPlayer serverPlayer) {
//                serverPlayer.server.execute(() -> {
//                    restoreFromPersistentData(serverPlayer, "克隆事件后的持久化恢复");
//
//                    // 方法3：保存到新玩家的持久化数据并同步到客户端
//                    serverPlayer.getCapability(RANK_CAPABILITY).ifPresent(cap -> {
//                        saveToPersistentData(serverPlayer, cap);
//
//                        // 关键修复：必须同步到客户端！
//                        syncToClient(serverPlayer, cap);
//
//                        EconomySystem.LOGGER.info("克隆完成后最终状态: Player={}, Rank={}",
//                                serverPlayer.getScoreboardName(), cap.getRank().getRankName());
//                    });
//                });
//            }
//        }
//
//        // 新增：统一的持久化数据恢复方法
//        private static void restoreFromPersistentData(Player player, String reason) {
//            if (player instanceof ServerPlayer serverPlayer) {
//                serverPlayer.getCapability(RANK_CAPABILITY).ifPresent(cap -> {
//                    // 检查当前是否已有有效数据
//                    if (cap.getRank() != null && cap.getRank() != RankRegistry.NO_RANK) {
//                        EconomySystem.LOGGER.debug("{}: Player={}, 已有有效数据Rank={}",
//                                reason, serverPlayer.getScoreboardName(), cap.getRank().getRankName());
//                        return; // 已有有效数据，不需要恢复
//                    }
//
//                    // 从持久化数据恢复
//                    CompoundTag playerData = serverPlayer.getPersistentData();
//                    CompoundTag persistedData = playerData.getCompound(Player.PERSISTED_NBT_TAG);
//
//                    String rankKey = EconomySystem.MODID + ":rank";
//                    if (persistedData.contains(rankKey)) {
//                        CompoundTag rankData = persistedData.getCompound(rankKey);
//                        cap.deserializeNBT(rankData);
//                        EconomySystem.LOGGER.info("{}: Player={}, 从持久化数据恢复Rank={}",
//                                reason, serverPlayer.getScoreboardName(), cap.getRank().getRankName());
//                    } else {
//                        // 持久化数据也没有，设置默认值
//                        cap.setRank(RankRegistry.NO_RANK);
//                        EconomySystem.LOGGER.warn("{}: Player={}, 持久化数据为空，设置为默认Rank",
//                                reason, serverPlayer.getScoreboardName());
//                    }
//                });
//            }
//        }
//
//        // 新增：统一的持久化数据保存方法
//        private static void saveToPersistentData(ServerPlayer player, IRankCapability cap) {
//            CompoundTag rankData = cap.serializeNBT();
//            CompoundTag playerData = player.getPersistentData();
//            CompoundTag persistedData = playerData.getCompound(Player.PERSISTED_NBT_TAG);
//            persistedData.put(EconomySystem.MODID + ":rank", rankData);
//            playerData.put(Player.PERSISTED_NBT_TAG, persistedData);
//
//            EconomySystem.LOGGER.debug("保存到持久化数据: Player={}, Rank={}",
//                    player.getScoreboardName(), cap.getRank().getRankName());
//        }
//
//        // 新增：统一的数据同步方法
//        private static void syncToClient(ServerPlayer player, IRankCapability cap) {
//            try {
//                EconomySystem_NetworkManager.INSTANCE.send(
//                        PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
//                        new Packet_SyncRankTitle(player)
//                );
//                EconomySystem.LOGGER.info("同步到客户端: Player={}, Rank={}",
//                        player.getScoreboardName(), cap.getRank().getRankName());
//            } catch (Exception e) {
//                EconomySystem.LOGGER.error("同步到客户端失败: Player={}", player.getScoreboardName(), e);
//            }
//        }
//
//        // 玩家重生事件监听
//        @SubscribeEvent
//        public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
//            Player player = event.getEntity();
//
//            if (player instanceof ServerPlayer serverPlayer) {
//                // 延迟执行确保数据加载完成
//                serverPlayer.server.execute(() -> {
//                    // 关键修复：重生时强制从持久化数据恢复
//                    restoreFromPersistentData(serverPlayer, "重生事件");
//
//                    serverPlayer.getCapability(RANK_CAPABILITY).ifPresent(cap -> {
//                        // 确保数据保存到持久化存储
//                        saveToPersistentData(serverPlayer, cap);
//
//                        // 同步到客户端
//                        syncToClient(serverPlayer, cap);
//                    });
//                });
//            }
//        }
//
//        // 玩家维度切换事件 - 关键修复：监听维度切换
//        @SubscribeEvent
//        public static void onChangeDimension(net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
//            Player player = event.getEntity();
//
//            if (player instanceof ServerPlayer serverPlayer) {
//                EconomySystem.LOGGER.info("玩家维度切换: {} 从 {} 到 {}",
//                        serverPlayer.getScoreboardName(),
//                        event.getFrom().location(),
//                        event.getTo().location());
//
//                // 延迟一tick确保玩家实体完全初始化
//                serverPlayer.server.execute(() -> {
//                    // 维度切换后确保从持久化数据加载
//                    restoreFromPersistentData(serverPlayer, "维度切换");
//
//                    serverPlayer.getCapability(RANK_CAPABILITY).ifPresent(cap -> {
//                        // 确保数据保存到持久化存储（修复：切换维度时也要保存）
//                        saveToPersistentData(serverPlayer, cap);
//
//                        // 同步到客户端
//                        syncToClient(serverPlayer, cap);
//
//                        EconomySystem.LOGGER.info("维度切换后数据: Player={}, Rank={}",
//                                serverPlayer.getScoreboardName(), cap.getRank().getRankName());
//                    });
//                });
//            }
//        }
//
//        // 新增：玩家实体完全追踪时同步数据（重要修复）
//        @SubscribeEvent
//        public static void onStartTracking(net.minecraftforge.event.entity.player.PlayerEvent.StartTracking event) {
//            // 当其他玩家开始追踪这个玩家时（包括自己重新连接）
//            if (event.getTarget() instanceof ServerPlayer targetPlayer) {
//                targetPlayer.getCapability(RANK_CAPABILITY).ifPresent(cap -> {
//                    syncToClient(targetPlayer, cap);
//                    EconomySystem.LOGGER.debug("开始追踪时同步: Target={}, Rank={}",
//                            targetPlayer.getScoreboardName(), cap.getRank().getRankName());
//                });
//            }
//        }
//
//        // 玩家登录时加载持久化数据
//        @SubscribeEvent
//        public static void onPlayerLogin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
//            Player player = event.getEntity();
//
//            if (player instanceof ServerPlayer serverPlayer) {
//                // 强制从持久化数据加载
//                restoreFromPersistentData(serverPlayer, "登录事件");
//
//                // 延迟同步确保客户端准备就绪
//                serverPlayer.server.execute(() -> {
//                    serverPlayer.getCapability(RANK_CAPABILITY).ifPresent(cap -> {
//                        syncToClient(serverPlayer, cap);
//                        EconomySystem.LOGGER.info("登录后同步数据: Player={}, Rank={}",
//                                serverPlayer.getScoreboardName(), cap.getRank().getRankName());
//                    });
//                });
//            }
//        }
//
//        // 玩家退出时保存数据
//        @SubscribeEvent
//        public static void onPlayerLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
//            Player player = event.getEntity();
//
//            player.getCapability(RANK_CAPABILITY).ifPresent(cap -> {
//                if (player instanceof ServerPlayer serverPlayer) {
//                    saveToPersistentData(serverPlayer, cap);
//                    EconomySystem.LOGGER.info("退出时保存Rank数据: {}, Rank={}",
//                            player.getScoreboardName(), cap.getRank().getRankName());
//                }
//            });
//        }
//
//        // 新增：定期保存数据（可选，增加安全性）
//        @SubscribeEvent
//        public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
//            // 每600ticks（30秒）自动保存一次
//            if (event.phase == net.minecraftforge.event.TickEvent.Phase.END &&
//                    event.player.tickCount % 600 == 0 &&
//                    event.player instanceof ServerPlayer serverPlayer) {
//
//                serverPlayer.getCapability(RANK_CAPABILITY).ifPresent(cap -> {
//                    saveToPersistentData(serverPlayer, cap);
//                    // 可选：定期同步到客户端
//                    if (event.player.tickCount % 1200 == 0) { // 每60秒同步一次
//                        syncToClient(serverPlayer, cap);
//                    }
//                });
//            }
//        }
//
//        // 新增：玩家加入世界时同步数据（Forge特有事件）
//        @SubscribeEvent
//        public static void onPlayerJoinWorld(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
//            if (event.getEntity() instanceof ServerPlayer serverPlayer) {
//                // 延迟一tick确保所有数据就绪
//                serverPlayer.server.execute(() -> {
//                    serverPlayer.getCapability(RANK_CAPABILITY).ifPresent(cap -> {
//                        syncToClient(serverPlayer, cap);
//                        EconomySystem.LOGGER.debug("加入世界时同步: Player={}, Rank={}",
//                                serverPlayer.getScoreboardName(), cap.getRank().getRankName());
//                    });
//                });
//            }
//        }
//    }
//
//    //获取玩家的rank，工具方法
//    public static Rank getPlayerRank(Player player) {
//        if (RANK_CAPABILITY == null) {
//            EconomySystem.LOGGER.warn("RANK_CAPABILITY尚未初始化");
//            return RankRegistry.NO_RANK;
//        }
//
//        LazyOptional<IRankCapability> capOptional = player.getCapability(RANK_CAPABILITY);
//        if (!capOptional.isPresent()) {
//            EconomySystem.LOGGER.warn("玩家 {} 的Rank Capability不存在", player.getScoreboardName());
//            return RankRegistry.NO_RANK;
//        }
//
//        Rank rank = capOptional.map(IRankCapability::getRank).orElse(RankRegistry.NO_RANK);
//        return rank;
//    }
//
//    // 工具方法：设置玩家Rank
//    public static void setPlayerRank(Player player, Rank rank) {
//        if (RANK_CAPABILITY == null) {
//            EconomySystem.LOGGER.error("RANK_CAPABILITY尚未初始化，无法设置Rank");
//            return;
//        }
//
//        LazyOptional<IRankCapability> capOptional = player.getCapability(RANK_CAPABILITY);
//        if (!capOptional.isPresent()) {
//            EconomySystem.LOGGER.error("玩家 {} 的Rank Capability不存在，无法设置Rank", player.getScoreboardName());
//            return;
//        }
//
//        capOptional.ifPresent(cap -> {
//            cap.setRank(rank);
//            EconomySystem.LOGGER.info("设置玩家Rank: {}, Rank={}", player.getScoreboardName(), rank.getRankName());
//
//            // 立即保存到持久化数据
//            if (player instanceof ServerPlayer serverPlayer) {
//                saveToPersistentData(serverPlayer, cap);
//                // 立即同步到客户端
//                syncToClient(serverPlayer);
//            }
//        });
//    }
//
//    // 新增：统一的持久化数据保存方法（公开版本）
//    public static void saveToPersistentData(ServerPlayer player, IRankCapability cap) {
//        CompoundTag rankData = cap.serializeNBT();
//        CompoundTag playerData = player.getPersistentData();
//        CompoundTag persistedData = playerData.getCompound(Player.PERSISTED_NBT_TAG);
//        persistedData.put(EconomySystem.MODID + ":rank", rankData);
//        playerData.put(Player.PERSISTED_NBT_TAG, persistedData);
//    }
//
//    // 新增：统一的数据同步方法（公开版本）
//    private static void syncToClient(ServerPlayer player) {
//        if (RANK_CAPABILITY == null) return;
//
//        player.getCapability(RANK_CAPABILITY).ifPresent(cap -> {
//            try {
//                EconomySystem_NetworkManager.INSTANCE.send(
//                        PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
//                        new Packet_SyncRankTitle(player)
//                );
//                EconomySystem.LOGGER.info("同步到客户端: Player={}, Rank={}",
//                        player.getScoreboardName(), cap.getRank().getRankName());
//            } catch (Exception e) {
//                EconomySystem.LOGGER.error("同步到客户端失败: Player={}", player.getScoreboardName(), e);
//            }
//        });
//    }
//}