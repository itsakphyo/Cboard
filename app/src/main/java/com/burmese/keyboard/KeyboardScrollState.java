package com.burmese.keyboard;

/** Owns drag threshold and clamped pixel offset for a vertically scrollable keyboard region. */
final class KeyboardScrollState {
    private float offset;
    private float maxOffset;
    private float downY;
    private float lastY;
    private boolean candidate;
    private boolean dragging;

    void reset(float maxOffset) {
        this.maxOffset = Math.max(0, maxOffset);
        offset = 0;
        endGesture();
    }

    void begin(float y, boolean insideScrollableRegion) {
        downY = y;
        lastY = y;
        candidate = insideScrollableRegion && maxOffset > 0;
        dragging = false;
    }

    boolean move(float y, float touchSlop) {
        if (!candidate) return false;
        if (!dragging) {
            if (Math.abs(y - downY) <= touchSlop) return false;
            dragging = true;
            offset = clamp(offset + downY - y);
        } else {
            offset = clamp(offset + lastY - y);
        }
        lastY = y;
        return true;
    }

    void endGesture() {
        candidate = false;
        dragging = false;
    }

    boolean isDragging() { return dragging; }
    float offset() { return offset; }
    float maxOffset() { return maxOffset; }

    void setOffset(float offset) {
        this.offset = clamp(offset);
    }

    private float clamp(float value) {
        return Math.max(0, Math.min(maxOffset, value));
    }
}
