package com.mo.economy_system.platform.network;

/**
 * Marker for a semantic EconomySystem message.
 *
 * <p>Loader payload wrappers and buffer codecs are target-owned. During the
 * incremental migration the NeoForge packets implement this marker alongside
 * {@code CustomPacketPayload}; the loader type will be removed once codecs have
 * moved completely into their targets.</p>
 */
public interface EconomyNetworkMessage {
}
