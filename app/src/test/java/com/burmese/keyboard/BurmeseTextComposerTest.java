package com.burmese.keyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class BurmeseTextComposerTest {
    @Test public void preposedEIsVisibleWhileWaitingForItsConsonant() {
        Harness harness = new Harness();

        harness.type("ေ");

        assertEquals("ေ", harness.text());
        assertTrue(harness.composer.hasComposingText());
    }

    @Test public void followingConsonantMovesBeforeVisiblePreposedE() {
        Harness harness = new Harness();

        harness.type("က");
        harness.type("ေ");
        assertEquals("ကေ", harness.text());
        harness.type("ခ");

        assertEquals("ကခေ", harness.text());
    }

    @Test public void yaMedialCanJoinAfterConsonantAndPreposedE() {
        Harness harness = new Harness();

        harness.type("ေ");
        harness.type("ပ");
        harness.type("ျ");
        harness.type("ာ");
        harness.type("်");

        assertEquals("ပျော်", harness.text());
    }

    @Test public void raMedialCanJoinAfterConsonantAndPreposedE() {
        Harness harness = new Harness();

        harness.type("ေ");
        harness.type("က");
        harness.type("ြ");

        assertEquals("ကြေ", harness.text());
    }

    @Test public void medialCanJoinWhenAConsonantWasAlreadyCommitted() {
        Harness harness = new Harness();

        harness.type("ပ");
        harness.type("ေ");
        harness.type("ျ");
        harness.type("ာ");
        harness.type("်");

        assertEquals("ပျော်", harness.text());
    }

    @Test public void medialsAreKeptInUnicodeOrder() {
        Harness harness = new Harness();

        harness.type("ေ");
        harness.type("က");
        harness.type("ြ");
        harness.type("ျ");

        assertEquals("ကျြေ", harness.text());
    }

    @Test public void newBaseFinishesThePreviousCluster() {
        Harness harness = new Harness();

        harness.type("ေ");
        harness.type("က");
        harness.type("ြ");
        harness.type("ခ");

        assertEquals("ကြေခ", harness.text());
        assertFalse(harness.composer.hasComposingText());
    }

    @Test public void finishKeepsVisibleCompositionAndClearsState() {
        Harness harness = new Harness();
        harness.type("ေ");
        harness.type("က");

        harness.finish();

        assertEquals("ကေ", harness.text());
        assertFalse(harness.composer.hasComposingText());
        assertFalse(harness.composer.finish());
    }

    @Test public void cancelRemovesVisibleCompositionAndClearsState() {
        BurmeseTextComposer composer = new BurmeseTextComposer();
        composer.acceptText("ေ");

        assertTrue(composer.cancel());
        assertFalse(composer.hasComposingText());
        assertFalse(composer.cancel());
    }

    @Test public void unrelatedCharactersCommitWithoutComposition() {
        BurmeseTextComposer composer = new BurmeseTextComposer();

        BurmeseTextComposer.Edit edit = composer.acceptText("ခ");

        assertEquals(BurmeseTextComposer.Effect.COMMIT_TEXT, edit.effect);
        assertEquals("ခ", edit.text);
        assertFalse(composer.hasComposingText());
    }

    private static final class Harness {
        final BurmeseTextComposer composer = new BurmeseTextComposer();
        final StringBuilder committed = new StringBuilder();
        String composing = "";

        void type(String text) {
            BurmeseTextComposer.Edit edit = composer.acceptText(text);
            switch (edit.effect) {
                case COMMIT_TEXT:
                    committed.append(edit.text);
                    break;
                case SET_COMPOSING_TEXT:
                    composing = edit.text;
                    break;
                case FINISH_AND_COMMIT_TEXT:
                    committed.append(composing);
                    composing = "";
                    committed.append(edit.text);
                    break;
                default:
                    throw new AssertionError("Unknown effect " + edit.effect);
            }
        }

        void finish() {
            if (!composer.finish()) return;
            committed.append(composing);
            composing = "";
        }

        String text() {
            return committed.toString() + composing;
        }
    }
}
