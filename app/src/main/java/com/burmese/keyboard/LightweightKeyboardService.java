package com.burmese.keyboard;

import android.inputmethodservice.InputMethodService;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

/** Android editor adapter. Keyboard navigation remains functional without an input connection. */
public final class LightweightKeyboardService extends InputMethodService
        implements UnicodeKeyboardView.Listener {
    private KeyboardController controller;
    private final BurmeseTextComposer textComposer = new BurmeseTextComposer();
    private UnicodeKeyboardView keyboard;

    @Override public void onCreate() {
        super.onCreate();
        controller = new KeyboardController();
    }

    @Override public View onCreateInputView() {
        keyboard = new UnicodeKeyboardView(this, this);
        keyboard.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, keyboard.desiredHeightPx()));
        renderLayer();
        return keyboard;
    }

    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        InputConnection connection = getCurrentInputConnection();
        if (connection != null) {
            finishTextComposition(connection);
        } else {
            textComposer.clear();
        }
        controller.releaseTransientState();
        renderLayer();
    }

    @Override public void onFinishInput() {
        InputConnection connection = getCurrentInputConnection();
        if (connection != null) {
            finishTextComposition(connection);
        } else {
            textComposer.clear();
        }
        super.onFinishInput();
    }

    @Override public void onKey(KeyboardLayout.Key key) {
        KeyboardController.Layer before = controller.layer();
        KeyboardController.Result result = controller.press(key);
        if (before != controller.layer()) renderLayer();
        applyResult(key, result);
    }

    @Override public void onLongPress(KeyboardLayout.Key key) {
        KeyboardController.Layer before = controller.layer();
        KeyboardController.Result result = controller.longPress(key);
        if (before != controller.layer()) renderLayer();
        applyResult(key, result);
    }

    private void applyResult(KeyboardLayout.Key key, KeyboardController.Result result) {
        InputConnection connection = getCurrentInputConnection();
        if (connection == null) return;
        switch (result.effect) {
            case COMMIT_TEXT:
                commitText(connection, key, result.text);
                break;
            case BACKSPACE:
                if (textComposer.cancel()) {
                    connection.commitText("", 1);
                    break;
                }
                deleteSelectionOrPreviousCodePoint(connection);
                break;
            case ENTER:
                finishTextComposition(connection);
                sendEnter(connection);
                break;
            case NONE:
                if (key.action == KeyboardLayout.Action.SHOW_ALPHA
                        || key.action == KeyboardLayout.Action.SHOW_SYMBOLS
                        || key.action == KeyboardLayout.Action.SHOW_MORE_SYMBOLS
                        || key.action == KeyboardLayout.Action.CYCLE_LANGUAGE) {
                    finishTextComposition(connection);
                }
                break;
        }
    }

    private void commitText(InputConnection connection, KeyboardLayout.Key key, String text) {
        if (key.action == KeyboardLayout.Action.TEXT) {
            applyComposerEdit(connection, textComposer.acceptText(text));
            return;
        }

        finishTextComposition(connection);
        connection.commitText(text, 1);
    }

    private void applyComposerEdit(
            InputConnection connection, BurmeseTextComposer.Edit edit) {
        switch (edit.effect) {
            case COMMIT_TEXT:
                connection.commitText(edit.text, 1);
                break;
            case SET_COMPOSING_TEXT:
                if (!connection.setComposingText(edit.text, 1)) {
                    textComposer.clear();
                    connection.commitText(edit.text, 1);
                }
                break;
            case FINISH_AND_COMMIT_TEXT:
                connection.beginBatchEdit();
                try {
                    connection.finishComposingText();
                    connection.commitText(edit.text, 1);
                } finally {
                    connection.endBatchEdit();
                }
                break;
        }
    }

    private void finishTextComposition(InputConnection connection) {
        if (textComposer.finish()) connection.finishComposingText();
    }

    private void renderLayer() {
        if (keyboard != null) keyboard.show(controller.page());
    }

    private void deleteSelectionOrPreviousCodePoint(InputConnection connection) {
        CharSequence selection = connection.getSelectedText(0);
        if (selection != null && selection.length() > 0) {
            connection.commitText("", 1);
            return;
        }

        CharSequence before = connection.getTextBeforeCursor(2, 0);
        if (before == null || before.length() == 0) {
            sendKey(connection, KeyEvent.KEYCODE_DEL);
            return;
        }
        int count = Character.charCount(Character.codePointBefore(before, before.length()));
        if (!connection.deleteSurroundingText(count, 0)) sendKey(connection, KeyEvent.KEYCODE_DEL);
    }

    private void sendEnter(InputConnection connection) {
        EditorInfo info = getCurrentInputEditorInfo();
        int options = info == null ? EditorInfo.IME_ACTION_NONE : info.imeOptions;
        int action = options & EditorInfo.IME_MASK_ACTION;
        boolean forceEnter = (options & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0;
        if (!forceEnter && action != EditorInfo.IME_ACTION_NONE
                && action != EditorInfo.IME_ACTION_UNSPECIFIED
                && connection.performEditorAction(action)) {
            return;
        }
        sendKey(connection, KeyEvent.KEYCODE_ENTER);
    }

    private void sendKey(InputConnection connection, int keyCode) {
        connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
        connection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
    }
}
