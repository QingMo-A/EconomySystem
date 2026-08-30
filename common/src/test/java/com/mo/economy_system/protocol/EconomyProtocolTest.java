package com.mo.economy_system.protocol;

import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EconomyProtocolTest {
    private static final List<String> EXPECTED_MANIFEST = List.of(
            "CLIENT_TO_SERVER|economy_system:economy_system/packet_balance_request",
            "SERVER_TO_CLIENT|economy_system:economy_system/packet_balance_response",
            "CLIENT_TO_SERVER|economy_system:economy_system/packet_balance_log_request",
            "SERVER_TO_CLIENT|economy_system:economy_system/packet_balance_log_response",
            "CLIENT_TO_SERVER|economy_system:economy_system/packet_transfer",
            "CLIENT_TO_SERVER|economy_system:economy_system/packet_shop_data_request",
            "SERVER_TO_CLIENT|economy_system:economy_system/packet_shop_data_response",
            "CLIENT_TO_SERVER|economy_system:economy_system/packet_shop_buy_item",
            "CLIENT_TO_SERVER|economy_system:economy_system/sales_order/packet_create_sales_order",
            "CLIENT_TO_SERVER|economy_system:economy_system/demand_order/packet_create_demand_order",
            "CLIENT_TO_SERVER|economy_system:economy_system/packet_market_data_request",
            "SERVER_TO_CLIENT|economy_system:economy_system/packet_market_data_response",
            "CLIENT_TO_SERVER|economy_system:economy_system/sales_order/packet_purchase_sales_order",
            "CLIENT_TO_SERVER|economy_system:economy_system/demand_order/packet_confirm_demand_order",
            "CLIENT_TO_SERVER|economy_system:economy_system/demand_order/packet_deliver_demand_order",
            "CLIENT_TO_SERVER|economy_system:economy_system/sales_order/packet_remove_sales_order",
            "CLIENT_TO_SERVER|economy_system:economy_system/demand_order/packet_remove_demand_order",
            "CLIENT_TO_SERVER|economy_system:territory_system/packet_territory_data_request",
            "SERVER_TO_CLIENT|economy_system:territory_system/packet_territory_data_response",
            "CLIENT_TO_SERVER|economy_system:territory_system/packet_teleport_to_territory",
            "CLIENT_TO_SERVER|economy_system:territory_system/packet_invite_player",
            "CLIENT_TO_SERVER|economy_system:territory_system/packet_remove_territory",
            "CLIENT_TO_SERVER|economy_system:territory_system/packet_remove_player",
            "SERVER_TO_CLIENT|economy_system:check_system/packet_check",
            "CLIENT_TO_SERVER|economy_system:check_system/packet_check_result_request",
            "SERVER_TO_CLIENT|economy_system:check_system/packet_check_result_response",
            "SERVER_TO_CLIENT|economy_system:check_system/packet_get",
            "CLIENT_TO_SERVER|economy_system:check_system/packet_get_result_request",
            "SERVER_TO_CLIENT|economy_system:check_system/packet_get_result_response",
            "CLIENT_TO_SERVER|economy_system:check_system/packet_chunk",
            "SERVER_TO_CLIENT|economy_system:check_system/packet_chunk_response",
            "CLIENT_TO_SERVER|economy_system:economy_system/packet_delivery_box_data_request",
            "SERVER_TO_CLIENT|economy_system:economy_system/packet_delivery_box_data_response",
            "CLIENT_TO_SERVER|economy_system:economy_system/packet_delivery_box_claim_item",
            "CLIENT_TO_SERVER|economy_system:packet_server_player_list_request",
            "SERVER_TO_CLIENT|economy_system:packet_server_player_list_response",
            "CLIENT_TO_SERVER|economy_system:territory_system/packet_modify_mode",
            "CLIENT_TO_SERVER|economy_system:territory_system/packet_unlock_territory_buff",
            "CLIENT_TO_SERVER|economy_system:territory_system/packet_upgrade_territory_buff",
            "CLIENT_TO_SERVER|economy_system:territory_system/packet_single_territory_data_request",
            "SERVER_TO_CLIENT|economy_system:territory_system/packet_single_territory_data_response",
            "CLIENT_TO_SERVER|economy_system:territory_system/packet_update_territory_permission",
            "CLIENT_TO_SERVER|economy_system:territory_system/packet_transfer_territory_ownership",
            "CLIENT_TO_SERVER|economy_system:territory_system/packet_update_territory_rule",
            "CLIENT_TO_SERVER|economy_system:mailbox/packet_data_request",
            "SERVER_TO_CLIENT|economy_system:mailbox/packet_data_response",
            "CLIENT_TO_SERVER|economy_system:mailbox/packet_mark_read",
            "CLIENT_TO_SERVER|economy_system:mailbox/packet_delete",
            "CLIENT_TO_SERVER|economy_system:mailbox/packet_claim_attachment",
            "CLIENT_TO_SERVER|economy_system:mailbox/packet_claim_all",
            "CLIENT_TO_SERVER|economy_system:mailbox/packet_send_player",
            "SERVER_TO_CLIENT|economy_system:mailbox/packet_send_result",
            "SERVER_TO_CLIENT|economy_system:mailbox/packet_notification"
    );

    @Test
    void manifestIsAppendOnlyAndComplete() {
        assertEquals("bridge-3", EconomyProtocol.VERSION);
        assertEquals(53, EconomyProtocol.ALL.size());
        assertEquals(37, EconomyProtocol.ALL.stream()
                .filter(spec -> spec.direction() == EconomyMessageDirection.CLIENT_TO_SERVER)
                .count());
        assertEquals(16, EconomyProtocol.ALL.stream()
                .filter(spec -> spec.direction() == EconomyMessageDirection.SERVER_TO_CLIENT)
                .count());

        for (int index = 0; index < EconomyProtocol.ALL.size(); index++) {
            EconomyMessageSpec spec = EconomyProtocol.ALL.get(index);
            assertEquals(index, spec.discriminator());
            assertSame(spec, EconomyProtocol.byDiscriminator(index));
            assertSame(spec, EconomyProtocol.byId(spec.id()));
        }

        assertEquals(EXPECTED_MANIFEST, EconomyProtocol.ALL.stream()
                .map(spec -> spec.direction() + "|" + spec.id())
                .toList());
    }

    @Test
    void forgeDiscriminatorMustFitOneByte() {
        assertThrows(IllegalArgumentException.class, () -> new EconomyMessageSpec(
                -1,
                "economy_system:test",
                EconomyMessageDirection.CLIENT_TO_SERVER
        ));
        assertThrows(IllegalArgumentException.class, () -> new EconomyMessageSpec(
                256,
                "economy_system:test",
                EconomyMessageDirection.CLIENT_TO_SERVER
        ));
    }

    @Test
    void registryRejectsDuplicatesAndMutationAfterFreeze() {
        EconomyMessageRegistry registry = new EconomyMessageRegistry();
        EconomyMessageType<DummyMessage> type = registry.register(
                0,
                "economy_system:test/dummy",
                EconomyMessageDirection.CLIENT_TO_SERVER,
                DummyMessage.class
        );
        assertSame(type, registry.typeOf(0));
        assertSame(type, registry.typeOf(type.id()));
        assertSame(type, registry.typeOf(DummyMessage.class));

        assertThrows(IllegalArgumentException.class, () -> registry.register(
                0,
                "economy_system:test/other",
                EconomyMessageDirection.SERVER_TO_CLIENT,
                OtherDummyMessage.class
        ));

        registry.freeze();
        assertTrue(registry.isFrozen());
        assertThrows(IllegalStateException.class, () -> registry.register(
                1,
                "economy_system:test/after_freeze",
                EconomyMessageDirection.CLIENT_TO_SERVER,
                OtherDummyMessage.class
        ));
    }

    @Test
    void bindingValidationRejectsDuplicateCanonicalEntries() {
        EconomyMessageType<DummyMessage> duplicate = new EconomyMessageType<>(
                EconomyProtocol.BALANCE_REQUEST,
                DummyMessage.class
        );
        assertThrows(IllegalStateException.class, () -> EconomyProtocol.validateBindings(
                Collections.nCopies(EconomyProtocol.ALL.size(), duplicate)
        ));
    }

    private record DummyMessage() implements EconomyNetworkMessage {
    }

    private record OtherDummyMessage() implements EconomyNetworkMessage {
    }
}
