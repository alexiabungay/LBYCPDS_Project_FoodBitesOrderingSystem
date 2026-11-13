package services;

import java.util.ArrayList;
import java.util.List;

public final class Csv {
    private Csv() { }

    public static String escape(String text) {
        if (text == null) {
            return "";
        }

        String singleLineText = text.replace("\r", " ").replace("\n", " ");
        boolean needsQuotes = singleLineText.contains(",") || singleLineText.contains("\"");
        if (needsQuotes) {
            singleLineText = "\"" + singleLineText.replace("\"", "\"\"") + "\"";
        }
        return singleLineText;
    }

    public static String join(List<String> fields) {
        StringBuilder joinedLine = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                joinedLine.append(',');
            }
            joinedLine.append(escape(fields.get(i)));
        }
        return joinedLine.toString();
    }

    public static List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);

            if (insideQuotes) {
                if (character == '"') {
                    boolean isDoubledQuote = i + 1 < line.length() && line.charAt(i + 1) == '"';
                    if (isDoubledQuote) {
                        currentField.append('"');
                        i++;
                    } else {
                        insideQuotes = false;
                    }
                } else {
                    currentField.append(character);
                }
            } else if (character == '"') {
                insideQuotes = true;
            } else if (character == ',') {
                fields.add(currentField.toString().trim());
                currentField.setLength(0);
            } else {
                currentField.append(character);
            }
        }

        fields.add(currentField.toString().trim());
        return fields;
    }
}
