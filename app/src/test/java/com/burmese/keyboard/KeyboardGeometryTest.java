package com.burmese.keyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public final class KeyboardGeometryTest {
    private static final float WIDTH = 1080;
    private static final float HEIGHT = 810;
    private static final float GAP = 9;

    @Test public void outerPaddingAndInterKeyGapsDoNotPressKeys() {
        KeyboardLayout.Page page = KeyboardLayout.forLayer(KeyboardController.Layer.BURMESE);
        KeyboardLayout.Slot first = page.slots.get(0);
        KeyboardLayout.Slot second = page.slots.get(1);
        float firstRight = KeyboardGeometry.right(first, WIDTH, GAP);
        float secondLeft = KeyboardGeometry.left(second, WIDTH, GAP);
        float centerY = (KeyboardGeometry.top(first, HEIGHT, GAP)
                + KeyboardGeometry.bottom(first, HEIGHT, GAP)) / 2;

        assertEquals(-1, KeyboardGeometry.slotAt(page, WIDTH, HEIGHT, GAP, 1, centerY));
        assertEquals(0, KeyboardGeometry.slotAt(page, WIDTH, HEIGHT, GAP,
                KeyboardGeometry.left(first, WIDTH, GAP), centerY));
        assertEquals(-1, KeyboardGeometry.slotAt(page, WIDTH, HEIGHT, GAP,
                (firstRight + secondLeft) / 2, centerY));
        assertEquals(1, KeyboardGeometry.slotAt(page, WIDTH, HEIGHT, GAP,
                secondLeft, centerY));
        assertEquals(-1, KeyboardGeometry.slotAt(page, WIDTH, HEIGHT, GAP,
                WIDTH - 1, centerY));
    }

    @Test public void verticalGapsDoNotPressRows() {
        KeyboardLayout.Page page = KeyboardLayout.forLayer(KeyboardController.Layer.BURMESE);
        KeyboardLayout.Slot firstRow = page.slots.get(0);
        KeyboardLayout.Slot secondRow = page.slots.get(10);
        float centerX = (KeyboardGeometry.left(firstRow, WIDTH, GAP)
                + KeyboardGeometry.right(firstRow, WIDTH, GAP)) / 2;
        float gapY = (KeyboardGeometry.bottom(firstRow, HEIGHT, GAP)
                + KeyboardGeometry.top(secondRow, HEIGHT, GAP)) / 2;

        assertEquals(-1, KeyboardGeometry.slotAt(page, WIDTH, HEIGHT, GAP, centerX, 1));
        assertEquals(0, KeyboardGeometry.slotAt(page, WIDTH, HEIGHT, GAP,
                centerX, KeyboardGeometry.top(firstRow, HEIGHT, GAP)));
        assertEquals(-1, KeyboardGeometry.slotAt(page, WIDTH, HEIGHT, GAP, centerX, gapY));
        assertEquals(10, KeyboardGeometry.slotAt(page, WIDTH, HEIGHT, GAP,
                centerX, KeyboardGeometry.top(secondRow, HEIGHT, GAP)));
    }

    @Test public void weightedSpacebarOwnsItsRenderedArea() {
        KeyboardLayout.Page page = KeyboardLayout.forLayer(KeyboardController.Layer.BURMESE);
        int spaceIndex = find(page, KeyboardLayout.Action.SPACE);
        KeyboardLayout.Slot space = page.slots.get(spaceIndex);

        float centerX = (KeyboardGeometry.left(space, WIDTH, GAP)
                + KeyboardGeometry.right(space, WIDTH, GAP)) / 2;
        float centerY = (KeyboardGeometry.top(space, HEIGHT, GAP)
                + KeyboardGeometry.bottom(space, HEIGHT, GAP)) / 2;
        assertEquals(spaceIndex,
                KeyboardGeometry.slotAt(page, WIDTH, HEIGHT, GAP, centerX, centerY));
        assertTrue(KeyboardGeometry.right(space, WIDTH, GAP)
                - KeyboardGeometry.left(space, WIDTH, GAP) > WIDTH * 0.4f);
    }

    @Test public void emojiGridUsesEightStableColumns() {
        List<String> emojis = new ArrayList<>();
        for (int index = 0; index < 32; index++) emojis.add("emoji-" + index);
        KeyboardLayout.Page page = KeyboardLayout.forState(
                KeyboardController.Layer.EMOJI,
                KeyboardController.Alphabet.BURMESE, emojis);

        assertEquals("emoji-0", keyAt(page, WIDTH * 0.0625f, HEIGHT * 0.125f).output);
        assertEquals("emoji-7", keyAt(page, WIDTH * 0.9375f, HEIGHT * 0.125f).output);
        assertEquals("emoji-16", keyAt(page, WIDTH * 0.0625f, HEIGHT * 0.495f).output);
        assertEquals("emoji-31", keyAt(page, WIDTH * 0.9375f, HEIGHT * 0.68f).output);
    }

    @Test public void scrollingRevealsTheNextEmojiRowWithoutStretchingIt() {
        List<String> emojis = new ArrayList<>();
        for (int index = 0; index < 33; index++) emojis.add("emoji-" + index);
        KeyboardLayout.Page page = KeyboardLayout.forState(
                KeyboardController.Layer.EMOJI,
                KeyboardController.Alphabet.BURMESE, emojis);
        KeyboardLayout.Slot onlyEmoji = page.slots.get(32);
        float offset = page.verticalScroll.maxOffset * HEIGHT;

        assertEquals(0.125f, onlyEmoji.right - onlyEmoji.left, 0.0001f);
        assertEquals("emoji-32", onlyEmoji.key.output);
        assertEquals("emoji-32", keyAt(page, WIDTH * 0.0625f, HEIGHT * 0.68f, offset).output);
        assertEquals(-1, KeyboardGeometry.slotAt(page, WIDTH, HEIGHT, GAP,
                WIDTH * 0.5f, HEIGHT * 0.68f, offset));
    }

    @Test public void lastEmojiInFullCatalogIsReachableAtMaximumScroll() {
        List<String> emojis = new ArrayList<>();
        for (int index = 0; index < 1927; index++) emojis.add("emoji-" + index);
        KeyboardLayout.Page page = KeyboardLayout.forState(
                KeyboardController.Layer.EMOJI,
                KeyboardController.Alphabet.BURMESE, emojis);
        float offset = page.verticalScroll.maxOffset * HEIGHT;

        assertEquals("emoji-1926",
                keyAt(page, WIDTH * 0.8125f, HEIGHT * 0.68f, offset).output);
    }

    @Test public void emojiUtilityRowStaysFixedWhileContentScrolls() {
        List<String> emojis = new ArrayList<>();
        for (int index = 0; index < 100; index++) emojis.add("emoji-" + index);
        KeyboardLayout.Page page = KeyboardLayout.forState(
                KeyboardController.Layer.EMOJI,
                KeyboardController.Alphabet.BURMESE, emojis);
        float offset = page.verticalScroll.maxOffset * HEIGHT;

        assertEquals(KeyboardLayout.Action.CYCLE_LANGUAGE,
                keyAt(page, WIDTH * 0.07f, HEIGHT * 0.865f, offset).action);
        assertEquals(KeyboardLayout.Action.SPACE,
                keyAt(page, WIDTH * 0.5f, HEIGHT * 0.865f, offset).action);
        assertEquals(KeyboardLayout.Action.BACKSPACE,
                keyAt(page, WIDTH * 0.93f, HEIGHT * 0.865f, offset).action);
    }

    @Test public void invalidCoordinatesNeverResolveToAKey() {
        KeyboardLayout.Page page = KeyboardLayout.forLayer(KeyboardController.Layer.BURMESE);
        assertEquals(-1, KeyboardGeometry.slotAt(page, 0, HEIGHT, GAP, 0, 0));
        assertEquals(-1, KeyboardGeometry.slotAt(page, WIDTH, 0, GAP, 0, 0));
        assertEquals(-1, KeyboardGeometry.slotAt(page, WIDTH, HEIGHT, GAP, -1, 10));
        assertEquals(-1, KeyboardGeometry.slotAt(page, WIDTH, HEIGHT, GAP, WIDTH, 10));
        assertEquals(-1, KeyboardGeometry.slotAt(page, WIDTH, HEIGHT, GAP, 10, HEIGHT));
    }

    private static KeyboardLayout.Key keyAt(KeyboardLayout.Page page, float x, float y) {
        return keyAt(page, x, y, 0);
    }

    private static KeyboardLayout.Key keyAt(
            KeyboardLayout.Page page, float x, float y, float scrollOffset) {
        int slot = KeyboardGeometry.slotAt(
                page, WIDTH, HEIGHT, GAP, x, y, scrollOffset);
        if (slot < 0) throw new AssertionError("No key at " + x + ", " + y);
        return page.slots.get(slot).key;
    }

    private static int find(KeyboardLayout.Page page, KeyboardLayout.Action action) {
        for (int index = 0; index < page.slots.size(); index++) {
            if (page.slots.get(index).key.action == action) return index;
        }
        throw new AssertionError("Missing action " + action);
    }
}
