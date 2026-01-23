package com.mo.economy_system.commands.notice_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.server.notice.NoticeData;
import com.mo.economy_system.server.notice.NoticeManager;
import com.mo.economy_system.server.serverui.tips.TipPushHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 公告管理指令
 * /notice add "标题" "内容" - 添加新公告（引号内可含空格）
 * /notice delete <ID> - 删除指定ID的公告
 * /notice list - 列出所有公告
 * /notice reload - 重新加载公告配置
 *
 * 支持 & 符号代替颜色符号：
 * &a(绿) &b(青) &c(红) &d(粉) &e(黄) &f(白) &l(粗体) &m(删除线) &n(下划线) &o(斜体) &r(重置)
 */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID)
public class Command_Notice {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // /notice add "标题" "内容"
        dispatcher.register(
                Commands.literal("notice")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("add")
                                .then(Commands.argument("input", StringArgumentType.greedyString())
                                        .executes(Command_Notice::executeAddNotice)
                                )
                        )
        );

        // /notice delete <ID>
        dispatcher.register(
                Commands.literal("notice")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("delete")
                                .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                        .executes(Command_Notice::executeDeleteNotice)
                                )
                        )
        );

        // /notice list
        dispatcher.register(
                Commands.literal("notice")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("list")
                                .executes(Command_Notice::executeListNotices)
                        )
        );

        // /notice reload
        dispatcher.register(
                Commands.literal("notice")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("reload")
                                .executes(Command_Notice::executeReload)
                        )
        );
    }

    /**
     * 添加新公告
     * 用法：/notice add "标题" "内容"
     * 示例：/notice add "新活动" "&e周末开启双倍经验活动！&r"
     *
     * 支持 & 符号代替颜色符号
     */
    private static int executeAddNotice(CommandContext<CommandSourceStack> context) {
        String input = StringArgumentType.getString(context, "input");

        // 解析引号包裹的参数
        String[] parts = parseQuotedStrings(input);
        if (parts.length < 2) {
            context.getSource().sendFailure(Component.literal("§c格式错误！正确格式: /notice add \"标题\" \"内容\""));
            return 0;
        }

        String title = parts[0];
        String content = parts[1];

        // 将 & 替换为 §
        String formattedTitle = title.replace("&", "§");
        String formattedContent = content.replace("&", "§");

        int newId = NoticeManager.getMaxNoticeId() + 1;
        long now = System.currentTimeMillis();

        NoticeData newNotice = new NoticeData(newId, formattedTitle, formattedContent, now);

        if (NoticeManager.addNotice(newNotice)) {
            context.getSource().sendSuccess(
                    () -> Component.literal("§a成功添加公告 [ID:" + newId + "]: " + formattedTitle),
                    true
            );
            // 向全服广播新公告提示
            TipPushHelper.broadcastTipToAllPlayers("§b§l您有新的公告需要查看", 15000);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("§c添加公告失败"));
            return 0;
        }
    }

    /**
     * 解析引号包裹的字符串
     * 例如：输入 "标题" "内容" -> 返回 ["标题", "内容"]
     */
    private static String[] parseQuotedStrings(String input) {
        java.util.List<String> result = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '"') {
                if (inQuotes) {
                    // 结束引号
                    result.add(current.toString());
                    current = new StringBuilder();
                }
                inQuotes = !inQuotes;
            } else if (inQuotes) {
                current.append(c);
            }
        }

        // 如果最后一个引号没有闭合
        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result.toArray(new String[0]);
    }

    /**
     * 删除指定ID的公告
     * 用法：/notice delete <ID>
     */
    private static int executeDeleteNotice(CommandContext<CommandSourceStack> context) {
        int noticeId = IntegerArgumentType.getInteger(context, "id");

        if (NoticeManager.deleteNotice(noticeId)) {
            context.getSource().sendSuccess(
                    () -> Component.literal("§a成功删除公告 [ID:" + noticeId + "]"),
                    true
            );
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("§c删除失败：未找到ID为 " + noticeId + " 的公告"));
            return 0;
        }
    }

    /**
     * 列出所有公告
     * 用法：/notice list
     */
    private static int executeListNotices(CommandContext<CommandSourceStack> context) {
        var notices = NoticeManager.getNotices();

        if (notices.isEmpty()) {
            context.getSource().sendSuccess(
                    () -> Component.literal("§e当前没有公告"),
                    false
            );
            return 0;
        }

        context.getSource().sendSuccess(
                () -> Component.literal("§6===== 公告列表 (共 " + notices.size() + " 条) ====="),
                false
        );

        for (NoticeData notice : notices) {
            String timeStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm")
                    .format(new java.util.Date(notice.getPublishTime()));
            context.getSource().sendSuccess(
                    () -> Component.literal("§e[ID:" + notice.getNoticeId() + "] §f" + notice.getNoticeTitle()
                            + " §7(" + timeStr + ")"),
                    false
            );
        }

        return notices.size();
    }

    /**
     * 重新加载公告配置
     * 用法：/notice reload
     */
    private static int executeReload(CommandContext<CommandSourceStack> context) {
        NoticeManager.loadFromConfig();
        int count = NoticeManager.getNotices().size();

        context.getSource().sendSuccess(
                () -> Component.literal("§a已重新加载公告配置，当前共 " + count + " 条公告"),
                true
        );
        return 1;
    }
}
