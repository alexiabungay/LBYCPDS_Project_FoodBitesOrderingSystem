package services;

import java.util.ArrayList;
import java.util.List;

public final class TextUtil {
    private TextUtil() { }

    public static List<String> wrap(String text, int width) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String safeText = text == null ? "" : text.trim();

        for (String word : safeText.split("\\s+")) {
            while (word.length() > width) {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }
                result.add(word.substring(0, width));
                word = word.substring(width);
            }
            if (current.length() == 0) {
                current.append(word);
            } else if (current.length() + 1 + word.length() <= width) {
                current.append(' ').append(word);
            } else {
                result.add(current.toString());
                current.setLength(0);
                current.append(word);
            }
        }
        if (current.length() > 0 || result.isEmpty()) {
            result.add(current.toString());
        }
        return result;
    }
}