package com.burmese.keyboard;

/** Keeps a visible, editable Burmese cluster while positioning preposed e. */
final class BurmeseTextComposer {
    enum Effect { COMMIT_TEXT, SET_COMPOSING_TEXT, FINISH_AND_COMMIT_TEXT }

    static final class Edit {
        final Effect effect;
        final String text;

        private Edit(Effect effect, String text) {
            this.effect = effect;
            this.text = text;
        }

        static Edit commit(String text) { return new Edit(Effect.COMMIT_TEXT, text); }
        static Edit compose(String text) { return new Edit(Effect.SET_COMPOSING_TEXT, text); }
        static Edit finishAndCommit(String text) {
            return new Edit(Effect.FINISH_AND_COMMIT_TEXT, text);
        }
    }

    private static final String PREPOSED_E = "ေ";
    private static final int FIRST_MEDIAL = 0x103B;
    private static final int LAST_MEDIAL = 0x103E;

    private String composingText = "";
    private boolean clusterStarted;

    Edit acceptText(String text) {
        if (composingText.isEmpty()) {
            if (PREPOSED_E.equals(text)) {
                composingText = PREPOSED_E;
                clusterStarted = false;
                return Edit.compose(composingText);
            }
            return Edit.commit(text);
        }

        if (!clusterStarted) {
            if (isMedial(text)) {
                composingText = insertMedial(text);
            } else if (isCombiningText(text)) {
                composingText += text;
            } else {
                composingText = text + composingText;
            }
            clusterStarted = true;
            return Edit.compose(composingText);
        }

        if (isMedial(text)) {
            composingText = insertMedial(text);
            return Edit.compose(composingText);
        }
        if (isCombiningText(text)) {
            composingText += text;
            return Edit.compose(composingText);
        }

        composingText = "";
        clusterStarted = false;
        return Edit.finishAndCommit(text);
    }

    boolean finish() {
        if (composingText.isEmpty()) return false;
        composingText = "";
        clusterStarted = false;
        return true;
    }

    boolean cancel() {
        return finish();
    }

    boolean hasComposingText() {
        return !composingText.isEmpty();
    }

    void clear() {
        composingText = "";
        clusterStarted = false;
    }

    private String insertMedial(String medial) {
        int preposedIndex = composingText.indexOf(PREPOSED_E);
        if (preposedIndex < 0) return composingText + medial;

        int newMedial = medial.codePointAt(0);
        int insertionIndex = preposedIndex;
        for (int offset = 0; offset < preposedIndex;) {
            int codePoint = composingText.codePointAt(offset);
            if (isMedialCodePoint(codePoint) && codePoint > newMedial) {
                insertionIndex = offset;
                break;
            }
            offset += Character.charCount(codePoint);
        }
        return composingText.substring(0, insertionIndex)
                + medial + composingText.substring(insertionIndex);
    }

    private static boolean isMedial(String text) {
        if (text.codePointCount(0, text.length()) != 1) return false;
        return isMedialCodePoint(text.codePointAt(0));
    }

    private static boolean isMedialCodePoint(int codePoint) {
        return codePoint >= FIRST_MEDIAL && codePoint <= LAST_MEDIAL;
    }

    private static boolean isCombiningText(String text) {
        if (text.isEmpty()) return false;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            int type = Character.getType(codePoint);
            if (type != Character.NON_SPACING_MARK
                    && type != Character.COMBINING_SPACING_MARK
                    && type != Character.ENCLOSING_MARK) {
                return false;
            }
            offset += Character.charCount(codePoint);
        }
        return true;
    }
}
