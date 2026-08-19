package com.mo.economy_system.api;

import com.mo.economy_system.api.account.EconomyAccountApi;
import com.mo.economy_system.api.mailbox.EconomyMailboxApi;
import com.mo.economy_system.api.market.EconomyMarketApi;
import com.mo.economy_system.api.territory.EconomyTerritoryApi;

/**
 * One server-side API session bound to a concrete Minecraft server level.
 *
 * <p>All mutating calls are server-authoritative and are expected to run on the server thread.
 * Third-party mods should obtain sessions through {@code EconomySystemApi.forLevel(...)} and must
 * not retain them beyond the lifetime of the underlying server.</p>
 */
public interface EconomyApiSession {
  EconomyApiCapabilities capabilities();
  EconomyAccountApi accounts();
  EconomyMailboxApi mailbox();
  EconomyMarketApi market();
  EconomyTerritoryApi territories();
}
