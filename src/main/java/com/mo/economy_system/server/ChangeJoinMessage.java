package com.mo.economy_system.server;

import com.mo.economy_system.EconomySystem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

// 注册事件入口，只有注册了这个类才能执行后面的代码
@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)

public class ChangeJoinMessage {
    private static ForgeConfigSpec COMMON_CONFIG_SPEC;    //声明COMMON_CONFIG_SPEC变量为配置规则容器，存储后面的配置项
    private static ForgeConfigSpec.ConfigValue<String> JOIN_MESSAGE;     //声明JOIN_MESSAGE为forge配置文件规定的字符串变量
    private static ForgeConfigSpec.ConfigValue<String> LEAVE_MESSAGE;   //同上

    // 静态代码块，类初始化只会执行一次
    static {
        ForgeConfigSpec.Builder configBuilder = new ForgeConfigSpec.Builder();    //用builder这个类，创建一个配置文件构建器，放后面的规则

        // 进服消息配置
        JOIN_MESSAGE = configBuilder
                .comment("玩家进服自定义消息 | 占位符：%player%=玩家名 | 颜色代码：§a绿 §c红 §6金 §b蓝")
                .define("join_message", "§7[§a+§7]§b鱼友§6%player%§b来和你VAN辣！");     //本质给上面创建配置文件的对象赋值，赋值了配置文件注释和内容，然后再赋值给JOIN_MESSAGE

        // 离开消息配置
        LEAVE_MESSAGE = configBuilder
                .comment("玩家离开自定义消息 | 占位符：%player%=玩家名 | 颜色代码：§a绿 §c红 §6金 §b蓝")
                .define("leave_message", "§7[§c-§7]§b鱼友§6%player%§b不想和你VAN辣！");  //同上

        COMMON_CONFIG_SPEC = configBuilder.build();         //configbuilder又有进服消息，也有出服消息，再全部统一构建成配置规则
    }

    // 注册配置文件
    public static void registerConfig() {
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.COMMON,          // 配置类型：通用配置（服务端优先）
                COMMON_CONFIG_SPEC,             // 把上面构建好的配置规则拿下来用进行注册
                "economy_system-join_message.toml"  // 配置文件的名字
        );
    }

    // 获取配置文件的内容
    private static String getConfigValue(ForgeConfigSpec.ConfigValue<String> configValue) {
        // 配置未加载，那么返回默认值；配置加载了但是是空的，返回默认值，如果返回的不是空的，那么返回自定义值
        if (!COMMON_CONFIG_SPEC.isLoaded()) {
            return configValue.getDefault();
        }
        String customValue = configValue.get();
        return (customValue == null || customValue.isBlank()) ? configValue.getDefault() : customValue;
    }

    // 监听玩家进服事件，类似单片机中断
    @SubscribeEvent
    public static void onPlayerJoinServer(PlayerEvent.PlayerLoggedInEvent event) {   //括号里的内容是监听的对象事件
        // 通过监听到的信息获取事件关联的实体，再检查是不是属于服务器玩家，如果是的话就把名字赋值给serverPlayer变量
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        // 防止服务器未初始化导致空指针
        if (serverPlayer.getServer() == null) {
            return;
        }

        // 获取配置消息后替换占位符
        String rawMsg = getConfigValue(JOIN_MESSAGE);
        String formattedMsg = rawMsg.replace("%player%", serverPlayer.getName().getString());

        // 发送消息给所有在线玩家（false=非强制系统消息）
        serverPlayer.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal(formattedMsg),
                false
        );
    }

    // 监听玩家离开事件
    @SubscribeEvent
    public static void onPlayerLeaveServer(PlayerEvent.PlayerLoggedOutEvent event) {

        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (serverPlayer.getServer() == null) {
            return;
        }

        String rawMsg = getConfigValue(LEAVE_MESSAGE);
        String formattedMsg = rawMsg.replace("%player%", serverPlayer.getName().getString());

        serverPlayer.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal(formattedMsg),
                false
        );
    }
}