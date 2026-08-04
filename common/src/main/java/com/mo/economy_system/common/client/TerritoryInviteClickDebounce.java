package com.mo.economy_system.common.client;

/** Client-local tick debounce for territory invitation buttons. */
public final class TerritoryInviteClickDebounce {
    private final long cooldownTicks;
    private long lockedUntil;

    public TerritoryInviteClickDebounce(long cooldownTicks) {
        if (cooldownTicks <= 0) throw new IllegalArgumentException("cooldownTicks");
        this.cooldownTicks = cooldownTicks;
    }

    public boolean tryAcquire(long tick) {
        if (tick < 0) throw new IllegalArgumentException("tick");
        if (tick < lockedUntil) return false;
        lockedUntil = Math.addExact(tick, cooldownTicks);
        return true;
    }

    public boolean available(long tick) {
        if (tick < 0) throw new IllegalArgumentException("tick");
        return tick >= lockedUntil;
    }
}
