package com.burmese.keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Owns every keyboard-layer transition and produces editor-side effects. */
final class KeyboardController {
    enum Alphabet { BURMESE, ENGLISH }
    enum Layer {
        BURMESE,
        BURMESE_SHIFTED,
        ENGLISH,
        ENGLISH_SHIFTED,
        SYMBOLS,
        MORE_SYMBOLS,
        EMOJI
    }
    enum Effect { NONE, COMMIT_TEXT, BACKSPACE, ENTER }

    static final class Result {
        final Effect effect;
        final String text;

        private Result(Effect effect, String text) {
            this.effect = effect;
            this.text = text;
        }

        static Result none() { return new Result(Effect.NONE, ""); }
        static Result commit(String text) { return new Result(Effect.COMMIT_TEXT, text); }
        static Result effect(Effect effect) { return new Result(effect, ""); }
    }

    private final List<String> emojis;
    private Layer layer = Layer.BURMESE;
    private Alphabet alphabet = Alphabet.BURMESE;

    KeyboardController() {
        this(KeyboardLayout.EMOJIS);
    }

    KeyboardController(List<String> emojis) {
        if (emojis == null) throw new IllegalArgumentException("Emoji list must not be null");
        this.emojis = Collections.unmodifiableList(new ArrayList<>(emojis));
    }

    Layer layer() { return layer; }
    Alphabet alphabet() { return alphabet; }
    KeyboardLayout.Page page() {
        return KeyboardLayout.forState(layer, alphabet, emojis);
    }

    void reset() {
        layer = Layer.BURMESE;
        alphabet = Alphabet.BURMESE;
    }

    void releaseTransientState() {
        if (layer == Layer.BURMESE_SHIFTED) layer = Layer.BURMESE;
        if (layer == Layer.ENGLISH_SHIFTED) layer = Layer.ENGLISH;
    }

    Result press(KeyboardLayout.Key key) {
        switch (key.action) {
            case TOGGLE_SHIFT:
                if (layer == Layer.BURMESE) {
                    layer = Layer.BURMESE_SHIFTED;
                } else if (layer == Layer.BURMESE_SHIFTED) {
                    layer = Layer.BURMESE;
                } else if (layer == Layer.ENGLISH) {
                    layer = Layer.ENGLISH_SHIFTED;
                } else if (layer == Layer.ENGLISH_SHIFTED) {
                    layer = Layer.ENGLISH;
                }
                return Result.none();
            case SHOW_ALPHA:
                layer = alphabet == Alphabet.BURMESE ? Layer.BURMESE : Layer.ENGLISH;
                return Result.none();
            case SHOW_SYMBOLS:
                layer = Layer.SYMBOLS;
                return Result.none();
            case SHOW_MORE_SYMBOLS:
                layer = Layer.MORE_SYMBOLS;
                return Result.none();
            case CYCLE_LANGUAGE:
                cycleLanguage();
                return Result.none();
            case TEXT:
                return commitAndReleaseShift(key.output);
            case SPACE:
                return commitAndReleaseShift(key.output);
            case BACKSPACE:
                return Result.effect(Effect.BACKSPACE);
            case ENTER:
                return Result.effect(Effect.ENTER);
            default:
                throw new AssertionError("Unknown action: " + key.action);
        }
    }

    Result longPress(KeyboardLayout.Key key) {
        if (key.longPressOutput.isEmpty()) return Result.none();
        return commitAndReleaseShift(key.longPressOutput);
    }

    private Result commitAndReleaseShift(String text) {
        if (layer == Layer.BURMESE_SHIFTED) layer = Layer.BURMESE;
        if (layer == Layer.ENGLISH_SHIFTED) layer = Layer.ENGLISH;
        return Result.commit(text);
    }

    private void cycleLanguage() {
        if (layer == Layer.EMOJI) {
            alphabet = Alphabet.BURMESE;
            layer = Layer.BURMESE;
        } else if (alphabet == Alphabet.BURMESE) {
            alphabet = Alphabet.ENGLISH;
            layer = Layer.ENGLISH;
        } else {
            layer = Layer.EMOJI;
        }
    }
}
