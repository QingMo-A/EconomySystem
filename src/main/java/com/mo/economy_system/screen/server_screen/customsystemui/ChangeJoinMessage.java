package com.mo.economy_system.screen.server_screen.customsystemui;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.network.packets.Packet_SystemMessage;
import com.mo.economy_system.server.rank.PlayerRankManager;
import com.mo.economy_system.server.rank.Rank;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.mo.economy_system.compat.network.NetworkDirection;

// 注册事件入口，只有注册了这个类才能执行后面的代码
@EventBusSubscriber(modid = EconomySystem.MODID)
public class ChangeJoinMessage {
    private static ModConfigSpec COMMON_CONFIG_SPEC;    //声明COMMON_CONFIG_SPEC变量为配置规则容器，存储后面的配置项

    //无rank消息
    private static ModConfigSpec.ConfigValue<String> JOIN_MESSAGE;     //声明JOIN_MESSAGE为forge配置文件规定的字符串变量
    private static ModConfigSpec.ConfigValue<String> LEAVE_MESSAGE;   //同上

    // Rank专属进服消息
    private static ModConfigSpec.ConfigValue<String> JOIN_MESSAGE_NO_RANK;
    private static ModConfigSpec.ConfigValue<String> JOIN_MESSAGE_FISH;
    private static ModConfigSpec.ConfigValue<String> JOIN_MESSAGE_FISH_PLUS;
    private static ModConfigSpec.ConfigValue<String> JOIN_MESSAGE_FISH_PLUS_PLUS;
    private static ModConfigSpec.ConfigValue<String> JOIN_MESSAGE_OPERATOR;

    // Rank专属离开消息
    private static ModConfigSpec.ConfigValue<String> LEAVE_MESSAGE_NO_RANK;
    private static ModConfigSpec.ConfigValue<String> LEAVE_MESSAGE_FISH;
    private static ModConfigSpec.ConfigValue<String> LEAVE_MESSAGE_FISH_PLUS;
    private static ModConfigSpec.ConfigValue<String> LEAVE_MESSAGE_FISH_PLUS_PLUS;
    private static ModConfigSpec.ConfigValue<String> LEAVE_MESSAGE_OPERATOR;

    // 静态代码块，类初始化只会执行一次
    static {
        ModConfigSpec.Builder configBuilder = new ModConfigSpec.Builder();    //用builder这个类，创建一个配置文件构建器，放后面的规则

        // 进服消息配置
        JOIN_MESSAGE = configBuilder
                .comment("玩家进服自定义消息 | 占位符：%player%=玩家名 | 颜色代码：§a绿 §c红 §6金 §b蓝")
                .define("join_message", "§7[§a+§7]§b鱼友§e%player%§b来和你VAN辣！");     //本质给上面创建配置文件的对象赋值，赋值了配置文件注释和内容，然后再赋值给JOIN_MESSAGE

        // 离开消息配置
        LEAVE_MESSAGE = configBuilder
                .comment("玩家离开自定义消息 | 占位符：%player%=玩家名 | 颜色代码：§a绿 §c红 §6金 §b蓝")
                .define("leave_message", "§7[§c-§7]§b鱼友§e%player%§b不想和你VAN辣！");  //同上

        // Rank专属进服消息配置
        JOIN_MESSAGE_NO_RANK = configBuilder
                .comment("NO_RANK玩家进服消息 | 占位符：%player%=玩家名")
                .define("join_message_no_rank", "§7[§a+§7]§b鱼友§e%player%§b来和你VAN辣！");

        JOIN_MESSAGE_FISH = configBuilder
                .comment("FISH玩家进服消息 | 占位符：%player%=玩家名")
                .define("join_message_fish", "§a[§aFISH§a]§b鱼友§6%player%§b来和你VAN辣！");

        JOIN_MESSAGE_FISH_PLUS = configBuilder
                .comment("FISH+玩家进服消息 | 占位符：%player%=玩家名")
                .define("join_message_fish_plus", "§b[§bFISH+§b]§b鱼友§6%player%§b来和你VAN辣！");

        JOIN_MESSAGE_FISH_PLUS_PLUS = configBuilder
                .comment("FISH++玩家进服消息 | 占位符：%player%=玩家名")
                .define("join_message_fish_plus_plus", "§6[§6FISH++§6]§b鱼友§6%player%§b来和你VAN辣！");

        JOIN_MESSAGE_OPERATOR = configBuilder
                .comment("OPERATOR玩家进服消息 | 占位符：%player%=玩家名")
                .define("join_message_operator", "§c[§cOPERATOR§c]§b鱼友§6%player%§b来和你VAN辣！");

        // Rank专属离开消息配置
        LEAVE_MESSAGE_NO_RANK = configBuilder
                .comment("NO_RANK玩家离开消息 | 占位符：%player%=玩家名")
                .define("leave_message_no_rank", "§7[§c-§7]§b鱼友§e%player%§b不想和你VAN辣！");

        LEAVE_MESSAGE_FISH = configBuilder
                .comment("FISH玩家离开消息 | 占位符：%player%=玩家名")
                .define("leave_message_fish", "§a[§aFISH§a]§b鱼友§6%player%§b不想和你VAN辣！");

        LEAVE_MESSAGE_FISH_PLUS = configBuilder
                .comment("FISH+玩家离开消息 | 占位符：%player%=玩家名")
                .define("leave_message_fish_plus", "§b[§bFISH+§b]§b鱼友§6%player%§b不想和你VAN辣！");

        LEAVE_MESSAGE_FISH_PLUS_PLUS = configBuilder
                .comment("FISH++玩家离开消息 | 占位符：%player%=玩家名")
                .define("leave_message_fish_plus_plus", "§6[§6FISH++§6]§b鱼友§6%player%§b不想和你VAN辣！");

        LEAVE_MESSAGE_OPERATOR = configBuilder
                .comment("OPERATOR玩家离开消息 | 占位符：%player%=玩家名")
                .define("leave_message_operator", "§c[§cOPERATOR§c]§B鱼友§6%player%§b不想和你VAN辣！");

        COMMON_CONFIG_SPEC = configBuilder.build();         //configbuilder又有进服消息，也有出服消息，再全部统一构建成配置规则
    }

    // 获取配置文件的内容
    public static String getConfigValue(ModConfigSpec.ConfigValue<String> configValue) {
        // 配置未加载，那么返回默认值；配置加载了但是是空的，返回默认值，如果返回的不是空的，那么返回自定义值
        if (!COMMON_CONFIG_SPEC.isLoaded()) {
            return configValue.getDefault();
        }
        String customValue = configValue.get();
        return (customValue == null || customValue.isBlank()) ? configValue.getDefault() : customValue;
    }

    // 根据玩家Rank获取进服消息
    public static String getJoinMessageByRank(Rank rank) {
        return switch (rank.getRankLevel()) {
            case 0 -> getConfigValue(JOIN_MESSAGE_NO_RANK);
            case 1 -> getConfigValue(JOIN_MESSAGE_FISH);
            case 2 -> getConfigValue(JOIN_MESSAGE_FISH_PLUS);
            case 3 -> getConfigValue(JOIN_MESSAGE_FISH_PLUS_PLUS);
            case 4 -> getConfigValue(JOIN_MESSAGE_OPERATOR);
            default -> getConfigValue(JOIN_MESSAGE); // 默认消息
        };
    }

    // 根据玩家Rank获取离开消息
    public static String getLeaveMessageByRank(Rank rank) {
        return switch (rank.getRankLevel()) {
            case 0 -> getConfigValue(LEAVE_MESSAGE_NO_RANK);
            case 1 -> getConfigValue(LEAVE_MESSAGE_FISH);
            case 2 -> getConfigValue(LEAVE_MESSAGE_FISH_PLUS);
            case 3 -> getConfigValue(LEAVE_MESSAGE_FISH_PLUS_PLUS);
            case 4 -> getConfigValue(LEAVE_MESSAGE_OPERATOR);
            default -> getConfigValue(LEAVE_MESSAGE); // 默认消息
        };
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

        // 获取玩家Rank
        Rank playerRank = PlayerRankManager.getPlayerRankServer(serverPlayer);

        // 根据Rank获取消息并替换占位符
        String rawMsg = getJoinMessageByRank(playerRank);
        String formattedMsg = rawMsg.replace("%player%", serverPlayer.getName().getString());

        // 服务端发网络包给所有在线玩家的客户端（使用 Packet_SystemMessage）
        Component content = Component.literal(formattedMsg);
        int borderColor = getRankBorderColor(playerRank);  // 获取该玩家Rank的边框颜色
        for (ServerPlayer onlinePlayer : serverPlayer.getServer().getPlayerList().getPlayers()) {
            EconomySystem_NetworkManager.INSTANCE.sendTo(
                    new Packet_SystemMessage(content, borderColor),
                    onlinePlayer,
                    NetworkDirection.PLAY_TO_CLIENT
            );
        }
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

        // 获取玩家Rank
        Rank playerRank = PlayerRankManager.getPlayerRankServer(serverPlayer);
        // 根据Rank获取消息并替换占位符
        String rawMsg = getLeaveMessageByRank(playerRank);
        String formattedMsg = rawMsg.replace("%player%", serverPlayer.getName().getString());

        // 服务端发网络包给所有在线玩家的客户端（使用 Packet_SystemMessage）
        Component content = Component.literal(formattedMsg);
        int borderColor = getRankBorderColor(playerRank);  // 获取该玩家Rank的边框颜色
        for (ServerPlayer onlinePlayer : serverPlayer.getServer().getPlayerList().getPlayers()) {
            EconomySystem_NetworkManager.INSTANCE.sendTo(
                    new Packet_SystemMessage(content, borderColor),
                    onlinePlayer,
                    NetworkDirection.PLAY_TO_CLIENT
            );
        }
    }

    /**
     * 获取Rank对应的边框颜色
     */
    private static int getRankBorderColor(Rank rank) {
        if (rank == null) return 0xAAAAAA;  // 默认灰色
        return switch (rank.getRankName()) {
            case "NO_RANK" -> 0xAAAAAA;     // 灰色
            case "FISH" -> 0x55FF55;        // 绿色
            case "FISH+" -> 0x55FFFF;       // 蓝色
            case "FISH++" -> 0xFFAA00;      // 金色
            case "OPERATOR" -> 0xFF5555;    // 红色
            default -> 0xAAAAAA;
        };
    }
}
