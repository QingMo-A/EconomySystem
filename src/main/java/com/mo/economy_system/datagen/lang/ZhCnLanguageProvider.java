package com.mo.economy_system.datagen.lang;

import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.Util;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.LanguageProvider;

public class ZhCnLanguageProvider extends LanguageProvider {
    public ZhCnLanguageProvider(DataGenerator gen, String modid) {
        super(gen.getPackOutput(), modid, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        // Tab | 创造页标题
        add("itemGroup.economy_system.tab", "EconomySystem");
        add("itemGroup.blueprint.tab", "蓝图");

        // Item | 物品
        add("item.economy_system.guitar", "吉他");
        add("item.economy_system.blueprint", "蓝图");
        add("item.economy_system.wormhole_potion", "虫洞药水");
        add("item.economy_system.recall_potion", "回忆药水");
        add("item.economy_system.claim_wand", "圈地杖");
        add("item.economy_system.supporter_hat", "赞助者帽子");
        add("item.economy_system.dreamingfish", "启程锦鲤");
        add("item.economy_system.easy_aid_kit", "简易急救包");
        add("item.economy_system.advanced_aid_kit", "高级急救包");
        add("item.economy_system.professional_aid_kit", "专业急救包");
        add("item.economy_system.revival_charm", "重生锦鲤");
        add("item.economy_system.restore_uninfected_potion", "基因复苏药剂");
        add("item.economy_system.clue_item", "线索");

        // ToolTips | 物品描述
        add("item.economy_system.dreamingfish.tooltip", "§6为什么一条破鱼叫锦鲤....\n纪念一下添加了一个物品花了10多个小时的笨笨的hhy");

        // Enchantment | 附魔
        add("enchantment.economy_system.carefully", "细心");
        add("enchantment.economy_system.carefully.desc", "增加从尸体中搜寻的货币数量");
        add("enchantment.economy_system.bounty_hunter", "赏金猎人");
        add("enchantment.economy_system.bounty_hunter.desc", "增加从尸体中搜寻到货币的概率");

        // Key Setting | 按键设置
        add("key.economy_system.open_screen", "打开菜单");
        add("key.categories.economy_system", "Economy System");

        // EconomySystem Command | 经济指令
        add(Util_MessageKeys.COIN_COMMAND_BALANCE, "§b余额: §6%s §b枚梦鱼币");
        add(Util_MessageKeys.COIN_COMMAND_ADD, "§b添加 §6%s §b枚梦鱼币到你的账户上");
        add(Util_MessageKeys.COIN_COMMAND_MIN, "§b从你的账户中移除 §c%s §b枚梦鱼币");
        add(Util_MessageKeys.COIN_COMMAND_INSUFFICIENT_BALANCE, "§c余额不足!!");
        add(Util_MessageKeys.COIN_COMMAND_SET, "§b将你的账户余额设置为 §6%s §b枚梦鱼币");
        add(Util_MessageKeys.TRANSFER_SUCCESSFULLY_MESSAGE_KEY, "§b已将 §6%s §b枚梦鱼币发给 §e%s");
        add(Util_MessageKeys.RECEIVE_SUCCESSFULLY_MESSAGE_KEY, "§b你收到来自 §e%s §b的 §6%s §b枚梦鱼币");
        add(Util_MessageKeys.TRANSFER_FAILED_MESSAGE_KEY, "§c转账失败, §b可能是因为 §e目标不存在 余额不足 §b或是 §e目标为自己");

        // TPA Command | TPA指令
        add(Util_MessageKeys.TPA_SELF_ERROR, "§c你不能向自己发送传送请求!!");
        add(Util_MessageKeys.TPA_NO_POTION, "§c你需要一个虫洞药水才能发送传送请求");
        add(Util_MessageKeys.TPA_REQUEST_SENT, "§b已向 §e%s §b发送了传送请求");
        add(Util_MessageKeys.TPA_ACCEPT, "§a点击同意");
        add(Util_MessageKeys.TPA_DENY, "§c点击拒绝");
        add(Util_MessageKeys.TPA_NO_REQUEST, "§c你没有待处理的传送请求");
        add(Util_MessageKeys.TPA_SENDER_OFFLINE, "§c发送传送请求的玩家已离线");
        add(Util_MessageKeys.TPA_SENDER_NO_POTION, "§e%s §c没有虫洞药水");
        add(Util_MessageKeys.TPA_TELEPORTED, "§b你已被传送到 §e%s");
        add(Util_MessageKeys.TPA_ACCEPTED, "§a你接受了来自 §e%s §a的传送请求");
        add(Util_MessageKeys.TPA_DENIED, "§c你拒绝了传送请求");
        add(Util_MessageKeys.TPA_TIMEOUT_SENDER, "§c你向 §e%s §c发送的传送请求已超时");
        add(Util_MessageKeys.TPA_TIMEOUT_TARGET, "§e%s §c的传送请求已超时");

        // Recall Potion | 回忆药水
        add(Util_MessageKeys.RECALL_POTION_ERROR_DIMENSION_NOT_FOUND, "§c错误：无法找到你的重生维度!!");

        // Screen_Home | 主页
        add(Util_MessageKeys.HOME_TITLE_KEY, "主页");
        add(Util_MessageKeys.HOME_FETCHING_BALANCE_TEXT_KEY, "§b获取余额中...");
        add(Util_MessageKeys.HOME_BALANCE_TEXT_KEY, "§b余额: §6%s");
        add(Util_MessageKeys.HOME_SHOP_BUTTON_KEY, "系统商店");
        add(Util_MessageKeys.HOME_MARKET_BUTTON_KEY, "玩家市场");
        add(Util_MessageKeys.HOME_DELIVERY_BOX_BUTTON_KEY, "收货箱");
        add(Util_MessageKeys.HOME_TERRITORY_BUTTON_KEY, "我的领地");
        add(Util_MessageKeys.HOME_ABOUT_BUTTON_KEY, "关于本模组");

        // Reward | 击杀奖励
        add(Util_MessageKeys.MOB_REWARD_MESSAGE_KEY, "§b你从 §e%s §b的尸体上找到了 §6%s §b枚梦鱼币");

        // Screen_Shop | 商店
        add(Util_MessageKeys.SHOP_TITLE_KEY, "商店");
        add(Util_MessageKeys.SHOP_HINT_TEXT_KEY, "输入商品任意信息");
        add(Util_MessageKeys.SHOP_ITEM_PRICE_KEY, "§6%s §b枚梦鱼币 §6/ §b个");
        add(Util_MessageKeys.SHOP_ITEM_ID_KEY, "§8物品ID: §7%s");
        add(Util_MessageKeys.SHOP_ITEM_BASIC_PRICE_KEY, "§8物品基础价格: §7%s");
        add(Util_MessageKeys.SHOP_ITEM_CURRENT_PRICE_KEY, "§8物品当前价格: §7%s");
        add(Util_MessageKeys.SHOP_ITEM_CHANGE_PRICE_KEY, "§8变化: §7%s");
        add(Util_MessageKeys.SHOP_ITEM_FLUCTUATION_FACTOR_KEY, "§8涨幅系数: §7%s");
        add(Util_MessageKeys.SHOP_LOADING_SHOP_DATA_TEXT_KEY, "没有商品数据喵~");
        add(Util_MessageKeys.SHOP_BUY_BUTTON_KEY, "§7购买");
        add(Util_MessageKeys.SHOP_BUY_SUCCESSFULLY_MESSAGE_KEY, "§a你成功花费 §6%s §a枚梦鱼币购买了 §7%s §a个 §8%s");
        add(Util_MessageKeys.SHOP_BUY_FAILED_MESSAGE_KEY, "你没有足够的梦鱼币来购买此商品");
        add(Util_MessageKeys.SHOP_REFRESH_MESSAGE_KEY, "商品价格已刷新");
        add(Util_MessageKeys.SHOP_INVALID_ITEM_MESSAGE_KEY, "无效的商品");
        add(Util_MessageKeys.SHOP_BUY_FAILED_INVENTORY_FULL_MESSAGE_KEY, "购买失败，背包空间不足");
        add(Util_MessageKeys.SHOP_BUY_ERROR_MESSAGE_KEY, "购买错误");

        // Screen_BuyItem | 商店购买界面
        add(Util_MessageKeys.SHOP_BUY_TITLE_KEY, "购买商品");
        add(Util_MessageKeys.SHOP_BUY_HINT_TEXT_KEY, "请输入要购买的数量");
        add(Util_MessageKeys.SHOP_BUY_BUY_BUTTON_KEY, "购买");
        add(Util_MessageKeys.SHOP_BUY_NO_ITEM_TEXT_KEY, "未知物品");
        add(Util_MessageKeys.SHOP_BUY_NO_ITEM_MESSAGE_KEY, "未知物品");
        add(Util_MessageKeys.SHOP_BUY_INVALID_COUNT_MESSAGE_KEY, "非法数量");
        add(Util_MessageKeys.SHOP_BUY_COUNT_TEXT_KEY, "数量: ");

        // Screen_Market | 市场
        add(Util_MessageKeys.MARKET_TITLE_KEY, "市场");
        add(Util_MessageKeys.MARKET_HINT_TEXT_KEY, "输入商品任意信息");
        add(Util_MessageKeys.MARKET_SELLER_NAME_KEY, "订单发布者: %s");
        add(Util_MessageKeys.MARKET_SELLER_UUID_KEY, "订单发布者UUID: %s");
        add(Util_MessageKeys.MARKET_TRADE_ID_KEY, "交易ID: %s");
        add(Util_MessageKeys.MARKET_ITEM_ID_KEY, "物品ID: %s");
        add(Util_MessageKeys.MARKET_ITEM_NAME_AND_COUNT_KEY, "%s x %s");
        add(Util_MessageKeys.MARKET_ITEM_PRICE_KEY, "%s 枚梦鱼币");
        add(Util_MessageKeys.MARKET_NO_ITEMS_TEXT_KEY, "还没有人发布订单喵~");
        add(Util_MessageKeys.MARKET_LIST_BUTTON_KEY, "上架");
        add(Util_MessageKeys.MARKET_REQUEST_BUTTON_KEY, "求购");
        add(Util_MessageKeys.MARKET_SWITCH_DISPLAY_TYPE_0_BUTTON_KEY, "无显示类型");
        add(Util_MessageKeys.MARKET_SWITCH_DISPLAY_TYPE_1_BUTTON_KEY, "仅显示自己的订单");
        add(Util_MessageKeys.MARKET_SWITCH_DISPLAY_TYPE_2_BUTTON_KEY, "仅显示非自己的订单");
        add(Util_MessageKeys.MARKET_SWITCH_DISPLAY_TYPE_3_BUTTON_KEY, "仅显示出货单");
        add(Util_MessageKeys.MARKET_SWITCH_DISPLAY_TYPE_4_BUTTON_KEY, "仅显示订购单");
        add(Util_MessageKeys.MARKET_BUY_BUTTON_KEY, "购买");
        add(Util_MessageKeys.MARKET_REMOVE_BUTTON_KEY, "下架");
        add(Util_MessageKeys.MARKET_ITEM_DOES_NOT_EXIST_MESSAGE_KEY, "物品不存在或已被购买");
        add(Util_MessageKeys.MARKET_PURCHASE_FAILED_MESSAGE_KEY, "你没有足够的钱来购买这个物品");
        add(Util_MessageKeys.MARKET_PURCHASE_SUCCESSFULLY_MESSAGE_KEY, "你成功用 %s 枚梦鱼币购买了 %s x %s");
        add(Util_MessageKeys.MARKET_COLLECT_MONEY_MESSAGE_KEY, "你的物品 \"%s\" 已经被 %s 用 %s 枚梦鱼币买走");
        add(Util_MessageKeys.MARKET_REMOVE_FAILED_MESSAGE_KEY, "物品不存在或已被购买");
        add(Util_MessageKeys.MARKET_UNMATCHED_SELLER_MESSAGE_KEY, "你只能移除你自己上架的物品");
        add(Util_MessageKeys.MARKET_ITEM_HAS_BEEN_RETURNED_MESSAGE_KEY, "商品已经被成功移除并且返回到你的背包中");

        // Screen_CreateSalesOrder | 创建出货单
        add(Util_MessageKeys.LIST_TITLE_KEY, "上架商品");
        add(Util_MessageKeys.LIST_NO_ITEM_IN_HAND_TEXT_KEY, "你没有手持物品");
        add(Util_MessageKeys.LIST_PRICE_TEXT_KEY, "价格: ");
        add(Util_MessageKeys.LIST_LIST_BUTTON_KEY, "上架");
        add(Util_MessageKeys.LIST_NO_ITEM_IN_HAND_MESSAGE_KEY, "你没有手持物品");
        add(Util_MessageKeys.LIST_INVALID_PRICE_MESSAGE_KEY, "非法价格");
        add(Util_MessageKeys.LIST_SUCCESSFULLY_MESSAGE_KEY, "上架成功!");
        add(Util_MessageKeys.LIST_INSUFFICIENT_ITEM_MESSAGE_KEY, "你没有足够的物品去上架");
        add(Util_MessageKeys.LIST_UNMATCHED_ITEM_MESSAGE_KEY, "你手中的物品与上架的物品不匹配");
        add(Util_MessageKeys.LIST_ITEM_TAX_PAYMENT_FAILED_MESSAGE_KEY, "你没有足够的梦鱼币来交付商品税: %s 枚梦鱼币");
        add(Util_MessageKeys.LIST_HINT_TEXT_KEY, "输入价格(整数且大于0)");

        // Screen_CreateDemandOrder | 创建求购单
        add(Util_MessageKeys.REQUEST_TITLE_KEY, "求购商品");
        add(Util_MessageKeys.REQUEST_ITEM_ID_HINT_TEXT_KEY, "输入你想要求购的物品ID");
        add(Util_MessageKeys.REQUEST_ITEM_COUNT_HINT_TEXT_KEY, "输入你想要求购的物品数量");
        add(Util_MessageKeys.REQUEST_PRICE_HINT_TEXT_KEY, "输入价格(整数且大于0)");
        add(Util_MessageKeys.REQUEST_ITEM_ID_TEXT_KEY, "物品ID: ");
        add(Util_MessageKeys.REQUEST_ITEM_COUNT_TEXT_KEY, "物品数量: ");
        add(Util_MessageKeys.REQUEST_PRICE_TEXT_KEY, "价格: ");
        add(Util_MessageKeys.REQUEST_REQUEST_BUTTON_KEY, "求购");
        add(Util_MessageKeys.REQUEST_UNKNOWN_ITEM_ID_KEY, "未知的物品ID");
        add(Util_MessageKeys.REQUEST_INVALID_ITEM_COUNT_KEY, "非法的物品数量");
        add(Util_MessageKeys.REQUEST_EXCESSIVE_ITEM_COUNT_KEY, "过大的物品数量");
        add(Util_MessageKeys.REQUEST_INVALID_PRICE_KEY, "非法价格");
        add(Util_MessageKeys.REQUEST_DELIVER_BUTTON_KEY, "交付");
        add(Util_MessageKeys.REQUEST_DELIVERED_STATUS_KEY, "已交付");
        add(Util_MessageKeys.REQUEST_CANCEL_KEY, "取消");
        add(Util_MessageKeys.REQUEST_CLAIM_BUTTON_KEY, "领取");
        add(Util_MessageKeys.DELIVERY_NOT_ENOUGH_ITEMS_KEY, "你没有足够的物品来交付");
        add(Util_MessageKeys.DELIVERY_SUCCESS_KEY, "你成功交付了 %s x %s");
        add(Util_MessageKeys.CLAIM_SUCCESS_KEY, "你成功领取了 %s x %s");
        add(Util_MessageKeys.CLAIM_NOT_OWNER_KEY, "你不能领取不属于你的物品");
        add(Util_MessageKeys.ORDER_DELIVERED_BY_PLAYER_KEY, "你的订购单 %s x %s 被 %s 交付");

        // Screen_DeliveryBox | 收货箱
        add(Util_MessageKeys.DELIVERY_BOX_TITLE_KEY, "收货箱");
        add(Util_MessageKeys.DELIVERY_BOX_HINT_TEXT_KEY, "输入商品的任意信息");
        add(Util_MessageKeys.DELIVERY_BOX_NO_ITEMS_TEXT_KEY, "你的收货箱是空的喵~");
        add(Util_MessageKeys.DELIVERY_BOX_SOURCE_KEY, "来源: %s");
        add(Util_MessageKeys.DELIVERY_BOX_DATA_ID_KEY, "数据ID: %s");
        add(Util_MessageKeys.DELIVERY_BOX_ITEM_ID_KEY, "物品ID: %s");
        add(Util_MessageKeys.DELIVERY_BOX_CLAIM_BUTTON_KEY, "领取");
        add(Util_MessageKeys.DELIVERY_BOX_ITEM_NAME_AND_COUNT_KEY, "%s x %s");

        // RedPacket Command | 红包指令
        add(Util_MessageKeys.RED_PACKET_INSUFFICIENT_BALANCE, "§c你没有足够的梦鱼币创建红包");
        add(Util_MessageKeys.RED_PACKET_ALREADY_ACTIVE, "§c你已经有一个活动的红包");
        add(Util_MessageKeys.RED_PACKET_CREATED_SUCCESSFULLY, "§a红包创建成功");
        add(Util_MessageKeys.RED_PACKET_NO_AVAILABLE, "§c没有可领取的红包");
        add(Util_MessageKeys.RED_PACKET_ALREADY_CLAIMED, "§c你已经领取过这个红包了。");
        add(Util_MessageKeys.RED_PACKET_CLAIM_SUCCESS, "§a你从 %s 的红包中领取了 %s 枚梦鱼币！");
        add(Util_MessageKeys.RED_PACKET_CLAIM_BUTTON, "[抢]");
        add(Util_MessageKeys.RED_PACKET_BROADCAST, "%s 发了一个红包！");
        add(Util_MessageKeys.RED_PACKET_NO_ACTIVE, "§c没有可以取消的红包");
        add(Util_MessageKeys.RED_PACKET_CANCELLED, "§a红包已被取消");
        add(Util_MessageKeys.RED_PACKET_FULLY_CLAIMED, "§a来自 %s 的红包已经被抢完了");
        add(Util_MessageKeys.RED_PACKET_EXPIRED_REFUNDED, "§c你的红包已经过期, %s 枚梦鱼币已经返回到你的账户");
        add(Util_MessageKeys.RED_PACKET_EXPIRED_BROADCAST, "§c来自 %s 的红包已经过期了");
        add(Util_MessageKeys.RED_PACKET_CLAIM_BROADCAST, "%s 从 %s 的红包中抢到了 %s 枚梦鱼币");

        // Screen_Territory | 我的领地
        add(Util_MessageKeys.TERRITORY_TITLE_KEY, "我的领地");
        add(Util_MessageKeys.TERRITORY_HINT_TEXT_KEY, "输入领地的任意信息");
        add(Util_MessageKeys.TERRITORY_NO_TERRITORIES_TEXT_KEY, "你还没有自己的领地喵~");
        add(Util_MessageKeys.TERRITORY_TERRITORY_NAME_TEXT_KEY, "领地名称: %s");
        add(Util_MessageKeys.TERRITORY_TERRITORY_AREA_TEXT_KEY, "领地范围: %s");
        add(Util_MessageKeys.TERRITORY_TELEPORT_BUTTON_KEY, "传送");
        add(Util_MessageKeys.TERRITORY_MANAGE_BUTTON_KEY, "管理");
        add(Util_MessageKeys.TERRITORY_TERRITORY_NAME_KEY, "领地名称: %s");
        add(Util_MessageKeys.TERRITORY_TERRITORY_UUID_KEY, "领地UUID: %s");
        add(Util_MessageKeys.TERRITORY_TERRITORY_OWNER_NAME_KEY, "领地所有者名称: %s");
        add(Util_MessageKeys.TERRITORY_TERRITORY_OWNER_UUID_KEY, "领地所有者UUID: %s");
        add(Util_MessageKeys.TERRITORY_TERRITORY_BACK_POINT_KEY, "领地传送点: %s %s %s");
        add(Util_MessageKeys.TERRITORY_TERRITORY_NO_AUTHORIZED_PLAYER_KEY, "你的领地中还没有成员哦");

        // Screen_ManageTerritory | 管理领地
        add(Util_MessageKeys.TERRITORY_MANAGEMENT_COPY_ID, "复制领地ID");
        add(Util_MessageKeys.TERRITORY_MANAGEMENT_COPY_SUCCESS, "领地ID已复制到剪贴板！");
        add(Util_MessageKeys.TERRITORY_MANAGEMENT_INVITE_PLAYER, "邀请玩家");
        add(Util_MessageKeys.TERRITORY_MANAGEMENT_DELETE_TERRITORY, "删除领地");
        add(Util_MessageKeys.TERRITORY_MANAGEMENT_BUFF, "领地Buff");
        add(Util_MessageKeys.TERRITORY_MANAGEMENT_PERMISSIONS, "领地权限");
        add(Util_MessageKeys.TERRITORY_MANAGEMENT_TRANSFER_OWNERSHIP, "领地权限转让");
        add(Util_MessageKeys.TERRITORY_MANAGEMENT_RESIZE_TERRITORY, "更改领地范围");
        add(Util_MessageKeys.TERRITORY_NOT_FOUND, "未找到要删除的领地！");
        add(Util_MessageKeys.TERRITORY_NO_OWNER_PERMISSION, "你不是此领地的所有者，无法删除！");
        add(Util_MessageKeys.TERRITORY_REMOVE_SUCCESS, "成功删除领地: %s");
        add(Util_MessageKeys.TERRITORY_MANAGEMENT_KICK_PLAYER, "踢出");
        add(Util_MessageKeys.TERRITORY_NOT_EXIST, "领地不存在！");
        add(Util_MessageKeys.TERRITORY_NO_PERMISSION, "你无权管理此领地！");
        add(Util_MessageKeys.TERRITORY_PLAYER_KICKED, "你被从领地 %s 中踢出");
        add(Util_MessageKeys.TERRITORY_PLAYER_REMOVED, "成功将玩家移出领地！");

        // Screen_InvitePlayer | 邀请玩家
        add(Util_MessageKeys.INVITE_TITLE_KEY, "邀请玩家");
        add(Util_MessageKeys.INVITE_INVITE_BUTTON_KEY, "发送邀请");
        add(Util_MessageKeys.INVITE_NO_NAME_KEY, "请输入玩家名称");
        add(Util_MessageKeys.INVITE_NO_PERMISSION, "§c你没有权限邀请玩家加入此领地！");
        add(Util_MessageKeys.INVITE_PLAYER_OFFLINE, "§c玩家 %s 不在线！");
        add(Util_MessageKeys.INVITE_ALREADY_MEMBER, "§c玩家 %s 已经是你的领地成员了！");
        add(Util_MessageKeys.INVITE_SENT, "§a已向玩家 %S 发送邀请！");
        add(Util_MessageKeys.INVITE_RECEIVED, "§e玩家 %s 邀请你加入领地: %s");
        add(Util_MessageKeys.INVITE_INSTRUCTIONS, "§a输入 /accept_invite 接受 或 /decline_invite 拒绝");
        add(Util_MessageKeys.INVITE_SELF_ERROR, "§c你不能向自己发出邀请!");
        add(Util_MessageKeys.INVITE_NOT_IN_TERRITORY, "你不在自己的领地范围内，无法发送邀请！");
        add(Util_MessageKeys.INVITE_SENT_TO_PLAYER, "邀请已发送给 %s");
        add(Util_MessageKeys.INVITE_RECEIVED_PLAYER, "%s 邀请你加入领地: %s");
        add(Util_MessageKeys.COMMAND_PLAYER_ONLY, "此指令仅能由玩家执行！");
        add(Util_MessageKeys.TERRITORY_SETBACKPOINT_NO_PERMISSION, "你不在自己的领地范围内，无法设置回城点！");
        add(Util_MessageKeys.TERRITORY_SETBACKPOINT_SUCCESS, "成功设置回城点为: %s, %s, %s");
        add(Util_MessageKeys.INVITE_NO_PENDING, "没有待接受的领地邀请！");
        add(Util_MessageKeys.INVITE_TARGET_NOT_FOUND, "目标领地不存在！");
        add(Util_MessageKeys.INVITE_ACCEPTED, "成功接受邀请，现在你有权进入领地: %s");
        add(Util_MessageKeys.INVITE_DECLINE_NO_PENDING, "没有待拒绝的领地邀请！");
        add(Util_MessageKeys.INVITE_DECLINED, "已拒绝邀请。");
        add(Util_MessageKeys.INVITE_BACK_BUTTON, "返回");
        add(Util_MessageKeys.INVITE_ACCEPT_BUTTON, "§a[同意]");
        add(Util_MessageKeys.INVITE_DECLINE_BUTTON, "§c[拒绝]");

        // Screen_TerritoryBuff | 领地增益


        // Claim Territory | 圈地
        add(Util_MessageKeys.CLAIM_WAND_SELECT_POINTS, "§c请先用圈地杖选定两个点！");
        add(Util_MessageKeys.CLAIM_INSUFFICIENT_BALANCE, "§c余额不足，圈地所需价格为 %s 枚梦鱼币");
        add(Util_MessageKeys.CLAIM_SUCCESS, "§a领地创建成功！§r名称: §b%s §r价格: %s 枚梦鱼币");
        add(Util_MessageKeys.CLAIM_WAND_FIRST_POSITION_SET, "§a第一个点已确定 §r坐标: §b%s %s %s");
        add(Util_MessageKeys.CLAIM_WAND_SECOND_POSITION_SET, "§a第二个点已确定 §r坐标: §b%s %s %s");
        add(Util_MessageKeys.CLAIM_WAND_OVERLAP_ERROR, "§c圈地失败！新领地与现有领地重叠，请重新选择点位");
        add(Util_MessageKeys.CLAIM_WAND_Y_MISMATCH_ERROR, "§c圈地失败！圈地两点y轴坐标不一致");
        add(Util_MessageKeys.CLAIM_WAND_VOLUME, "领地大小:§b %s 格");
        add(Util_MessageKeys.CLAIM_WAND_PRICE, "圈地所需价格:§b %s 枚梦鱼币");
        add(Util_MessageKeys.CLAIM_WAND_INSTRUCTION, "§b如果不满意，第三次右键即可取消。§e如果满意，请执行指令 §b/confirm_claim <领地名称> §e来确认购买！");
        add(Util_MessageKeys.CLAIM_WAND_CANCEL, "§c圈地已取消！");
        add(Util_MessageKeys.CLAIM_WAND_TIMEOUT, "§c圈地已超时自动取消！");
        add(Util_MessageKeys.CLAIM_RESIZE_FAILED, "§c修改领地范围失败");
        add(Util_MessageKeys.CLAIM_RESIZE_SUCCESS, "§a修改领地范围成功");
        add(Util_MessageKeys.CLAIM_RESIZE_INSUFFICIENT_BALANCE, "§c你没有足够的钱来修改领地范围");
        add(Util_MessageKeys.CLAIM_WAND_CONFIRM_EXPAND, "§b是否要将领地扩大?");
        add(Util_MessageKeys.CLAIM_WAND_RESIZE_COST_DETAILS, "§b先前范围: %s §e现在范围: %s §b需要补的差价: %s");
        add(Util_MessageKeys.CLAIM_WAND_CONFIRM_SHRINK, "§b是否要将领地缩小? §c多余金额不返还");
        add(Util_MessageKeys.CLAIM_WAND_VOLUME_CHANGE, "§b先前范围: %s §e现在范围: %s");
        add(Util_MessageKeys.CLAIM_WAND_ENTER_RESIZE_MODE, "§a进入领地修改模式");
        add(Util_MessageKeys.CLAIM_WAND_EXIT_RESIZE_MODE, "§c退出修改模式");

        // Territory Teleport | 领地传送
        add(Util_MessageKeys.TELEPORT_TARGET_NOT_FOUND, "§c未找到目标领地！");
        add(Util_MessageKeys.TELEPORT_NO_PERMISSION, "§c你没有权限传送到此领地！");
        add(Util_MessageKeys.TELEPORT_NO_BACKPOINT, "§c该领地没有设置回城点，无法传送！");
        add(Util_MessageKeys.TELEPORT_DIMENSION_NOT_FOUND, "§c无法找到目标维度！");
        add(Util_MessageKeys.TELEPORT_NO_POTION, "§c传送失败，你没有足够的回忆药水！");
        add(Util_MessageKeys.TELEPORT_SUCCESS, "§a已成功传送到领地: %s");
        add(Util_MessageKeys.TELEPORT_FAILED, "§c传送失败，发生未知错误！");

        // Screen_About | 关于页
        add(Util_MessageKeys.ABOUT_TITLE_KEY, "关于");
        add(Util_MessageKeys.ABOUT_MOD_NAME_KEY, "Economy System");
        add(Util_MessageKeys.ABOUT_AUTHOR_NAME_KEY, "作者: %s");
        add(Util_MessageKeys.ABOUT_GITHUB_URL_KEY, "Github仓库地址: %s");
        add(Util_MessageKeys.ABOUT_TEXT_SHOW_KEY, "点击复制");
        add(Util_MessageKeys.ABOUT_COPY_URL, "Github仓库链接已经复制到粘贴板了");
        add(Util_MessageKeys.ABOUT_BACK_BUTTON_KEY, "返回");
    }
}
