package com.burmese.keyboard;

/** Shared normalized geometry used by rendering and touch hit-testing. */
final class KeyboardGeometry {
    static int slotAt(KeyboardLayout.Page page, float width, float height,
            float gap, float x, float y) {
        return slotAt(page, width, height, gap, x, y, 0);
    }

    static int slotAt(KeyboardLayout.Page page, float width, float height,
            float gap, float x, float y, float verticalScrollOffset) {
        if (page == null || width <= 0 || height <= 0 || x < 0 || x >= width
                || y < 0 || y >= height) {
            return -1;
        }
        int scrollingSlots = page.verticalScroll == null ? 0 : page.verticalScroll.slotCount;
        for (int index = page.slots.size() - 1; index >= scrollingSlots; index--) {
            KeyboardLayout.Slot slot = page.slots.get(index);
            if (x >= left(slot, width, gap) && x <= right(slot, width, gap)
                    && y >= top(slot, height, gap) && y <= bottom(slot, height, gap)) {
                return index;
            }
        }
        if (page.verticalScroll == null
                || y < page.verticalScroll.viewportTop * height
                || y > page.verticalScroll.viewportBottom * height) {
            return -1;
        }
        float contentY = y + verticalScrollOffset;
        for (int index = scrollingSlots - 1; index >= 0; index--) {
            KeyboardLayout.Slot slot = page.slots.get(index);
            if (x >= left(slot, width, gap) && x <= right(slot, width, gap)
                    && contentY >= top(slot, height, gap)
                    && contentY <= bottom(slot, height, gap)) {
                return index;
            }
        }
        return -1;
    }

    static float left(KeyboardLayout.Slot slot, float width, float gap) {
        return start(slot.left, width, gap, slot.horizontalInset);
    }

    static float right(KeyboardLayout.Slot slot, float width, float gap) {
        return end(slot.right, width, gap, slot.horizontalInset);
    }

    static float top(KeyboardLayout.Slot slot, float height, float gap) {
        return start(slot.top, height, gap, slot.verticalInset);
    }

    static float bottom(KeyboardLayout.Slot slot, float height, float gap) {
        return end(slot.bottom, height, gap, slot.verticalInset);
    }

    static float panelLeft(KeyboardLayout.Panel panel, float width, float gap) {
        return start(panel.left, width, gap, 1.0f);
    }

    static float panelRight(KeyboardLayout.Panel panel, float width, float gap) {
        return end(panel.right, width, gap, 1.0f);
    }

    static float panelTop(KeyboardLayout.Panel panel, float height, float gap) {
        return start(panel.top, height, gap, 1.0f);
    }

    static float panelBottom(KeyboardLayout.Panel panel, float height, float gap) {
        return end(panel.bottom, height, gap, 1.0f);
    }

    private static float start(float fraction, float size, float gap, float inset) {
        if (inset == 0) return fraction * size;
        float padding = fraction == 0 ? gap : gap / 2.0f;
        return fraction * size + padding * inset;
    }

    private static float end(float fraction, float size, float gap, float inset) {
        if (inset == 0) return fraction * size;
        float padding = fraction == 1 ? gap : gap / 2.0f;
        return fraction * size - padding * inset;
    }

    private KeyboardGeometry() {}
}
