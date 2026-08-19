package com.mo.economy_system.datagen.lang;

import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.data.DataGenerator;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class EnUsLanguageProvider extends LanguageProvider {
  public EnUsLanguageProvider(DataGenerator gen, String modid) {
    super(gen.getPackOutput(), modid, "en_us");
  }

  @Override
  protected void addTranslations() {
    // Tab | 创造页标题
    add("itemGroup.economy_system.tab", "EconomySystem");

    // Item
    add("item.economy_system.guitar", "Guitar");
    add("item.economy_system.wormhole_potion", "Wormhole Potion");
    add("item.economy_system.recall_potion", "Recall Potion");
    add("item.economy_system.claim_wand", "Claim Wand");
    add("item.economy_system.supporter_hat", "Supporter Hat");
    add("item.economy_system.player_doll_hat", "Player Doll Hat");
    add("item.economy_system.poxiaojin_doll_hat", "poxiaojin");
    add("item.economy_system.hanhanyu_doll_hat", "__HanHanYu__");
    add("item.economy_system.player_351987654321_doll_hat", "351987654321");
    add("tooltip.economy_system.supporter_hat.unbound", "Unbound supporter identity");
    add("message.supporter_hat.hold_hat", "Hold a supporter hat in your main hand.");
    add("message.supporter_hat.invalid_identity", "The supporter identity is invalid.");
    add("message.supporter_hat.bound", "Supporter hat bound to %s (%s)");

    // Enchantment
    add("enchantment.economy_system.carefully", "Careful");
    add(
        "enchantment.economy_system.carefully.desc",
        "Increases the amount of currency found on bodies");
    add("enchantment.economy_system.bounty_hunter", "Bounty Hunter");
    add(
        "enchantment.economy_system.bounty_hunter.desc",
        "Increases the chance to find currency on bodies");

    // Key Setting
    add("key.economy_system.open_screen", "Open Economy Menu");
    add("key.categories.economy_system", "Economy System");

    // EconomySystem Command | 经济指令
    add(Util_MessageKeys.COIN_COMMAND_BALANCE, "Coin Command Balance: %s");
    add(Util_MessageKeys.COIN_COMMAND_ADD, "Coin Command Add: %s");
    add(Util_MessageKeys.COIN_COMMAND_MIN, "Coin Command Min: %s");
    add(Util_MessageKeys.COIN_COMMAND_INSUFFICIENT_BALANCE, "Coin Command Insufficient Balance");
    add(Util_MessageKeys.COIN_COMMAND_SET, "Coin Command Set: %s");
    add(Util_MessageKeys.TRANSFER_SUCCESSFULLY_MESSAGE_KEY, "Transfer Successfully: %s %s");
    add(Util_MessageKeys.RECEIVE_SUCCESSFULLY_MESSAGE_KEY, "Receive Successfully: %s %s");
    add(Util_MessageKeys.TRANSFER_FAILED_MESSAGE_KEY, "Transfer Failed");

    // TPA Command | TPA指令
    add(Util_MessageKeys.TPA_SELF_ERROR, "Tpa Self Error");
    add(Util_MessageKeys.TPA_NO_POTION, "Tpa No Potion");
    add(Util_MessageKeys.TPA_REQUEST_SENT, "Tpa Request Sent: %s");
    add(Util_MessageKeys.TPA_ACCEPT, "Tpa Accept");
    add(Util_MessageKeys.TPA_DENY, "Tpa Deny");
    add(Util_MessageKeys.TPA_NO_REQUEST, "Tpa No Request");
    add(Util_MessageKeys.TPA_SENDER_OFFLINE, "Tpa Sender Offline");
    add(Util_MessageKeys.TPA_SENDER_NO_POTION, "Tpa Sender No Potion: %s");
    add(Util_MessageKeys.TPA_TELEPORTED, "Tpa Teleported: %s");
    add(Util_MessageKeys.TPA_ACCEPTED, "Tpa Accepted: %s");
    add(Util_MessageKeys.TPA_DENIED, "Tpa Denied");
    add(Util_MessageKeys.TPA_TIMEOUT_SENDER, "Tpa Timeout Sender: %s");
    add(Util_MessageKeys.TPA_TIMEOUT_TARGET, "Tpa Timeout Target: %s");

    // Recall Potion | 回忆药水
    add(
        Util_MessageKeys.RECALL_POTION_ERROR_DIMENSION_NOT_FOUND,
        "Recall Potion Error Dimension Not Found");

    // Screen_Home | 主页
    add(Util_MessageKeys.HOME_TITLE_KEY, "Home");
    add(Util_MessageKeys.HOME_FETCHING_BALANCE_TEXT_KEY, "Fetching Balance");
    add(Util_MessageKeys.HOME_BALANCE_TEXT_KEY, "Balance: %s");
    add(Util_MessageKeys.HOME_SHOP_BUTTON_KEY, "Shop");
    add(Util_MessageKeys.HOME_MARKET_BUTTON_KEY, "Market");
    add(Util_MessageKeys.HOME_DELIVERY_BOX_BUTTON_KEY, "Mailbox");
    add(Util_MessageKeys.HOME_TERRITORY_BUTTON_KEY, "Territory");
    add(Util_MessageKeys.HOME_ABOUT_BUTTON_KEY, "About");
    add("screen.home.balance", "Balance");
    add("screen.home.trade", "Trading activity");
    add("screen.home.sell_orders", "Sell orders: %s");
    add("screen.home.demand_orders", "Demand orders: %s");
    add("screen.home.leaderboard", "Leaderboard");
    add("screen.home.leaderboard.self", "%s (You)");
    add("screen.home.loading", "Loading account data...");
    add("screen.home.sync_failed", "Account data could not be loaded");
    add("screen.home.sync_timeout", "Account data request timed out");
    add("screen.home.retry", "Retry");
    add("screen.home.leaderboard.empty", "No account data available");
    add("screen.home.version", "EconomySystem");
    add("screen.invite.sync_failed", "Player list could not be loaded");
    add("screen.invite.sync_timeout", "Player list request timed out");
    add("screen.invite.retry", "Retry");
    add("screen.territory.confirm.remove_title", "Remove territory");
    add("screen.territory.confirm.member_title", "Remove authorized member");
    add("screen.territory.confirm.remove_body", "Remove territory %s? This cannot be undone.");
    add("screen.territory.confirm.member_body", "Remove %s from this territory?");
    add("screen.territory.confirm.confirm", "Remove");
    add("screen.territory.confirm.cancel", "Cancel");

    // Reward | 击杀奖励
    add(Util_MessageKeys.MOB_REWARD_MESSAGE_KEY, "Mob Reward: %s %s");
    add("message.starter_kit.success", "Starter kit claimed: %s DreamingFish Coins");
    add("message.starter_kit.already_claimed", "You have already claimed the starter kit");
    add("message.starter_kit.balance_limit", "Your balance cannot receive the complete starter-kit reward");
    add("message.starter_kit.persist_failed", "The starter kit could not be saved; no reward was kept");
    add("message.starter_kit.state_unknown", "Starter-kit state could not be verified; contact an administrator");
    add("message.update.available", "A new version is available: %s");
    add("message.update.current", "EconomySystem is up to date");
    add("message.update.unavailable", "Could not check for EconomySystem updates");
    add("message.update.invalid_response", "The EconomySystem update response was invalid");
    add("message.update.copy_link", "[Copy download link]");
    add("message.update.copy_link_hover", "Copy the release link");

    // Screen_Shop | 商店
    add(Util_MessageKeys.SHOP_TITLE_KEY, "Shop");
    add("screen.shop.search", "Search shop");
    add("screen.shop.esc", "Press ESC to return");
    add("screen.shop.loading", "Loading shop...");
    add("screen.shop.empty", "No shop items found");
    add("screen.shop.sync_failed", "Shop data could not be loaded");
    add("screen.shop.sync_timeout", "Shop data request timed out");
    add("screen.shop.retry", "Retry");
    add("screen.shop.price", "Price: %s");
    add("screen.shop.buy", "Buy");
    add("screen.shop.purchase.title", "Confirm purchase");
    add("screen.shop.purchase.quantity", "Quantity");
    add("screen.shop.purchase.unit_price", "Unit price: %s");
    add("screen.shop.purchase.total", "Total: %s");
    add("screen.shop.purchase.confirm", "Buy");
    add("screen.shop.purchase.back", "Back");
    add("screen.shop.purchase.invalid_quantity", "Enter a positive quantity");
    add("screen.shop.purchase.inventory_full", "Your inventory does not have enough space");
    add("screen.shop.purchase.price_overflow", "Purchase total is too large");
    add(Util_MessageKeys.SHOP_HINT_TEXT_KEY, "Shop Hint");
    add(Util_MessageKeys.SHOP_ITEM_PRICE_KEY, "Shop Item Price: %s");
    add(Util_MessageKeys.SHOP_ITEM_ID_KEY, "Shop Item Id: %s");
    add(Util_MessageKeys.SHOP_ITEM_BASIC_PRICE_KEY, "Shop Item Basic Price: %s");
    add(Util_MessageKeys.SHOP_ITEM_CURRENT_PRICE_KEY, "Shop Item Current Price: %s");
    add(Util_MessageKeys.SHOP_ITEM_CHANGE_PRICE_KEY, "Shop Item Change Price: %s");
    add(Util_MessageKeys.SHOP_ITEM_FLUCTUATION_FACTOR_KEY, "Shop Item Fluctuation Factor: %s");
    add(Util_MessageKeys.SHOP_LOADING_SHOP_DATA_TEXT_KEY, "Loading...");
    add(Util_MessageKeys.SHOP_NO_MATCHING_ITEMS_TEXT_KEY, "No matching items.");
    add(Util_MessageKeys.SHOP_NO_ITEMS_AVAILABLE_TEXT_KEY, "No items available.");
    add(Util_MessageKeys.SHOP_SEARCH_HINT_TEXT_KEY, "Search items...");
    add(Util_MessageKeys.SHOP_ESC_HINT_TEXT_KEY, "Press ESC to return");
    add(Util_MessageKeys.SHOP_BUY_BUTTON_KEY, "Shop Buy");
    add(Util_MessageKeys.SHOP_BUY_SUCCESSFULLY_MESSAGE_KEY, "Shop Buy Successfully: %s %s %s");
    add(Util_MessageKeys.SHOP_BUY_FAILED_MESSAGE_KEY, "Shop Buy Failed");
    add(Util_MessageKeys.SHOP_REFRESH_MESSAGE_KEY, "Shop Refresh");
    add(Util_MessageKeys.SHOP_INVALID_ITEM_MESSAGE_KEY, "Shop Invalid Item");
    add(
        Util_MessageKeys.SHOP_BUY_FAILED_INVENTORY_FULL_MESSAGE_KEY,
        "Shop Buy Failed Inventory Full");
    add(Util_MessageKeys.SHOP_BUY_ERROR_MESSAGE_KEY, "Shop Buy Error");

    // Screen_BuyItem | 商店购买界面
    add(Util_MessageKeys.SHOP_BUY_TITLE_KEY, "Shop Buy Title");
    add(Util_MessageKeys.SHOP_BUY_HINT_TEXT_KEY, "Shop Buy Hint");
    add(Util_MessageKeys.SHOP_BUY_BUY_BUTTON_KEY, "Buy");
    add(Util_MessageKeys.SHOP_BUY_NO_ITEM_TEXT_KEY, "Shop Buy No Item");
    add(Util_MessageKeys.SHOP_BUY_NO_ITEM_MESSAGE_KEY, "Shop Buy No Item");
    add(Util_MessageKeys.SHOP_BUY_INVALID_COUNT_MESSAGE_KEY, "Shop Buy Invalid Count");
    add(Util_MessageKeys.SHOP_BUY_COUNT_TEXT_KEY, "Count: ");
    add(Util_MessageKeys.SHOP_BUY_TOTAL_PRICE_TEXT_KEY, "Total: %s");
    add(
        Util_MessageKeys.SHOP_BUY_INVENTORY_INSUFFICIENT_TEXT_KEY,
        "Not enough inventory space (%s slots missing)");

    // Screen_Market | 市场
    add(Util_MessageKeys.MARKET_TITLE_KEY, "Market Title");
    add("screen.market.search", "Search market");
    add("screen.market.esc", "Press ESC to return");
    add("screen.market.create_sales", "List item");
    add("screen.market.create_demand", "Request item");
    add("screen.market.sales", "Sales order");
    add("screen.market.demand", "Demand order");
    add("screen.market.price", "Price: %s");
    add("screen.market.buy", "Buy");
    add("screen.market.remove_sales", "Remove");
    add("screen.market.deliver", "Deliver");
    add("screen.market.confirm", "Confirm");
    add("screen.market.remove_demand", "Cancel");
    add("screen.market.done", "Done");
    add("screen.market.filter.all", "All");
    add("screen.market.filter.mine", "Mine");
    add("screen.market.filter.sales", "Sales");
    add("screen.market.filter.demand", "Demand");
    add("screen.market.empty", "No market orders found");
    add("screen.market.sync_failed", "Market data could not be loaded");
    add("screen.market.sync_timeout", "Market data request timed out");
    add("screen.market.retry", "Retry");
    add("screen.market.create.sales_title", "List market item");
    add("screen.market.create.demand_title", "Request market item");
    add("screen.market.create.inventory", "Choose an inventory item");
    add("screen.market.create.selected", "Listing settings");
    add("screen.market.create.item_id", "Item ID");
    add("screen.market.create.quantity", "Quantity");
    add("screen.market.create.price", "Total price");
    add("screen.market.create.all", "All");
    add("screen.market.create.submit", "Publish order");
    add("screen.market.create.back", "Back");
    add("screen.market.create.no_item", "Choose an item to list");
    add("screen.market.create.unknown_item", "Unknown item ID");
    add("screen.market.create.invalid_quantity", "Quantity is not valid for this item");
    add("screen.market.create.invalid_price", "Enter a positive total price");
    add("screen.market.confirm.title", "Confirm market action");
    add("screen.market.confirm.buy_title", "Confirm purchase");
    add("screen.market.confirm.remove_sales_title", "Confirm order removal");
    add("screen.market.confirm.remove_demand_title", "Confirm demand cancellation");
    add("screen.market.confirm.deliver_title", "Confirm delivery");
    add("screen.market.confirm.confirm_title", "Confirm collection");
    add("screen.market.confirm.item", "Item: %s");
    add("screen.market.confirm.price", "Total price: %s");
    add("screen.market.confirm.sales_warning", "This action will transfer the listed item and balance.");
    add("screen.market.confirm.demand_warning", "This action changes the demand order state.");
    add("screen.market.confirm.confirm", "Confirm");
    add("screen.market.confirm.cancel", "Cancel");

    // Shared balance-log page
    add("screen.balance_log.title", "Balance log");
    add("screen.balance_log.esc", "Press ESC to return");
    add("screen.balance_log.loading", "Loading balance history...");
    add("screen.balance_log.empty", "No balance history");
    add("screen.balance_log.sync_failed", "Balance history could not be loaded");
    add("screen.balance_log.sync_timeout", "Balance history request timed out");
    add("screen.balance_log.retry", "Retry");
    add("screen.balance_log.previous", "Previous");
    add("screen.balance_log.next", "Next");
    add(Util_MessageKeys.MARKET_HINT_TEXT_KEY, "Market Hint");
    add(Util_MessageKeys.MARKET_SELLER_NAME_KEY, "Market Seller Name: %s");
    add(Util_MessageKeys.MARKET_SELLER_UUID_KEY, "Market Seller Uuid: %s");
    add(Util_MessageKeys.MARKET_TRADE_ID_KEY, "Market Trade Id: %s");
    add(Util_MessageKeys.MARKET_ITEM_ID_KEY, "Market Item Id: %s");
    add(Util_MessageKeys.MARKET_ITEM_NAME_AND_COUNT_KEY, "Market Item Name And Count: %s %s");
    add(Util_MessageKeys.MARKET_ITEM_PRICE_KEY, "Market Item Price: %s");
    add(Util_MessageKeys.MARKET_NO_ITEMS_TEXT_KEY, "Market No Items");
    add(Util_MessageKeys.MARKET_LIST_BUTTON_KEY, "Market List");
    add(Util_MessageKeys.MARKET_REQUEST_BUTTON_KEY, "Market Request");
    add(Util_MessageKeys.MARKET_SWITCH_DISPLAY_TYPE_0_BUTTON_KEY, "Market Switch Display Type 0");
    add(Util_MessageKeys.MARKET_SWITCH_DISPLAY_TYPE_1_BUTTON_KEY, "Market Switch Display Type 1");
    add(Util_MessageKeys.MARKET_SWITCH_DISPLAY_TYPE_2_BUTTON_KEY, "Market Switch Display Type 2");
    add(Util_MessageKeys.MARKET_SWITCH_DISPLAY_TYPE_3_BUTTON_KEY, "Market Switch Display Type 3");
    add(Util_MessageKeys.MARKET_SWITCH_DISPLAY_TYPE_4_BUTTON_KEY, "Market Switch Display Type 4");
    add(Util_MessageKeys.MARKET_BUY_BUTTON_KEY, "Market Buy");
    add(Util_MessageKeys.MARKET_REMOVE_BUTTON_KEY, "Market Remove");
    add(Util_MessageKeys.MARKET_ITEM_DOES_NOT_EXIST_MESSAGE_KEY, "Market Item Does Not Exist");
    add(Util_MessageKeys.MARKET_PURCHASE_FAILED_MESSAGE_KEY, "Market Purchase Failed");
    add(
        Util_MessageKeys.MARKET_PURCHASE_SUCCESSFULLY_MESSAGE_KEY,
        "Market Purchase Successfully: %s %s %s");
    add(Util_MessageKeys.MARKET_COLLECT_MONEY_MESSAGE_KEY, "Market Collect Money: %s %s %s");
    add(Util_MessageKeys.MARKET_REMOVE_FAILED_MESSAGE_KEY, "Market Remove Failed");
    add(Util_MessageKeys.MARKET_UNMATCHED_SELLER_MESSAGE_KEY, "Market Unmatched Seller");
    add(
        Util_MessageKeys.MARKET_ITEM_HAS_BEEN_RETURNED_MESSAGE_KEY,
        "Market Item Has Been Returned");

    // Screen_CreateSalesOrder | 创建出货单
    add(Util_MessageKeys.LIST_TITLE_KEY, "List Title");
    add(Util_MessageKeys.LIST_NO_ITEM_IN_HAND_TEXT_KEY, "List No Item In Hand");
    add(Util_MessageKeys.LIST_PRICE_TEXT_KEY, "List Price");
    add(Util_MessageKeys.LIST_LIST_BUTTON_KEY, "List List");
    add(Util_MessageKeys.LIST_NO_ITEM_IN_HAND_MESSAGE_KEY, "List No Item In Hand");
    add(Util_MessageKeys.LIST_INVALID_PRICE_MESSAGE_KEY, "List Invalid Price");
    add(Util_MessageKeys.LIST_INVALID_QUANTITY_MESSAGE_KEY, "Invalid quantity");
    add(
        Util_MessageKeys.LIST_UNSUPPORTED_ITEM_DATA_MESSAGE_KEY,
        "This item's data is not supported by the market");
    add(
        Util_MessageKeys.LIST_MARKET_FULL_MESSAGE_KEY,
        "The market is full; please try again later");
    add(
        Util_MessageKeys.LIST_CREATE_FAILED_MESSAGE_KEY,
        "Could not create the sales order; please try again later");
    add(
        Util_MessageKeys.LIST_ROLLBACK_FAILED_MESSAGE_KEY,
        "Order creation failed and state recovery needs administrator attention");
    add(Util_MessageKeys.LIST_SUCCESSFULLY_MESSAGE_KEY, "List Successfully");
    add(Util_MessageKeys.LIST_INSUFFICIENT_ITEM_MESSAGE_KEY, "List Insufficient Item");
    add(Util_MessageKeys.LIST_UNMATCHED_ITEM_MESSAGE_KEY, "List Unmatched Item");
    add(
        Util_MessageKeys.LIST_ITEM_TAX_PAYMENT_FAILED_MESSAGE_KEY,
        "List Item Tax Payment Failed: %s");
    add(Util_MessageKeys.LIST_HINT_TEXT_KEY, "List Hint");

    // Screen_CreateDemandOrder | 创建求购单
    add(Util_MessageKeys.REQUEST_TITLE_KEY, "Request Title");
    add(Util_MessageKeys.REQUEST_ITEM_ID_HINT_TEXT_KEY, "Request Item Id Hint");
    add(Util_MessageKeys.REQUEST_ITEM_COUNT_HINT_TEXT_KEY, "Request Item Count Hint");
    add(Util_MessageKeys.REQUEST_PRICE_HINT_TEXT_KEY, "Request Price Hint");
    add(Util_MessageKeys.REQUEST_ITEM_ID_TEXT_KEY, "Request Item Id");
    add(Util_MessageKeys.REQUEST_ITEM_COUNT_TEXT_KEY, "Request Item Count");
    add(Util_MessageKeys.REQUEST_PRICE_TEXT_KEY, "Request Price");
    add(Util_MessageKeys.REQUEST_REQUEST_BUTTON_KEY, "Request Request");
    add(Util_MessageKeys.REQUEST_UNKNOWN_ITEM_ID_KEY, "Request Unknown Item Id");
    add(Util_MessageKeys.REQUEST_INVALID_ITEM_COUNT_KEY, "Request Invalid Item Count");
    add(Util_MessageKeys.REQUEST_EXCESSIVE_ITEM_COUNT_KEY, "Request Excessive Item Count");
    add(Util_MessageKeys.REQUEST_INVALID_PRICE_KEY, "Request Invalid Price");
    add(Util_MessageKeys.REQUEST_DELIVER_BUTTON_KEY, "Request Deliver");
    add(Util_MessageKeys.REQUEST_DELIVERED_STATUS_KEY, "Request Delivered Status");
    add(Util_MessageKeys.REQUEST_CANCEL_KEY, "Request Cancel");
    add(Util_MessageKeys.REQUEST_CLAIM_BUTTON_KEY, "Request Claim");
    add(Util_MessageKeys.DELIVERY_NOT_ENOUGH_ITEMS_KEY, "Delivery Not Enough Items");
    add(Util_MessageKeys.DELIVERY_SUCCESS_KEY, "Delivery Success: %s %s");
    add(Util_MessageKeys.CLAIM_SUCCESS_KEY, "Claim Success: %s %s");
    add(Util_MessageKeys.CLAIM_NOT_OWNER_KEY, "Claim Not Owner");
    add(Util_MessageKeys.ORDER_DELIVERED_BY_PLAYER_KEY, "Order Delivered By Player: %s %s %s");

    // Screen_DeliveryBox | 收货箱
    add(Util_MessageKeys.DELIVERY_BOX_TITLE_KEY, "Mailbox Title");
    add(Util_MessageKeys.DELIVERY_BOX_HINT_TEXT_KEY, "Mailbox Hint");
    add(Util_MessageKeys.DELIVERY_BOX_NO_ITEMS_TEXT_KEY, "Mailbox No Items");
    add(Util_MessageKeys.DELIVERY_BOX_SOURCE_KEY, "Mailbox Source: %s");
    add(Util_MessageKeys.DELIVERY_BOX_DATA_ID_KEY, "Mailbox Data Id: %s");
    add(Util_MessageKeys.DELIVERY_BOX_ITEM_ID_KEY, "Mailbox Item Id: %s");
    add(Util_MessageKeys.DELIVERY_BOX_CLAIM_BUTTON_KEY, "Mailbox Claim");
    add(
        Util_MessageKeys.DELIVERY_BOX_ITEM_NAME_AND_COUNT_KEY,
        "Mailbox Item Name And Count: %s %s");
    add("screen.delivery_box.search", "Search mailbox");
    add("screen.delivery_box.esc", "Press ESC to return");
    add("screen.delivery_box.loading", "Loading mailbox...");
    add("screen.delivery_box.empty", "Mailbox is empty");
    add("screen.delivery_box.sync_failed", "Mailbox could not be loaded");
    add("screen.delivery_box.sync_timeout", "Mailbox request timed out");
    add("screen.delivery_box.retry", "Retry");
    add("screen.mailbox.category.all", "All");
    add("screen.mailbox.category.unread", "Unread");
    add("screen.mailbox.category.market", "Market");
    add("screen.mailbox.category.system", "System");
    add("screen.mailbox.category.player", "Players");
    add("screen.mailbox.category.announcement", "Announcements");
    add("screen.mailbox.category_empty", "No mail in this category");
    add("screen.mailbox.sender.market", "From: Market System");
    add("screen.mailbox.sender.system", "From: System");
    add("screen.mailbox.sender.player", "From: Player");
    add("screen.mailbox.sender.compensation", "From: System Compensation");
    add("screen.mailbox.sender.announcement", "From: System Announcement");
    add("screen.mailbox.subject.market_return", "Market order return");
    add("screen.mailbox.subject.system_delivery", "System item mail");
    add("screen.mailbox.subject.player_delivery", "Player item mail");
    add("screen.mailbox.subject.compensation", "System compensation");
    add("screen.mailbox.subject.announcement", "System announcement");
    add("screen.mailbox.body.market_return", "Your market order has ended. Unhandled items were returned as an attachment.");
    add("screen.mailbox.body.system_delivery", "The system sent you an item attachment.");
    add("screen.mailbox.body.player_delivery", "Another player sent you an item attachment.");
    add("screen.mailbox.body.compensation", "You received system compensation.");
    add("screen.mailbox.body.announcement", "The system published an announcement.");
    add("screen.mailbox.attachments", "Attachments (%s)");
    add("screen.mailbox.source", "Source: %s");
    add("screen.mailbox.compose", "Compose");
    add("screen.mailbox.compose.title", "Compose mail");
    add("screen.mailbox.compose.recipient", "Recipient");
    add("screen.mailbox.compose.subject", "Subject");
    add("screen.mailbox.compose.body", "Message");
    add("screen.mailbox.compose.attachments", "Attachments %s/%s");
    add("screen.mailbox.compose.send", "Send");
    add("screen.mailbox.compose.back", "Back");
    add("screen.mailbox.claim_short", "Get");
    add("screen.mailbox.claim_all", "Claim all");
    add("screen.mailbox.delete", "Delete mail");
    add("screen.mailbox.dismiss", "Hide announcement");
    add("message.mailbox.delete.has_attachments", "Claim all attachments before deleting this mail");
    add("message.mailbox.claim_all.success", "Claimed %s attachment(s)");
    add("message.mailbox.new_mail", "You received new mail from %s");
    add("message.mailbox.send.success", "Mail sent successfully");
    add("message.mailbox.send.recipient_not_found", "Player not found");
    add("message.mailbox.send.cannot_send_to_self", "You cannot send mail to yourself");
    add("message.mailbox.send.invalid_content", "Mail content is invalid");
    add("message.mailbox.send.invalid_attachment", "An attachment changed; select it again");
    add("message.mailbox.send.recipient_mailbox_full", "The recipient's mailbox is full");
    add("message.mailbox.send.rate_limited", "You are sending mail too quickly");
    add("message.mailbox.send.failed", "Mail could not be sent");

    // RedPacket Command | 红包指令
    add(Util_MessageKeys.RED_PACKET_INSUFFICIENT_BALANCE, "Red Packet Insufficient Balance");
    add(Util_MessageKeys.RED_PACKET_ALREADY_ACTIVE, "Red Packet Already Active");
    add(Util_MessageKeys.RED_PACKET_CREATED_SUCCESSFULLY, "Red Packet Created Successfully");
    add(Util_MessageKeys.RED_PACKET_NO_AVAILABLE, "Red Packet No Available");
    add(Util_MessageKeys.RED_PACKET_ALREADY_CLAIMED, "Red Packet Already Claimed");
    add(Util_MessageKeys.RED_PACKET_CLAIM_SUCCESS, "Red Packet Claim Success: %s %s");
    add(Util_MessageKeys.RED_PACKET_CLAIM_BUTTON, "Red Packet Claim Button");
    add(Util_MessageKeys.RED_PACKET_BROADCAST, "Red Packet Broadcast: %s");
    add(Util_MessageKeys.RED_PACKET_NO_ACTIVE, "Red Packet No Active");
    add(Util_MessageKeys.RED_PACKET_CANCELLED, "Red Packet Cancelled");
    add(Util_MessageKeys.RED_PACKET_FULLY_CLAIMED, "Red Packet Fully Claimed: %s");
    add(Util_MessageKeys.RED_PACKET_EXPIRED_REFUNDED, "Red Packet Expired Refunded: %s");
    add(Util_MessageKeys.RED_PACKET_EXPIRED_BROADCAST, "Red Packet Expired Broadcast: %s");
    add(Util_MessageKeys.RED_PACKET_CLAIM_BROADCAST, "Red Packet Claim Broadcast: %s %s %s");

    // Screen_Territory | 我的领地
      add(Util_MessageKeys.TERRITORY_TITLE_KEY, "Territories");
      add("screen.territory.list.search", "Search territories");
      add("screen.territory.list.loading", "Loading territory data...");
      add("screen.territory.list.empty", "No territories found");
      add("screen.territory.list.esc", "Press ESC to return");
      add("screen.territory.list.owned", "Owned");
      add("screen.territory.list.authorized", "Authorized");
      add("screen.territory.list.dimension.overworld", "Overworld");
      add("screen.territory.list.dimension.nether", "Nether");
      add("screen.territory.list.dimension.end", "The End");
      add("screen.territory.list.sync_timeout", "Territory data request timed out");
    add(Util_MessageKeys.TERRITORY_HINT_TEXT_KEY, "Territory Hint");
    add(Util_MessageKeys.TERRITORY_NO_TERRITORIES_TEXT_KEY, "Territory No Territories");
    add(Util_MessageKeys.TERRITORY_TERRITORY_NAME_TEXT_KEY, "Territory Territory Name: %s");
    add(Util_MessageKeys.TERRITORY_TERRITORY_AREA_TEXT_KEY, "Territory Territory Area: %s");
    add(Util_MessageKeys.TERRITORY_TELEPORT_BUTTON_KEY, "Territory Teleport");
    add(Util_MessageKeys.TERRITORY_MANAGE_BUTTON_KEY, "Territory Manage");
    add(Util_MessageKeys.TERRITORY_TERRITORY_NAME_KEY, "Territory Territory Name: %s");
    add(Util_MessageKeys.TERRITORY_TERRITORY_UUID_KEY, "Territory Territory Uuid: %s");
    add(Util_MessageKeys.TERRITORY_TERRITORY_OWNER_NAME_KEY, "Territory Territory Owner Name: %s");
    add(Util_MessageKeys.TERRITORY_TERRITORY_OWNER_UUID_KEY, "Territory Territory Owner Uuid: %s");
    add(
        Util_MessageKeys.TERRITORY_TERRITORY_BACK_POINT_KEY,
        "Territory Territory Back Point: %s %s %s");
    add(
        Util_MessageKeys.TERRITORY_TERRITORY_NO_AUTHORIZED_PLAYER_KEY,
        "Territory Territory No Authorized Player");

    // Screen_ManageTerritory | 管理领地
    add(Util_MessageKeys.TERRITORY_MANAGEMENT_COPY_ID, "Territory Management Copy Id");
    add(Util_MessageKeys.TERRITORY_MANAGEMENT_COPY_SUCCESS, "Territory Management Copy Success");
    add(Util_MessageKeys.TERRITORY_MANAGEMENT_INVITE_PLAYER, "Territory Management Invite Player");
    add(
        Util_MessageKeys.TERRITORY_MANAGEMENT_DELETE_TERRITORY,
        "Territory Management Delete Territory");
    add(Util_MessageKeys.TERRITORY_MANAGEMENT_BUFF, "Territory Management Buff");
    add(Util_MessageKeys.TERRITORY_MANAGEMENT_PERMISSIONS, "Territory Management Permissions");
    add(
        Util_MessageKeys.TERRITORY_MANAGEMENT_TRANSFER_OWNERSHIP,
        "Territory Management Transfer Ownership");
    add(
        Util_MessageKeys.TERRITORY_MANAGEMENT_RESIZE_TERRITORY,
        "Territory Management Resize Territory");
    add(Util_MessageKeys.TERRITORY_NOT_FOUND, "Territory Not Found");
    add(Util_MessageKeys.TERRITORY_SYNC_FAILED, "Failed to synchronize territory data. Please try again.");
    add(Util_MessageKeys.TERRITORY_NO_OWNER_PERMISSION, "Territory No Owner Permission");
    add(Util_MessageKeys.TERRITORY_REMOVE_SUCCESS, "Territory Remove Success: %s");
    add(Util_MessageKeys.TERRITORY_MANAGEMENT_KICK_PLAYER, "Territory Management Kick Player");
    add(Util_MessageKeys.TERRITORY_NOT_EXIST, "Territory Not Exist");
    add(Util_MessageKeys.TERRITORY_NO_PERMISSION, "Territory No Permission");
    add(Util_MessageKeys.TERRITORY_PLAYER_KICKED, "Territory Player Kicked: %s");
    add(Util_MessageKeys.TERRITORY_PLAYER_REMOVED, "Territory Player Removed");

    // Screen_InvitePlayer | 邀请玩家
    add(Util_MessageKeys.INVITE_TITLE_KEY, "Invite Title");
    add("screen.invite.search", "Search player");
    add("screen.invite.territory", "Territory: %s");
    add("button.territory.invite", "Invite");
    add(Util_MessageKeys.INVITE_INVITE_BUTTON_KEY, "Invite Invite");
    add(Util_MessageKeys.INVITE_NO_NAME_KEY, "Invite No Name");
    add(Util_MessageKeys.INVITE_NO_PERMISSION, "Invite No Permission");
    add("message.invite.accept", "[Accept]");
    add("message.invite.decline", "[Decline]");
    add("message.invite.territory_not_found", "Territory not found.");
    add("message.invite.target_offline", "That player is offline.");
    add("message.invite.cannot_invite_owner", "The territory owner cannot be invited.");
    add("message.invite.cannot_invite_self", "You cannot invite yourself.");
    add("message.invite.already_pending", "An invitation for this player and territory is already pending.");
    add("message.invite.rate_limited", "Please wait before sending another invitation.");
    add("message.invite.store_full", "The server invitation queue is full.");
    add("message.invite.create_failed", "The invitation could not be created.");
    add("message.invite.not_found", "No matching pending invitation was found.");
    add("message.invite.not_target", "This invitation is not addressed to you.");
    add("message.invite.multiple_pending", "You have multiple invitations; use the invitation-specific command.");
    add("message.invite.owner_changed", "The territory owner changed; this invitation is no longer valid.");
    add("message.invite.persist_failed", "The membership change could not be saved; try again.");
    add("message.invite.state_unknown", "Membership state is uncertain; contact an administrator.");
    add("message.invite.busy", "This invitation is already being processed.");
    add(Util_MessageKeys.INVITE_ALREADY_MEMBER, "That player is already a territory member.");
    add(Util_MessageKeys.INVITE_SENT, "Invitation sent to %s for territory %s.");
    add(Util_MessageKeys.INVITE_RECEIVED, "%s invited you to join territory %s.");
    add(Util_MessageKeys.INVITE_NOT_IN_TERRITORY, "Invite Not In Territory");
    add(Util_MessageKeys.COMMAND_PLAYER_ONLY, "Command Player Only");
    add(
        Util_MessageKeys.TERRITORY_SETBACKPOINT_NO_PERMISSION,
        "Territory Setbackpoint No Permission");
    add(
        Util_MessageKeys.TERRITORY_SETBACKPOINT_SUCCESS,
        "Territory Setbackpoint Success: %s %s %s");
    add(Util_MessageKeys.INVITE_NO_PENDING, "You have no pending territory invitations.");
    add(Util_MessageKeys.INVITE_ACCEPTED, "Accepted the invitation to territory %s.");
    add(Util_MessageKeys.INVITE_DECLINED, "Declined the invitation to territory %s.");
    add("message.invite.accepted_by", "%s accepted the invitation to territory %s.");
    add("message.invite.declined_by", "%s declined the invitation to territory %s.");
    add("screen.invite.loading", "Loading players...");
    add("screen.invite.empty", "No eligible online players.");
    add(Util_MessageKeys.INVITE_BACK_BUTTON, "Back");
    add(Util_MessageKeys.INVITE_ACCEPT_BUTTON, "Accept");
    add(Util_MessageKeys.INVITE_DECLINE_BUTTON, "Decline");

    // Screen_TerritoryBuff | 领地增益
    add(Util_MessageKeys.TERRITORY_BUFF_TITLE_KEY, "Territory Buffs");
    add(Util_MessageKeys.TERRITORY_BUFF_TEXT_NO_BUFFS_TEXT_KEY, "No buffs available");
    add(Util_MessageKeys.TERRITORY_BUFF_TEXT_BE_LOCKED_TEXT_KEY, "Locked");
    add(Util_MessageKeys.TERRITORY_BUFF_TEXT_BE_UNLOCKED_TEXT_KEY, "Unlocked");
    add(Util_MessageKeys.TERRITORY_BUFF_TOOLTIP_BUFF_ID_TEXT_KEY, "Buff ID: %s");
    add(Util_MessageKeys.TERRITORY_BUFF_TOOLTIP_BUFF_NAME_TEXT_KEY, "Buff Name: %s");
    add(Util_MessageKeys.TERRITORY_BUFF_TOOLTIP_BUFF_CURRENT_LEVEL_TEXT_KEY, "Current Level: %s");
    add(Util_MessageKeys.TERRITORY_BUFF_TOOLTIP_BUFF_MAX_LEVEL_TEXT_KEY, "Max Level: %s");
    add(Util_MessageKeys.TERRITORY_BUFF_TOOLTIP_BUFF_EFFECT_ID_TEXT_KEY, "Effect ID: %s");
    add(Util_MessageKeys.TERRITORY_BUFF_TOOLTIP_BUFF_UNLOCK_STATE_KEY, "Unlocked: %s");
    add(Util_MessageKeys.TERRITORY_BUFF_COST_LABEL_KEY, "[Upgrade Cost]");
    add(Util_MessageKeys.TERRITORY_BUFF_BUTTON_UNLOCK_KEY, "Unlock");
    add(Util_MessageKeys.TERRITORY_BUFF_BUTTON_UPGRADE_KEY, "Upgrade");
    add(Util_MessageKeys.TERRITORY_BUFF_BUTTON_MAX_KEY, "Max");
    add(
        Util_MessageKeys.TERRITORY_BUFF_MESSAGE_BUFF_MAX_LEVEL_KEY,
        "This buff is already at max level");
    add(
        Util_MessageKeys.TERRITORY_BUFF_MESSAGE_REQUIREMENT_ITEM_FAIL_KEY,
        "Required items not found");
    add(
        Util_MessageKeys.TERRITORY_BUFF_MESSAGE_REQUIREMENT_XP_LEVEL_FAIL_KEY,
        "Insufficient XP level");

    // Claim Territory | 圈地
    add(Util_MessageKeys.CLAIM_WAND_SELECT_POINTS, "Claim Wand Select Points");
    add(Util_MessageKeys.CLAIM_INSUFFICIENT_BALANCE, "Claim Insufficient Balance: %s");
    add(Util_MessageKeys.CLAIM_SUCCESS, "Claim Success: %s %s");
    add(Util_MessageKeys.CLAIM_WAND_FIRST_POSITION_SET, "Claim Wand First Position Set: %s %s %s");
    add(
        Util_MessageKeys.CLAIM_WAND_SECOND_POSITION_SET,
        "Claim Wand Second Position Set: %s %s %s");
    add(Util_MessageKeys.CLAIM_WAND_OVERLAP_ERROR, "Claim Wand Overlap Error");
    add(Util_MessageKeys.CLAIM_WAND_Y_MISMATCH_ERROR, "Claim Wand Y Mismatch Error");
    add(Util_MessageKeys.CLAIM_WAND_VOLUME, "Claim Wand Volume: %s");
    add(Util_MessageKeys.CLAIM_WAND_PRICE, "Claim Wand Price: %s");
    add(Util_MessageKeys.CLAIM_WAND_INSTRUCTION, "Claim Wand Instruction");
    add(Util_MessageKeys.CLAIM_WAND_CANCEL, "Claim Wand Cancel");
    add(Util_MessageKeys.CLAIM_WAND_TIMEOUT, "Claim Wand Timeout");
    add(Util_MessageKeys.CLAIM_RESIZE_FAILED, "Claim Resize Failed");
    add(Util_MessageKeys.CLAIM_RESIZE_SUCCESS, "Claim Resize Success");
    add(Util_MessageKeys.CLAIM_RESIZE_INSUFFICIENT_BALANCE, "Claim Resize Insufficient Balance");
    add(Util_MessageKeys.CLAIM_WAND_CONFIRM_EXPAND, "Claim Wand Confirm Expand");
    add(
        Util_MessageKeys.CLAIM_WAND_RESIZE_COST_DETAILS,
        "Claim Wand Resize Cost Details: %s %s %s");
    add(Util_MessageKeys.CLAIM_WAND_CONFIRM_SHRINK, "Claim Wand Confirm Shrink");
    add(Util_MessageKeys.CLAIM_WAND_VOLUME_CHANGE, "Claim Wand Volume Change: %s %s");
    add(Util_MessageKeys.CLAIM_WAND_ENTER_RESIZE_MODE, "Claim Wand Enter Resize Mode");
    add(Util_MessageKeys.CLAIM_WAND_EXIT_RESIZE_MODE, "Claim Wand Exit Resize Mode");

    // Territory Teleport | 领地传送
    add(Util_MessageKeys.TELEPORT_TARGET_NOT_FOUND, "Teleport Target Not Found");
    add(Util_MessageKeys.TELEPORT_NO_PERMISSION, "Teleport No Permission");
    add(Util_MessageKeys.TELEPORT_NO_BACKPOINT, "Teleport No Backpoint");
    add(Util_MessageKeys.TELEPORT_DIMENSION_NOT_FOUND, "Teleport Dimension Not Found");
    add(Util_MessageKeys.TELEPORT_NO_POTION, "Teleport No Potion");
    add(Util_MessageKeys.TELEPORT_SUCCESS, "Teleport Success: %s");
    add(Util_MessageKeys.TELEPORT_FAILED, "Teleport Failed");
    add(Util_MessageKeys.TELEPORT_UNSAFE_DESTINATION, "The territory backpoint is unsafe. Ask its owner to repair it.");
    add(Util_MessageKeys.TELEPORT_COOLDOWN, "Please wait before teleporting again.");
    add(Util_MessageKeys.TELEPORT_ROLLBACK_FAILED, "Teleport failed and the recall potion could not be restored. Contact an administrator.");
    add(Util_MessageKeys.TELEPORT_STATE_UNKNOWN, "Teleport status could not be confirmed. The recall potion was not refunded; contact an administrator.");

    // Screen_About | 关于页
    add(Util_MessageKeys.ABOUT_TITLE_KEY, "About Title");
    add(Util_MessageKeys.ABOUT_MOD_NAME_KEY, "About Mod Name");
    add(Util_MessageKeys.ABOUT_AUTHOR_NAME_KEY, "About Author Name: %s");
    add(Util_MessageKeys.ABOUT_GITHUB_URL_KEY, "About Github Url: %s");
    add(Util_MessageKeys.ABOUT_TEXT_SHOW_KEY, "About Text Show");
    add(Util_MessageKeys.ABOUT_COPY_URL, "About Copy Url");
    add(Util_MessageKeys.ABOUT_BACK_BUTTON_KEY, "About Back");
    add("screen.about.esc", "Press ESC to return");
    add(Util_MessageKeys.REQUEST_CREATE_SUCCESS, "Demand order created");
    add(Util_MessageKeys.REQUEST_INVALID_ITEM_ID, "Invalid item ID");
    add(Util_MessageKeys.REQUEST_ITEM_NOT_FOUND, "Item not found");
    add(Util_MessageKeys.REQUEST_INVALID_QUANTITY, "Invalid demand quantity");
    add(
        Util_MessageKeys.REQUEST_QUANTITY_EXCEEDS_LIMIT,
        "Quantity exceeds this item's stack limit");
    add(Util_MessageKeys.REQUEST_INVALID_PRICE, "Invalid total price");
    add(
        Util_MessageKeys.REQUEST_INSUFFICIENT_FUNDS,
        "Insufficient balance to freeze the total price");
    add(Util_MessageKeys.REQUEST_MARKET_FULL, "The market is full; please try again later");
    add(Util_MessageKeys.REQUEST_UNSUPPORTED_ITEM, "This item's default form is not supported");
    add(
        Util_MessageKeys.REQUEST_CREATE_FAILED,
        "Could not create the demand order; please try again later");
    add(
        Util_MessageKeys.REQUEST_REFUND_FAILED,
        "Order creation failed and the refund needs administrator attention");
    add(
        Util_MessageKeys.DELIVERY_BALANCE_LIMIT_KEY,
        "Your balance cannot receive the full demand-order payment");
    add(Util_MessageKeys.REQUEST_CANCEL_SUCCESS, "Demand order cancelled and fully refunded");
    add(Util_MessageKeys.REQUEST_CANCEL_NOT_FOUND, "Demand order not found");
    add(Util_MessageKeys.REQUEST_CANCEL_NOT_OWNER, "You cannot cancel this demand order");
    add(Util_MessageKeys.REQUEST_CANCEL_DELIVERED, "A delivered demand order cannot be cancelled");
    add(
        Util_MessageKeys.REQUEST_CANCEL_BALANCE_LIMIT,
        "The refund would exceed the owner's balance limit");
    add(Util_MessageKeys.REQUEST_CANCEL_FAILED, "Could not cancel the demand order");
    add("message.request.cancel_state_unknown", "Market state is uncertain; refresh before retrying");
    add(
        Util_MessageKeys.REQUEST_CANCEL_ROLLBACK_FAILED,
        "Cancellation failed and order recovery needs administrator attention");
    add(Util_MessageKeys.MARKET_SEARCH_HINT, "Search item ID or order creator");
    add(Util_MessageKeys.MARKET_LOADING, "Loading");
    add(Util_MessageKeys.MARKET_STALE, "Page changed; refreshing");
    add(Util_MessageKeys.MARKET_SYNC_FAILED, "Market data synchronization failed");
    add(Util_MessageKeys.MARKET_SELLER, "Seller");
    add(Util_MessageKeys.MARKET_REQUESTER, "Requester");
    add(Util_MessageKeys.MARKET_PURCHASE_SUCCESS, "Purchased %s x%s for %s MengYu Coins");
    add(Util_MessageKeys.MARKET_REMOVE_SALES_SUCCESS, "Removed %s x%s from the market");
    add(Util_MessageKeys.MARKET_REMOVE_SALES_NOT_FOUND, "This sales order no longer exists");
    add(Util_MessageKeys.MARKET_REMOVE_SALES_WRONG_TYPE, "This is not a sales order");
    add(Util_MessageKeys.MARKET_REMOVE_SALES_NOT_OWNER, "You cannot remove this order");
    add(Util_MessageKeys.MARKET_REMOVE_SALES_OWNER_OFFLINE, "The original owner must be online");
    add(Util_MessageKeys.MARKET_REMOVE_SALES_INVENTORY_FULL, "The owner's main inventory is full");
    add(Util_MessageKeys.MARKET_REMOVE_SALES_ORDER_CHANGED, "The order changed; please retry");
    add(Util_MessageKeys.MARKET_REMOVE_SALES_PERSIST_FAILED, "Could not persist the market change");
    add(Util_MessageKeys.MARKET_REMOVE_SALES_ITEM_FAILED, "Could not restore the listed item");
    add(
        Util_MessageKeys.MARKET_REMOVE_SALES_ROLLBACK_FAILED,
        "Rollback needs administrator attention");
    add(Util_MessageKeys.MARKET_REMOVE_SALES_FAILED, "Could not remove the sales order");
    add(Util_MessageKeys.MARKET_REMOVE_SALES_OPERATOR_NOTICE, "%s removed your %s x%s sales order");
    add(
        Util_MessageKeys.MARKET_CONFIRM_DEMAND_SUCCESS,
        "Claimed %s x%s from the delivered demand order");
    add(Util_MessageKeys.MARKET_CONFIRM_DEMAND_NOT_FOUND, "This demand order no longer exists");
    add(Util_MessageKeys.MARKET_CONFIRM_DEMAND_WRONG_TYPE, "This is not a demand order");
    add(
        Util_MessageKeys.MARKET_CONFIRM_DEMAND_NOT_DELIVERED,
        "This demand order has not been delivered");
    add(Util_MessageKeys.MARKET_CONFIRM_DEMAND_NOT_OWNER, "You cannot claim this order");
    add(Util_MessageKeys.MARKET_CONFIRM_DEMAND_OWNER_OFFLINE, "The original owner must be online");
    add(
        Util_MessageKeys.MARKET_CONFIRM_DEMAND_INVENTORY_FULL,
        "The owner's main inventory is full");
    add(Util_MessageKeys.MARKET_CONFIRM_DEMAND_ORDER_CHANGED, "The order changed; please retry");
    add(
        Util_MessageKeys.MARKET_CONFIRM_DEMAND_PERSIST_FAILED,
        "Could not persist the market change");
    add(Util_MessageKeys.MARKET_CONFIRM_DEMAND_ITEM_FAILED, "Could not restore the delivered item");
    add(
        Util_MessageKeys.MARKET_CONFIRM_DEMAND_ROLLBACK_FAILED,
        "Rollback needs administrator attention");
    add(Util_MessageKeys.MARKET_CONFIRM_DEMAND_FAILED, "Could not confirm the demand order");
    add(
        Util_MessageKeys.MARKET_CONFIRM_DEMAND_OPERATOR_NOTICE,
        "%s claimed %s x%s from your delivered demand order");
    add(Util_MessageKeys.MARKET_PURCHASE_NOT_FOUND, "This sales order no longer exists");
    add(Util_MessageKeys.MARKET_PURCHASE_WRONG_TYPE, "This order is not a sales order");
    add(Util_MessageKeys.MARKET_PURCHASE_SELF, "You cannot purchase your own sales order");
    add(Util_MessageKeys.MARKET_PURCHASE_INSUFFICIENT_FUNDS, "Insufficient balance");
    add(
        Util_MessageKeys.MARKET_PURCHASE_SELLER_BALANCE_LIMIT,
        "The seller cannot receive the full payment");
    add(
        Util_MessageKeys.MARKET_PURCHASE_INVENTORY_FULL,
        "Your main inventory does not have enough space");
    add(
        Util_MessageKeys.MARKET_PURCHASE_ORDER_CHANGED,
        "The order changed; please refresh and try again");
    add(Util_MessageKeys.MARKET_PURCHASE_PERSIST_FAILED, "Could not persist the market change");
    add(
        Util_MessageKeys.MARKET_PURCHASE_ITEM_FAILED,
        "Could not deliver the complete item quantity");
    add(
        Util_MessageKeys.MARKET_PURCHASE_PAYMENT_FAILED,
        "Payment failed; no purchase was completed");
    add(
        Util_MessageKeys.MARKET_PURCHASE_ROLLBACK_FAILED,
        "Purchase rollback needs administrator attention");
    add(Util_MessageKeys.MARKET_PURCHASE_FAILED, "Purchase failed");
    add(Util_MessageKeys.MARKET_PURCHASE_SELLER_NOTICE, "Sold %s x%s to %s for %s MengYu Coins");
    add(Util_MessageKeys.MARKET_DELIVER_DEMAND_SUCCESS, "Delivered %s x%s for %s MengYu Coins");
    add("message.market.deliver_demand.not_found", "Demand order not found");
    add("message.market.deliver_demand.wrong_type", "This is not a demand order");
    add("message.market.deliver_demand.already_delivered", "Demand order already delivered");
    add("message.market.deliver_demand.self", "You cannot deliver your own demand order");
    add("message.market.deliver_demand.invalid_order", "Demand order data is invalid");
    add("message.market.deliver_demand.item_failed", "The requested item could not be restored");
    add("message.market.deliver_demand.insufficient_items", "Not enough matching items");
    add("message.market.deliver_demand.balance_limit", "Your balance cannot receive the full payment");
    add("message.market.deliver_demand.inventory_failed", "Inventory update failed");
    add("message.market.deliver_demand.payment_failed", "Payment failed");
    add("message.market.deliver_demand.order_changed", "Demand order changed; refresh and try again");
    add("message.market.deliver_demand.persist_failed", "Market data could not be saved");
    add("message.market.deliver_demand.state_unknown", "Market state is uncertain; refresh before retrying");
    add("message.market.deliver_demand.rollback_failed", "Delivery rollback needs administrator attention");
    add("message.market.expired.sales_return", "Your sales order for %s x%s expired; the items were returned to your mailbox");
    add("message.market.expired.demand_refunded", "Your demand order for %s expired; %s coins were refunded");
    add("message.market.expired.demand_delivered", "Your delivered demand item %s x%s expired; it was moved to your mailbox");
    add(Util_MessageKeys.MARKET_DELIVER_DEMAND_FAILED, "Could not deliver the demand order");
    add(
        Util_MessageKeys.MARKET_DELIVER_DEMAND_REQUESTER_NOTICE,
        "%s delivered %s x%s to your demand order");
  }
}
