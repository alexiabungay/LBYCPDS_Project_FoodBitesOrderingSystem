package services;

import features.AddOn;
import features.ItemSales;
import features.MenuItem;
import features.Order;
import features.OrderItem;
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
                List<String> wrapped = TextUtil.wrap(row[column], widths[column]);
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

    public static String forOrdering(List<MenuItem> items) {
        DisplayMenu table = new DisplayMenu(
                new String[]{"No.", "Item", "Price (PHP)", "Stock", "Status"},
                new Align[]{Align.RIGHT, Align.LEFT, Align.RIGHT, Align.RIGHT, Align.LEFT},
                new int[]{4, 44, 11, 6, 11});
        int number = 1;
        for (MenuItem item : items) {
            String status = item.isAvailable() && item.getStock() > 0 ? "Available" : "Unavailable";
            table.addRow(String.valueOf(number++), item.getName(), String.format("%.2f", item.getPrice()),
                    String.valueOf(item.getStock()), status);
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

    public static String forOrders(List<Order> orders) {
        DisplayMenu table = new DisplayMenu(
                new String[]{"Order #", "Type", "Status", "Items", "Total (PHP)"},
                new Align[]{Align.RIGHT, Align.LEFT, Align.LEFT, Align.LEFT, Align.RIGHT},
                new int[]{7, 10, 16, 40, 11});
        for (Order order : orders) {
            StringBuilder lines = new StringBuilder();
            for (OrderItem orderItem : order.getItems()) {
                if (lines.length() > 0) {
                    lines.append(", ");
                }
                lines.append(orderItem.getQuantity()).append("x ").append(orderItem.getName());
            }
            table.addRow(String.valueOf(order.getId()), String.valueOf(order.getDetails().getType()),
                    String.valueOf(order.getStatus()), lines.toString(),
                    String.format("%.2f", order.getBill().getTotal()));
        }
        return table.render();
    }

    public static String forBestSellers(List<MenuItem> items) {
        DisplayMenu table = new DisplayMenu(
                new String[]{"Rank", "Item", "Category", "Units Sold", "Revenue (PHP)"},
                new Align[]{Align.RIGHT, Align.LEFT, Align.LEFT, Align.RIGHT, Align.RIGHT},
                new int[]{4, 44, 10, 10, 13});
        int rank = 1;
        for (MenuItem item : items) {
            table.addRow(String.valueOf(rank++), item.getName(), item.getCategory().toString(),
                    String.valueOf(item.getTotalSales()), String.format("%.2f", item.getTotalRevenue()));
        }
        return table.render();
    }

    public static String forInventory(List<MenuItem> items, int lowStockLimit) {
        DisplayMenu table = new DisplayMenu(
                new String[]{"ID", "Item", "Category", "Stock", "Status", "Alert"},
                new Align[]{Align.RIGHT, Align.LEFT, Align.LEFT, Align.RIGHT, Align.LEFT, Align.LEFT},
                new int[]{6, 44, 10, 6, 11, 9});
        for (MenuItem item : items) {
            table.addRow(String.valueOf(item.getId()), item.getName(), item.getCategory().toString(),
                    String.valueOf(item.getStock()), item.isAvailable() ? "Available" : "Unavailable",
                    item.getStock() <= lowStockLimit ? "LOW STOCK" : "");
        }
        return table.render();
    }

    public static String forSalesSummary(String[][] metrics) {
        DisplayMenu table = new DisplayMenu(
                new String[]{"Summary", "Value"},
                new Align[]{Align.LEFT, Align.RIGHT},
                new int[]{24, 20});
        for (String[] metric : metrics) {
            table.addRow(metric[0], metric[1]);
        }
        return table.render();
    }

    public static String forSalesItems(List<ItemSales> sales) {
        DisplayMenu table = new DisplayMenu(
                new String[]{"Rank", "Item", "Units Sold", "Revenue (PHP)"},
                new Align[]{Align.RIGHT, Align.LEFT, Align.RIGHT, Align.RIGHT},
                new int[]{4, 44, 10, 13});
        int rank = 1;
        for (ItemSales sale : sales) {
            table.addRow(String.valueOf(rank++), sale.getName(), String.valueOf(sale.getUnits()),
                    String.format("%.2f", sale.getRevenue()));
        }
        return table.render();
    }
}