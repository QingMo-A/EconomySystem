package com.mo.economy_system.common.network;

import com.mo.economy_system.common.market.MarketLedger;

/** Shared defensive bounds applied by every loader-specific wire codec. */
public final class EconomyNetworkLimits {
  public static final int MAX_SHOP_ENTRIES = 1_024;
  public static final int MAX_SHOP_ITEM_ID_LENGTH = 128;
  public static final int MAX_ITEM_RESOURCE_ID_LENGTH = 256;
  public static final int MAX_SHOP_DESCRIPTION_LENGTH = 2_048;
  public static final int MAX_ITEM_DATA_LENGTH = 32_767;
  public static final int MAX_ACCOUNT_ENTRIES = 16_384;
  public static final int MAX_PLAYER_LIST_ENTRIES = 16_384;
  public static final int MAX_PLAYER_NAME_LENGTH = 64;
  public static final int MAX_BALANCE_LOG_ENTRIES = 100;
  public static final int MAX_BALANCE_LOG_CATEGORY_LENGTH = 64;
  public static final int MAX_BALANCE_LOG_REASON_LENGTH = 256;
  public static final int MAX_MARKET_PAGE_SIZE = 9;
  public static final int MAX_MARKET_QUERY_LENGTH = 64;
  public static final int MAX_MARKET_OWNER_NAME_LENGTH = 64;
  public static final int MAX_MARKET_ORDERS = MarketLedger.MAX_ORDERS;
  public static final int MAX_MARKET_RESPONSE_ESTIMATED_BYTES = 768 * 1024;
  public static final int MAX_MARKET_RESPONSE_WIRE_BYTES = MAX_MARKET_RESPONSE_ESTIMATED_BYTES;
  public static final int MAX_TERRITORIES_PER_RESPONSE = 2_048;
  public static final int MAX_TERRITORY_NAME_LENGTH = 128;
  public static final int MAX_TERRITORY_MEMBERS = 1_024;
  public static final int MAX_TERRITORY_BUFFS = 128;
  public static final int MAX_TERRITORY_RULES = 32;
  public static final int MAX_TERRITORY_BUFF_COST_LEVELS = 128;
  public static final int MAX_TERRITORY_COST_ITEMS = 64;
  public static final int MAX_TERRITORY_TEXT_LENGTH = 2_048;
  public static final int MAX_TERRITORY_RESPONSE_ESTIMATED_BYTES = 1024 * 1024;
  public static final int MAX_TERRITORY_RESPONSE_WIRE_BYTES = 1024 * 1024;
  public static final int MAX_CHECK_RESULT_JSON_LENGTH = 30_000;
  public static final int MAX_CHECK_FILES = 1_024;
  public static final int MAX_CHECK_FILE_NAME_LENGTH = 255;
  public static final int MAX_CHECK_SKIPPED_FILES = 256;
  public static final long MAX_CHECK_SINGLE_FILE_BYTES = 536_870_912L;
  public static final long MAX_CHECK_TOTAL_HASHED_BYTES = 2_147_483_648L;
  public static final int MAX_CHECK_SCAN_SECONDS = 30;
  public static final int MAX_PENDING_CHECKS = 1_024;
  public static final long CHECK_REQUEST_TTL_TICKS = 1_200;
  public static final long CHECK_REQUEST_COOLDOWN_TICKS = 100;

  private EconomyNetworkLimits() {}
}
