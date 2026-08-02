package com.mo.economy_system.common.market;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MarketActionOutcomeTest {
  @Test
  void purchaseFactoriesHaveExactStates() {
    MarketOrder order = order();
    assertPurchase(
        PurchaseSalesOrderOutcome.success(order),
        PurchaseSalesOrderResult.SUCCESS,
        order,
        MarketMutationState.CHANGED);
    assertPurchase(
        PurchaseSalesOrderOutcome.validationFailure(PurchaseSalesOrderResult.NOT_FOUND),
        PurchaseSalesOrderResult.NOT_FOUND,
        null,
        MarketMutationState.UNCHANGED);
    assertPurchase(
        PurchaseSalesOrderOutcome.rolledBackFailure(PurchaseSalesOrderResult.ORDER_CHANGED, order),
        PurchaseSalesOrderResult.ORDER_CHANGED,
        order,
        MarketMutationState.UNCHANGED);
    assertPurchase(
        PurchaseSalesOrderOutcome.changedFailure(PurchaseSalesOrderResult.ROLLBACK_FAILED, order),
        PurchaseSalesOrderResult.ROLLBACK_FAILED,
        order,
        MarketMutationState.CHANGED);
    assertPurchase(
        PurchaseSalesOrderOutcome.uncertainFailure(PurchaseSalesOrderResult.ORDER_REMOVE_FAILED),
        PurchaseSalesOrderResult.ORDER_REMOVE_FAILED,
        null,
        MarketMutationState.UNKNOWN);
  }

  @Test
  void confirmFactoriesHaveExactStates() {
    MarketOrder order = order();
    assertConfirm(
        ConfirmDemandOrderOutcome.success(order),
        ConfirmDemandOrderResult.SUCCESS,
        order,
        MarketMutationState.CHANGED);
    assertConfirm(
        ConfirmDemandOrderOutcome.validationFailure(ConfirmDemandOrderResult.NOT_FOUND),
        ConfirmDemandOrderResult.NOT_FOUND,
        null,
        MarketMutationState.UNCHANGED);
    assertConfirm(
        ConfirmDemandOrderOutcome.rolledBackFailure(ConfirmDemandOrderResult.ORDER_CHANGED, order),
        ConfirmDemandOrderResult.ORDER_CHANGED,
        order,
        MarketMutationState.UNCHANGED);
    assertConfirm(
        ConfirmDemandOrderOutcome.changedFailure(ConfirmDemandOrderResult.ROLLBACK_FAILED, order),
        ConfirmDemandOrderResult.ROLLBACK_FAILED,
        order,
        MarketMutationState.CHANGED);
    assertConfirm(
        ConfirmDemandOrderOutcome.uncertainFailure(ConfirmDemandOrderResult.ORDER_REMOVE_FAILED),
        ConfirmDemandOrderResult.ORDER_REMOVE_FAILED,
        null,
        MarketMutationState.UNKNOWN);
  }

  @Test
  void removeFactoriesHaveExactStates() {
    MarketOrder order = order();
    assertRemove(
        RemoveSalesOrderOutcome.success(order),
        RemoveSalesOrderResult.SUCCESS,
        order,
        MarketMutationState.CHANGED);
    assertRemove(
        RemoveSalesOrderOutcome.validationFailure(RemoveSalesOrderResult.NOT_FOUND),
        RemoveSalesOrderResult.NOT_FOUND,
        null,
        MarketMutationState.UNCHANGED);
    assertRemove(
        RemoveSalesOrderOutcome.rolledBackFailure(RemoveSalesOrderResult.ORDER_CHANGED, order),
        RemoveSalesOrderResult.ORDER_CHANGED,
        order,
        MarketMutationState.UNCHANGED);
    assertRemove(
        RemoveSalesOrderOutcome.changedFailure(RemoveSalesOrderResult.ROLLBACK_FAILED, order),
        RemoveSalesOrderResult.ROLLBACK_FAILED,
        order,
        MarketMutationState.CHANGED);
    assertRemove(
        RemoveSalesOrderOutcome.uncertainFailure(RemoveSalesOrderResult.ORDER_REMOVE_FAILED),
        RemoveSalesOrderResult.ORDER_REMOVE_FAILED,
        null,
        MarketMutationState.UNKNOWN);
  }

  @Test
  void factoriesRejectNullAndSuccessMisuse() {
    MarketOrder order = order();
    assertThrows(NullPointerException.class, () -> PurchaseSalesOrderOutcome.success(null));
    assertThrows(
        NullPointerException.class, () -> ConfirmDemandOrderOutcome.changedFailure(null, order));
    assertThrows(
        NullPointerException.class, () -> RemoveSalesOrderOutcome.rolledBackFailure(null, order));
    assertThrows(
        NullPointerException.class,
        () ->
            PurchaseSalesOrderOutcome.rolledBackFailure(PurchaseSalesOrderResult.NOT_FOUND, null));
    assertThrows(
        NullPointerException.class,
        () -> ConfirmDemandOrderOutcome.changedFailure(ConfirmDemandOrderResult.NOT_FOUND, null));
    assertThrows(
        NullPointerException.class,
        () -> RemoveSalesOrderOutcome.changedFailure(RemoveSalesOrderResult.NOT_FOUND, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> PurchaseSalesOrderOutcome.validationFailure(PurchaseSalesOrderResult.SUCCESS));
    assertThrows(
        IllegalArgumentException.class,
        () -> ConfirmDemandOrderOutcome.uncertainFailure(ConfirmDemandOrderResult.SUCCESS));
    assertThrows(
        IllegalArgumentException.class,
        () -> RemoveSalesOrderOutcome.rolledBackFailure(RemoveSalesOrderResult.SUCCESS, order));
  }

  @Test
  void noCompatibilityConstructionOrMethodsRemain() {
    for (Class<?> type : outcomeTypes()) {
      assertTrue(
          Arrays.stream(type.getDeclaredConstructors())
              .allMatch(c -> Modifier.isPrivate(c.getModifiers())));
      assertFalse(hasMethod(type, "marketChanged"));
      assertFalse(hasMethod(type, "failure"));
      assertFalse(hasMethod(type, "afterRemoval"));
      assertFalse(
          Arrays.stream(type.getConstructors())
              .map(Constructor::getParameterTypes)
              .anyMatch(parameters -> Arrays.stream(parameters).anyMatch(p -> p == boolean.class)));
    }
  }

  private static Class<?>[] outcomeTypes() {
    return new Class<?>[] {
      PurchaseSalesOrderOutcome.class,
      ConfirmDemandOrderOutcome.class,
      RemoveSalesOrderOutcome.class
    };
  }

  private static boolean hasMethod(Class<?> type, String name) {
    return Arrays.stream(type.getMethods()).map(Method::getName).anyMatch(name::equals);
  }

  private static void assertPurchase(
      PurchaseSalesOrderOutcome actual,
      PurchaseSalesOrderResult result,
      MarketOrder order,
      MarketMutationState state) {
    assertEquals(result, actual.result());
    assertEquals(order, actual.purchasedOrder().orElse(null));
    assertEquals(state, actual.mutationState());
  }

  private static void assertConfirm(
      ConfirmDemandOrderOutcome actual,
      ConfirmDemandOrderResult result,
      MarketOrder order,
      MarketMutationState state) {
    assertEquals(result, actual.result());
    assertEquals(order, actual.confirmedOrder().orElse(null));
    assertEquals(state, actual.mutationState());
  }

  private static void assertRemove(
      RemoveSalesOrderOutcome actual,
      RemoveSalesOrderResult result,
      MarketOrder order,
      MarketMutationState state) {
    assertEquals(result, actual.result());
    assertEquals(order, actual.removedOrder().orElse(null));
    assertEquals(state, actual.mutationState());
  }

  private static MarketOrder order() {
    return new MarketOrder(
        MarketOrderType.SALES,
        UUID.randomUUID(),
        MarketOrderCodecTest.item(),
        2,
        10,
        "seller",
        UUID.randomUUID(),
        1,
        2,
        false);
  }
}
