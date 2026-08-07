package com.mo.economy_system.ui.geometry;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UiGeometryTest {
    @Test
    void rectangleUsesHalfOpenHitBounds() {
        UiRect rect = new UiRect(10, 20, 30, 40);
        assertTrue(rect.contains(10, 20));
        assertTrue(rect.contains(39, 59));
        assertFalse(rect.contains(40, 59));
        assertFalse(rect.contains(39, 60));
    }

    @Test
    void scaleFitsVirtualCanvasAtAllViewports() {
        UiScale scale = UiScale.fit(1280, 720, 640, 360);
        assertEquals(2.0f, scale.value(), 0.001f);
        assertEquals(640, scale.virtualWidth());
        assertEquals(360, scale.virtualHeight());
        assertEquals(320, scale.toVirtualX(640));
    }

    @Test
    void insetNeverProducesNegativeDimensions() {
        assertEquals(new UiRect(20, 20, 0, 0), new UiRect(0, 0, 10, 10).inset(UiInsets.all(20)));
    }
}
