package com.mo.economy_system.platform.item;

import com.mo.economy_system.common.network.EconomyNetworkLimits;

/** Central defensive bounds for persisted and transferred item snapshots. */
public final class ItemStackSnapshotLimits {
    public static final int MAX_ITEM_ID_LENGTH = EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH;
    public static final int MAX_CUSTOM_NAME_JSON_LENGTH = 8_192;
    public static final int MAX_LORE_LINES = 64;
    public static final int MAX_LORE_LINE_JSON_LENGTH = 8_192;
    public static final int MAX_LORE_TOTAL_JSON_LENGTH = 32_768;
    public static final int MAX_ENCHANTMENTS = 64;
    public static final int MAX_STORED_ENCHANTMENTS = 64;
    public static final int MAX_ENCHANTMENT_ID_LENGTH = EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH;
    public static final int MAX_CUSTOM_DATA_BYTES = EconomyNetworkLimits.MAX_ITEM_DATA_LENGTH;
    public static final int MAX_CUSTOM_DATA_DEPTH = 16;
    public static final int MAX_ENCODED_SNAPSHOT_BYTES = 65_536;

    private ItemStackSnapshotLimits() {}
}
