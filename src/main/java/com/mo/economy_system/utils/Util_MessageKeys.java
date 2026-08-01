package com.mo.economy_system.utils;

public class Util_MessageKeys {
    // EconomySystem Command | 经济指令
    public static final String COIN_COMMAND_BALANCE = "message.coin_command_balance";
    public static final String COIN_COMMAND_ADD = "message.coin_command_add";
    public static final String COIN_COMMAND_MIN = "message.coin_command_min";
    public static final String COIN_COMMAND_INSUFFICIENT_BALANCE = "message.coin_command_insufficient_balance";
    public static final String COIN_COMMAND_SET = "message.coin_command_set";
    public static final String TRANSFER_SUCCESSFULLY_MESSAGE_KEY = "message.transfer.transfer_successfully";
    public static final String RECEIVE_SUCCESSFULLY_MESSAGE_KEY = "message.transfer.receive_successfully";
    public static final String TRANSFER_FAILED_MESSAGE_KEY = "message.transfer.transfer_failed";

    // TPA Command | TPA指令
    public static final String TPA_SELF_ERROR = "message.tpa.self_error";
    public static final String TPA_NO_POTION = "message.tpa.no_potion";
    public static final String TPA_REQUEST_SENT = "message.tpa.request_sent";
    public static final String TPA_ACCEPT = "message.tpa.accept";
    public static final String TPA_DENY = "message.tpa.deny";
    public static final String TPA_NO_REQUEST = "message.tpa.no_request";
    public static final String TPA_SENDER_OFFLINE = "message.tpa.sender_offline";
    public static final String TPA_SENDER_NO_POTION = "message.tpa.sender_no_potion";
    public static final String TPA_TELEPORTED = "message.tpa.teleported";
    public static final String TPA_ACCEPTED = "message.tpa.accepted";
    public static final String TPA_DENIED = "message.tpa.denied";
    public static final String TPA_TIMEOUT_SENDER = "message.tpa.timeout_sender";
    public static final String TPA_TIMEOUT_TARGET = "message.tpa.timeout_target";

    // Recall Potion | 回忆药水
    public static final String RECALL_POTION_ERROR_DIMENSION_NOT_FOUND = "message.recall_potion.error_dimension_not_found";

    // Screen_Home | 主页
    public static final String HOME_TITLE_KEY = "screen.home.title";
    public static final String HOME_FETCHING_BALANCE_TEXT_KEY = "text.home.fetching_balance";
    public static final String HOME_BALANCE_TEXT_KEY = "text.home.balance";
    public static final String HOME_SHOP_BUTTON_KEY = "button.home.shop";
    public static final String HOME_MARKET_BUTTON_KEY = "button.home.market";
    public static final String HOME_DELIVERY_BOX_BUTTON_KEY = "button.home.delivery_box";
    public static final String HOME_TERRITORY_BUTTON_KEY = "button.home.territory";
    public static final String HOME_ABOUT_BUTTON_KEY = "button.home.about";

    // Reward | 击杀奖励
    public static final String MOB_REWARD_MESSAGE_KEY = "message.mob_reward";

    // Screen_Shop | 商店
    public static final String SHOP_TITLE_KEY = "screen.shop.title";
    public static final String SHOP_ITEM_PRICE_KEY = "screen.shop.item.price";
    public static final String SHOP_ITEM_ID_KEY = "screen.shop.item.id";
    public static final String SHOP_ITEM_BASIC_PRICE_KEY = "screen.shop.item.basic_price";
    public static final String SHOP_ITEM_CURRENT_PRICE_KEY = "screen.shop.item.current_price";
    public static final String SHOP_ITEM_CHANGE_PRICE_KEY = "screen.shop.item.change_price";
    public static final String SHOP_ITEM_FLUCTUATION_FACTOR_KEY = "screen.shop.item.fluctuation_factor";
    public static final String SHOP_LOADING_SHOP_DATA_TEXT_KEY = "text.shop.loading_shop_data";
    public static final String SHOP_NO_MATCHING_ITEMS_TEXT_KEY = "text.shop.no_matching_items";
    public static final String SHOP_NO_ITEMS_AVAILABLE_TEXT_KEY = "text.shop.no_items_available";
    public static final String SHOP_SEARCH_HINT_TEXT_KEY = "text.shop.search_hint";
    public static final String SHOP_ESC_HINT_TEXT_KEY = "text.shop.esc_hint";
    public static final String SHOP_BUY_BUTTON_KEY = "button.shop.buy";
    public static final String SHOP_HINT_TEXT_KEY = "text.shop.hint";
    public static final String SHOP_BUY_PRICE_TEXT_KEY = "text_buy.price";
    public static final String SHOP_BUY_SUCCESSFULLY_MESSAGE_KEY = "message.shop.buy_successfully";
    public static final String SHOP_BUY_FAILED_MESSAGE_KEY = "message.shop.buy_failed";
    public static final String SHOP_INVALID_ITEM_MESSAGE_KEY = "message.shop.invalid_item";
    public static final String SHOP_BUY_FAILED_INVENTORY_FULL_MESSAGE_KEY = "message.shop.buy_failed_inventory_full";
    public static final String SHOP_BUY_ERROR_MESSAGE_KEY = "message.shop.buy_error";
    public static final String SHOP_REFRESH_MESSAGE_KEY = "message.shop.shop_refresh";

    // Screen_BuyItem | 商店购买界面
    public static final String SHOP_BUY_TITLE_KEY = "screen.shop_buy.title";
    public static final String SHOP_BUY_HINT_TEXT_KEY = "text.shop_buy.hint";
    public static final String SHOP_BUY_BUY_BUTTON_KEY = "button.shop_buy.buy";
    public static final String SHOP_BUY_NO_ITEM_TEXT_KEY = "text.shop_buy.no_item";
    public static final String SHOP_BUY_NO_ITEM_MESSAGE_KEY = "message.shop_buy.no_item";
    public static final String SHOP_BUY_INVALID_COUNT_MESSAGE_KEY = "message.shop_buy.invalid_count";
    public static final String SHOP_BUY_COUNT_TEXT_KEY = "text.shop_buy.count";
    public static final String SHOP_BUY_TOTAL_PRICE_TEXT_KEY = "text.shop_buy.total_price";
    public static final String SHOP_BUY_INVENTORY_INSUFFICIENT_TEXT_KEY = "text.shop_buy.inventory_insufficient";

    // Screen_Market | 市场
    public static final String MARKET_TITLE_KEY = "screen.market.title";
    public static final String MARKET_HINT_TEXT_KEY = "text.market.hint";
    public static final String MARKET_SELLER_NAME_KEY = "screen.market.item.seller_name";
    public static final String MARKET_SELLER_UUID_KEY = "screen.market.item.seller_uuid";
    public static final String MARKET_TRADE_ID_KEY = "screen.market.item.trade_id";
    public static final String MARKET_ITEM_ID_KEY = "screen.market.item.id";
    public static final String MARKET_ITEM_NAME_AND_COUNT_KEY = "screen.market.item.name_and_count";
    public static final String MARKET_ITEM_PRICE_KEY = "screen.market.item.price";
    public static final String MARKET_NO_ITEMS_TEXT_KEY = "text.market.no_items";
    public static final String MARKET_LIST_BUTTON_KEY = "button.market.list";
    public static final String MARKET_REQUEST_BUTTON_KEY = "button.market.request";
    public static final String MARKET_SWITCH_DISPLAY_TYPE_0_BUTTON_KEY = "button.market.switch_display_type_0";
    public static final String MARKET_SWITCH_DISPLAY_TYPE_1_BUTTON_KEY = "button.market.switch_display_type_1";
    public static final String MARKET_SWITCH_DISPLAY_TYPE_2_BUTTON_KEY = "button.market.switch_display_type_2";
    public static final String MARKET_SWITCH_DISPLAY_TYPE_3_BUTTON_KEY = "button.market.switch_display_type_3";
    public static final String MARKET_SWITCH_DISPLAY_TYPE_4_BUTTON_KEY = "button.market.switch_display_type_4";
    public static final String MARKET_BUY_BUTTON_KEY = "button.market.buy";
    public static final String MARKET_REMOVE_BUTTON_KEY = "button.market.remove";
    public static final String MARKET_ITEM_DOES_NOT_EXIST_MESSAGE_KEY = "message.market.item_does_not_exist";
    public static final String MARKET_PURCHASE_FAILED_MESSAGE_KEY = "message.market.purchase_failed";
    public static final String MARKET_PURCHASE_SUCCESSFULLY_MESSAGE_KEY = "message.market.purchase_successfully";
    public static final String MARKET_COLLECT_MONEY_MESSAGE_KEY = "message.market.collect_money";
    public static final String MARKET_REMOVE_FAILED_MESSAGE_KEY = "message.market.remove_failed";
    public static final String MARKET_UNMATCHED_SELLER_MESSAGE_KEY = "message.market.unmatched_seller";
    public static final String MARKET_ITEM_HAS_BEEN_RETURNED_MESSAGE_KEY = "message.market.item_has_been_returned";

    // Screen_CreateSalesOrder | 创建出货单
    public static final String LIST_TITLE_KEY = "screen.list.title";
    public static final String LIST_NO_ITEM_IN_HAND_TEXT_KEY = "text.list.no_item_in_hand";
    public static final String LIST_PRICE_TEXT_KEY = "text.list.price";
    public static final String LIST_LIST_BUTTON_KEY = "button.list.list";
    public static final String LIST_NO_ITEM_IN_HAND_MESSAGE_KEY = "message.list.no_item_in_hand";
    public static final String LIST_INVALID_PRICE_MESSAGE_KEY = "message.list.invalid_price";
    public static final String LIST_HINT_TEXT_KEY = "text.list.hint";
    public static final String LIST_SUCCESSFULLY_MESSAGE_KEY = "message.list.list_successfully";
    public static final String LIST_INSUFFICIENT_ITEM_MESSAGE_KEY = "message.list.list_insufficient_item";
    public static final String LIST_UNMATCHED_ITEM_MESSAGE_KEY = "message.list.list_unmatched_item";
    public static final String LIST_ITEM_TAX_PAYMENT_FAILED_MESSAGE_KEY = "message.list.list_item_tax_payment_failed";
    public static final String LIST_INVALID_QUANTITY_MESSAGE_KEY = "message.list.invalid_quantity";
    public static final String LIST_UNSUPPORTED_ITEM_DATA_MESSAGE_KEY = "message.list.unsupported_item_data";
    public static final String LIST_MARKET_FULL_MESSAGE_KEY = "message.list.market_full";
    public static final String LIST_CREATE_FAILED_MESSAGE_KEY = "message.list.create_failed";
    public static final String LIST_ROLLBACK_FAILED_MESSAGE_KEY = "message.list.rollback_failed";
    public static final String REQUEST_CREATE_SUCCESS = "message.request.create_success";
    public static final String REQUEST_INVALID_ITEM_ID = "message.request.invalid_item_id";
    public static final String REQUEST_ITEM_NOT_FOUND = "message.request.item_not_found";
    public static final String REQUEST_INVALID_QUANTITY = "message.request.invalid_quantity";
    public static final String REQUEST_QUANTITY_EXCEEDS_LIMIT = "message.request.quantity_exceeds_limit";
    public static final String REQUEST_INVALID_PRICE = "message.request.invalid_price";
    public static final String REQUEST_INSUFFICIENT_FUNDS = "message.request.insufficient_funds";
    public static final String REQUEST_MARKET_FULL = "message.request.market_full";
    public static final String REQUEST_UNSUPPORTED_ITEM = "message.request.unsupported_item";
    public static final String REQUEST_CREATE_FAILED = "message.request.create_failed";
    public static final String REQUEST_REFUND_FAILED = "message.request.refund_failed";

    // Screen_CreateDemandOrder | 创建求购单
    public static final String REQUEST_TITLE_KEY = "screen.request.title";
    public static final String REQUEST_PRICE_TEXT_KEY = "text.request.price";
    public static final String REQUEST_ITEM_COUNT_TEXT_KEY = "text.request.item_count";
    public static final String REQUEST_ITEM_ID_TEXT_KEY = "text.request.item_id";
    public static final String REQUEST_REQUEST_BUTTON_KEY = "button.request.request";
    public static final String REQUEST_PRICE_HINT_TEXT_KEY = "text.request.price.hint";
    public static final String REQUEST_ITEM_COUNT_HINT_TEXT_KEY = "text.request.item_count.hint";
    public static final String REQUEST_ITEM_ID_HINT_TEXT_KEY = "text.request.item_id.hint";
    public static final String REQUEST_UNKNOWN_ITEM_ID_KEY = "text.request.unknown_item_id";
    public static final String REQUEST_INVALID_ITEM_COUNT_KEY = "text.request.invalid_item_count";
    public static final String REQUEST_EXCESSIVE_ITEM_COUNT_KEY = "text.request.excessive_item_count";
    public static final String REQUEST_INVALID_PRICE_KEY = "text.request.invalid_price";
    public static final String REQUEST_DELIVER_BUTTON_KEY = "button.request.deliver";
    public static final String REQUEST_DELIVERED_STATUS_KEY = "button.request.delivered_status";
    public static final String REQUEST_CANCEL_KEY = "button.request.cancel";
    public static final String REQUEST_CLAIM_BUTTON_KEY = "button.request.claim";
    public static final String DELIVERY_NOT_ENOUGH_ITEMS_KEY = "message.delivery.not_enough_items";
    public static final String DELIVERY_BALANCE_LIMIT_KEY = "message.delivery.balance_limit";
    public static final String REQUEST_CANCEL_SUCCESS = "message.request.cancel_success";
    public static final String REQUEST_CANCEL_NOT_FOUND = "message.request.cancel_not_found";
    public static final String REQUEST_CANCEL_NOT_OWNER = "message.request.cancel_not_owner";
    public static final String REQUEST_CANCEL_DELIVERED = "message.request.cancel_delivered";
    public static final String REQUEST_CANCEL_BALANCE_LIMIT = "message.request.cancel_balance_limit";
    public static final String REQUEST_CANCEL_FAILED = "message.request.cancel_failed";
    public static final String REQUEST_CANCEL_ROLLBACK_FAILED = "message.request.cancel_rollback_failed";
    public static final String DELIVERY_SUCCESS_KEY = "message.delivery.success";
    public static final String CLAIM_SUCCESS_KEY = "message.request.claim.success";
    public static final String CLAIM_NOT_OWNER_KEY = "message.claim.not_owner";
    public static final String ORDER_DELIVERED_BY_PLAYER_KEY = "message.order.delivered_by_player";

    // Screen_DeliveryBox | 收货箱
    public static final String DELIVERY_BOX_TITLE_KEY = "screen.delivery_box.title";
    public static final String DELIVERY_BOX_HINT_TEXT_KEY = "text.delivery_box.hint";
    public static final String DELIVERY_BOX_NO_ITEMS_TEXT_KEY = "text.delivery_box.no_items";
    public static final String DELIVERY_BOX_SOURCE_KEY = "screen.delivery_box.item.source";
    public static final String DELIVERY_BOX_DATA_ID_KEY = "screen.delivery_box.item.data_id";
    public static final String DELIVERY_BOX_ITEM_ID_KEY = "screen.delivery_box.item.item_id";
    public static final String DELIVERY_BOX_CLAIM_BUTTON_KEY = "button.delivery_box.claim";
    public static final String DELIVERY_BOX_ITEM_NAME_AND_COUNT_KEY = "screen.delivery_box.item.name_and_count";

    // RedPacket Command | 红包指令
    public static final String RED_PACKET_INSUFFICIENT_BALANCE = "message.red_packet.insufficient_balance";
    public static final String RED_PACKET_ALREADY_ACTIVE = "message.red_packet.already_active";
    public static final String RED_PACKET_CREATED_SUCCESSFULLY = "message.red_packet.created_successfully";
    public static final String RED_PACKET_NO_AVAILABLE = "message.red_packet.no_available";
    public static final String RED_PACKET_ALREADY_CLAIMED = "message.red_packet.already_claimed";
    public static final String RED_PACKET_CLAIM_SUCCESS = "message.red_packet.claim_success";
    public static final String RED_PACKET_CLAIM_BUTTON = "message.red_packet.claim_button";
    public static final String RED_PACKET_BROADCAST = "message.red_packet.broadcast";
    public static final String RED_PACKET_NO_ACTIVE = "message.red_packet.no_active";
    public static final String RED_PACKET_CANCELLED = "message.red_packet.cancelled";
    public static final String RED_PACKET_FULLY_CLAIMED = "message.red_packet.fully_claimed";
    public static final String RED_PACKET_EXPIRED_REFUNDED = "message.red_packet.expired_refunded";
    public static final String RED_PACKET_EXPIRED_BROADCAST = "message.red_packet.expired_broadcast";
    public static final String RED_PACKET_CLAIM_BROADCAST = "message.red_packet.claim_broadcast";

    // Screen_Territory | 我的领地
    public static final String TERRITORY_TITLE_KEY = "screen.territory.title";
    public static final String TERRITORY_HINT_TEXT_KEY = "text.territory.hint";
    public static final String TERRITORY_NO_TERRITORIES_TEXT_KEY = "text.territory.no_territories";
    public static final String TERRITORY_TERRITORY_NAME_TEXT_KEY = "text.territory.territory_name";
    public static final String TERRITORY_TERRITORY_AREA_TEXT_KEY = "text.territory.territory_area";
    public static final String TERRITORY_TELEPORT_BUTTON_KEY = "button.territory.teleport";
    public static final String TERRITORY_MANAGE_BUTTON_KEY = "button.territory.manage";
    public static final String TERRITORY_TERRITORY_NAME_KEY = "screen.territory.territory_name";
    public static final String TERRITORY_TERRITORY_UUID_KEY = "screen.territory.territory_uuid";
    public static final String TERRITORY_TERRITORY_OWNER_NAME_KEY = "screen.territory.territory_owner_name";
    public static final String TERRITORY_TERRITORY_OWNER_UUID_KEY = "screen.territory.territory_owner_name_uuid";
    public static final String TERRITORY_TERRITORY_BACK_POINT_KEY = "screen.territory.territory_back_point";
    public static final String TERRITORY_TERRITORY_NO_AUTHORIZED_PLAYER_KEY = "screen.territory.territory_no_authorized_player";

    // Screen_ManageTerritory | 管理领地
    public static final String TERRITORY_MANAGEMENT_COPY_ID = "message.territory_management.copy_id";
    public static final String TERRITORY_MANAGEMENT_INVITE_PLAYER = "message.territory_management.invite_player";
    public static final String TERRITORY_MANAGEMENT_DELETE_TERRITORY = "message.territory_management.delete_territory";
    public static final String TERRITORY_MANAGEMENT_COPY_SUCCESS = "message.territory_management.copy_success";
    public static final String TERRITORY_MANAGEMENT_KICK_PLAYER = "message.territory_management.kick_player";
    public static final String TERRITORY_MANAGEMENT_BUFF = "message.territory_management.buff";
    public static final String TERRITORY_MANAGEMENT_PERMISSIONS = "message.territory_management.permissions";
    public static final String TERRITORY_MANAGEMENT_TRANSFER_OWNERSHIP = "message.territory_management.transfer_ownership";
    public static final String TERRITORY_MANAGEMENT_RESIZE_TERRITORY = "message.territory_management.resize_territory";
    public static final String TERRITORY_NOT_FOUND = "message.territory.not_found";
    public static final String TERRITORY_NO_OWNER_PERMISSION = "message.territory.no_owner_permission";
    public static final String TERRITORY_REMOVE_SUCCESS = "message.territory.remove_success";
    public static final String TERRITORY_NOT_EXIST = "message.territory.not_exist";
    public static final String TERRITORY_NO_PERMISSION = "message.territory.no_permission";
    public static final String TERRITORY_PLAYER_KICKED = "message.territory.player_kicked";
    public static final String TERRITORY_PLAYER_REMOVED = "message.territory.player_removed";

    // Screen_InvitePlayer | 邀请玩家
    public static final String INVITE_TITLE_KEY = "screen.invite.title";
    public static final String INVITE_INVITE_BUTTON_KEY = "button.invite.invite";
    public static final String INVITE_NO_NAME_KEY = "message.invite.no_name";
    public static final String INVITE_BACK_BUTTON = "button.invite.back";
    public static final String INVITE_NO_PERMISSION = "message.invite.no_permission";
    public static final String INVITE_PLAYER_OFFLINE = "message.invite.player_offline";
    public static final String INVITE_ALREADY_MEMBER = "message.invite.already_member";
    public static final String INVITE_SENT = "message.invite.sent";
    public static final String INVITE_RECEIVED = "message.invite.received";
    public static final String INVITE_INSTRUCTIONS = "message.invite.instructions";
    public static final String INVITE_SELF_ERROR = "message.invite.self_error";
    public static final String INVITE_NOT_IN_TERRITORY = "message.invite.not_in_territory";
    public static final String INVITE_SENT_TO_PLAYER = "message.invite.sent_to_player";
    public static final String INVITE_RECEIVED_PLAYER = "message.invite.received_player";
    public static final String COMMAND_PLAYER_ONLY = "message.command.player_only";
    public static final String TERRITORY_SETBACKPOINT_NO_PERMISSION = "message.territory.setbackpoint.no_permission";
    public static final String TERRITORY_SETBACKPOINT_SUCCESS = "message.territory.setbackpoint.success";
    public static final String INVITE_NO_PENDING = "message.invite.no_pending";
    public static final String INVITE_TARGET_NOT_FOUND = "message.invite.target_not_found";
    public static final String INVITE_ACCEPTED = "message.invite.accepted";
    public static final String INVITE_ACCEPT_BUTTON = "button.invite.accept";
    public static final String INVITE_DECLINE_NO_PENDING = "message.invite.decline_no_pending";
    public static final String INVITE_DECLINED = "message.invite.declined";
    public static final String INVITE_DECLINE_BUTTON = "button.invite.decline";

    // Screen_TerritoryBuff | 领地增益
    public static final String TERRITORY_BUFF_TITLE_KEY = "screen.territory_buff.title";
    public static final String TERRITORY_BUFF_TEXT_NO_BUFFS_TEXT_KEY  = "text.territory_buff.no_buffs";
    public static final String TERRITORY_BUFF_TEXT_BE_LOCKED_TEXT_KEY = "text.territory_buff.be_locked";
    public static final String TERRITORY_BUFF_TEXT_BE_UNLOCKED_TEXT_KEY = "text.territory_buff.be_unlocked";
    public static final String TERRITORY_BUFF_TOOLTIP_BUFF_ID_TEXT_KEY = "tooltip.territory_buff.buff_id";
    public static final String TERRITORY_BUFF_TOOLTIP_BUFF_NAME_TEXT_KEY = "tooltip.territory_buff.buff_name";
    public static final String TERRITORY_BUFF_TOOLTIP_BUFF_CURRENT_LEVEL_TEXT_KEY = "tooltip.territory_buff.buff_current_level";
    public static final String TERRITORY_BUFF_TOOLTIP_BUFF_MAX_LEVEL_TEXT_KEY = "tooltip.territory_buff.buff_max_level";
    public static final String TERRITORY_BUFF_TOOLTIP_BUFF_EFFECT_ID_TEXT_KEY = "tooltip.territory_buff.buff_effect_id";
    public static final String TERRITORY_BUFF_TOOLTIP_BUFF_UNLOCK_STATE_KEY = "tooltip.territory_buff.buff_unlock_state";
    public static final String TERRITORY_BUFF_COST_LABEL_KEY = "text.territory_buff.cost_label";
    public static final String TERRITORY_BUFF_BUTTON_UNLOCK_KEY = "button.territory_buff.unlock";
    public static final String TERRITORY_BUFF_BUTTON_UPGRADE_KEY = "button.territory_buff.upgrade";
    public static final String TERRITORY_BUFF_BUTTON_MAX_KEY = "button.territory_buff.max";
    public static final String TERRITORY_BUFF_MESSAGE_BUFF_MAX_LEVEL_KEY = "message.territory_buff.buff_max_level";
    public static final String TERRITORY_BUFF_MESSAGE_REQUIREMENT_ITEM_FAIL_KEY = "message.territory_buff.requirement_item_fail";
    public static final String TERRITORY_BUFF_MESSAGE_REQUIREMENT_XP_LEVEL_FAIL_KEY = "message.territory_buff.requirement_xp_level_fail";


    // Claim Territory | 圈地
    public static final String CLAIM_WAND_SELECT_POINTS = "message.claim_wand.select_points";
    public static final String CLAIM_INSUFFICIENT_BALANCE = "message.claim.insufficient_balance";
    public static final String CLAIM_SUCCESS = "message.claim.success";
    public static final String CLAIM_WAND_FIRST_POSITION_SET = "message.claim_wand.first_position_set";
    public static final String CLAIM_WAND_SECOND_POSITION_SET = "message.claim_wand.second_position_set";
    public static final String CLAIM_WAND_OVERLAP_ERROR = "message.claim_wand.overlap_error";
    public static final String CLAIM_WAND_Y_MISMATCH_ERROR = "message.claim_wand.y_mismatch_error";
    public static final String CLAIM_WAND_VOLUME = "message.claim_wand.volume";
    public static final String CLAIM_WAND_PRICE = "message.claim_wand.price";
    public static final String CLAIM_WAND_INSTRUCTION = "message.claim_wand.instruction";
    public static final String CLAIM_WAND_CANCEL = "message.claim_wand.cancel";
    public static final String CLAIM_WAND_TIMEOUT = "message.claim_wand.timeout";
    public static final String CLAIM_RESIZE_FAILED = "message.claim.resize_failed";
    public static final String CLAIM_RESIZE_SUCCESS = "message.claim.resize_success";
    public static final String CLAIM_RESIZE_INSUFFICIENT_BALANCE = "message.claim.resize_insufficient_balance";
    public static final String CLAIM_WAND_CONFIRM_EXPAND = "message.claim_wand.confirm_expand";
    public static final String CLAIM_WAND_RESIZE_COST_DETAILS = "message.claim_wand.resize_cost_details";
    public static final String CLAIM_WAND_CONFIRM_SHRINK = "message.claim_wand.confirm_shrink";
    public static final String CLAIM_WAND_VOLUME_CHANGE = "message.claim_wand.volume_change";
    public static final String CLAIM_WAND_ENTER_RESIZE_MODE = "message.claim_wand.enter_resize_mode";
    public static final String CLAIM_WAND_EXIT_RESIZE_MODE = "message.claim_wand.exit_resize_mode";

    // Territory Teleport | 领地传送
    public static final String TELEPORT_TARGET_NOT_FOUND = "message.teleport.target_not_found";
    public static final String TELEPORT_NO_PERMISSION = "message.teleport.no_permission";
    public static final String TELEPORT_NO_BACKPOINT = "message.teleport.no_backpoint";
    public static final String TELEPORT_DIMENSION_NOT_FOUND = "message.teleport.dimension_not_found";
    public static final String TELEPORT_NO_POTION = "message.teleport.no_potion";
    public static final String TELEPORT_SUCCESS = "message.teleport.success";
    public static final String TELEPORT_FAILED = "message.teleport.failed";

    // Screen_About | 关于页
    public static final String ABOUT_TITLE_KEY = "screen.about.title";
    public static final String ABOUT_MOD_NAME_KEY = "screen.about.mod_name";
    public static final String ABOUT_AUTHOR_NAME_KEY = "screen.about.author_name";
    public static final String ABOUT_GITHUB_URL_KEY = "screen.about.github_url";
    public static final String ABOUT_TEXT_SHOW_KEY = "screen.about.show_github_text";
    public static final String ABOUT_COPY_URL = "message.about.copy_github_url";
    public static final String ABOUT_BACK_BUTTON_KEY = "button.about.back";
}

