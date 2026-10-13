package com.burmese.keyboard;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.StringReader;
import java.util.List;

public final class EmojiCatalogTest {
    @Test public void parsesOnlyNumberedBrowserEmojiRows() throws Exception {
        String chart = "Full Emoji List, v18.0\n"
                + "№\tCode\tBrowser\tSample\tCLDR Short Name\n"
                + "1\tU+1F600\t😀\t😀\tgrinning face\n"
                + "family\n"
                + "2\tU+1F468 U+200D U+1F469\t👨‍👩\t👨‍👩\tfamily\n";

        List<String> result = EmojiCatalog.parse(new StringReader(chart));

        assertEquals(2, result.size());
        assertEquals("😀", result.get(0));
        assertEquals("👨‍👩", result.get(1));
    }
}
