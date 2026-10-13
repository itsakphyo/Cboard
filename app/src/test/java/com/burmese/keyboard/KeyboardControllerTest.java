package com.burmese.keyboard;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public final class KeyboardControllerTest {
    @Test public void shiftTogglesReliably() {
        KeyboardController controller = new KeyboardController();

        for (int index = 0; index < 100; index++) {
            controller.press(find(controller.page(), KeyboardLayout.Action.TOGGLE_SHIFT));
            assertEquals(KeyboardController.Layer.BURMESE_SHIFTED, controller.layer());
            controller.press(find(controller.page(), KeyboardLayout.Action.TOGGLE_SHIFT));
            assertEquals(KeyboardController.Layer.BURMESE, controller.layer());
        }
    }

    @Test public void shiftedTextIsOneShot() {
        KeyboardController controller = new KeyboardController();
        controller.press(find(controller.page(), KeyboardLayout.Action.TOGGLE_SHIFT));

        KeyboardController.Result result = controller.press(
                find(controller.page(), KeyboardLayout.Action.TEXT));

        assertEquals(KeyboardController.Effect.COMMIT_TEXT, result.effect);
        assertFalse(result.text.isEmpty());
        assertEquals(KeyboardController.Layer.BURMESE, controller.layer());
    }

    @Test public void backspaceAndEnterDoNotReleaseShift() {
        KeyboardController controller = new KeyboardController();
        controller.press(find(controller.page(), KeyboardLayout.Action.TOGGLE_SHIFT));

        assertEquals(KeyboardController.Effect.BACKSPACE,
                controller.press(find(controller.page(), KeyboardLayout.Action.BACKSPACE)).effect);
        assertEquals(KeyboardController.Layer.BURMESE_SHIFTED, controller.layer());
        assertEquals(KeyboardController.Effect.ENTER,
                controller.press(find(controller.page(), KeyboardLayout.Action.ENTER)).effect);
        assertEquals(KeyboardController.Layer.BURMESE_SHIFTED, controller.layer());
    }

    @Test public void alphabetKeyAlwaysReturnsAfterEnter() {
        KeyboardController controller = new KeyboardController();
        controller.press(find(controller.page(), KeyboardLayout.Action.SHOW_SYMBOLS));
        assertEquals(KeyboardController.Layer.SYMBOLS, controller.layer());

        assertEquals(KeyboardController.Effect.ENTER,
                controller.press(find(controller.page(), KeyboardLayout.Action.ENTER)).effect);
        assertEquals(KeyboardController.Layer.SYMBOLS, controller.layer());

        controller.press(find(controller.page(), KeyboardLayout.Action.SHOW_ALPHA));
        assertEquals(KeyboardController.Layer.BURMESE, controller.layer());
    }

    @Test public void everySymbolsPageHasAWorkingReturnPath() {
        KeyboardController controller = new KeyboardController();
        KeyboardController.Layer[] layers = {
                KeyboardController.Layer.SYMBOLS,
                KeyboardController.Layer.MORE_SYMBOLS
        };

        for (KeyboardController.Layer target : layers) {
            controller.reset();
            controller.press(find(controller.page(), KeyboardLayout.Action.SHOW_SYMBOLS));
            if (target == KeyboardController.Layer.MORE_SYMBOLS) {
                controller.press(find(controller.page(), KeyboardLayout.Action.SHOW_MORE_SYMBOLS));
            }
            assertEquals(target, controller.layer());
            controller.press(find(controller.page(), KeyboardLayout.Action.SHOW_ALPHA));
            assertEquals(KeyboardController.Layer.BURMESE, controller.layer());
        }
    }

    @Test public void navigationDoesNotDependOnVisibleLabel() {
        KeyboardController controller = new KeyboardController();
        controller.press(new KeyboardLayout.Key(
                "ABC", "", KeyboardLayout.Action.SHOW_SYMBOLS, 1));
        assertEquals(KeyboardController.Layer.SYMBOLS, controller.layer());

        controller.press(new KeyboardLayout.Key(
                "not ABC", "", KeyboardLayout.Action.SHOW_ALPHA, 1));
        assertEquals(KeyboardController.Layer.BURMESE, controller.layer());
    }

    @Test public void staleShiftEventCannotEscapeSymbolsLayer() {
        KeyboardController controller = new KeyboardController();
        controller.press(find(controller.page(), KeyboardLayout.Action.SHOW_SYMBOLS));

        controller.press(new KeyboardLayout.Key(
                "stale shift", "", KeyboardLayout.Action.TOGGLE_SHIFT, 1));

        assertEquals(KeyboardController.Layer.SYMBOLS, controller.layer());
    }

    @Test public void repeatedPageCyclesRemainDeterministic() {
        KeyboardController controller = new KeyboardController();
        for (int index = 0; index < 1000; index++) {
            controller.press(find(controller.page(), KeyboardLayout.Action.SHOW_SYMBOLS));
            controller.press(find(controller.page(), KeyboardLayout.Action.SHOW_MORE_SYMBOLS));
            controller.press(find(controller.page(), KeyboardLayout.Action.SHOW_ALPHA));
            controller.press(find(controller.page(), KeyboardLayout.Action.CYCLE_LANGUAGE));
            assertEquals(KeyboardController.Layer.ENGLISH, controller.layer());
            controller.press(find(controller.page(), KeyboardLayout.Action.CYCLE_LANGUAGE));
            assertEquals(KeyboardController.Layer.EMOJI, controller.layer());
            controller.press(find(controller.page(), KeyboardLayout.Action.CYCLE_LANGUAGE));
            assertEquals(KeyboardController.Layer.BURMESE, controller.layer());
        }
    }

    @Test public void inputRestartOnlyReleasesTransientShift() {
        KeyboardController controller = new KeyboardController();
        controller.press(find(controller.page(), KeyboardLayout.Action.TOGGLE_SHIFT));
        controller.releaseTransientState();
        assertEquals(KeyboardController.Layer.BURMESE, controller.layer());

        controller.press(find(controller.page(), KeyboardLayout.Action.SHOW_SYMBOLS));
        controller.releaseTransientState();
        assertEquals(KeyboardController.Layer.SYMBOLS, controller.layer());
    }

    @Test public void mainPageMatchesTextReferenceExactly() {
        KeyboardLayout.Page page = KeyboardLayout.forLayer(KeyboardController.Layer.BURMESE);
        assertArrayEquals(new String[] {"ဈ", "ဝ", "ဋ", "ုံ", "ော", "ဪ", "ရ", "ဂ", "ဟ", "၏"},
                outputs(page, 0, 10));
        assertArrayEquals(new String[] {"ဆ", "တ", "န", "မ", "အ", "ပ", "က", "င", "သ", "စ"},
                outputs(page, 10, 20));
        assertArrayEquals(new String[] {"ေ", "ျ", "ိ", "်", "ါ", "့", "ြ", "ု", "ူ", "း"},
                outputs(page, 20, 30));
        assertArrayEquals(new String[] {"ဖ", "ထ", "ခ", "လ", "ဘ", "ည", "ာ", "ယ"},
                outputs(page, 31, 39));
        assertEquals(46, page.slots.size());
        for (KeyboardLayout.Slot slot : page.slots) {
            assertFalse("Emoji key must not be present", "☺".equals(slot.key.label));
        }
    }

    @Test public void firstRowLongPressesCommitEnglishNumberHints() {
        KeyboardController controller = new KeyboardController();
        KeyboardLayout.Page page = controller.page();
        String[] numbers = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};

        for (int index = 0; index < numbers.length; index++) {
            KeyboardLayout.Key key = page.slots.get(index).key;
            assertEquals(numbers[index], key.hint);
            assertEquals(numbers[index], key.longPressOutput);
            assertEquals(numbers[index], controller.longPress(key).text);
        }

        KeyboardLayout.Page shifted = KeyboardLayout.forLayer(
                KeyboardController.Layer.BURMESE_SHIFTED);
        for (int index = 0; index < numbers.length; index++) {
            assertTrue(shifted.slots.get(index).key.longPressOutput.isEmpty());
        }
    }

    @Test public void shiftedPageMatchesReferenceScreenshot() {
        KeyboardLayout.Page page = KeyboardLayout.forLayer(KeyboardController.Layer.BURMESE_SHIFTED);
        assertArrayEquals(new String[] {"၁", "၂", "၃", "၄", "၅", "၆", "၇", "၈", "၉", "၀"},
                outputs(page, 0, 10));
        assertArrayEquals(new String[] {"ဍ", "ဏ္ဍ", "ဣ", "၎င်း", "ဤ", "၌", "ဥ", "၍", "ဿ", "ဏ"},
                outputs(page, 10, 20));
        assertArrayEquals(new String[] {"ဗ", "ှ", "ီ", "္", "ွ", "ံ", "ဲ", "ဒ", "ဓ", "ဏ္ဌ"},
                outputs(page, 20, 30));
        assertArrayEquals(new String[] {"ဇ", "ဌ", "ဃ", "ဠ", "ဎ", "ဉ", "ဦ", "ဧ"},
                outputs(page, 31, 39));
    }

    @Test public void symbolsPagesMatchTextReference() {
        KeyboardLayout.Page symbols = KeyboardLayout.forLayer(KeyboardController.Layer.SYMBOLS);
        assertArrayEquals(new String[] {"၁", "၂", "၃", "၄", "၅", "၆", "၇", "၈", "၉", "၀"},
                outputs(symbols, 0, 10));
        assertArrayEquals(new String[] {"@", "#", "$", "_", "&", "-", "+", "(", ")", "/"},
                outputs(symbols, 10, 20));
        assertArrayEquals(new String[] {"*", "\"", "'", ":", ";", "!", "?"},
                outputs(symbols, 21, 28));

        KeyboardLayout.Page more = KeyboardLayout.forLayer(KeyboardController.Layer.MORE_SYMBOLS);
        assertArrayEquals(new String[] {"~", "`", "|", "•", "√", "π", "÷", "×", "§", "∆"},
                outputs(more, 0, 10));
        assertArrayEquals(new String[] {"£", "¢", "€", "¥", "^", "°", "=", "{", "}", "\\"},
                outputs(more, 10, 20));
        assertArrayEquals(new String[] {"%", "©", "®", "™", "✓", "[", "]"},
                outputs(more, 21, 28));
    }

    @Test public void englishKeyboardIsStandardQwertyWithOneShotShift() {
        KeyboardController controller = new KeyboardController();
        controller.press(find(controller.page(), KeyboardLayout.Action.CYCLE_LANGUAGE));

        assertEquals(KeyboardController.Layer.ENGLISH, controller.layer());
        assertArrayEquals(new String[] {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
                outputs(controller.page(), 0, 10));
        assertArrayEquals(new String[] {"a", "s", "d", "f", "g", "h", "j", "k", "l"},
                outputs(controller.page(), 10, 19));
        assertArrayEquals(new String[] {"z", "x", "c", "v", "b", "n", "m"},
                outputs(controller.page(), 20, 27));

        controller.press(find(controller.page(), KeyboardLayout.Action.TOGGLE_SHIFT));
        assertEquals(KeyboardController.Layer.ENGLISH_SHIFTED, controller.layer());
        assertArrayEquals(new String[] {"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"},
                outputs(controller.page(), 0, 10));
        assertEquals("Q", controller.press(controller.page().slots.get(0).key).text);
        assertEquals(KeyboardController.Layer.ENGLISH, controller.layer());
    }

    @Test public void englishSymbolsUseLatinDigitsAndReturnToEnglish() {
        KeyboardController controller = new KeyboardController();
        controller.press(find(controller.page(), KeyboardLayout.Action.CYCLE_LANGUAGE));
        controller.press(find(controller.page(), KeyboardLayout.Action.SHOW_SYMBOLS));

        assertArrayEquals(new String[] {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
                outputs(controller.page(), 0, 10));
        assertEquals("ABC", find(controller.page(), KeyboardLayout.Action.SHOW_ALPHA).label);
        controller.press(find(controller.page(), KeyboardLayout.Action.SHOW_MORE_SYMBOLS));
        assertEquals(KeyboardController.Layer.MORE_SYMBOLS, controller.layer());
        controller.press(find(controller.page(), KeyboardLayout.Action.SHOW_ALPHA));
        assertEquals(KeyboardController.Layer.ENGLISH, controller.layer());
    }

    @Test public void globeCyclesBurmeseEnglishEmojiAndBack() {
        KeyboardController controller = new KeyboardController();

        controller.press(find(controller.page(), KeyboardLayout.Action.CYCLE_LANGUAGE));
        assertEquals(KeyboardController.Layer.ENGLISH, controller.layer());
        assertEquals(KeyboardController.Alphabet.ENGLISH, controller.alphabet());
        controller.press(find(controller.page(), KeyboardLayout.Action.CYCLE_LANGUAGE));
        assertEquals(KeyboardController.Layer.EMOJI, controller.layer());
        controller.press(find(controller.page(), KeyboardLayout.Action.CYCLE_LANGUAGE));
        assertEquals(KeyboardController.Layer.BURMESE, controller.layer());
        assertEquals(KeyboardController.Alphabet.BURMESE, controller.alphabet());
    }

    @Test public void emojiLayoutContainsEveryEntryAndOnlyThreeUtilityKeys() {
        List<String> emojis = new ArrayList<>();
        for (int index = 0; index < 65; index++) emojis.add("emoji-" + index);
        KeyboardController controller = new KeyboardController(emojis);
        controller.press(find(controller.page(), KeyboardLayout.Action.CYCLE_LANGUAGE));
        controller.press(find(controller.page(), KeyboardLayout.Action.CYCLE_LANGUAGE));

        KeyboardLayout.Page page = controller.page();
        assertEquals("emoji-0", page.slots.get(0).key.output);
        assertEquals("emoji-64", page.slots.get(64).key.output);
        assertEquals(68, page.slots.size());
        assertNotNull(page.verticalScroll);
        assertEquals(65, page.verticalScroll.slotCount);
        assertEquals(KeyboardLayout.Action.CYCLE_LANGUAGE, page.slots.get(65).key.action);
        assertEquals(KeyboardLayout.Action.SPACE, page.slots.get(66).key.action);
        assertEquals(KeyboardLayout.Action.BACKSPACE, page.slots.get(67).key.action);
        for (KeyboardLayout.Slot slot : page.slots) {
            assertFalse("Emoji layout must not contain Enter",
                    slot.key.action == KeyboardLayout.Action.ENTER);
        }
    }

    @Test public void combinedLabelsHaveExplicitSafeSizing() {
        KeyboardLayout.Page main = KeyboardLayout.forLayer(KeyboardController.Layer.BURMESE);
        assertTrue(main.slots.get(3).key.textScale < 1.0f);
        assertTrue(main.slots.get(4).key.textScale < 1.0f);
        assertTrue(main.slots.get(5).key.textScale < 1.0f);

        KeyboardLayout.Page symbols = KeyboardLayout.forLayer(KeyboardController.Layer.SYMBOLS);
        assertTrue(find(symbols, KeyboardLayout.Action.SHOW_ALPHA).textScale < 1.0f);
        assertTrue(find(symbols, KeyboardLayout.Action.CYCLE_LANGUAGE).label.isEmpty());
    }

    @Test public void removedNumericPageAndSwitchAreAbsentFromEveryPage() {
        for (KeyboardController.Layer layer : KeyboardController.Layer.values()) {
            KeyboardLayout.Page page = KeyboardLayout.forLayer(layer);
            for (KeyboardLayout.Slot slot : page.slots) {
                assertFalse("12/34 switch must not remain", "12\n34".equals(slot.key.label));
            }
        }
    }

    @Test public void everyLayerHasValidSlotsAndKeys() {
        for (KeyboardController.Layer layer : KeyboardController.Layer.values()) {
            KeyboardLayout.Page page = KeyboardLayout.forLayer(layer);
            assertFalse(page.slots.isEmpty());
            for (int index = 0; index < page.slots.size(); index++) {
                KeyboardLayout.Slot slot = page.slots.get(index);
                assertNotNull(slot.key.label);
                assertTrue(slot.key.weight > 0);
                assertTrue(slot.left >= 0 && slot.right <= 1 && slot.left < slot.right);
                assertTrue(slot.top >= 0 && slot.top < slot.bottom);
                if (page.verticalScroll == null
                        || index >= page.verticalScroll.slotCount) {
                    assertTrue(slot.bottom <= 1);
                }
                if (slot.key.action == KeyboardLayout.Action.TEXT
                        || slot.key.action == KeyboardLayout.Action.SPACE) {
                    assertFalse(slot.key.output.isEmpty());
                }
            }
        }
    }

    @Test public void navigationKeysUseRegularCorners() {
        KeyboardController controller = new KeyboardController();
        assertFalse(find(controller.page(), KeyboardLayout.Action.TOGGLE_SHIFT).pill);
        assertFalse(find(controller.page(), KeyboardLayout.Action.SHOW_SYMBOLS).pill);
        assertFalse(find(controller.page(), KeyboardLayout.Action.ENTER).pill);

        controller.press(find(controller.page(), KeyboardLayout.Action.SHOW_SYMBOLS));
        assertFalse(find(controller.page(), KeyboardLayout.Action.SHOW_ALPHA).pill);
        assertFalse(find(controller.page(), KeyboardLayout.Action.CYCLE_LANGUAGE).pill);
        assertFalse(find(controller.page(), KeyboardLayout.Action.ENTER).pill);
    }

    private static String[] outputs(KeyboardLayout.Page page, int start, int end) {
        List<String> values = new ArrayList<>();
        for (int index = start; index < end; index++) {
            values.add(page.slots.get(index).key.output);
        }
        return values.toArray(new String[values.size()]);
    }

    private static KeyboardLayout.Key find(
            KeyboardLayout.Page page, KeyboardLayout.Action action) {
        for (KeyboardLayout.Slot slot : page.slots) {
            if (slot.key.action == action) return slot.key;
        }
        throw new AssertionError("Missing action " + action);
    }
}
