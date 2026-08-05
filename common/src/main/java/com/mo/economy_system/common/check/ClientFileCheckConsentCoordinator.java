package com.mo.economy_system.common.check;

import java.util.Objects;

public final class ClientFileCheckConsentCoordinator {
  public enum Decision {
    OPEN,
    DUPLICATE,
    BUSY
  }

  private ClientFileCheckTaskCoordinator.RequestIdentity active;

  public synchronized Decision receive(ClientFileCheckTaskCoordinator.RequestIdentity request) {
    Objects.requireNonNull(request, "request");
    if (active == null) {
      active = request;
      return Decision.OPEN;
    }
    return active.equals(request) ? Decision.DUPLICATE : Decision.BUSY;
  }

  public synchronized boolean finish(ClientFileCheckTaskCoordinator.RequestIdentity request) {
    if (!Objects.equals(active, request)) return false;
    active = null;
    return true;
  }

  public synchronized void invalidate() {
    active = null;
  }

  public synchronized ClientFileCheckTaskCoordinator.RequestIdentity active() {
    return active;
  }
}
