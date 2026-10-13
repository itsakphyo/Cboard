package com.burmese.keyboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Immutable key data and normalized geometry for every keyboard page. */
final class KeyboardLayout {
    enum Action {
        TEXT,
        TOGGLE_SHIFT,
        SHOW_ALPHA,
        SHOW_SYMBOLS,
        SHOW_MORE_SYMBOLS,
        CYCLE_LANGUAGE,
        BACKSPACE,
        SPACE,
        ENTER
    }

    enum Style { NORMAL, FUNCTION, ENTER }

    static final class Key {
        final String label;
        final String[] labelLines;
        final String output;
        final String hint;
        final String longPressOutput;
        final Action action;
        final Style style;
        final float weight;
        final float textScale;
        final boolean pill;

        Key(String label, String output, Action action, float weight) {
            this(label, output, "", action, defaultStyle(action), weight, 1.0f, false);
        }

        Key(String label, String output, String hint, Action action, Style style,
                float weight, float textScale, boolean pill) {
            this(label, output, hint, "", action, style, weight, textScale, pill);
        }

        Key(String label, String output, String hint, String longPressOutput,
                Action action, Style style, float weight, float textScale, boolean pill) {
            if (label == null || output == null || hint == null || action == null
                    || longPressOutput == null || style == null || weight <= 0 || textScale <= 0) {
                throw new IllegalArgumentException("Invalid keyboard key");
            }
            this.label = label;
            this.labelLines = label.split("\\n", -1);
            this.output = output;
            this.hint = hint;
            this.longPressOutput = longPressOutput;
            this.action = action;
            this.style = style;
            this.weight = weight;
            this.textScale = textScale;
            this.pill = pill;
        }

        private static Style defaultStyle(Action action) {
            if (action == Action.ENTER) return Style.ENTER;
            return action == Action.TEXT ? Style.NORMAL : Style.FUNCTION;
        }
    }

    static final class Slot {
        final Key key;
        final float left;
        final float top;
        final float right;
        final float bottom;
        final boolean drawBackground;
        final float horizontalInset;
        final float verticalInset;

        Slot(Key key, float left, float top, float right, float bottom,
                boolean drawBackground, float horizontalInset, float verticalInset) {
            if (key == null || left < 0 || top < 0 || right > 1
                    || left >= right || top >= bottom || horizontalInset < 0
                    || verticalInset < 0) {
                throw new IllegalArgumentException("Invalid keyboard slot");
            }
            this.key = key;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.drawBackground = drawBackground;
            this.horizontalInset = horizontalInset;
            this.verticalInset = verticalInset;
        }
    }

    static final class VerticalScrollRegion {
        final int slotCount;
        final float viewportTop;
        final float viewportBottom;
        final float maxOffset;

        VerticalScrollRegion(int slotCount, float viewportTop,
                float viewportBottom, float maxOffset) {
            if (slotCount < 0 || viewportTop < 0 || viewportBottom > 1
                    || viewportTop >= viewportBottom || maxOffset < 0) {
                throw new IllegalArgumentException("Invalid vertical scroll region");
            }
            this.slotCount = slotCount;
            this.viewportTop = viewportTop;
            this.viewportBottom = viewportBottom;
            this.maxOffset = maxOffset;
        }
    }

    static final class Panel {
        final float left;
        final float top;
        final float right;
        final float bottom;
        final Style style;

        Panel(float left, float top, float right, float bottom, Style style) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.style = style;
        }
    }

    static final class Page {
        final List<Slot> slots;
        final List<Panel> panels;
        final boolean shiftActive;
        final VerticalScrollRegion verticalScroll;

        Page(List<Slot> slots, List<Panel> panels) {
            this(slots, panels, false, null);
        }

        Page(List<Slot> slots, List<Panel> panels, boolean shiftActive) {
            this(slots, panels, shiftActive, null);
        }

        Page(List<Slot> slots, List<Panel> panels, boolean shiftActive,
                VerticalScrollRegion verticalScroll) {
            if (slots == null || slots.isEmpty() || panels == null) {
                throw new IllegalArgumentException("Invalid keyboard page");
            }
            int scrollingSlots = verticalScroll == null ? 0 : verticalScroll.slotCount;
            if (scrollingSlots > slots.size()) {
                throw new IllegalArgumentException("Scroll region exceeds slot count");
            }
            for (int index = scrollingSlots; index < slots.size(); index++) {
                if (slots.get(index).bottom > 1) {
                    throw new IllegalArgumentException("Fixed slot is outside the page");
                }
            }
            this.slots = Collections.unmodifiableList(new ArrayList<>(slots));
            this.panels = Collections.unmodifiableList(new ArrayList<>(panels));
            this.shiftActive = shiftActive;
            this.verticalScroll = verticalScroll;
        }
    }

    static Page forLayer(KeyboardController.Layer layer) {
        KeyboardController.Alphabet alphabet = layer == KeyboardController.Layer.ENGLISH
                || layer == KeyboardController.Layer.ENGLISH_SHIFTED
                ? KeyboardController.Alphabet.ENGLISH : KeyboardController.Alphabet.BURMESE;
        return forState(layer, alphabet, EMOJIS);
    }

    static Page forState(KeyboardController.Layer layer, KeyboardController.Alphabet alphabet) {
        return forState(layer, alphabet, EMOJIS);
    }

    static Page forState(KeyboardController.Layer layer, KeyboardController.Alphabet alphabet,
            List<String> emojis) {
        switch (layer) {
            case BURMESE:
                return burmese(false);
            case BURMESE_SHIFTED:
                return burmese(true);
            case ENGLISH:
                return english(false);
            case ENGLISH_SHIFTED:
                return english(true);
            case SYMBOLS:
                return symbols(alphabet);
            case MORE_SYMBOLS:
                return moreSymbols(alphabet);
            case EMOJI:
                return emoji(emojis == null || emojis.isEmpty() ? EMOJIS : emojis);
            default:
                throw new AssertionError("Unknown layer: " + layer);
        }
    }

    private static Page burmese(boolean shifted) {
        List<Key> first = shifted
                ? numberedTextKeys(new String[] {"၁", "၂", "၃", "၄", "၅", "၆", "၇", "၈", "၉", "၀"})
                : Arrays.asList(
                        numberedTextWithLongPress("ဈ", "1", 1.0f),
                        numberedTextWithLongPress("ဝ", "2", 1.0f),
                        numberedTextWithLongPress("ဋ", "3", 1.0f),
                        numberedDisplayWithLongPress("ုံ", "ုံ", "4", 0.78f),
                        numberedDisplayWithLongPress("ော", "ော", "5", 0.78f),
                        numberedTextWithLongPress("ဪ", "6", 0.76f),
                        numberedTextWithLongPress("ရ", "7", 1.0f),
                        numberedTextWithLongPress("ဂ", "8", 1.0f),
                        numberedTextWithLongPress("ဟ", "9", 1.0f),
                        numberedTextWithLongPress("၏", "0", 0.82f));

        List<Key> second = shifted
                ? Arrays.asList(
                        styledText("ဍ", 1.0f, Style.NORMAL, 1.0f),
                        styledText("ဏ္ဍ", 1.0f, Style.NORMAL, 0.78f),
                        styledText("ဣ", 1.0f, Style.NORMAL, 1.0f),
                        styledText("၎င်း", 1.0f, Style.NORMAL, 0.78f),
                        styledText("ဤ", 1.0f, Style.NORMAL, 1.0f),
                        styledText("၌", 1.0f, Style.NORMAL, 1.0f),
                        styledText("ဥ", 1.0f, Style.NORMAL, 1.0f),
                        styledText("၍", 1.0f, Style.NORMAL, 1.0f),
                        styledText("ဿ", 1.0f, Style.NORMAL, 1.0f),
                        styledText("ဏ", 1.0f, Style.NORMAL, 1.0f))
                : textKeys(new String[] {"ဆ", "တ", "န", "မ", "အ", "ပ", "က", "င", "သ", "စ"});

        List<Key> third = shifted
                ? Arrays.asList(
                        text("ဗ"), mark("ှ"), mark("ီ"), mark("္"), mark("ွ"),
                        mark("ံ"), mark("ဲ"), text("ဒ"), text("ဓ"),
                        styledText("ဏ္ဌ", 1.0f, Style.NORMAL, 0.78f))
                : Arrays.asList(
                        mark("ေ"), mark("ျ"), mark("ိ"), mark("်"), mark("ါ"),
                        mark("့"), mark("ြ"), mark("ု"), mark("ူ"), mark("း"));

        List<Key> fourthCharacters = shifted
                ? Arrays.asList(
                        styledText("ဇ", 1.0f, Style.NORMAL, 1.0f),
                        styledText("ဌ", 1.0f, Style.NORMAL, 1.0f),
                        styledText("ဃ", 1.0f, Style.NORMAL, 1.0f),
                        styledText("ဠ", 1.0f, Style.NORMAL, 1.0f),
                        styledText("ဎ", 1.0f, Style.NORMAL, 1.0f),
                        styledText("ဉ", 1.0f, Style.NORMAL, 1.0f),
                        styledText("ဦ", 1.0f, Style.NORMAL, 1.0f),
                        styledText("ဧ", 1.0f, Style.NORMAL, 1.0f))
                : textKeys(new String[] {"ဖ", "ထ", "ခ", "လ", "ဘ", "ည", "ာ", "ယ"});

        List<Key> fourth = new ArrayList<>();
        fourth.add(action("", Action.TOGGLE_SHIFT,
                1.0f, Style.FUNCTION, false, 1.18f));
        fourth.addAll(fourthCharacters);
        fourth.add(action("⌫", Action.BACKSPACE, 1.0f,
                Style.FUNCTION, false, 1.08f));

        List<Key> utility = Arrays.asList(
                action("?၁၂၃", Action.SHOW_SYMBOLS, 1.55f,
                        Style.FUNCTION, false, 0.66f),
                styledText("၊", 1.0f, Style.FUNCTION, 1.0f),
                globeKey(),
                output("မြန်မာ", " ", Action.SPACE, 4.4f,
                        Style.NORMAL, false, 0.66f),
                styledText("။", 1.0f, Style.FUNCTION, 1.0f),
                action("", Action.ENTER, 1.55f, Style.ENTER, false, 0.85f));

        Page page = regularPage(Arrays.asList(first, second, third, fourth, utility));
        return shifted ? new Page(page.slots, page.panels, true) : page;
    }

    private static Page english(boolean shifted) {
        List<Key> first = letterKeys(shifted ? "QWERTYUIOP" : "qwertyuiop");
        List<Key> second = letterKeys(shifted ? "ASDFGHJKL" : "asdfghjkl");

        List<Key> third = new ArrayList<>();
        third.add(action("", Action.TOGGLE_SHIFT,
                1.35f, Style.FUNCTION, false, 1.0f));
        third.addAll(letterKeys(shifted ? "ZXCVBNM" : "zxcvbnm"));
        third.add(action("⌫", Action.BACKSPACE,
                1.35f, Style.FUNCTION, false, 1.08f));

        List<Key> utility = Arrays.asList(
                action("?123", Action.SHOW_SYMBOLS, 1.55f,
                        Style.FUNCTION, false, 0.72f),
                styledText(",", 1.0f, Style.FUNCTION, 1.0f),
                globeKey(),
                output("English", " ", Action.SPACE, 4.4f,
                        Style.NORMAL, false, 0.62f),
                styledText(".", 1.0f, Style.FUNCTION, 1.0f),
                action("", Action.ENTER, 1.55f, Style.ENTER, false, 0.85f));

        Page page = regularPage(Arrays.asList(first, second, third, utility));
        return shifted ? new Page(page.slots, page.panels, true) : page;
    }

    private static Page symbols(KeyboardController.Alphabet alphabet) {
        String[] digits = alphabet == KeyboardController.Alphabet.ENGLISH
                ? new String[] {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"}
                : new String[] {"၁", "၂", "၃", "၄", "၅", "၆", "၇", "၈", "၉", "၀"};
        return regularPage(Arrays.asList(
                textKeys(digits),
                textKeys(new String[] {"@", "#", "$", "_", "&", "-", "+", "(", ")", "/"}),
                Arrays.asList(
                        action("=\\<", Action.SHOW_MORE_SYMBOLS, 1.55f,
                                Style.FUNCTION, false, 0.72f),
                        text("*"), text("\""), text("'"), text(":"), text(";"),
                        text("!"), text("?"),
                        action("⌫", Action.BACKSPACE, 1.55f,
                                Style.FUNCTION, false, 1.08f)),
                symbolUtilityRow(false, alphabet)));
    }

    private static Page moreSymbols(KeyboardController.Alphabet alphabet) {
        return regularPage(Arrays.asList(
                textKeys(new String[] {"~", "`", "|", "•", "√", "π", "÷", "×", "§", "∆"}),
                textKeys(new String[] {"£", "¢", "€", "¥", "^", "°", "=", "{", "}", "\\"}),
                Arrays.asList(
                        action(alphabet == KeyboardController.Alphabet.ENGLISH ? "?123" : "?၁၂၃",
                                Action.SHOW_SYMBOLS, 1.55f,
                                Style.FUNCTION, false, 0.72f),
                        text("%"), text("©"), text("®"), text("™", 0.78f),
                        text("✓"), text("["), text("]"),
                        action("⌫", Action.BACKSPACE, 1.55f,
                                Style.FUNCTION, false, 1.08f)),
                symbolUtilityRow(true, alphabet)));
    }

    static final List<String> EMOJIS = Collections.unmodifiableList(Arrays.asList(new String[] {
        "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂",
        "🙂", "🙃", "🫠", "🫫", "😉", "😊", "😇", "🥰",
        "😍", "🤩", "😘", "😗", "☺", "😚", "😙", "🥲",
        "😋", "😛", "😜", "🤪", "😝", "🤑", "🤗", "🤭",
        "🫢", "🫣", "🤫", "🤔", "🫡", "🤐", "🤨", "😐",
        "😑", "😶", "🫥", "😶‍🌫️", "😏", "😒", "🙄", "😬",
        "😮‍💨", "🤥", "🫨", "🙂‍↔️", "🙂‍↕️", "😌", "😔", "😪",
        "🤤", "😴", "🫩", "😷", "🤒", "🤕", "🤢", "🤮",
        "🤧", "🥵", "🥶", "🥴", "😵", "😵‍💫", "🤯", "🤠",
        "🥳", "🥸", "😎", "🤓", "🧐", "😕", "🫤", "😟",
        "🙁", "☹", "😮", "😯", "😲", "😳", "🫪", "🥺",
        "🥹", "😦", "😧", "😨", "😰", "😥", "😢", "😭",
        "😱", "😖", "😣", "😞", "😓", "😩", "😫", "🥱",
        "😤", "😡", "😠", "🤬", "😈", "👿", "💀", "☠",
        "💩", "🤡", "👹", "👺", "👻", "👽", "👾", "🤖",
        "😺", "😸", "😹", "😻", "😼", "😽", "🙀", "😿",
        "😾", "🙈", "🙉", "🙊", "💌", "💘", "💝", "💖",
        "💗", "💓", "💞", "💕", "💟", "❣", "💔", "❤️‍🔥",
        "❤️‍🩹", "❤", "🩷", "🧡", "💛", "💚", "💙", "🩵",
        "💜", "🤎", "🖤", "🩶", "🤍", "💋", "💯", "💢",
        "🫯", "💥", "💫", "💦", "💨", "🕳", "💬", "👁️‍🗨️",
        "🗨", "🗯", "💭", "💤", "👋", "🤚", "🖐", "✋",
        "🖖", "🫱", "🫲", "🫳", "🫴", "🫷", "🫸", "👌",
        "🤌", "🤏", "✌", "🤞", "🫰", "🤟", "🤘", "🤙",
        "👈", "👉", "👆", "🖕", "👇", "☝", "🫵", "👍",
        "👎", "🫹", "🫺", "✊", "👊", "🤛", "🤜", "👏",
        "🙌", "🫶", "👐", "🤲", "🤝", "🙏", "✍", "💅",
        "🤳", "💪", "🦾", "🦿", "🦵", "🦶", "👂", "🦻",
        "👃", "🧠", "🫀", "🫁", "🦷", "🦴", "👀", "👁",
        "👅", "👄", "🫦", "👶", "🧒", "👦", "👧", "🧑",
        "👱", "👨", "🧔", "🧔‍♂️", "🧔‍♀️", "👨‍🦰", "👨‍🦱", "👨‍🦳",
        "👨‍🦲", "👩", "👩‍🦰", "🧑‍🦰", "👩‍🦱", "🧑‍🦱", "👩‍🦳", "🧑‍🦳",
        "👩‍🦲", "🧑‍🦲", "👱‍♀️", "👱‍♂️", "🧓", "👴", "👵", "🙍",
        "🙍‍♂️", "🙍‍♀️", "🙎", "🙎‍♂️", "🙎‍♀️", "🙅", "🙅‍♂️", "🙅‍♀️",
        "🙆", "🙆‍♂️", "🙆‍♀️", "💁", "💁‍♂️", "💁‍♀️", "🙋", "🙋‍♂️",
        "🙋‍♀️", "🧏", "🧏‍♂️", "🧏‍♀️", "🙇", "🙇‍♂️", "🙇‍♀️", "🤦",
        "🤦‍♂️", "🤦‍♀️", "🤷", "🤷‍♂️", "🤷‍♀️", "🧑‍⚕️", "👨‍⚕️", "👩‍⚕️",
        "🧑‍🎓", "👨‍🎓", "👩‍🎓", "🧑‍🏫", "👨‍🏫", "👩‍🏫", "🧑‍⚖️", "👨‍⚖️",
        "👩‍⚖️", "🧑‍🌾", "👨‍🌾", "👩‍🌾", "🧑‍🍳", "👨‍🍳", "👩‍🍳", "🧑‍🔧",
        "👨‍🔧", "👩‍🔧", "🧑‍🏭", "👨‍🏭", "👩‍🏭", "🧑‍💼", "👨‍💼", "👩‍💼",
        "🧑‍🔬", "👨‍🔬", "👩‍🔬", "🧑‍💻", "👨‍💻", "👩‍💻", "🧑‍🎤", "👨‍🎤",
        "👩‍🎤", "🧑‍🎨", "👨‍🎨", "👩‍🎨", "🧑‍✈️", "👨‍✈️", "👩‍✈️", "🧑‍🚀",
        "👨‍🚀", "👩‍🚀", "🧑‍🚒", "👨‍🚒", "👩‍🚒", "👮", "👮‍♂️", "👮‍♀️",
        "🕵", "🕵️‍♂️", "🕵️‍♀️", "💂", "💂‍♂️", "💂‍♀️", "🥷", "👷",
        "👷‍♂️", "👷‍♀️", "🫅", "🤴", "👸", "👳", "👳‍♂️", "👳‍♀️",
        "👲", "🧕", "🤵", "🤵‍♂️", "🤵‍♀️", "👰", "👰‍♂️", "👰‍♀️",
        "🤰", "🫃", "🫄", "🤱", "👩‍🍼", "👨‍🍼", "🧑‍🍼", "👼",
        "🎅", "🤶", "🧑‍🎄", "🦸", "🦸‍♂️", "🦸‍♀️", "🦹", "🦹‍♂️",
        "🦹‍♀️", "🧙", "🧙‍♂️", "🧙‍♀️", "🧚", "🧚‍♂️", "🧚‍♀️", "🧛",
        "🧛‍♂️", "🧛‍♀️", "🧜", "🧜‍♂️", "🧜‍♀️", "🧝", "🧝‍♂️", "🧝‍♀️",
        "🧞", "🧞‍♂️", "🧞‍♀️", "🧟", "🧟‍♂️", "🧟‍♀️", "🧌", "🫈",
        "💆", "💆‍♂️", "💆‍♀️", "💇", "💇‍♂️", "💇‍♀️", "🚶", "🚶‍♂️",
        "🚶‍♀️", "🚶‍➡️", "🚶‍♀️‍➡️", "🚶‍♂️‍➡️", "🧍", "🧍‍♂️", "🧍‍♀️", "🧎",
        "🧎‍♂️", "🧎‍♀️", "🧎‍➡️", "🧎‍♀️‍➡️", "🧎‍♂️‍➡️", "🧑‍🦯", "🧑‍🦯‍➡️", "👨‍🦯",
        "👨‍🦯‍➡️", "👩‍🦯", "👩‍🦯‍➡️", "🧑‍🦼", "🧑‍🦼‍➡️", "👨‍🦼", "👨‍🦼‍➡️", "👩‍🦼",
        "👩‍🦼‍➡️", "🧑‍🦽", "🧑‍🦽‍➡️", "👨‍🦽", "👨‍🦽‍➡️", "👩‍🦽", "👩‍🦽‍➡️", "🏃",
        "🏃‍♂️", "🏃‍♀️", "🏃‍➡️", "🏃‍♀️‍➡️", "🏃‍♂️‍➡️", "🧑‍🩰", "💃", "🕺",
        "🕴", "👯", "👯‍♂️", "👯‍♀️", "🧖", "🧖‍♂️", "🧖‍♀️", "🧗",
        "🧗‍♂️", "🧗‍♀️", "🤺", "🏇", "⛷", "🏂", "🏌", "🏌️‍♂️",
        "🏌️‍♀️", "🏄", "🏄‍♂️", "🏄‍♀️", "🚣", "🚣‍♂️", "🚣‍♀️", "🏊",
        "🏊‍♂️", "🏊‍♀️", "⛹", "⛹️‍♂️", "⛹️‍♀️", "🏋", "🏋️‍♂️", "🏋️‍♀️",
        "🚴", "🚴‍♂️", "🚴‍♀️", "🚵", "🚵‍♂️", "🚵‍♀️", "🤸", "🤸‍♂️",
        "🤸‍♀️", "🤼", "🤼‍♂️", "🤼‍♀️", "🤽", "🤽‍♂️", "🤽‍♀️", "🤾",
        "🤾‍♂️", "🤾‍♀️", "🤹", "🤹‍♂️", "🤹‍♀️", "🧘", "🧘‍♂️", "🧘‍♀️",
        "🛀", "🛌", "🧑‍🤝‍🧑", "👭", "👫", "👬", "💏", "👩‍❤️‍💋‍👨",
        "👨‍❤️‍💋‍👨", "👩‍❤️‍💋‍👩", "💑", "👩‍❤️‍👨", "👨‍❤️‍👨", "👩‍❤️‍👩", "👨‍👩‍👦", "👨‍👩‍👧",
        "👨‍👩‍👧‍👦", "👨‍👩‍👦‍👦", "👨‍👩‍👧‍👧", "👨‍👨‍👦", "👨‍👨‍👧", "👨‍👨‍👧‍👦", "👨‍👨‍👦‍👦", "👨‍👨‍👧‍👧",
        "👩‍👩‍👦", "👩‍👩‍👧", "👩‍👩‍👧‍👦", "👩‍👩‍👦‍👦", "👩‍👩‍👧‍👧", "👨‍👦", "👨‍👦‍👦", "👨‍👧",
        "👨‍👧‍👦", "👨‍👧‍👧", "👩‍👦", "👩‍👦‍👦", "👩‍👧", "👩‍👧‍👦", "👩‍👧‍👧", "🗣",
        "👤", "👥", "🫂", "👪", "🧑‍🧑‍🧒", "🧑‍🧑‍🧒‍🧒", "🧑‍🧒", "🧑‍🧒‍🧒",
        "👣", "🫆", "🦰", "🦱", "🦳", "🦲", "🐵", "🐒",
        "🦍", "🦧", "🐶", "🐕", "🦮", "🐕‍🦺", "🐩", "🐺",
        "🦊", "🦝", "🐱", "🐈", "🐈‍⬛", "🦁", "🐯", "🐅",
        "🐆", "🐴", "🫎", "🫏", "🐎", "🦄", "🦓", "🦌",
        "🦬", "🐮", "🐂", "🐃", "🐄", "🐷", "🐖", "🐗",
        "🐽", "🐏", "🐑", "🐐", "🐪", "🐫", "🦙", "🦒",
        "🐘", "🦣", "🦏", "🦛", "🐭", "🐁", "🐀", "🐹",
        "🐰", "🐇", "🐿", "🦫", "🦔", "🦇", "🐻", "🐻‍❄️",
        "🐨", "🐼", "🦥", "🦦", "🦨", "🦘", "🦡", "🐾",
        "🦃", "🐔", "🐓", "🐣", "🐤", "🐥", "🐦", "🐧",
        "🕊", "🦅", "🦆", "🦢", "🦉", "🦤", "🪶", "🦩",
        "🦚", "🦜", "🪽", "🐦‍⬛", "🪿", "🐦‍🔥", "🐸", "🐊",
        "🐢", "🦎", "🐍", "🐲", "🐉", "🦕", "🦖", "🐳",
        "🐋", "🐬", "🫍", "🦭", "🐟", "🐠", "🐡", "🦈",
        "🐙", "🐚", "🪸", "🪼", "🦀", "🦞", "🦐", "🦑",
        "🦪", "🐌", "🦋", "🫌", "🐛", "🐜", "🐝", "🪲",
        "🐞", "🦗", "🪳", "🕷", "🕸", "🦂", "🦟", "🪰",
        "🪱", "🦠", "💐", "🌸", "💮", "🪷", "🏵", "🌹",
        "🥀", "🌺", "🌻", "🌼", "🌷", "🪻", "🌱", "🪴",
        "🌲", "🌳", "🌴", "🌵", "🌾", "🌿", "☘", "🍀",
        "🍁", "🍂", "🍃", "🪹", "🪺", "🍄", "🪾", "🍇",
        "🍈", "🍉", "🍊", "🍋", "🍋‍🟩", "🍌", "🍍", "🥭",
        "🍎", "🍏", "🍐", "🍑", "🍒", "🍓", "🫐", "🥝",
        "🍅", "🫒", "🥥", "🥑", "🍆", "🥔", "🥕", "🌽",
        "🌶", "🫑", "🥒", "🫝", "🥬", "🥦", "🧄", "🧅",
        "🥜", "🫘", "🌰", "🫚", "🫛", "🍄‍🟫", "🫜", "🍞",
        "🥐", "🥖", "🫓", "🥨", "🥯", "🥞", "🧇", "🧀",
        "🍖", "🍗", "🥩", "🥓", "🍔", "🍟", "🍕", "🌭",
        "🥪", "🌮", "🌯", "🫔", "🥙", "🧆", "🥚", "🍳",
        "🥘", "🍲", "🫕", "🥣", "🥗", "🍿", "🧈", "🧂",
        "🥫", "🍱", "🍘", "🍙", "🍚", "🍛", "🍜", "🍝",
        "🍠", "🍢", "🍣", "🍤", "🍥", "🥮", "🍡", "🥟",
        "🥠", "🥡", "🍦", "🍧", "🍨", "🍩", "🍪", "🎂",
        "🍰", "🧁", "🥧", "🍫", "🍬", "🍭", "🍮", "🍯",
        "🍼", "🥛", "☕", "🫖", "🍵", "🍶", "🍾", "🍷",
        "🍸", "🍹", "🍺", "🍻", "🥂", "🥃", "🫗", "🥤",
        "🧋", "🧃", "🧉", "🧊", "🥢", "🍽", "🍴", "🥄",
        "🔪", "🫙", "🏺", "🌍", "🌎", "🌏", "🌐", "🗺",
        "🗾", "🧭", "🏔", "⛰", "🛘", "🌋", "🗻", "🏕",
        "🏖", "🏜", "🏝", "🏞", "🏟", "🏛", "🏗", "🧱",
        "🪨", "🪵", "🛖", "🏘", "🏚", "🏠", "🏡", "🏢",
        "🏣", "🏤", "🏥", "🏦", "🏨", "🏩", "🏪", "🏫",
        "🏬", "🏭", "🏯", "🏰", "💒", "🗼", "🗽", "⛪",
        "🕌", "🛕", "🕍", "⛩", "🕋", "⛲", "⛺", "🌁",
        "🌃", "🏙", "🌄", "🌅", "🌆", "🌇", "🌉", "♨",
        "🎠", "🛝", "🎡", "🎢", "💈", "🎪", "🚂", "🚃",
        "🚄", "🚅", "🚆", "🚇", "🚈", "🚉", "🚊", "🚝",
        "🚞", "🚋", "🚌", "🚍", "🚎", "🚐", "🚑", "🚒",
        "🚓", "🚔", "🚕", "🚖", "🚗", "🚘", "🚙", "🛻",
        "🚚", "🚛", "🚜", "🏎", "🏍", "🛵", "🦽", "🦼",
        "🛺", "🚲", "🛴", "🛹", "🛼", "🚏", "🛣", "🛤",
        "🛢", "⛽", "🛞", "🚨", "🚥", "🚦", "🛑", "🚧",
        "🛙", "⚓", "🛟", "⛵", "🛶", "🚤", "🛳", "⛴",
        "🛥", "🚢", "✈", "🛩", "🛫", "🛬", "🪂", "💺",
        "🚁", "🚟", "🚠", "🚡", "🛰", "🚀", "🛸", "🛎",
        "🧳", "⌛", "⏳", "⌚", "⏰", "⏱", "⏲", "🕰",
        "🕛", "🕧", "🕐", "🕜", "🕑", "🕝", "🕒", "🕞",
        "🕓", "🕟", "🕔", "🕠", "🕕", "🕡", "🕖", "🕢",
        "🕗", "🕣", "🕘", "🕤", "🕙", "🕥", "🕚", "🕦",
        "🌑", "🌒", "🌓", "🌔", "🌕", "🌖", "🌗", "🌘",
        "🌙", "🌚", "🌛", "🌜", "🌡", "☀", "🌝", "🌞",
        "🪐", "⭐", "🌟", "🌠", "🌌", "☁", "⛅", "⛈",
        "🌤", "🌥", "🌦", "🌧", "🌨", "🌩", "🌪", "🌫",
        "🌬", "🌀", "🌈", "🌂", "☂", "☔", "⛱", "⚡",
        "❄", "☃", "⛄", "☄", "🪋", "🔥", "💧", "🌊",
        "🎃", "🎄", "🎆", "🎇", "🧨", "✨", "🎈", "🎉",
        "🎊", "🎋", "🎍", "🎎", "🎏", "🎐", "🎑", "🧧",
        "🎀", "🎁", "🎗", "🎟", "🎫", "🎖", "🏆", "🏅",
        "🥇", "🥈", "🥉", "⚽", "⚾", "🥎", "🏀", "🏐",
        "🏈", "🏉", "🎾", "🥏", "🎳", "🏏", "🏑", "🏒",
        "🥍", "🏓", "🏸", "🥊", "🥋", "🥅", "⛳", "⛸",
        "🎣", "🤿", "🎽", "🎿", "🛷", "🥌", "🎯", "🪀",
        "🪁", "🔫", "🎱", "🔮", "🪄", "🎮", "🕹", "🎰",
        "🎲", "🧩", "🧸", "🪅", "🪩", "🪆", "♠", "♥",
        "♦", "♣", "♟", "🃏", "🀄", "🎴", "🎭", "🖼",
        "🎨", "🧵", "🪡", "🧶", "🪢", "👓", "🕶", "🥽",
        "🥼", "🦺", "👔", "👕", "👖", "🧣", "🧤", "🧥",
        "🧦", "👗", "👘", "🥻", "🩱", "🩲", "🩳", "👙",
        "👚", "🪭", "👛", "👜", "👝", "🛍", "🎒", "🩴",
        "👞", "👟", "🥾", "🥿", "👠", "👡", "🩰", "👢",
        "🪮", "👑", "👒", "🎩", "🎓", "🧢", "🪖", "⛑",
        "📿", "💄", "💍", "💎", "🔇", "🔈", "🔉", "🔊",
        "📢", "📣", "📯", "🔔", "🔕", "🎼", "🎵", "🎶",
        "🎙", "🎚", "🎛", "🎤", "🎧", "📻", "🎷", "🎺",
        "🪊", "🪗", "🎸", "🎹", "🎻", "🪕", "🥁", "🪘",
        "🪇", "🪈", "🪉", "📱", "📲", "☎", "📞", "📟",
        "📠", "🔋", "🪫", "🔌", "💻", "🖥", "🖨", "⌨",
        "🖱", "🖲", "💽", "💾", "💿", "📀", "🧮", "🎥",
        "🎞", "📽", "🎬", "📺", "📷", "📸", "📹", "📼",
        "🔍", "🔎", "🕯", "💡", "🔦", "🏮", "🪔", "📔",
        "📕", "📖", "📗", "📘", "📙", "📚", "📓", "📒",
        "📃", "📜", "📄", "📰", "🗞", "📑", "🔖", "🏷",
        "🪙", "💰", "🪎", "💴", "💵", "💶", "💷", "💸",
        "💳", "🧾", "💹", "✉", "📧", "📨", "📩", "📤",
        "📥", "📦", "📫", "📪", "📬", "📭", "📮", "🗳",
        "✏", "✒", "🖋", "🖊", "🖌", "🖍", "📝", "🪌",
        "💼", "📁", "📂", "🗂", "📅", "📆", "🗒", "🗓",
        "📇", "📈", "📉", "📊", "📋", "📌", "📍", "📎",
        "🖇", "📏", "📐", "✂", "🗃", "🗄", "🗑", "🔒",
        "🔓", "🔏", "🔐", "🔑", "🗝", "🪍", "🔨", "🪓",
        "⛏", "⚒", "🛠", "🗡", "⚔", "💣", "🪃", "🏹",
        "🛡", "🪚", "🔧", "🪛", "🔩", "⚙", "🗜", "⚖",
        "🦯", "🔗", "⛓️‍💥", "⛓", "🪝", "🧰", "🧲", "🪜",
        "🪏", "⚗", "🧪", "🧫", "🧬", "🔬", "🔭", "📡",
        "💉", "🩸", "💊", "🩹", "🩼", "🩺", "🩻", "🚪",
        "🛗", "🪞", "🪟", "🛏", "🛋", "🪑", "🚽", "🪠",
        "🚿", "🛁", "🪤", "🪒", "🧴", "🧷", "🧹", "🧺",
        "🧻", "🪣", "🧼", "🫧", "🪥", "🧽", "🧯", "🛒",
        "🚬", "⚰", "🪦", "⚱", "🧿", "🪬", "🗿", "🪧",
        "🪪", "🏧", "🚮", "🚰", "♿", "🚹", "🚺", "🚻",
        "🚼", "🚾", "🛂", "🛃", "🛄", "🛅", "⚠", "🚸",
        "⛔", "🚫", "🚳", "🚭", "🚯", "🚱", "🚷", "📵",
        "🔞", "☢", "☣", "⬆", "↗", "➡", "↘", "⬇",
        "↙", "⬅", "↖", "↕", "↔", "↩", "↪", "⤴",
        "⤵", "🔃", "🔄", "🔙", "🔚", "🔛", "🔜", "🔝",
        "🛐", "⚛", "🕉", "✡", "☸", "☯", "✝", "☦",
        "☪", "☮", "🕎", "🔯", "🪯", "♈", "♉", "♊",
        "♋", "♌", "♍", "♎", "♏", "♐", "♑", "♒",
        "♓", "⛎", "🔀", "🔁", "🔂", "▶", "⏩", "⏭",
        "⏯", "◀", "⏪", "⏮", "🔼", "⏫", "🔽", "⏬",
        "⏸", "⏹", "⏺", "⏏", "🎦", "🔅", "🔆", "📶",
        "🛜", "📳", "📴", "♀", "♂", "⚧", "✖", "➕",
        "➖", "➗", "🟰", "♾", "‼", "⁉", "❓", "❔",
        "❕", "❗", "〰", "💱", "💲", "⚕", "♻", "⚜",
        "🔱", "📛", "🔰", "⭕", "✅", "☑", "✔", "❌",
        "❎", "➰", "➿", "〽", "✳", "✴", "❇", "©",
        "®", "™", "🫟", "#️⃣", "*️⃣", "0️⃣", "1️⃣", "2️⃣",
        "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣", "🔟",
        "🔠", "🔡", "🔢", "🔣", "🔤", "🅰", "🆎", "🅱",
        "🆑", "🆒", "🆓", "ℹ", "🆔", "Ⓜ", "🆕", "🆖",
        "🅾", "🆗", "🅿", "🆘", "🆙", "🆚", "🈁", "🈂",
        "🈷", "🈶", "🈯", "🉐", "🈹", "🈚", "🈲", "🉑",
        "🈸", "🈴", "🈳", "㊗", "㊙", "🈺", "🈵", "🔴",
        "🟠", "🟡", "🟢", "🔵", "🟣", "🟤", "⚫", "⚪",
        "🟥", "🟧", "🟨", "🟩", "🟦", "🟪", "🟫", "⬛",
        "⬜", "◼", "◻", "◾", "◽", "▪", "▫", "🔶",
        "🔷", "🔸", "🔹", "🔺", "🔻", "💠", "🔘", "🔳",
        "🔲", "🏁", "🚩", "🎌", "🏴", "🏳", "🏳️‍🌈", "🏳️‍⚧️",
        "🏴‍☠️", "🇦🇨", "🇦🇩", "🇦🇪", "🇦🇫", "🇦🇬", "🇦🇮", "🇦🇱",
        "🇦🇲", "🇦🇴", "🇦🇶", "🇦🇷", "🇦🇸", "🇦🇹", "🇦🇺", "🇦🇼",
        "🇦🇽", "🇦🇿", "🇧🇦", "🇧🇧", "🇧🇩", "🇧🇪", "🇧🇫", "🇧🇬",
        "🇧🇭", "🇧🇮", "🇧🇯", "🇧🇱", "🇧🇲", "🇧🇳", "🇧🇴", "🇧🇶",
        "🇧🇷", "🇧🇸", "🇧🇹", "🇧🇻", "🇧🇼", "🇧🇾", "🇧🇿", "🇨🇦",
        "🇨🇨", "🇨🇩", "🇨🇫", "🇨🇬", "🇨🇭", "🇨🇮", "🇨🇰", "🇨🇱",
        "🇨🇲", "🇨🇳", "🇨🇴", "🇨🇵", "🇨🇶", "🇨🇷", "🇨🇺", "🇨🇻",
        "🇨🇼", "🇨🇽", "🇨🇾", "🇨🇿", "🇩🇪", "🇩🇬", "🇩🇯", "🇩🇰",
        "🇩🇲", "🇩🇴", "🇩🇿", "🇪🇦", "🇪🇨", "🇪🇪", "🇪🇬", "🇪🇭",
        "🇪🇷", "🇪🇸", "🇪🇹", "🇪🇺", "🇫🇮", "🇫🇯", "🇫🇰", "🇫🇲",
        "🇫🇴", "🇫🇷", "🇬🇦", "🇬🇧", "🇬🇩", "🇬🇪", "🇬🇫", "🇬🇬",
        "🇬🇭", "🇬🇮", "🇬🇱", "🇬🇲", "🇬🇳", "🇬🇵", "🇬🇶", "🇬🇷",
        "🇬🇸", "🇬🇹", "🇬🇺", "🇬🇼", "🇬🇾", "🇭🇰", "🇭🇲", "🇭🇳",
        "🇭🇷", "🇭🇹", "🇭🇺", "🇮🇨", "🇮🇩", "🇮🇪", "🇮🇱", "🇮🇲",
        "🇮🇳", "🇮🇴", "🇮🇶", "🇮🇷", "🇮🇸", "🇮🇹", "🇯🇪", "🇯🇲",
        "🇯🇴", "🇯🇵", "🇰🇪", "🇰🇬", "🇰🇭", "🇰🇮", "🇰🇲", "🇰🇳",
        "🇰🇵", "🇰🇷", "🇰🇼", "🇰🇾", "🇰🇿", "🇱🇦", "🇱🇧", "🇱🇨",
        "🇱🇮", "🇱🇰", "🇱🇷", "🇱🇸", "🇱🇹", "🇱🇺", "🇱🇻", "🇱🇾",
        "🇲🇦", "🇲🇨", "🇲🇩", "🇲🇪", "🇲🇫", "🇲🇬", "🇲🇭", "🇲🇰",
        "🇲🇱", "🇲🇲", "🇲🇳", "🇲🇴", "🇲🇵", "🇲🇶", "🇲🇷", "🇲🇸",
        "🇲🇹", "🇲🇺", "🇲🇻", "🇲🇼", "🇲🇽", "🇲🇾", "🇲🇿", "🇳🇦",
        "🇳🇨", "🇳🇪", "🇳🇫", "🇳🇬", "🇳🇮", "🇳🇱", "🇳🇴", "🇳🇵",
        "🇳🇷", "🇳🇺", "🇳🇿", "🇴🇲", "🇵🇦", "🇵🇪", "🇵🇫", "🇵🇬",
        "🇵🇭", "🇵🇰", "🇵🇱", "🇵🇲", "🇵🇳", "🇵🇷", "🇵🇸", "🇵🇹",
        "🇵🇼", "🇵🇾", "🇶🇦", "🇷🇪", "🇷🇴", "🇷🇸", "🇷🇺", "🇷🇼",
        "🇸🇦", "🇸🇧", "🇸🇨", "🇸🇩", "🇸🇪", "🇸🇬", "🇸🇭", "🇸🇮",
        "🇸🇯", "🇸🇰", "🇸🇱", "🇸🇲", "🇸🇳", "🇸🇴", "🇸🇷", "🇸🇸",
        "🇸🇹", "🇸🇻", "🇸🇽", "🇸🇾", "🇸🇿", "🇹🇦", "🇹🇨", "🇹🇩",
        "🇹🇫", "🇹🇬", "🇹🇭", "🇹🇯", "🇹🇰", "🇹🇱", "🇹🇲", "🇹🇳",
        "🇹🇴", "🇹🇷", "🇹🇹", "🇹🇻", "🇹🇼", "🇹🇿", "🇺🇦", "🇺🇬",
        "🇺🇲", "🇺🇳", "🇺🇸", "🇺🇾", "🇺🇿", "🇻🇦", "🇻🇨", "🇻🇪",
        "🇻🇬", "🇻🇮", "🇻🇳", "🇻🇺", "🇼🇫", "🇼🇸", "🇽🇰", "🇾🇪",
        "🇾🇹", "🇿🇦", "🇿🇲", "🇿🇼", "🏴󠁧󠁢󠁥󠁮󠁧󠁿", "🏴󠁧󠁢󠁳󠁣󠁴󠁿", "🏴󠁧󠁢󠁷󠁬󠁳󠁿"
    }));

    private static Page emoji(List<String> emojis) {
        List<String> emojiList = (emojis != null && !emojis.isEmpty()) ? emojis : EMOJIS;
        List<Slot> slots = new ArrayList<>(emojiList.size() + 3);
        float viewportTop = 0.04f;
        float viewportBottom = 0.765f;
        float rowHeight = 0.17f;
        float rowPitch = 0.185f;

        for (int index = 0; index < emojiList.size(); index++) {
            int row = index / 8;
            int column = index % 8;
            float top = viewportTop + row * rowPitch;
            slots.add(new Slot(styledText(emojiList.get(index), 1.0f, Style.NORMAL, 1.0f),
                    column / 8.0f, top, (column + 1) / 8.0f, top + rowHeight,
                    true, 1.0f, 1.0f));
        }

        List<Key> utility = Arrays.asList(
                action("", Action.CYCLE_LANGUAGE, 1.55f,
                        Style.FUNCTION, false, 1.0f),
                output("", " ", Action.SPACE, 6.9f,
                        Style.NORMAL, false, 1.0f),
                action("⌫", Action.BACKSPACE, 1.55f,
                        Style.FUNCTION, false, 1.08f));
        addWeightedRow(slots, utility, 0.78f, 0.95f);
        int rowCount = (emojiList.size() + 7) / 8;
        float contentBottom = rowCount == 0
                ? viewportBottom : viewportTop + (rowCount - 1) * rowPitch + rowHeight;
        VerticalScrollRegion scroll = new VerticalScrollRegion(
                emojiList.size(), viewportTop, viewportBottom,
                Math.max(0, contentBottom - viewportBottom));
        return new Page(slots, Collections.<Panel>emptyList(), false, scroll);
    }

    private static List<Key> symbolUtilityRow(
            boolean moreSymbols, KeyboardController.Alphabet alphabet) {
        return Arrays.asList(
                action(alphabet == KeyboardController.Alphabet.ENGLISH ? "ABC" : "ကခ",
                        Action.SHOW_ALPHA, 1.55f,
                        Style.FUNCTION, false, 0.72f),
                styledText(moreSymbols ? "<" : ",", 1.0f, Style.FUNCTION, 1.0f),
                globeKey(),
                output(alphabet == KeyboardController.Alphabet.ENGLISH ? "English" : "မြန်မာ",
                        " ", Action.SPACE, 4.4f,
                        Style.NORMAL, false, 0.66f),
                styledText(moreSymbols ? ">" : ".", 1.0f, Style.FUNCTION, 1.0f),
                action("", Action.ENTER, 1.55f, Style.ENTER, false, 0.85f));
    }

    private static Page regularPage(List<List<Key>> rows) {
        float[][] bands = rows.size() == 5
                ? new float[][] {
                        {0.04f, 0.21f}, {0.225f, 0.395f}, {0.41f, 0.58f},
                        {0.595f, 0.765f}, {0.78f, 0.95f}}
                : new float[][] {
                        {0.04f, 0.225f}, {0.285f, 0.47f},
                        {0.53f, 0.715f}, {0.775f, 0.95f}};
        if (rows.size() != bands.length) {
            throw new IllegalArgumentException("Unsupported row count: " + rows.size());
        }
        List<Slot> slots = new ArrayList<>();
        for (int row = 0; row < rows.size(); row++) {
            addWeightedRow(slots, rows.get(row), bands[row][0], bands[row][1], false);
        }
        return new Page(slots, Collections.<Panel>emptyList());
    }

    private static void addWeightedRow(List<Slot> slots, List<Key> keys,
            float top, float bottom) {
        addWeightedRow(slots, keys, top, bottom, true);
    }

    private static void addWeightedRow(List<Slot> slots, List<Key> keys,
            float top, float bottom, boolean verticalInset) {
        float totalWeight = 0;
        for (Key key : keys) totalWeight += key.weight;
        float cursor = 0;
        for (Key key : keys) {
            float left = cursor / totalWeight;
            cursor += key.weight;
            slots.add(new Slot(key, left, top, cursor / totalWeight, bottom,
                    true, 1.0f, verticalInset ? 1.0f : 0.0f));
        }
    }

    private static List<Key> textKeys(String[] values) {
        Key[] result = new Key[values.length];
        for (int index = 0; index < values.length; index++) result[index] = text(values[index]);
        return Arrays.asList(result);
    }

    private static List<Key> letterKeys(String letters) {
        Key[] result = new Key[letters.length()];
        for (int index = 0; index < letters.length(); index++) {
            String letter = String.valueOf(letters.charAt(index));
            result[index] = text(letter);
        }
        return Arrays.asList(result);
    }

    private static List<Key> numberedTextKeys(String[] values) {
        Key[] result = new Key[values.length];
        for (int index = 0; index < values.length; index++) {
            result[index] = numberedText(values[index], String.valueOf((index + 1) % 10), 1.0f);
        }
        return Arrays.asList(result);
    }

    private static Key mark(String output) {
        return output(output, output, Action.TEXT, 1.0f,
                Style.NORMAL, false, 0.88f);
    }

    private static Key numberedText(String value, String hint, float textScale) {
        return new Key(value, value, hint, Action.TEXT, Style.NORMAL, 1.0f, textScale, false);
    }

    private static Key numberedTextWithLongPress(String value, String hint, float textScale) {
        return new Key(value, value, hint, hint, Action.TEXT, Style.NORMAL,
                1.0f, textScale, false);
    }

    private static Key numberedDisplay(String label, String value, String hint, float textScale) {
        return new Key(label, value, hint, Action.TEXT, Style.NORMAL, 1.0f, textScale, false);
    }

    private static Key numberedDisplayWithLongPress(
            String label, String value, String hint, float textScale) {
        return new Key(label, value, hint, hint, Action.TEXT, Style.NORMAL,
                1.0f, textScale, false);
    }

    private static Key text(String value) { return text(value, 1.0f); }

    private static Key text(String value, float weight) {
        return output(value, value, Action.TEXT, weight, Style.NORMAL, false, 1.0f);
    }

    private static Key styledText(String value, float weight, Style style, float textScale) {
        return output(value, value, Action.TEXT, weight, style, false, textScale);
    }

    private static Key action(String label, Action action, float weight,
            Style style, boolean pill, float textScale) {
        return output(label, "", action, weight, style, pill, textScale);
    }

    private static Key globeKey() {
        return action("", Action.CYCLE_LANGUAGE, 1.0f,
                Style.FUNCTION, false, 1.0f);
    }

    private static Key output(String label, String value, Action action, float weight,
            Style style, boolean pill, float textScale) {
        return new Key(label, value, "", action, style, weight, textScale, pill);
    }

    private KeyboardLayout() {}
}
