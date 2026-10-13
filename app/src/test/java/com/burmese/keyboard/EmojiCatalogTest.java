package com.burmese.keyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public final class EmojiCatalogTest {
    @Test public void providesCompleteEmojiCatalog() {
        assertEquals(1927, EmojiCatalog.EMOJIS.size());
        assertEquals("😀", EmojiCatalog.EMOJIS.get(0));
        assertEquals("🏴󠁧󠁢󠁷󠁬󠁳󠁿", EmojiCatalog.EMOJIS.get(1926));
        for (String emoji : EmojiCatalog.EMOJIS) {
            assertNotNull(emoji);
            assertFalse(emoji.isEmpty());
        }
    }

    @Test public void emojiLayoutDefaultsToCompleteCatalog() {
        KeyboardLayout.Page page = KeyboardLayout.forLayer(KeyboardController.Layer.EMOJI);
        assertEquals(1927 + 3, page.slots.size());
        assertNotNull(page.verticalScroll);
        assertEquals(1927, page.verticalScroll.slotCount);
        assertEquals("😀", page.slots.get(0).key.output);
        assertEquals("🏴󠁧󠁢󠁷󠁬󠁳󠁿", page.slots.get(1926).key.output);
    }
}
