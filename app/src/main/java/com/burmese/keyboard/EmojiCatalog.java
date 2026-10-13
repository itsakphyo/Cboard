package com.burmese.keyboard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Parses the Browser column from the Unicode emoji chart supplied with the app. */
final class EmojiCatalog {
    static List<String> parse(Reader reader) throws IOException {
        BufferedReader buffered = new BufferedReader(reader);
        List<String> emojis = new ArrayList<>();
        String line;
        while ((line = buffered.readLine()) != null) {
            String[] columns = line.split("\\t", -1);
            if (columns.length < 3 || !isNumber(columns[0])) continue;
            String emoji = columns[2].trim();
            if (!emoji.isEmpty()) emojis.add(emoji);
        }
        return Collections.unmodifiableList(emojis);
    }

    private static boolean isNumber(String value) {
        if (value.isEmpty()) return false;
        for (int index = 0; index < value.length(); index++) {
            if (!Character.isDigit(value.charAt(index))) return false;
        }
        return true;
    }

    private EmojiCatalog() {}
}
