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
//import net.neoforged.api.distmarker.Dist;
//import net.neoforged.neoforge.capabilities.*;
//import net.neoforged.neoforge.common.util.LazyOptional;
//import net.neoforged.neoforge.common.util.NonNullSupplier;
//import net.neoforged.neoforge.event.AttachCapabilitiesEvent;
//import net.neoforged.bus.api.SubscribeEvent;
//import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
//import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
//import net.neoforged.fml.loading.FMLEnvironment;
//import net.neoforged.neoforge.network.PacketDistributor;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//import net.neoforged.neoforge.common.util.INBTSerializable; // 必须导入这个接口
//
///*
//等级（rank）系统通过Capability的实现思路
//Capability是一个能力系统，可以把属性给一个实体
//为了使用Capability，你需要先确定你想自定义你的能力可以干什么，比如可以施法，可以飞行，把这些作为一个接口，通常命名IxxCapability
//随后你需要写一个类实现你接口里的功能，这个类通常叫xxCapability
//接下来，你需要一个“provider”类，称为能力的提供者
//这个类的核心任务是重写forge里面的ICapabilityProvider接口定义的方法
//在这个provider类里面，你需要通过mod注解让forge知道你这个类，定义一个静态，数据类型为Capability<IxxxCapability>的变量，用来匹配 “是不是这个类型的能力”
//做完上面的步骤之后，对每个实体的Provider这个类创建独立的xxCapability对象，本质上就是你需要定义一个变量，类型是你写了功能的xxCapability这个类，这是整个Capability系统抽象的最底层，是玩家的一个“单元”
//然后创建一个懒加载容器，优化性能以及防止空指针
//所有的初始化做完了以后，通过监听RegisterCapabilitiesEvent让forge在游戏里注册你的能力
//随后重写getCapability，这个是为了让forge匹配是这个能力，而不是其他模组的其他能力
//完成了以后，通过监听你想要的事件，先对你这个provider类创建一个对象（实例）用addCapability方法把这个对象绑定到你想要绑定到的实体上
//最后编写工具类方法
//工具类方法需要通过调用getCapability匹配了是我想要的这个能力以后，从实体上面拿能力，然后调用你之前写的那些能干什么的代码，比如飞行什么什么什么
//
//1.把rank抽象成能力，这个能力可以设置rank，读取玩家现在的rank，此外为了存储玩家的rank，在IRankCapability接口定义两个方法（对齐INBTSerializable<CompoundTag>），为NBT序列化和反序列化（forge要求）
//接口存为IRankCapability
//2.通过RankCapability实现上面的接口
//3.forge规定：Capability要成功注册，需要实现ICapabilityProvider里面的接口
//接下来开始实现：
//4.创建RankCapabilityProvider这个类，在类开始之前加入MOD注解,模组加载时会加载这个类
//5.在这个类里面先设置唯一id，这样forge就知道是rank而不是其他模组或者同模组的其他能力
//6.设置静态变量RANK_CAPABILITY，类型为Capability<IRankCapability>，作为一个标签，用于能力的匹配，先不赋值
//7.设置一个变量rankCapability，类型为之前实现了接口IRankCapability，实际上是RankCapability这个你自己定义的能力类的对象，是底层，用来实现你想要赋予的能力,每个玩家都有独立的RankCapability
//8.加入懒加载容器lazyCapability，只有rank需要被使用了，才会通过10里面重写的getCapability方法把上面的对象rankCapability赋值给他。forge要求这么写，节省性能且防止空指针
//9.让forge事件把rank这个能力加入到游戏里
//10.重写getCapability，forge要求的，用来匹配是不是这个能力，是的话返回懒容器里面的rankCapability对象
//11.再次重写NBT序列化和反序列化，不过这次是写INBTSerializable<CompoundTag>这个接口里面的，为了让forge能存储数据
//12.再写一个注解，监听游戏内事件@EventBusSubscriber(modid = EconomySystem.MODID)
//定义一个内部类，当实体生成，先判断是不是玩家，然后创建一个provider的对象（实例），数据类型是这个“能力给予”的大类RankCapabilityProvider，provider通过addCapability塞给玩家
//
//13.接下来可以写你自己想要让外面调用的工具方法：
//getPlayerRank：先调用前面重写的getCapability，匹配能力成功以后，得到懒加载容器
//懒容器里面本质上就是最底层的rankCapability，调用get方法就可以返回现在的rank了
// */
//
//
//// 注册到模组事件总线
//@EventBusSubscriber(modid = EconomySystem.MODID)
//public class RankCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
//    // 设置唯一ID,避免冲突
//    public static final ResourceLocation RANK_CAP_ID = ResourceLocation.fromNamespaceAndPath(EconomySystem.MODID, "player_rank");
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
//    @EventBusSubscriber(modid = EconomySystem.MODID)
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
//
//                // 关键：客户端同步（Forge会自动同步NBT数据到客户端）
//                if (FMLEnvironment.dist == Dist.CLIENT) {
//                    player.getCapability(RANK_CAPABILITY).ifPresent(cap -> {
//                        // 客户端初始化时从服务端同步的NBT加载数据
//                    });
//                }
//            }
//        }
//    }
//
//    //获取玩家的rank，工具方法
//    public static Rank getPlayerRank(Player player) {
//        if (RANK_CAPABILITY == null) {
//            return RankRegistry.NO_RANK;
//        }
//
//        LazyOptional<IRankCapability> capOptional = player.getCapability(RANK_CAPABILITY);
//        // 正确的获取方式：使用orElseGet避免空指针
//        return capOptional.map(IRankCapability::getRank).orElse(RankRegistry.NO_RANK);
//    }
//
//    // 工具方法：设置玩家Rank
//    public static void setPlayerRank(Player player, Rank rank) {
//        if (RANK_CAPABILITY == null) {
//            return;
//        }
//
//        LazyOptional<IRankCapability> capOptional = player.getCapability(RANK_CAPABILITY);
//        capOptional.ifPresent(cap -> cap.setRank(rank));
//
//        // 新增：服务器端立即同步给所有能看到该玩家的客户端（包括自己）
//        if (player instanceof ServerPlayer serverPlayer) {
//            EconomySystem_NetworkManager.INSTANCE.send(
//                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> serverPlayer),
//                    new Packet_SyncRankTitle(serverPlayer)
//            );
//        }
//    }
//}
//
