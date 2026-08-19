package com.mo.economy_system.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.api.account.EconomyAccountApi;
import com.mo.economy_system.api.mailbox.EconomyMailboxApi;
import com.mo.economy_system.api.market.EconomyMarketApi;
import com.mo.economy_system.api.territory.EconomyTerritoryApi;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Prevents implementation-layer types from leaking into the stable common public API contract. */
class PublicApiSurfaceIsolationTest {
  private static final List<Class<?>> API_TYPES = List.of(
      EconomyApiCapabilities.class,
      EconomyApiSession.class,
      EconomyAccountApi.class,
      EconomyAccountApi.TransactionNote.class,
      EconomyAccountApi.LogEntry.class,
      EconomyAccountApi.LogPage.class,
      EconomyAccountApi.MutationStatus.class,
      EconomyAccountApi.TransferStatus.class,
      EconomyMailboxApi.class,
      EconomyMailboxApi.MailDraft.class,
      EconomyMailboxApi.MailItemGrant.class,
      EconomyMailboxApi.DeliveryStatus.class,
      EconomyMarketApi.class,
      EconomyMarketApi.OrderType.class,
      EconomyMarketApi.OrderView.class,
      EconomyTerritoryApi.class,
      EconomyTerritoryApi.Position.class,
      EconomyTerritoryApi.TerritoryView.class,
      EconomyTerritoryApi.Relationship.class);

  @Test
  void publicSignaturesOnlyExposeJavaOrApiTypes() {
    for (Class<?> apiType : API_TYPES) {
      for (Method method : apiType.getDeclaredMethods()) {
        assertAllowed(method.getGenericReturnType(), apiType.getName() + "#" + method.getName() + " return");
        for (Type parameter : method.getGenericParameterTypes()) {
          assertAllowed(parameter, apiType.getName() + "#" + method.getName() + " parameter");
        }
      }
      for (Constructor<?> constructor : apiType.getDeclaredConstructors()) {
        for (Type parameter : constructor.getGenericParameterTypes()) {
          assertAllowed(parameter, apiType.getName() + " constructor parameter");
        }
      }
      for (Field field : apiType.getDeclaredFields()) {
        if (!java.lang.reflect.Modifier.isPublic(field.getModifiers())) continue;
        assertAllowed(field.getGenericType(), apiType.getName() + "#" + field.getName() + " field");
      }
    }
  }

  private static void assertAllowed(Type type, String location) {
    if (type instanceof Class<?> clazz) {
      if (clazz.isArray()) {
        assertAllowed(clazz.getComponentType(), location);
        return;
      }
      String name = clazz.getName();
      assertTrue(clazz.isPrimitive()
              || name.startsWith("java.")
              || name.startsWith("com.mo.economy_system.api."),
          () -> location + " leaks implementation type " + name);
      return;
    }
    if (type instanceof ParameterizedType parameterized) {
      assertAllowed(parameterized.getRawType(), location);
      for (Type argument : parameterized.getActualTypeArguments()) assertAllowed(argument, location);
      return;
    }
    String name = type.getTypeName();
    assertTrue(name.startsWith("java.") || name.startsWith("com.mo.economy_system.api."),
        () -> location + " leaks implementation type " + name);
  }
}
