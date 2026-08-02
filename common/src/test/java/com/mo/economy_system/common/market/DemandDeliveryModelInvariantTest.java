package com.mo.economy_system.common.market;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DemandDeliveryModelInvariantTest {
  @Test void outcomeHasNoPublicConstructorOrLegacyBooleanApi() {
    for (var constructor : DemandOrderDeliveryOutcome.class.getDeclaredConstructors())
      assertFalse(Modifier.isPublic(constructor.getModifiers()));
    assertThrows(NoSuchMethodException.class,
        () -> DemandOrderDeliveryOutcome.class.getDeclaredMethod("marketChanged"));
    assertThrows(NoSuchMethodException.class,
        () -> DemandOrderDeliveryOutcome.class.getDeclaredMethod("failure"));
  }

  @Test void outcomeFactoriesEnforceOrderState() {
    MarketOrder pending = order(false), delivered = delivered(pending);
    assertEquals(MarketMutationState.CHANGED, DemandOrderDeliveryOutcome.success(delivered).mutationState());
    assertThrows(IllegalArgumentException.class, () -> DemandOrderDeliveryOutcome.success(pending));
    assertThrows(IllegalArgumentException.class, () -> DemandOrderDeliveryOutcome.rolledBackFailure(DemandOrderDeliveryResult.PAYMENT_FAILED, delivered));
    assertThrows(IllegalArgumentException.class, () -> DemandOrderDeliveryOutcome.changedFailure(DemandOrderDeliveryResult.ORDER_CHANGED, delivered));
    assertThrows(IllegalArgumentException.class, () -> DemandOrderDeliveryOutcome.validationFailure(DemandOrderDeliveryResult.SUCCESS));
  }

  @Test void transitionUpdatedCarriesOnlyDeliveredChange() {
    MarketOrder pending = order(false), delivered = delivered(pending);
    DemandDeliveryTransition transition = DemandDeliveryTransition.updated(pending, delivered);
    assertEquals(DemandDeliveryTransitionStatus.UPDATED, transition.status());
    assertEquals(pending, transition.previousOrder().orElseThrow());
    assertEquals(delivered, transition.updatedOrder().orElseThrow());
  }

  @Test void transitionRejectsNullWrongStateAndChangedFields() {
    MarketOrder pending = order(false), delivered = delivered(pending);
    assertThrows(NullPointerException.class, () -> DemandDeliveryTransition.updated(null, delivered));
    assertThrows(NullPointerException.class, () -> DemandDeliveryTransition.updated(pending, null));
    assertThrows(IllegalArgumentException.class, () -> DemandDeliveryTransition.updated(delivered, delivered));
    MarketOrder changed = new MarketOrder(delivered.type(), delivered.tradeId(), delivered.item(),
        delivered.quantity() + 1, delivered.totalPrice(), delivered.sellerName(), delivered.sellerId(),
        delivered.listingTime(), delivered.expirationTime(), true);
    assertThrows(IllegalArgumentException.class, () -> DemandDeliveryTransition.updated(pending, changed));
  }

  @Test void transitionFailureCarriesNoOrdersAndRejectsUpdated() {
    for (DemandDeliveryTransitionStatus status : DemandDeliveryTransitionStatus.values()) {
      if (status == DemandDeliveryTransitionStatus.UPDATED) continue;
      DemandDeliveryTransition transition = DemandDeliveryTransition.failure(status);
      assertTrue(transition.previousOrder().isEmpty()); assertTrue(transition.updatedOrder().isEmpty());
    }
    assertThrows(NullPointerException.class, () -> DemandDeliveryTransition.failure(null));
    assertThrows(IllegalArgumentException.class,
        () -> DemandDeliveryTransition.failure(DemandDeliveryTransitionStatus.UPDATED));
  }

  @Test void compensationIsCompleteOnlyWhenBothAttemptsSucceeded() {
    assertTrue(new DemandDeliveryCompensation(true, true, true, true, null, null).complete());
    assertFalse(new DemandDeliveryCompensation(false, true, true, true, null, null).complete());
    assertFalse(new DemandDeliveryCompensation(true, true, false, true, null, null).complete());
    assertFalse(new DemandDeliveryCompensation(true, false, true, true, null, null).complete());
    assertFalse(new DemandDeliveryCompensation(true, true, true, false, null, null).complete());
  }

  private static MarketOrder order(boolean delivered) {
    return new MarketOrder(MarketOrderType.DEMAND, UUID.randomUUID(), MarketOrderCodecTest.item(),
        3, 17, "requester", UUID.randomUUID(), 10, 99, delivered);
  }

  private static MarketOrder delivered(MarketOrder order) {
    return new MarketOrder(order.type(), order.tradeId(), order.item(), order.quantity(),
        order.totalPrice(), order.sellerName(), order.sellerId(), order.listingTime(),
        order.expirationTime(), true);
  }
}
