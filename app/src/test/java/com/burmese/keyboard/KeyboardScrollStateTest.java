package com.burmese.keyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class KeyboardScrollStateTest {
    @Test public void movementInsideTouchSlopRemainsATap() {
        KeyboardScrollState state = new KeyboardScrollState();
        state.reset(500);
        state.begin(300, true);

        assertFalse(state.move(294, 8));
        assertFalse(state.isDragging());
        assertEquals(0, state.offset(), 0.001f);
    }

    @Test public void dragConsumesGestureAndMovesContent() {
        KeyboardScrollState state = new KeyboardScrollState();
        state.reset(500);
        state.begin(300, true);

        assertTrue(state.move(280, 8));
        assertTrue(state.isDragging());
        assertEquals(20, state.offset(), 0.001f);
        assertTrue(state.move(250, 8));
        assertEquals(50, state.offset(), 0.001f);
    }

    @Test public void offsetIsClampedAtBothEnds() {
        KeyboardScrollState state = new KeyboardScrollState();
        state.reset(100);
        state.begin(200, true);
        state.move(0, 8);
        assertEquals(100, state.offset(), 0.001f);

        state.endGesture();
        state.begin(100, true);
        state.move(250, 8);
        assertEquals(0, state.offset(), 0.001f);
    }

    @Test public void fixedUtilityRowCannotStartScrolling() {
        KeyboardScrollState state = new KeyboardScrollState();
        state.reset(500);
        state.begin(300, false);

        assertFalse(state.move(100, 8));
        assertFalse(state.isDragging());
        assertEquals(0, state.offset(), 0.001f);
    }
}
