package com.mo.economy_system.platform;

/**
 * Explicit API capability facts exposed by a target adapter.
 *
 * <p>These flags describe real platform/API support only. Common controllers
 * own the fallback behavior; a target must not use a flag to redesign a page.</p>
 */
public record PlatformCapabilities(
    boolean supportsPlayerHeadRendering,
    boolean supportsItemRendering,
    boolean supportsClipboard,
    boolean supportsTerritoryResize) {
  public static PlatformCapabilities full() {
    return new PlatformCapabilities(true, true, true, true);
  }
}
