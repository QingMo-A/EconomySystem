package com.mo.economy_system.server.chattitle.capability;

import com.google.common.eventbus.Subscribe;
import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.ranktitle_system.Packet_SyncRankTitle;
import com.mo.economy_system.server.chattitle.Title;
import com.mo.economy_system.server.chattitle.TitleRegistry;
import com.mo.economy_system.server.headdisplay.HeadDisplay;
import com.mo.economy_system.server.rank.capability.IRankCapability;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TitleCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    public static final ResourceLocation Title_Capability_ID = new ResourceLocation(EconomySystem.MODID, "title");
    public static Capability<ITitleCapability> Title_CAPABILITY_BIAOSHI;
    private final ITitleCapability titleCapability = new TitleCapability();
    private final LazyOptional<ITitleCapability> lazyCapability = LazyOptional.of(() -> titleCapability);

    @SubscribeEvent
    //注册能力
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(ITitleCapability.class);
        Title_CAPABILITY_BIAOSHI = CapabilityManager.get(new CapabilityToken<>() {});
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        // 只有是Title这个能力才行
        if (cap == Title_CAPABILITY_BIAOSHI) {
            // cast()：LazyOptional源码提供的安全类型转换方法，将LazyOptional<IRankCapability>转为LazyOptional<T>
            return lazyCapability.cast();
        }
        // 返回LazyOptional空实例（非null），符合接口规范，避免空指针
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return titleCapability.serializeNBT();
    }

    // 从NBT读取称号数据（Forge自动调用，用于加载）
    @Override
    public void deserializeNBT(CompoundTag nbt) {
        titleCapability.deserializeNBT(nbt);
    }

    // 监听实体创建事件，给玩家附加称号能力
    @Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof Player) {
                TitleCapabilityProvider provider = new TitleCapabilityProvider();
                event.addCapability(Title_Capability_ID, provider);
                // 玩家下线时释放资源
                event.addListener(() -> provider.lazyCapability.invalidate());

                // 客户端同步
                if (FMLEnvironment.dist == Dist.CLIENT) {
                    ((Player) event.getObject()).getCapability(Title_CAPABILITY_BIAOSHI).ifPresent(cap -> {
                        // 客户端自动加载服务端同步的NBT数据
                    });
                }
            }
        }
    }

    // 工具方法：获取玩家的当前称号
    public static Title getPlayerTitle(Player player) {
        if (Title_CAPABILITY_BIAOSHI == null) {
            return TitleRegistry.getDefaultTitle(); // 能力未初始化时返回默认
        }

        // 从玩家身上获取能力
        LazyOptional<ITitleCapability> capOptional = player.getCapability(Title_CAPABILITY_BIAOSHI);
        // 如果获取成功，返回称号；否则返回默认
        return capOptional.map(ITitleCapability::getTitle).orElse(TitleRegistry.getDefaultTitle());
    }

    // 工具方法：给玩家设置称号
    public static void setPlayerTitle(Player player, Title title) {
        if (Title_CAPABILITY_BIAOSHI == null) {
            return; // 能力未初始化，不执行
        }
        LazyOptional<ITitleCapability> capOptional = player.getCapability(Title_CAPABILITY_BIAOSHI);
        capOptional.ifPresent(cap -> cap.setTitle(title));
        // 新增：服务器端立即同步
        if (player instanceof ServerPlayer serverPlayer) {
            EconomySystem_NetworkManager.INSTANCE.send(
                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> serverPlayer),
                    new Packet_SyncRankTitle(serverPlayer)
            );
        }
    }
}
