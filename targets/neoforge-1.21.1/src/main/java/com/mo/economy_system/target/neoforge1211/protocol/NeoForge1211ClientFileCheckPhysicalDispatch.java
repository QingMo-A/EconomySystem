package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.network.ClientFileCheckRequestMessage;
import com.mo.economy_system.common.network.ClientFileCheckResultResponseMessage;
import java.lang.reflect.InvocationTargetException;

/** Avoids resolving client-only screen classes while a dedicated server registers payloads. */
final class NeoForge1211ClientFileCheckPhysicalDispatch {
  private static final String CLIENT =
      "com.mo.economy_system.target.neoforge1211.client.NeoForge1211ClientFileCheckScreens";

  private NeoForge1211ClientFileCheckPhysicalDispatch() {}

  static void request(ClientFileCheckRequestMessage message) {
    invoke("openConsent", ClientFileCheckRequestMessage.class, message);
  }

  static void response(ClientFileCheckResultResponseMessage message) {
    invoke("openResult", ClientFileCheckResultResponseMessage.class, message);
  }

  private static void invoke(String method, Class<?> parameter, Object message) {
    try {
      Class.forName(CLIENT).getMethod(method, parameter).invoke(null, message);
    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException failure) {
      throw new IllegalStateException("Client file-check UI is unavailable", failure);
    } catch (InvocationTargetException failure) {
      Throwable cause = failure.getCause();
      if (cause instanceof RuntimeException runtime) throw runtime;
      if (cause instanceof Error error) throw error;
      throw new IllegalStateException("Client file-check UI failed", cause);
    }
  }
}
