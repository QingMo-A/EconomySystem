package com.mo.economy_system.protocol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Canonical NeoForge 1.21.1 protocol manifest shared by every target.
 *
 * <p>Discriminators are the Forge 1.20.1 wire numbers. Existing entries must
 * never be reordered, renumbered, or reused. New messages are appended only.</p>
 */
public final class EconomyProtocol {
    /** Bumped for deferred currency-reward fields appended to mailbox responses. */
    public static final String VERSION = "bridge-6";

    private static final List<EconomyMessageSpec> DECLARATIONS = new ArrayList<>();

    public static final EconomyMessageSpec BALANCE_REQUEST = c2s(0, "economy_system:economy_system/packet_balance_request");
    public static final EconomyMessageSpec BALANCE_RESPONSE = s2c(1, "economy_system:economy_system/packet_balance_response");
    public static final EconomyMessageSpec BALANCE_LOG_REQUEST = c2s(2, "economy_system:economy_system/packet_balance_log_request");
    public static final EconomyMessageSpec BALANCE_LOG_RESPONSE = s2c(3, "economy_system:economy_system/packet_balance_log_response");
    public static final EconomyMessageSpec TRANSFER = c2s(4, "economy_system:economy_system/packet_transfer");
    public static final EconomyMessageSpec SHOP_DATA_REQUEST = c2s(5, "economy_system:economy_system/packet_shop_data_request");
    public static final EconomyMessageSpec SHOP_DATA_RESPONSE = s2c(6, "economy_system:economy_system/packet_shop_data_response");
    public static final EconomyMessageSpec SHOP_BUY_ITEM = c2s(7, "economy_system:economy_system/packet_shop_buy_item");
    public static final EconomyMessageSpec CREATE_SALES_ORDER = c2s(8, "economy_system:economy_system/sales_order/packet_create_sales_order");
    public static final EconomyMessageSpec CREATE_DEMAND_ORDER = c2s(9, "economy_system:economy_system/demand_order/packet_create_demand_order");
    public static final EconomyMessageSpec MARKET_DATA_REQUEST = c2s(10, "economy_system:economy_system/packet_market_data_request");
    public static final EconomyMessageSpec MARKET_DATA_RESPONSE = s2c(11, "economy_system:economy_system/packet_market_data_response");
    public static final EconomyMessageSpec PURCHASE_SALES_ORDER = c2s(12, "economy_system:economy_system/sales_order/packet_purchase_sales_order");
    public static final EconomyMessageSpec CONFIRM_DEMAND_ORDER = c2s(13, "economy_system:economy_system/demand_order/packet_confirm_demand_order");
    public static final EconomyMessageSpec DELIVER_DEMAND_ORDER = c2s(14, "economy_system:economy_system/demand_order/packet_deliver_demand_order");
    public static final EconomyMessageSpec REMOVE_SALES_ORDER = c2s(15, "economy_system:economy_system/sales_order/packet_remove_sales_order");
    public static final EconomyMessageSpec REMOVE_DEMAND_ORDER = c2s(16, "economy_system:economy_system/demand_order/packet_remove_demand_order");
    public static final EconomyMessageSpec TERRITORY_DATA_REQUEST = c2s(17, "economy_system:territory_system/packet_territory_data_request");
    public static final EconomyMessageSpec TERRITORY_DATA_RESPONSE = s2c(18, "economy_system:territory_system/packet_territory_data_response");
    public static final EconomyMessageSpec TELEPORT_TO_TERRITORY = c2s(19, "economy_system:territory_system/packet_teleport_to_territory");
    public static final EconomyMessageSpec INVITE_PLAYER = c2s(20, "economy_system:territory_system/packet_invite_player");
    public static final EconomyMessageSpec REMOVE_TERRITORY = c2s(21, "economy_system:territory_system/packet_remove_territory");
    public static final EconomyMessageSpec REMOVE_PLAYER = c2s(22, "economy_system:territory_system/packet_remove_player");
    public static final EconomyMessageSpec CHECK = s2c(23, "economy_system:check_system/packet_check");
    public static final EconomyMessageSpec CHECK_RESULT_REQUEST = c2s(24, "economy_system:check_system/packet_check_result_request");
    public static final EconomyMessageSpec CHECK_RESULT_RESPONSE = s2c(25, "economy_system:check_system/packet_check_result_response");
    public static final EconomyMessageSpec GET = s2c(26, "economy_system:check_system/packet_get");
    public static final EconomyMessageSpec GET_RESULT_REQUEST = c2s(27, "economy_system:check_system/packet_get_result_request");
    public static final EconomyMessageSpec GET_RESULT_RESPONSE = s2c(28, "economy_system:check_system/packet_get_result_response");
    public static final EconomyMessageSpec CHUNK = c2s(29, "economy_system:check_system/packet_chunk");
    public static final EconomyMessageSpec CHUNK_RESPONSE = s2c(30, "economy_system:check_system/packet_chunk_response");
    public static final EconomyMessageSpec DELIVERY_BOX_DATA_REQUEST = c2s(31, "economy_system:economy_system/packet_delivery_box_data_request");
    public static final EconomyMessageSpec DELIVERY_BOX_DATA_RESPONSE = s2c(32, "economy_system:economy_system/packet_delivery_box_data_response");
    public static final EconomyMessageSpec DELIVERY_BOX_CLAIM_ITEM = c2s(33, "economy_system:economy_system/packet_delivery_box_claim_item");
    public static final EconomyMessageSpec SERVER_PLAYER_LIST_REQUEST = c2s(34, "economy_system:packet_server_player_list_request");
    public static final EconomyMessageSpec SERVER_PLAYER_LIST_RESPONSE = s2c(35, "economy_system:packet_server_player_list_response");
    public static final EconomyMessageSpec MODIFY_MODE = c2s(36, "economy_system:territory_system/packet_modify_mode");
    public static final EconomyMessageSpec UNLOCK_TERRITORY_BUFF = c2s(37, "economy_system:territory_system/packet_unlock_territory_buff");
    public static final EconomyMessageSpec UPGRADE_TERRITORY_BUFF = c2s(38, "economy_system:territory_system/packet_upgrade_territory_buff");
    public static final EconomyMessageSpec SINGLE_TERRITORY_DATA_REQUEST = c2s(39, "economy_system:territory_system/packet_single_territory_data_request");
    public static final EconomyMessageSpec SINGLE_TERRITORY_DATA_RESPONSE = s2c(40, "economy_system:territory_system/packet_single_territory_data_response");
    public static final EconomyMessageSpec UPDATE_TERRITORY_PERMISSION = c2s(41, "economy_system:territory_system/packet_update_territory_permission");
    public static final EconomyMessageSpec TRANSFER_TERRITORY_OWNERSHIP = c2s(42, "economy_system:territory_system/packet_transfer_territory_ownership");
    public static final EconomyMessageSpec UPDATE_TERRITORY_RULE = c2s(43, "economy_system:territory_system/packet_update_territory_rule");
    public static final EconomyMessageSpec MAILBOX_DATA_REQUEST = c2s(44, "economy_system:mailbox/packet_data_request");
    public static final EconomyMessageSpec MAILBOX_DATA_RESPONSE = s2c(45, "economy_system:mailbox/packet_data_response");
    public static final EconomyMessageSpec MAILBOX_MARK_READ = c2s(46, "economy_system:mailbox/packet_mark_read");
    public static final EconomyMessageSpec MAILBOX_DELETE = c2s(47, "economy_system:mailbox/packet_delete");
    public static final EconomyMessageSpec MAILBOX_CLAIM_ATTACHMENT = c2s(48, "economy_system:mailbox/packet_claim_attachment");
    public static final EconomyMessageSpec MAILBOX_CLAIM_ALL = c2s(49, "economy_system:mailbox/packet_claim_all");
    public static final EconomyMessageSpec MAILBOX_SEND_PLAYER = c2s(50, "economy_system:mailbox/packet_send_player");
    public static final EconomyMessageSpec MAILBOX_SEND_RESULT = s2c(51, "economy_system:mailbox/packet_send_result");
    public static final EconomyMessageSpec MAILBOX_NOTIFICATION = s2c(52, "economy_system:mailbox/packet_notification");
    public static final EconomyMessageSpec COMMISSION_DATA_REQUEST = c2s(53, "economy_system:commission/packet_data_request");
    public static final EconomyMessageSpec COMMISSION_DATA_RESPONSE = s2c(54, "economy_system:commission/packet_data_response");
    public static final EconomyMessageSpec COMMISSION_SUBMIT = c2s(55, "economy_system:commission/packet_submit");
    public static final EconomyMessageSpec COMMISSION_ACTION_RESPONSE = s2c(56, "economy_system:commission/packet_action_response");
    public static final EconomyMessageSpec RECYCLE_DATA_REQUEST = c2s(57, "economy_system:recycle/packet_data_request");
    public static final EconomyMessageSpec RECYCLE_DATA_RESPONSE = s2c(58, "economy_system:recycle/packet_data_response");
    public static final EconomyMessageSpec RECYCLE_SUBMIT = c2s(59, "economy_system:recycle/packet_submit");
    public static final EconomyMessageSpec RECYCLE_ACTION_RESPONSE = s2c(60, "economy_system:recycle/packet_action_response");

    public static final List<EconomyMessageSpec> ALL = List.copyOf(DECLARATIONS);
    private static final Map<Integer, EconomyMessageSpec> BY_DISCRIMINATOR;
    private static final Map<String, EconomyMessageSpec> BY_ID;

    static {
        Map<Integer, EconomyMessageSpec> byDiscriminator = new HashMap<>();
        Map<String, EconomyMessageSpec> byId = new HashMap<>();
        for (int index = 0; index < ALL.size(); index++) {
            EconomyMessageSpec spec = ALL.get(index);
            if (spec.discriminator() != index) {
                throw new IllegalStateException(
                        "Protocol discriminator " + spec.discriminator()
                                + " is out of append-only order at index " + index
                );
            }
            if (byDiscriminator.put(spec.discriminator(), spec) != null) {
                throw new IllegalStateException("Duplicate discriminator " + spec.discriminator());
            }
            if (byId.put(spec.id(), spec) != null) {
                throw new IllegalStateException("Duplicate message id " + spec.id());
            }
        }
        BY_DISCRIMINATOR = Map.copyOf(byDiscriminator);
        BY_ID = Map.copyOf(byId);
    }

    private EconomyProtocol() {
    }

    public static EconomyMessageSpec byDiscriminator(int discriminator) {
        return BY_DISCRIMINATOR.get(discriminator);
    }

    public static EconomyMessageSpec byId(String id) {
        return BY_ID.get(id);
    }

    /** Verifies that a target has bound every canonical message exactly once. */
    public static void validateBindings(Collection<EconomyMessageType<?>> bindings) {
        if (bindings.size() != ALL.size()) {
            throw new IllegalStateException(
                    "Expected " + ALL.size() + " message bindings but found " + bindings.size()
            );
        }
        Set<EconomyMessageSpec> seen = new HashSet<>();
        for (EconomyMessageType<?> binding : bindings) {
            EconomyMessageSpec expected = BY_DISCRIMINATOR.get(binding.discriminator());
            if (expected == null || !expected.equals(binding.spec())) {
                throw new IllegalStateException(
                        "Target binding does not match canonical protocol: " + binding
                );
            }
            if (!seen.add(binding.spec())) {
                throw new IllegalStateException(
                        "Target binding is duplicated: " + binding.spec().id()
                );
            }
        }
        if (!seen.containsAll(ALL)) {
            throw new IllegalStateException("Target bindings do not cover the canonical protocol");
        }
    }

    private static EconomyMessageSpec c2s(int discriminator, String id) {
        return declare(discriminator, id, EconomyMessageDirection.CLIENT_TO_SERVER);
    }

    private static EconomyMessageSpec s2c(int discriminator, String id) {
        return declare(discriminator, id, EconomyMessageDirection.SERVER_TO_CLIENT);
    }

    private static EconomyMessageSpec declare(
            int discriminator,
            String id,
            EconomyMessageDirection direction
    ) {
        EconomyMessageSpec spec = new EconomyMessageSpec(discriminator, id, direction);
        DECLARATIONS.add(spec);
        return spec;
    }
}
