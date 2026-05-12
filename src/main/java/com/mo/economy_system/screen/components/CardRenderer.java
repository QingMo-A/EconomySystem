package com.mo.economy_system.screen.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import com.mo.economy_system.screen.components.UiButtonRenderer;
import com.mo.economy_system.screen.components.UiButtonStyle;

/**
 * 卡片渲染工具类
 * 用于经济系统UI的现代化卡片风格渲染
 */
public class CardRenderer {

    // ==================== 颜色常量 ====================
    /** 卡片背景色（折中的半透明） */
    public static final int CARD_BG = 0x801A1A2A;
    /** 卡片背景色（悬停） */
    public static final int CARD_BG_HOVER = 0x901A1A2A;
    /** 卡片边框色 */
    public static final int CARD_BORDER = 0xFF4A5568;
    /** 卡片边框色（悬停） */
    public static final int CARD_BORDER_HOVER = 0xFF6A7588;
    /** 描述文字颜色 */
    public static final int TEXT_DESC = 0xB0FFFFFF;
    /** 数值文字颜色 */
    public static final int TEXT_VALUE = 0xFFFFFFFF;
    /** 标题文字颜色 */
    public static final int TEXT_TITLE = 0xFFFFFFFF;

    // 卡片主题色
    /** 商店卡片主题色（橙色） */
    public static final int THEME_SHOP = 0xFFFF8C00;
    /** 市场卡片主题色（蓝色） */
    public static final int THEME_MARKET = 0xFF4FC3F7;
    /** 快递箱卡片主题色（绿色） */
    public static final int THEME_DELIVERY = 0xFF27AE60;
    /** 领地卡片主题色（紫色） */
    public static final int THEME_TERRITORY = 0xFF9B59B6;
    /** 关于卡片主题色（灰色） */
    public static final int THEME_ABOUT = 0xFF888888;
    /** 余额卡片主题色（金色） */
    public static final int THEME_BALANCE = 0xFFFFD700;
    /** 富豪榜卡片主题色（青色） */
    public static final int THEME_LEADERBOARD = 0xFF1ABC9C;

    // 按钮颜色
    /** 按钮背景色 */
    public static final int BTN_BG = 0x20FFFFFF;
    /** 按钮背景色（悬停） */
    public static final int BTN_BG_HOVER = 0x35FFFFFF;
    /** 按钮边框色 */
    public static final int BTN_BORDER = 0x40FFFFFF;
    /** 按钮文字颜色 */
    public static final int BTN_TEXT = 0xB0FFFFFF;
    /** 按钮文字颜色（悬停） */
    public static final int BTN_TEXT_HOVER = 0xFFFFFFFF;
    /** 按钮装饰色 */
    public static final int BTN_ACCENT = 0xFF4FC3F7;

    // 版本信息颜色
    /** 版本背景色 */
    public static final int VERSION_BG = 0x40FFFFFF;
    /** 版本文字颜色 */
    public static final int VERSION_TEXT = 0xFFFFFFFF;
    /** 版本装饰色 */
    public static final int VERSION_ACCENT = 0xFF4FC3F7;

    // ==================== 基础图形绘制 ====================

    /**
     * 绘制圆角矩形
     */
    public static void drawRoundedRect(GuiGraphics guiGraphics, int x, int y, int width, int height, int radius, int color) {
        // 主体（四个圆角除外）
        guiGraphics.fill(RenderType.gui(), x + radius, y, x + width - radius, y + height, color);
        guiGraphics.fill(RenderType.gui(), x, y + radius, x + width, y + height - radius, color);

        // 四个圆角
        guiGraphics.fill(RenderType.gui(), x, y + radius, x + radius, y + radius + radius, color);
        guiGraphics.fill(RenderType.gui(), x + width - radius, y + radius, x + width, y + radius + radius, color);
        guiGraphics.fill(RenderType.gui(), x + radius, y, x + width - radius, y + radius, color);
        guiGraphics.fill(RenderType.gui(), x + radius, y + height - radius, x + width - radius, y + height, color);
    }

    /**
     * 绘制带边框的圆角矩形
     */
    public static void drawRoundedRectWithBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int radius, int fillColor, int borderColor) {
        // 绘制填充
        drawRoundedRect(guiGraphics, x, y, width, height, radius, fillColor);

        // 绘制边框
        int borderWidth = 1;
        guiGraphics.fill(RenderType.gui(), x + radius, y, x + width - radius, y + borderWidth, borderColor);
        guiGraphics.fill(RenderType.gui(), x + radius, y + height - borderWidth, x + width - radius, y + height, borderColor);
        guiGraphics.fill(RenderType.gui(), x, y + radius, x + borderWidth, y + height - radius, borderColor);
        guiGraphics.fill(RenderType.gui(), x + width - borderWidth, y + radius, x + width, y + height - radius, borderColor);

        // 四个圆角边框
        drawRoundedCornerBorder(guiGraphics, x, y, radius, borderWidth, borderColor, true, true);
        drawRoundedCornerBorder(guiGraphics, x + width - radius, y, radius, borderWidth, borderColor, true, false);
        drawRoundedCornerBorder(guiGraphics, x, y + height - radius, radius, borderWidth, borderColor, false, true);
        drawRoundedCornerBorder(guiGraphics, x + width - radius, y + height - radius, radius, borderWidth, borderColor, false, false);
    }

    /**
     * 绘制圆角边框角
     */
    private static void drawRoundedCornerBorder(GuiGraphics guiGraphics, int x, int y, int radius, int borderWidth, int color, boolean isTop, boolean isLeft) {
        for (int i = 0; i < radius; i++) {
            for (int j = 0; j < radius; j++) {
                // 简化的圆角边框检测
                boolean shouldDraw = false;
                if (isTop && isLeft) {
                    shouldDraw = (i == 0 || j == 0) && (i + j < radius);
                } else if (isTop && !isLeft) {
                    shouldDraw = (i == 0 || j == radius - 1) && (i + (radius - 1 - j) < radius);
                } else if (!isTop && isLeft) {
                    shouldDraw = (i == radius - 1 || j == 0) && ((radius - 1 - i) + j < radius);
                } else {
                    shouldDraw = (i == radius - 1 || j == radius - 1) && ((radius - 1 - i) + (radius - 1 - j) < radius);
                }
                if (shouldDraw) {
                    guiGraphics.fill(RenderType.gui(), x + j, y + i, x + j + 1, y + i + 1, color);
                }
            }
        }
    }

    /**
     * 绘制卡片背景（直角矩形 + 左侧主题色装饰条）
     */
    public static void drawCard(GuiGraphics guiGraphics, int x, int y, int width, int height, int themeColor, boolean isHovered) {
        // 背景色
        int bgColor = isHovered ? CARD_BG_HOVER : CARD_BG;
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + height, bgColor);

        // 边框色
        int borderColor = isHovered ? CARD_BORDER_HOVER : CARD_BORDER;
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(RenderType.gui(), x, y + height - 1, x + width, y + height, borderColor);
        guiGraphics.fill(RenderType.gui(), x + width - 1, y, x + width, y + height, borderColor);

        // 左侧主题色装饰条
        int stripAlpha = isHovered ? 0xFF : 0xCC;
        int stripColor = (stripAlpha << 24) | (themeColor & 0x00FFFFFF);
        int stripWidth = 3;
        guiGraphics.fill(RenderType.gui(), x, y, x + stripWidth, y + height, stripColor);
    }

    // ==================== 导航按钮卡片 ====================

    /**
     * 绘制导航按钮卡片
     */
    public static void drawNavCard(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height,
                                    String icon, String name, int themeColor, boolean isHovered) {
        // 绘制卡片背景
        drawCard(guiGraphics, x, y, width, height, themeColor, isHovered);

        // 内容内边距
        int padding = 8;

        // 计算文字位置（垂直居中）
        int textY = y + (height - font.lineHeight) / 2;

        // 图标
        int iconColor = isHovered ? TEXT_VALUE : TEXT_DESC;
        guiGraphics.drawString(font, icon, x + padding, textY, iconColor);

        // 名称
        int iconWidth = font.width(icon);
        int nameColor = isHovered ? TEXT_VALUE : TEXT_DESC;
        guiGraphics.drawString(font, name, x + padding + iconWidth + 4, textY, nameColor);

        // 悬停时右侧显示箭头
        if (isHovered) {
            String arrow = "▶";
            int arrowWidth = font.width(arrow);
            guiGraphics.drawString(font, arrow, x + width - padding - arrowWidth, textY, BTN_ACCENT);
        }
    }

    // ==================== 内容卡片 ====================

    /**
     * 绘制内容卡片（带标题）
     */
    public static void drawContentCard(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height,
                                       String title, String icon, int themeColor) {
        // 绘制卡片背景
        drawCard(guiGraphics, x, y, width, height, themeColor, false);

        // 标题栏分隔线
        int titleBarHeight = 24;
        guiGraphics.fill(RenderType.gui(), x + 3, y + titleBarHeight, x + width - 1, y + titleBarHeight + 1, CARD_BORDER);

        // 标题
        int padding = 8;
        int titleY = y + padding;
        int iconWidth = font.width(icon);
        guiGraphics.drawString(font, icon, x + padding, titleY, themeColor);
        guiGraphics.drawString(font, title, x + padding + iconWidth + 4, titleY, TEXT_TITLE);
    }

    /**
     * 绘制余额卡片（精简版，左侧小卡片）
     */
    public static void drawBalanceCard(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height,
                                       int balance, int playerRank) {
        drawBalanceCard(guiGraphics, font, x, y, width, height, balance, playerRank, false);
    }

    public static void drawBalanceCard(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height,
                                       int balance, int playerRank, boolean isHovered) {
        // 绘制卡片背景
        drawCard(guiGraphics, x, y, width, height, THEME_BALANCE, isHovered);

        int padding = 8;

        // 标题
        String titleIcon = "💰";
        String titleText = "你的余额";
        int titleY = y + padding;
        int iconWidth = font.width(titleIcon);
        guiGraphics.drawString(font, titleIcon, x + padding, titleY, THEME_BALANCE);
        guiGraphics.drawString(font, titleText, x + padding + iconWidth + 3, titleY, TEXT_TITLE);

        // 右侧排名
        String rightText = isHovered ? "日志 >>" : (playerRank > 0 ? "#" + playerRank : "--");
        int rightTextWidth = font.width(rightText);
        guiGraphics.drawString(font, rightText, x + width - padding - rightTextWidth, titleY, isHovered ? THEME_BALANCE : 0xFFAAFFAA);

        // 分隔线
        int separatorY = titleY + font.lineHeight + 3;
        guiGraphics.fill(RenderType.gui(), x + 3, separatorY, x + width - 1, separatorY + 1, CARD_BORDER);

        // 余额数值（大号居中）
        int balanceY = separatorY + 6;
        String balanceText = formatNumber(balance);
        int balanceTextWidth = font.width(balanceText);
        int balanceX = x + (width - balanceTextWidth) / 2;

        // 绘制余额（金色）
        guiGraphics.drawString(font, balanceText, balanceX, balanceY, THEME_BALANCE);

        // 单位
        String unitText = "梦鱼币";
        int unitWidth = font.width(unitText);
        int unitX = x + (width - unitWidth) / 2;
        guiGraphics.drawString(font, unitText, unitX, balanceY + font.lineHeight + 2, TEXT_DESC);
    }

    /**
     * 绘制交易信息卡片（右侧小卡片，点击跳转）
     */
    public static void drawTradeInfoCard(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height,
                                         int sellOrderCount, int buyOrderCount, boolean isHovered) {
        // 绘制卡片背景
        drawCard(guiGraphics, x, y, width, height, THEME_MARKET, isHovered);

        int padding = 8;

        // 标题
        String titleIcon = "📋";
        String titleText = "交易信息";
        int titleY = y + padding;
        int iconWidth = font.width(titleIcon);
        guiGraphics.drawString(font, titleIcon, x + padding, titleY, THEME_MARKET);
        guiGraphics.drawString(font, titleText, x + padding + iconWidth + 3, titleY, TEXT_TITLE);

        // 右侧点击提示
        if (isHovered) {
            String clickHint = "点击查看 >>";
            int hintWidth = font.width(clickHint);
            guiGraphics.drawString(font, clickHint, x + width - padding - hintWidth, titleY, 0xFF4FC3F7);
        }

        // 分隔线
        int separatorY = titleY + font.lineHeight + 3;
        guiGraphics.fill(RenderType.gui(), x + 3, separatorY, x + width - 1, separatorY + 1, CARD_BORDER);

        // 交易统计（两列显示）
        int statsY = separatorY + 6;
        int colWidth = (width - padding * 2) / 2;

        // 左列：卖单
        String sellLabel = "卖单";
        String sellCount = String.valueOf(sellOrderCount);
        int sellLabelWidth = font.width(sellLabel);
        int sellCountWidth = font.width(sellCount);
        guiGraphics.drawString(font, sellLabel, x + padding, statsY, TEXT_DESC);
        guiGraphics.drawString(font, sellCount, x + padding + colWidth - sellCountWidth, statsY, 0xFFFFAA00);

        // 右列：求购
        int buyColX = x + padding + colWidth;
        String buyLabel = "求购";
        String buyCount = String.valueOf(buyOrderCount);
        int buyLabelWidth = font.width(buyLabel);
        int buyCountWidth = font.width(buyCount);
        guiGraphics.drawString(font, buyLabel, buyColX, statsY, TEXT_DESC);
        guiGraphics.drawString(font, buyCount, buyColX + colWidth - buyCountWidth, statsY, 0xFF00FFFF);
    }

    /**
     * 绘制富豪榜卡片
     */
    public static void drawLeaderboardCard(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height,
                                            java.util.List<java.util.Map.Entry<String, Integer>> accounts,
                                            String playerName, int playerBalance, int scrollOffset) {
        // 绘制卡片背景
        drawCard(guiGraphics, x, y, width, height, THEME_LEADERBOARD, false);

        // 标题栏
        int titleBarHeight = 28;
        int padding = 10;
        int lineHeight = font.lineHeight + 4;

        // 标题
        String titleIcon = "🏆";
        String titleText = "富豪榜";
        int titleY = y + padding;
        int iconWidth = font.width(titleIcon);
        guiGraphics.drawString(font, titleIcon, x + padding, titleY, THEME_LEADERBOARD);
        guiGraphics.drawString(font, titleText, x + padding + iconWidth + 4, titleY, TEXT_TITLE);

        // 分隔线
        guiGraphics.fill(RenderType.gui(), x + 3, y + titleBarHeight, x + width - 1, y + titleBarHeight + 1, CARD_BORDER);

        // 富豪榜条目
        if (accounts != null && !accounts.isEmpty()) {
            int entryY = y + titleBarHeight + padding;
            int maxEntries = (height - titleBarHeight - padding * 2) / lineHeight;
            int displayCount = 0;

            // 显示前N名 + 自己
            int shownCount = 0;
            int playerIndex = -1;
            for (int i = 0; i < accounts.size(); i++) {
                if (accounts.get(i).getKey().equals(playerName)) {
                    playerIndex = i;
                    break;
                }
            }

            for (int i = 0; i < accounts.size() && displayCount < maxEntries; i++) {
                int actualIndex = i + scrollOffset;
                if (actualIndex >= accounts.size()) break;

                var entry = accounts.get(actualIndex);
                String entryName = entry.getKey();
                int entryBalance = entry.getValue();

                // 排名图标
                String rankIcon = getRankIcon(actualIndex + 1);
                int rankColor = getRankColor(actualIndex + 1);

                // 判断是否是自己
                boolean isSelf = entryName.equals(playerName);

                // 条目文字
                String entryText = String.format("[%d] %s", actualIndex + 1, entryName);
                String balanceText = formatNumber(entryBalance);

                // 绘制条目
                int textX = x + padding;
                int iconWidth2 = font.width(rankIcon);
                guiGraphics.drawString(font, rankIcon, textX, entryY, rankColor);
                guiGraphics.drawString(font, entryText, textX + iconWidth2 + 4, entryY, isSelf ? THEME_BALANCE : TEXT_DESC);

                // 余额（右对齐）
                int balanceTextWidth = font.width(balanceText);
                int balanceX = x + width - padding - balanceTextWidth;
                guiGraphics.drawString(font, balanceText, balanceX, entryY, isSelf ? THEME_BALANCE : TEXT_DESC);

                entryY += lineHeight;
                displayCount++;
            }
        }
    }

    // ==================== 版本信息 ====================

    /**
     * 绘制版本信息（左下角）- 自定义文字
     */
    public static void drawVersionInfo(GuiGraphics guiGraphics, Font font, int x, int y, int maxWidth, String titleText) {
        // 计算缩放
        int textWidth = font.width(titleText);
        float scale = Math.min(1.0f, (float) maxWidth / textWidth);

        // 背景卡片
        int bgWidth = (int) (textWidth * scale) + 16;
        int bgHeight = font.lineHeight + 10;
        int bgX = x;
        int bgY = y - bgHeight;

        drawCard(guiGraphics, bgX, bgY, bgWidth, bgHeight, VERSION_ACCENT, false);

        // 标题文字
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(bgX + 8, bgY + 5, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        guiGraphics.drawString(font, titleText, 0, 0, VERSION_TEXT);
        guiGraphics.pose().popPose();

        // 装饰线
        int lineY = bgY + bgHeight - 3;
        guiGraphics.fill(RenderType.gui(), bgX + 8, lineY, bgX + bgWidth - 8, lineY + 1, 0x30FFFFFF);
    }

    /**
     * 绘制版本信息（左下角）- 默认 EconomySystem
     */
    public static void drawVersionInfo(GuiGraphics guiGraphics, Font font, int x, int y, int maxWidth) {
        drawVersionInfo(guiGraphics, font, x, y, maxWidth, "🏠 §bEconomy§dSystem");
    }

    // ==================== 工具方法 ====================

    /**
     * 格式化数字（添加千分位分隔符）
     */
    public static String formatNumber(int num) {
        return String.format("%,d", num);
    }

    /**
     * 获取排名图标
     */
    public static String getRankIcon(int rank) {
        return switch (rank) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            default -> "  ";
        };
    }

    /**
     * 获取排名颜色
     */
    public static int getRankColor(int rank) {
        return switch (rank) {
            case 1 -> 0xFFFFD700;  // 金色
            case 2 -> 0xFFC0C0C0;  // 银色
            case 3 -> 0xFFCD7F32;  // 铜色
            default -> 0xFF888888; // 灰色
        };
    }

    /**
     * 截断文本以适应最大宽度
     */
    public static String truncateText(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        for (int i = text.length() - 1; i > 0; i--) {
            String truncated = text.substring(0, i) + "...";
            if (font.width(truncated) <= maxWidth) {
                return truncated;
            }
        }
        return "...";
    }

    // ==================== 商店商品卡片 ====================

    /**
     * 绘制商店商品卡片（优化配色）
     */
    public static void drawShopItemCard(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height,
                                        net.minecraft.world.item.ItemStack itemStack, String itemName,
                                        int price, int priceChange, boolean isHovered) {
        // 卡片背景（渐变效果）
        int cardBg = isHovered ? 0xC02A3A4A : 0xA01A2A3A;
        guiGraphics.fill(x, y, x + width, y + height, cardBg);

        // 边框（悬停时变亮）
        int borderColor = isHovered ? 0xFF6AB8FF : 0xFF4A6A8A;
        guiGraphics.fill(x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        guiGraphics.fill(x, y, x + 1, y + height, borderColor);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);

        // 顶部装饰条（蓝色）
        guiGraphics.fill(x, y, x + width, y + 3, 0xFF4FC3F7);

        int padding = 6;
        int iconSize = 32;

        // 物品名称（左上角）
        String displayName = truncateText(font, itemName, width - padding * 2);
        int nameX = x + padding;
        int nameY = y + padding;
        guiGraphics.drawString(font, displayName, nameX, nameY, 0xFFFFFFFF);

        // 物品图标（居中）
        int iconX = x + (width - iconSize) / 2;
        int iconY = y + (height - iconSize) / 2;

        // 绘制物品
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(iconX, iconY, 0);
        guiGraphics.pose().scale(2.0f, 2.0f, 1.0f);
        guiGraphics.renderItem(itemStack, 0, 0);
        guiGraphics.pose().popPose();

        // 价格（右下角）
        String priceText = "💰 " + formatNumber(price);
        int priceWidth = font.width(priceText);
        int priceX = x + width - padding - priceWidth;
        int priceY = y + height - padding - font.lineHeight;

        // 价格阴影
        guiGraphics.drawString(font, priceText, priceX + 1, priceY + 1, 0x40000000);
        // 价格本体（金色）
        guiGraphics.drawString(font, priceText, priceX, priceY, 0xFFFFD700);
    }

    /**
     * 绘制购买按钮
     */
    public static void drawBuyButton(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height,
                                     boolean isHovered) {
        // 按钮背景
        int bgColor = isHovered ? 0xFF4A4A00 : 0x3A3A00;
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + height, bgColor);

        // 边框
        int borderColor = isHovered ? 0xFFFFAA00 : 0xFFAA8800;
        guiGraphics.fill(RenderType.gui(), x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(RenderType.gui(), x, y + height - 1, x + width, y + height, borderColor);
        guiGraphics.fill(RenderType.gui(), x, y, x + 1, y + height, borderColor);
        guiGraphics.fill(RenderType.gui(), x + width - 1, y, x + width, y + height, borderColor);

        // 按钮文字
        String text = "购买";
        int textWidth = font.width(text);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - font.lineHeight) / 2;
        guiGraphics.drawString(font, text, textX, textY, 0xFFFFFFFF);
    }

    // ==================== 领地卡片 ====================

    /**
     * 领地类型枚举
     */
    public enum TerritoryType {
        OVERWORLD("🏠", "主世界", 0xFF4CAF50),
        NETHER("🔥", "下界", 0xFFFF5722),
        END("🌙", "末地", 0xFF9C27B0),
        AUTHORIZED("🚪", "有权限", 0xFF78909C);

        private final String icon;
        private final String name;
        private final int color;

        TerritoryType(String icon, String name, int color) {
            this.icon = icon;
            this.name = name;
            this.color = color;
        }

        public String getIcon() { return icon; }
        public String getName() { return name; }
        public int getColor() { return color; }
    }

    /**
     * 绘制领地卡片（按钮集成在卡片内部，详细信息直接显示）
     * @param guiGraphics 图形上下文
     * @param font 字体
     * @param x X坐标
     * @param y Y坐标
     * @param width 宽度
     * @param height 高度
     * @param territoryName 领地名称
     * @param type 领地类型
     * @param isOwned 是否拥有
     * @param isHovered 卡片是否悬停
     * @param teleportHovered 传送按钮是否悬停
     * @param manageHovered 管理按钮是否悬停（仅拥有者）
     * @param showManageButton 是否显示管理按钮
     * @param ownerName 拥有者名称
     * @param territoryId 领地ID
     * @param coordinateRange 坐标范围
     * @return 按钮区域 [传送按钮X1, Y1, X2, Y2, 管理按钮X1, Y1, X2, Y2]（管理按钮为0表示不存在）
     */
    public static int[] drawTerritoryCard(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height,
                                         String territoryName, TerritoryType type, boolean isOwned, boolean isHovered,
                                         boolean teleportHovered, boolean manageHovered, boolean showManageButton,
                                         String ownerName, String territoryId, String coordinateRange) {
        // ==================== 卡片背景 ====================
        int themeColor = isOwned ? THEME_TERRITORY : type.getColor();
        drawCard(guiGraphics, x, y, width, height, themeColor, isHovered);

        // 顶部细条强调
        guiGraphics.fill(x + 1, y, x + width - 1, y + 2, type.getColor());

        // ==================== 内容区域 ====================
        int padding = 8;
        int contentY = y + 6;

        // 领地名称（顶部，粗体，白色）
        String displayName = truncateText(font, territoryName, width - padding * 2);
        int nameWidth = font.width(displayName);
        int nameX = x + (width - nameWidth) / 2;
        guiGraphics.drawString(font, displayName, nameX, contentY, 0xFFFFFFFF);

        // 维度标签（名称下方，居中）
        String typeLabel = type.getIcon() + " " + type.getName();
        int typeLabelWidth = font.width(typeLabel);
        int typeLabelX = x + (width - typeLabelWidth) / 2;
        int typeLabelY = contentY + font.lineHeight + 2;
        guiGraphics.drawString(font, typeLabel, typeLabelX, typeLabelY, type.getColor());

        // ==================== 分隔线 ====================
        int separatorY = typeLabelY + font.lineHeight + 4;
        int separatorColor = 0x40FFFFFF;
        guiGraphics.fill(x + padding, separatorY, x + width - padding, separatorY + 1, separatorColor);

        // ==================== 信息区域 ====================
        int infoY = separatorY + 6;
        int lineHeight = 11;

        // 主人信息（左对齐，带图标）
        String ownerLabel = isOwned ? "👑" : "🔑";
        String ownerInfo = ownerLabel + " " + ownerName;
        String truncatedOwner = truncateText(font, ownerInfo, width / 2 - padding);
        guiGraphics.drawString(font, truncatedOwner, x + padding, infoY, isOwned ? 0xFFFFD700 : 0xFF87CEEB);

        // 所有权标识（右对齐）
        String statusText = isOwned ? "我的领地" : "已授权";
        int statusWidth = font.width(statusText);
        int statusX = x + width - padding - statusWidth;
        int statusColor = isOwned ? 0xFF90EE90 : 0xFFDDA0DD;
        guiGraphics.drawString(font, statusText, statusX, infoY, statusColor);
        infoY += lineHeight + 2;

        // 坐标信息（居中，简化显示）
        String[] parts = coordinateRange.split("→");
        String coordDisplay = "";
        if (parts.length == 2) {
            String start = parts[0].replaceAll("[\\[\\]]", "").trim();
            String[] startCoords = start.split(",");
            String end = parts[1].replaceAll("[\\[\\]]", "").trim();
            String[] endCoords = end.split(",");
            if (startCoords.length >= 3 && endCoords.length >= 3) {
                coordDisplay = "X: " + startCoords[0] + " ~ " + endCoords[0] + "  Z: " + startCoords[2] + " ~ " + endCoords[2];
            }
        }
        if (coordDisplay.isEmpty()) {
            coordDisplay = truncateText(font, coordinateRange, width - padding * 2);
        }
        int coordWidth = font.width(coordDisplay);
        int coordX = x + (width - coordWidth) / 2;
        guiGraphics.drawString(font, coordDisplay, coordX, infoY, 0xFF98FB98);

        // ==================== 按钮区域 ====================
        int buttonHeight = 18;
        int buttonSpacing = 4;
        int buttonBottomMargin = 4;
        int buttonY = y + height - buttonBottomMargin - buttonHeight;

        int[] result = new int[8]; // [teleportX1, Y1, X2, Y2, manageX1, Y1, X2, Y2]

        if (showManageButton) {
            // 两个按钮：传送 + 管理
            int totalButtonWidth = width - padding * 2;
            int singleButtonWidth = (totalButtonWidth - buttonSpacing) / 2;

            // 传送按钮（左）
            int teleportX = x + padding;
            drawTerritoryActionButton(guiGraphics, font, teleportX, buttonY, singleButtonWidth, buttonHeight,
                "📍 传送", teleportHovered, type.getColor());
            result[0] = teleportX;
            result[1] = buttonY;
            result[2] = teleportX + singleButtonWidth;
            result[3] = buttonY + buttonHeight;

            // 管理按钮（右）
            int manageX = teleportX + singleButtonWidth + buttonSpacing;
            drawTerritoryActionButton(guiGraphics, font, manageX, buttonY, singleButtonWidth, buttonHeight,
                "⚙️ 管理", manageHovered, THEME_TERRITORY);
            result[4] = manageX;
            result[5] = buttonY;
            result[6] = manageX + singleButtonWidth;
            result[7] = buttonY + buttonHeight;
        } else {
            // 只有传送按钮（全宽）
            int buttonWidth = width - padding * 2;
            int teleportX = x + padding;
            drawTerritoryActionButton(guiGraphics, font, teleportX, buttonY, buttonWidth, buttonHeight,
                "📍 传送", teleportHovered, type.getColor());
            result[0] = teleportX;
            result[1] = buttonY;
            result[2] = teleportX + buttonWidth;
            result[3] = buttonY + buttonHeight;
            // 管理按钮不存在
            result[4] = 0;
            result[5] = 0;
            result[6] = 0;
            result[7] = 0;
        }

        // ==================== ID标签（按钮上方，灰色小字） ====================
        String shortId = territoryId.length() > 10 ? territoryId.substring(0, 10) + "..." : territoryId;
        String idText = "ID: " + shortId;
        int idWidth = font.width(idText);
        int idX = x + (width - idWidth) / 2;
        int idY = buttonY - 11;
        guiGraphics.drawString(font, idText, idX, idY, 0x60808080);

        return result;
    }

    /**
     * 绘制领地操作按钮（内部小按钮）
     */
    private static void drawTerritoryActionButton(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height,
                                                   String text, boolean isHovered, int accentColor) {
        UiButtonStyle style = UiButtonStyle.accent(accentColor)
            .setPadding(6)
            .setStripeWidth(3)
            .setGlowHeight(4)
            .setBgAlpha(0x55)
            .setBgAlphaHover(0x70)
            .setBorderAlpha(0x25)
            .setBorderAlphaHover(0x40)
            .setTextShadow(false);
        UiButtonRenderer.drawStripedButton(guiGraphics, font, x, y, width, height,
            text, "", style, isHovered, UiButtonRenderer.TextAlign.CENTER, false);
    }

    /**
     * 绘制领地卡片（旧版，保留兼容性）
     */
    public static void drawTerritoryCard(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height,
                                         String territoryName, TerritoryType type, boolean isOwned, boolean isHovered) {
        drawTerritoryCard(guiGraphics, font, x, y, width, height, territoryName, type, isOwned, isHovered, false, false, isOwned, "", "", "");
    }
}

