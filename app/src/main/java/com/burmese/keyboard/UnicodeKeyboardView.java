package com.burmese.keyboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.OverScroller;

/** Lightweight renderer with one shared geometry model for drawing and touch input. */
@SuppressLint("ViewConstructor")
final class UnicodeKeyboardView extends View {
    interface Listener {
        void onKey(KeyboardLayout.Key key);
        void onLongPress(KeyboardLayout.Key key);
    }

    private static final int NO_POINTER = -1;
    private static final int NO_SLOT = -1;
    private static final int BACKGROUND = Color.rgb(238, 242, 248);
    private static final int NORMAL_KEY = Color.WHITE;
    private static final int FUNCTION_KEY = Color.rgb(222, 226, 227);
    private static final int PRESSED_KEY = Color.rgb(190, 198, 202);
    private static final int ENTER_KEY = Color.rgb(11, 87, 208);
    private static final int ENTER_PRESSED = Color.rgb(8, 73, 170);
    private static final int LABEL = Color.rgb(25, 25, 25);

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint.FontMetrics fontMetrics = new Paint.FontMetrics();
    private final Rect textBounds = new Rect();
    private final RectF drawRect = new RectF();
    private final RectF iconRect = new RectF();
    private final Listener listener;
    private final KeyboardScrollState scrollState = new KeyboardScrollState();
    private final OverScroller scroller;
    private final int touchSlop;
    private final int minimumFlingVelocity;
    private final int maximumFlingVelocity;
    private KeyboardLayout.Page page;
    private VelocityTracker velocityTracker;
    private int activePointerId = NO_POINTER;
    private int pressedSlot = NO_SLOT;
    private boolean pressedInside;
    private boolean longPressTriggered;
    private final Runnable longPressRunnable = new Runnable() {
        @Override public void run() {
            if (activePointerId == NO_POINTER || pressedSlot == NO_SLOT || !pressedInside) return;
            KeyboardLayout.Key key = page.slots.get(pressedSlot).key;
            if (key.action != KeyboardLayout.Action.BACKSPACE
                    && key.longPressOutput.isEmpty()) return;
            longPressTriggered = true;
            if (key.action == KeyboardLayout.Action.BACKSPACE) {
                listener.onKey(key);
                postDelayed(repeatBackspaceRunnable, 55);
            } else {
                listener.onLongPress(key);
            }
            postInvalidateOnAnimation();
        }
    };
    private final Runnable repeatBackspaceRunnable = new Runnable() {
        @Override public void run() {
            if (activePointerId == NO_POINTER || pressedSlot == NO_SLOT || !pressedInside
                    || !longPressTriggered) return;
            KeyboardLayout.Key key = page.slots.get(pressedSlot).key;
            if (key.action != KeyboardLayout.Action.BACKSPACE) return;
            listener.onKey(key);
            postDelayed(this, 55);
        }
    };

    UnicodeKeyboardView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        ViewConfiguration configuration = ViewConfiguration.get(context);
        scroller = new OverScroller(context);
        touchSlop = configuration.getScaledTouchSlop();
        minimumFlingVelocity = configuration.getScaledMinimumFlingVelocity();
        maximumFlingVelocity = configuration.getScaledMaximumFlingVelocity();
        setBackgroundColor(BACKGROUND);
        setFocusable(true);
        setClickable(true);
        show(KeyboardLayout.forLayer(KeyboardController.Layer.BURMESE));
    }

    void show(KeyboardLayout.Page page) {
        this.page = page;
        scroller.abortAnimation();
        scrollState.reset(maxScrollOffsetPx(page, getHeight()));
        clearPress();
        postInvalidateOnAnimation();
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        scroller.abortAnimation();
        scrollState.reset(maxScrollOffsetPx(page, height));
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            width = getResources().getDisplayMetrics().widthPixels;
        }
        int desiredHeight = desiredHeightPx();
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int height;
        if (heightMode == MeasureSpec.EXACTLY) {
            height = MeasureSpec.getSize(heightMeasureSpec);
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.min(desiredHeight, MeasureSpec.getSize(heightMeasureSpec));
        } else {
            height = desiredHeight;
        }
        setMeasuredDimension(width, height);
    }

    int desiredHeightPx() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return Math.round(Math.min(dp(200),
                    getResources().getDisplayMetrics().heightPixels * 0.48f));
        }
        return Math.round(Math.min(dp(300),
                getResources().getDisplayMetrics().widthPixels * 0.75f));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float gap = dp(3);
        for (KeyboardLayout.Panel panel : page.panels) {
            drawRect.set(
                    KeyboardGeometry.panelLeft(panel, getWidth(), gap),
                    KeyboardGeometry.panelTop(panel, getHeight(), gap),
                    KeyboardGeometry.panelRight(panel, getWidth(), gap),
                    KeyboardGeometry.panelBottom(panel, getHeight(), gap));
            drawBackground(canvas, drawRect, panel.style, false, false, null);
        }

        if (page.verticalScroll == null) {
            drawSlots(canvas, gap, 0, page.slots.size());
            return;
        }

        int saved = canvas.save();
        canvas.clipRect(0, page.verticalScroll.viewportTop * getHeight(),
                getWidth(), page.verticalScroll.viewportBottom * getHeight());
        canvas.translate(0, -scrollState.offset());
        drawSlots(canvas, gap, 0, page.verticalScroll.slotCount);
        canvas.restoreToCount(saved);
        drawSlots(canvas, gap, page.verticalScroll.slotCount, page.slots.size());
    }

    private void drawSlots(Canvas canvas, float gap, int start, int end) {
        for (int index = start; index < end; index++) {
            KeyboardLayout.Slot slot = page.slots.get(index);
            drawRect.set(
                    KeyboardGeometry.left(slot, getWidth(), gap),
                    KeyboardGeometry.top(slot, getHeight(), gap),
                    KeyboardGeometry.right(slot, getWidth(), gap),
                    KeyboardGeometry.bottom(slot, getHeight(), gap));
            if (page.verticalScroll != null && index < page.verticalScroll.slotCount
                    && (drawRect.bottom - scrollState.offset()
                            < page.verticalScroll.viewportTop * getHeight()
                    || drawRect.top - scrollState.offset()
                            > page.verticalScroll.viewportBottom * getHeight())) {
                continue;
            }
            boolean pressed = index == pressedSlot && pressedInside;
            if (slot.drawBackground) {
                drawBackground(canvas, drawRect, slot.key.style, slot.key.pill, pressed,
                        slot.key.action);
            } else if (pressed) {
                paint.setColor(PRESSED_KEY);
                canvas.drawRect(drawRect, paint);
            }
            boolean shiftIconFilled = pressed
                    || (slot.key.action == KeyboardLayout.Action.TOGGLE_SHIFT && page.shiftActive);
            drawLabel(canvas, drawRect, slot.key, shiftIconFilled);
        }
    }

    private void drawBackground(Canvas canvas, RectF rect, KeyboardLayout.Style style,
            boolean pill, boolean pressed, KeyboardLayout.Action action) {
        if (pressed) {
            paint.setColor(style == KeyboardLayout.Style.ENTER ? ENTER_PRESSED : PRESSED_KEY);
        } else if (action == KeyboardLayout.Action.TOGGLE_SHIFT) {
            paint.setColor(NORMAL_KEY);
        } else if (style == KeyboardLayout.Style.ENTER) {
            paint.setColor(ENTER_KEY);
        } else if (style == KeyboardLayout.Style.FUNCTION) {
            paint.setColor(FUNCTION_KEY);
        } else {
            paint.setColor(NORMAL_KEY);
        }
        float radius = pill ? rect.height() / 2.0f : dp(5);
        canvas.drawRoundRect(rect, radius, radius, paint);
    }

    private void drawLabel(Canvas canvas, RectF rect, KeyboardLayout.Key key, boolean pressed) {
        paint.setColor(key.style == KeyboardLayout.Style.ENTER ? Color.WHITE : LABEL);
        if (key.action == KeyboardLayout.Action.TOGGLE_SHIFT
                || key.action == KeyboardLayout.Action.ENTER
                || key.action == KeyboardLayout.Action.CYCLE_LANGUAGE) {
            drawNavigationIcon(canvas, rect, key.action, pressed);
            return;
        }
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(false);

        float textSize = fittedTextSize(key, rect);
        paint.setTextSize(textSize);
        paint.getFontMetrics(fontMetrics);
        float lineHeight = fontMetrics.descent - fontMetrics.ascent;
        float firstBaseline = rect.centerY()
                - lineHeight * key.labelLines.length / 2.0f - fontMetrics.ascent;
        for (int line = 0; line < key.labelLines.length; line++) {
            canvas.drawText(key.labelLines[line], rect.centerX(),
                    firstBaseline + line * lineHeight, paint);
        }

        if (!key.hint.isEmpty()) {
            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setFakeBoldText(true);
            paint.setTextSize(dp(9));
            canvas.drawText(key.hint, rect.right - dp(4), rect.top + dp(11), paint);
            paint.setFakeBoldText(false);
        }
    }

    private void drawNavigationIcon(Canvas canvas, RectF rect, KeyboardLayout.Action action,
            boolean pressed) {
        float centerX = rect.centerX();
        float centerY = rect.centerY();
        if (action == KeyboardLayout.Action.TOGGLE_SHIFT) {
            Path arrow = new Path();
            float tipY = centerY - rect.height() * 0.18f;
            float shoulderY = centerY - rect.height() * 0.01f;
            float stemBottom = centerY + rect.height() * 0.18f;
            float shoulder = rect.width() * 0.19f;
            float stem = rect.width() * 0.08f;
            arrow.moveTo(centerX, tipY);
            arrow.lineTo(centerX - shoulder, shoulderY);
            arrow.lineTo(centerX - stem, shoulderY);
            arrow.lineTo(centerX - stem, stemBottom);
            arrow.lineTo(centerX + stem, stemBottom);
            arrow.lineTo(centerX + stem, shoulderY);
            arrow.lineTo(centerX + shoulder, shoulderY);
            arrow.close();
            paint.setStyle(pressed ? Paint.Style.FILL : Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1.7f));
            paint.setStrokeJoin(Paint.Join.MITER);
            canvas.drawPath(arrow, paint);
            paint.setStyle(Paint.Style.FILL);
            return;
        }

        if (action == KeyboardLayout.Action.CYCLE_LANGUAGE) {
            float radius = Math.min(rect.width(), rect.height()) * 0.24f;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1.6f));
            paint.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawCircle(centerX, centerY, radius, paint);
            iconRect.set(centerX - radius * 0.46f, centerY - radius,
                    centerX + radius * 0.46f, centerY + radius);
            canvas.drawOval(iconRect, paint);
            canvas.drawLine(centerX - radius, centerY,
                    centerX + radius, centerY, paint);
            paint.setStyle(Paint.Style.FILL);
            return;
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2.5f));
        paint.setStrokeCap(Paint.Cap.SQUARE);
        paint.setStrokeJoin(Paint.Join.MITER);
        Path enter = new Path();
        float right = rect.right - rect.width() * 0.32f;
        float left = rect.left + rect.width() * 0.32f;
        enter.moveTo(right, rect.top + rect.height() * 0.32f);
        enter.lineTo(right, centerY);
        enter.lineTo(left, centerY);
        enter.moveTo(left, centerY);
        enter.lineTo(left + rect.width() * 0.12f, centerY - rect.height() * 0.12f);
        enter.moveTo(left, centerY);
        enter.lineTo(left + rect.width() * 0.12f, centerY + rect.height() * 0.12f);
        canvas.drawPath(enter, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private float fittedTextSize(KeyboardLayout.Key key, RectF rect) {
        float heightFactor = key.labelLines.length == 1 ? 0.46f : 0.31f;
        float textSize = Math.min(dp(23) * key.textScale,
                rect.height() * heightFactor * key.textScale);
        float minimum = dp(10);
        float maximumWidth = Math.max(1, rect.width() - dp(10));
        float maximumHeight = Math.max(1, rect.height() - dp(12));

        while (textSize > minimum) {
            paint.setTextSize(textSize);
            paint.getFontMetrics(fontMetrics);
            float totalHeight = (fontMetrics.descent - fontMetrics.ascent)
                    * key.labelLines.length;
            boolean fits = totalHeight <= maximumHeight;
            for (int line = 0; fits && line < key.labelLines.length; line++) {
                String value = key.labelLines[line];
                paint.getTextBounds(value, 0, value.length(), textBounds);
                float width = Math.max(paint.measureText(value), textBounds.width());
                fits = width <= maximumWidth;
            }
            if (fits) break;
            textSize -= dp(0.5f);
        }
        return Math.max(minimum, textSize);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                scroller.abortAnimation();
                activePointerId = event.getPointerId(0);
                obtainVelocityTracker(event);
                scrollState.begin(event.getY(0), isInsideScrollViewport(event.getY(0)));
                setInitialPress(event.getX(0), event.getY(0));
                if (pressedInside) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                return true;
            case MotionEvent.ACTION_MOVE:
                addVelocityMovement(event);
                int pointerIndex = event.findPointerIndex(activePointerId);
                if (pointerIndex >= 0
                        && scrollState.move(event.getY(pointerIndex), touchSlop)) {
                    cancelPressedKeyForScroll();
                    postInvalidateOnAnimation();
                    return true;
                }
                updatePressPosition(event);
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerId(event.getActionIndex()) == activePointerId) {
                    clearPress();
                    postInvalidateOnAnimation();
                }
                return true;
            case MotionEvent.ACTION_UP:
                addVelocityMovement(event);
                if (scrollState.isDragging()) {
                    flingFromGesture();
                    clearPress();
                    postInvalidateOnAnimation();
                    return true;
                }
                dispatchPressIfValid(event);
                return true;
            case MotionEvent.ACTION_CANCEL:
                clearPress();
                postInvalidateOnAnimation();
                return true;
            default:
                return true;
        }
    }

    private void setInitialPress(float x, float y) {
        pressedSlot = KeyboardGeometry.slotAt(page, getWidth(), getHeight(), dp(3), x, y,
                scrollState.offset());
        pressedInside = pressedSlot != NO_SLOT;
        longPressTriggered = false;
        if (pressedInside) {
            postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout());
        }
        postInvalidateOnAnimation();
    }

    private void updatePressPosition(MotionEvent event) {
        int pointerIndex = event.findPointerIndex(activePointerId);
        if (pointerIndex < 0 || pressedSlot == NO_SLOT) {
            pressedInside = false;
            removeCallbacks(longPressRunnable);
            removeCallbacks(repeatBackspaceRunnable);
        } else {
            int slot = KeyboardGeometry.slotAt(page, getWidth(), getHeight(), dp(3),
                    event.getX(pointerIndex), event.getY(pointerIndex), scrollState.offset());
            pressedInside = slot == pressedSlot;
            if (!pressedInside) {
                removeCallbacks(longPressRunnable);
                removeCallbacks(repeatBackspaceRunnable);
            }
        }
        postInvalidateOnAnimation();
    }

    private void dispatchPressIfValid(MotionEvent event) {
        int pointerIndex = event.findPointerIndex(activePointerId);
        KeyboardLayout.Key key = null;
        if (!longPressTriggered && pointerIndex >= 0 && pressedInside) {
            int slot = KeyboardGeometry.slotAt(page, getWidth(), getHeight(), dp(3),
                    event.getX(pointerIndex), event.getY(pointerIndex), scrollState.offset());
            if (slot == pressedSlot) key = page.slots.get(slot).key;
        }
        clearPress();
        postInvalidateOnAnimation();
        if (key != null) {
            performClick();
            listener.onKey(key);
        }
    }

    private void clearPress() {
        removeCallbacks(longPressRunnable);
        removeCallbacks(repeatBackspaceRunnable);
        activePointerId = NO_POINTER;
        pressedSlot = NO_SLOT;
        pressedInside = false;
        longPressTriggered = false;
        scrollState.endGesture();
        recycleVelocityTracker();
    }

    private boolean isInsideScrollViewport(float y) {
        return page.verticalScroll != null
                && y >= page.verticalScroll.viewportTop * getHeight()
                && y <= page.verticalScroll.viewportBottom * getHeight();
    }

    private void cancelPressedKeyForScroll() {
        removeCallbacks(longPressRunnable);
        removeCallbacks(repeatBackspaceRunnable);
        pressedInside = false;
        longPressTriggered = false;
    }

    private void obtainVelocityTracker(MotionEvent event) {
        recycleVelocityTracker();
        velocityTracker = VelocityTracker.obtain();
        velocityTracker.addMovement(event);
    }

    private void addVelocityMovement(MotionEvent event) {
        if (velocityTracker != null) velocityTracker.addMovement(event);
    }

    private void flingFromGesture() {
        if (velocityTracker == null) return;
        velocityTracker.computeCurrentVelocity(1000, maximumFlingVelocity);
        float fingerVelocity = velocityTracker.getYVelocity(activePointerId);
        if (Math.abs(fingerVelocity) < minimumFlingVelocity) return;
        scroller.fling(0, Math.round(scrollState.offset()), 0, Math.round(-fingerVelocity),
                0, 0, 0, Math.round(scrollState.maxOffset()));
    }

    private void recycleVelocityTracker() {
        if (velocityTracker == null) return;
        velocityTracker.recycle();
        velocityTracker = null;
    }

    @Override public void computeScroll() {
        if (!scroller.computeScrollOffset()) return;
        scrollState.setOffset(scroller.getCurrY());
        postInvalidateOnAnimation();
    }

    private static float maxScrollOffsetPx(KeyboardLayout.Page page, int height) {
        return page == null || page.verticalScroll == null
                ? 0 : page.verticalScroll.maxOffset * height;
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
