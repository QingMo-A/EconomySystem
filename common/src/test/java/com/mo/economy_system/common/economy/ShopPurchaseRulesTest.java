package com.mo.economy_system.common.economy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShopPurchaseRulesTest {
    @Test
    void computesAValidTotalWithoutIntOverflow() {
        assertEquals(150, ShopPurchaseRules.checkedTotalPrice(30, 5));
        assertEquals(Integer.MAX_VALUE, ShopPurchaseRules.checkedTotalPrice(Integer.MAX_VALUE, 1));
    }

    @Test
    void rejectsInvalidQuantityAndPrice() {
        assertEquals(-1, ShopPurchaseRules.checkedTotalPrice(0, 1));
        assertEquals(-1, ShopPurchaseRules.checkedTotalPrice(1, 0));
        assertEquals(
                -1,
                ShopPurchaseRules.checkedTotalPrice(1, ShopPurchaseRules.MAX_QUANTITY + 1)
        );
    }

    @Test
    void rejectsOverflowingTotal() {
        assertEquals(-1, ShopPurchaseRules.checkedTotalPrice(Integer.MAX_VALUE, 2));
    }
}
