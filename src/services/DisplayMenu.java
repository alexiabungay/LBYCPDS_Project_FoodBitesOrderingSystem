package services;

import features.AddOn;
import features.MenuItem;
import java.util.ArrayList;
import java.util.List;

public class DisplayMenu {
    public enum Align { LEFT, RIGHT, CENTER }

    private final String[] headers;
    private final Align[] aligns;
    private final int[] maxWidths;
    private final List<String[]> rows = new ArrayList<>();

    public DisplayMenu(String[] headers, Align[] aligns, int[] maxWidths) {
        this.headers = headers;
        this.aligns = aligns;
        this.maxWidths = maxWidths;
    }

    public void addRow(String... cells) {
        rows.add(cells);
    }

    public String render() {
        int columnCount = headers.length;
        int[] widths = new int[columnCount];
        for (int column = 0; column < columnCount; column++) {
            int widest = 0;
            for (String[] row : rows) {
                widest = Math.max(widest, row[column].length());
            }
            widths[column] = Math.max(headers[column].length(), Math.min(widest, maxWidths[column]));
        }

        String border = border(widths);
        StringBuilder table = new StringBuilder();
        table.append(border).append('\n');

        StringBuilder headerLine = new StringBuilder("|");
        for (int column = 0; column < columnCount; column++) {
            headerLine.append(' ').append(pad(headers[column], widths[column], Align.CENTER)).append(" |");
        }
        table.append(headerLine).append('\n').append(border).append('\n');

        for (String[] row : rows) {
            List<List<String>> wrappedCells = new ArrayList<>();
            int rowHeight = 1;
            for (int column = 0; column < columnCount; column++) {
                List<String> wrapped = wrap(row[column], widths[column]);
                wrappedCells.add(wrapped);
                rowHeight = Math.max(rowHeight, wrapped.size());
            }
            for (int lineIndex = 0; lineIndex < rowHeight; lineIndex++) {
                StringBuilder line = new StringBuilder("|");
                for (int column = 0; column < columnCount; column++) {
                    List<String> wrapped = wrappedCells.get(column);
                    String cellText = lineIndex < wrapped.size() ? wrapped.get(lineIndex) : "";
                    line.append(' ').append(pad(cellText, widths[column], aligns[column])).append(" |");
                }
                table.append(line).append('\n');
            }
        }
        table.append(border);
        return table.toString();
    }

    private static String border(int[] widths) {
        StringBuilder line = new StringBuilder("+");
        for (int width : widths) {
            line.append("-".repeat(width + 2)).append('+');
        }
        return line.toString();
    }

    private static String pad(String text, int width, Align align) {
        int space = width - text.length();
        if (space <= 0) {
            return text;
        }
        switch (align) {
            case RIGHT:
                return " ".repeat(space) + text;
            case CENTER:
                int left = space / 2;
                return " ".repeat(left) + text + " ".repeat(space - left);
            default:
                return text + " ".repeat(space);
        }
    }

    private static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        if (text.length() <= width) {
            lines.add(text);
            return lines;
        }
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            while (word.length() > width) {
                if (current.length() > 0) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                lines.add(word.substring(0, width));
                word = word.substring(width);
            }
            if (current.length() == 0) {
                current.append(word);
            } else if (current.length() + 1 + word.length() <= width) {
                current.append(' ').append(word);
            } else {
                lines.add(current.toString());
                current = new StringBuilder(word);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    public static String forMenu(List<MenuItem> items, boolean showCategory) {
        DisplayMenu table;
        if (showCategory) {
            table = new DisplayMenu(
                    new String[]{"ID", "Item", "Category", "Price (PHP)", "Stock", "Status"},
                    new Align[]{Align.RIGHT, Align.LEFT, Align.LEFT, Align.RIGHT, Align.RIGHT, Align.LEFT},
                    new int[]{6, 44, 10, 11, 6, 11});
        } else {
            table = new DisplayMenu(
                    new String[]{"ID", "Item", "Price (PHP)", "Stock", "Status"},
                    new Align[]{Align.RIGHT, Align.LEFT, Align.RIGHT, Align.RIGHT, Align.LEFT},
                    new int[]{6, 44, 11, 6, 11});
        }
        for (MenuItem item : items) {
            String status = item.isAvailable() && item.getStock() > 0 ? "Available" : "Unavailable";
            String price = String.format("%.2f", item.getPrice());
            if (showCategory) {
                table.addRow(String.valueOf(item.getId()), item.getName(), item.getCategory().toString(),
                        price, String.valueOf(item.getStock()), status);
            } else {
                table.addRow(String.valueOf(item.getId()), item.getName(),
                        price, String.valueOf(item.getStock()), status);
            }
        }
        return table.render();
    }

    public static String forAddOns(List<AddOn> addOns) {
        DisplayMenu table = new DisplayMenu(
                new String[]{"Add-on", "Price (PHP)"},
                new Align[]{Align.LEFT, Align.RIGHT},
                new int[]{44, 11});
        for (AddOn addOn : addOns) {
            table.addRow(addOn.getName(), String.format("+%.2f", addOn.getPrice()));
        }
        return table.render();
    }
}