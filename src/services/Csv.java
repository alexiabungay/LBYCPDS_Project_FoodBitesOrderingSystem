package services;

import java.util.ArrayList;
import java.util.List;

public class Csv {
    public static String[] parseLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return new String[0];
        }

        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens.toArray(new String[0]);
    }

    public static String formatLine(String[] fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i] == null ? "" : fields[i];
            if (field.contains(",") || field.contains("\"")) {
                field = "\"" + field.replace("\"", "\"\"") + "\"";
            }
            sb.append(field);
            if (i < fields.length - 1) sb.append(",");
        }
        return sb.toString();
    }
}
