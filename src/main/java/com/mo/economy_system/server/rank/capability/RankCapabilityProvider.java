package com.mo.economy_system.server.rank.capability;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.server.rank.Rank;
import com.mo.economy_system.server.rank.RankRegistry;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// 注册到模组事件总线
@Mod.EventBusSubscriber(modid = EconomySystem.MODID)
public class RankCapabilityProvider implements ICapabilityProvider {
    // 设置唯一ID,避免冲突
    public static final ResourceLocation RANK_CAP_ID = new ResourceLocation(EconomySystem.MODID, "player_rank");

    // 能力标识，后面靠这个匹配是什么模组的什么能力
    public static final Capability<IRankCapability> RANK_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    // rankCapability可以使用之前在接口里面定义的方法，后面是每一个玩家的一个实例
    private final IRankCapability rankCapability = new RankCapability();

    // 懒加载容器，forge官方要求这么写，节省资源+防空指针
    private final LazyOptional<IRankCapability> lazyCapability;

    // 构造方法，当需要用rank的时候，或者说是玩家上线了以后，才给懒加载容器赋值
    public RankCapabilityProvider() {
        // LazyOptional.of(() -> rankCapability)（Lambda表达式）
        // NonNullSupplier是Forge的函数式接口，仅包含一个无参、返回非null值的get()方法
        NonNullSupplier<IRankCapability> supplier = new NonNullSupplier<IRankCapability>() {
            @Override
            @NotNull // 强制返回值非null，匹配NonNullSupplier的接口要求
            public IRankCapability get() {
                // 核心逻辑：返回初始化好的Rank能力实例（不可能为null）
                return rankCapability;
            }
        };

        // 懒加载容器赋值
        this.lazyCapability = LazyOptional.of(supplier);
    }

    // 重写getCapability，forge要求，用于匹配哪个模组什么能力
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        // 只有是rank这个能力才行
        if (cap == RANK_CAPABILITY) {
            // cast()：LazyOptional源码提供的安全类型转换方法，将LazyOptional<IRankCapability>转为LazyOptional<T>
            return lazyCapability.cast();
        }
        // 返回LazyOptional空实例（非null），符合接口规范，避免空指针
        return LazyOptional.empty();
    }

    // 事件监听方法：实体创建时触发，给玩家附加Rank能力
    @SubscribeEvent
    public static void attachCapability(AttachCapabilitiesEvent<Entity> event) {
        // 只给玩家实体附加Rank能力（其他实体如怪物不处理）
        if (event.getObject() instanceof Player) {
            Player player = (Player) event.getObject();
            // 创建RankCapabilityProvider这个大类的一个实例（对象），把他塞给玩家
            event.addCapability(RANK_CAP_ID, new RankCapabilityProvider());
        }
    }

    //获取玩家的rank，工具方法
    public static Rank getPlayerRank(Player player) {
        // 从玩家身上获取Rank能力的LazyOptional容器（非null）
        LazyOptional<IRankCapability> capOptional = player.getCapability(RANK_CAPABILITY);

        // 初始化默认值（非null，避免后续返回null）
        Rank resultRank = RankRegistry.NO_RANK;

        // lambba表达式   capOptional.map(IRankCapability::getRank).orElse(RankRegistry.NO_RANK)
        //  - map(IRankCapability::getRank)：方法引用，等价于cap -> cap.getRank()
        //  - orElse()：空值时返回默认值
        //
        // 检查LazyOptional是否有效且包含实例（isPresent()是LazyOptional源码提供的方法，有效的话是1）
        if (capOptional.isPresent()) {
            IRankCapability capInstance = capOptional.resolve().orElse((IRankCapability) RankRegistry.NO_RANK);
            // 调用底层的getRank()方法，这里的capInstance实际上就是rankCapability，来获取玩家当前等级
            resultRank = capInstance.getRank();
        }

        // 5. 返回非null结果（要么是玩家实际等级，要么是默认等级）
        return resultRank;
    }

    // 工具方法：设置玩家Rank
    public static void setPlayerRank(Player player, Rank rank) {
        // 从玩家身上获取Rank能力的LazyOptional容器（非null）
        LazyOptional<IRankCapability> capOptional = player.getCapability(RANK_CAPABILITY);

        // capOptional.ifPresent(cap -> { cap.setRank(rank); })  （Lambda）
        // 检查LazyOptional是否有效且包含实例
        if (capOptional.isPresent()) {
            // 获取Rank能力实例（非null）
            IRankCapability capInstance = capOptional.resolve().orElse((IRankCapability) RankRegistry.NO_RANK);
            // 调用底层setRank()方法，设置玩家等级
            capInstance.setRank(rank);
        }
        // 无实例时不执行任何操作，避免空指针
    }
}

