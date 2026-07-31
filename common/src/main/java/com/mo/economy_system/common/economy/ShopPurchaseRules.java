package com.mo.economy_system.common.economy;

/** Pure validation rules shared by both shop-purchase handlers. */
public final class ShopPurchaseRules {
    public static final int MAX_QUANTITY = 2_304;

    private ShopPurchaseRules() {
    }

    /** Returns the total price, or {@code -1} when price/quantity is invalid. */
    public static int checkedTotalPrice(int unitPrice, int quantity) {
        if (unitPrice <= 0 || quantity <= 0 || quantity > MAX_QUANTITY) {
            return -1;
        }
        long total = (long) unitPrice * quantity;
        return total > Integer.MAX_VALUE ? -1 : (int) total;
    }
}
