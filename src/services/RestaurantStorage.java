package services;

import features.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

public class RestaurantStorage {
    private static final String RESTAURANT_HEADER = "restaurant_id,owner_email,name,description,address,dine_in,takeout,delivery";
    private static final String ITEM_HEADER = "item_name,description,category,price,stock,available,portion_sizes,add_ons";

    private final Path folder;
    private final Map<Integer, Path> files = new HashMap<>();

    public RestaurantStorage(String folder) { this.folder = Paths.get(folder); }

    public List<Path> listFiles() {
        List<Path> csvFiles = new ArrayList<>();
        if (!Files.isDirectory(folder)) return csvFiles;
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(folder, "*.csv")) {
            for (Path file : entries) csvFiles.add(file);
        } catch (IOException e) {
            System.err.println("! Could not read " + folder + ": " + e.getMessage());
        }
        Collections.sort(csvFiles);
        return csvFiles;
    }

    private List<String[]> readRows(Path file) throws IOException {
        List<String[]> rows = new ArrayList<>();
        boolean firstLine = true;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (firstLine) { line = line.replace("\uFEFF", ""); firstLine = false; } // Excel BOM
            if (!line.replace(",", "").trim().isEmpty()) rows.add(Csv.parseLine(line));
        }
        return rows;
    }

    public int readId(Path file) {
        try {
            List<String[]> rows = readRows(file);
            return rows.size() < 2 ? 0 : Integer.parseInt(get(rows.get(1), 0));
        } catch (IOException | NumberFormatException e) {
            return 0;
        }
    }

    public Restaurant read(Path file, int id, IntSupplier itemIds) throws IOException {
        List<String[]> rows = readRows(file);
        if (rows.size() < 2) throw new IOException("file needs a header row and a restaurant row");
        String[] restaurantRow = rows.get(1);
        if (get(restaurantRow, 2).isEmpty()) throw new IOException("restaurant name is empty");
        Restaurant restaurant = new Restaurant(id, -1, get(restaurantRow, 2), get(restaurantRow, 3), get(restaurantRow, 4));
        restaurant.setOwnerEmail(get(restaurantRow, 1));
        restaurant.setSupportedTypes(flag(get(restaurantRow, 5)), flag(get(restaurantRow, 6)), flag(get(restaurantRow, 7)));

        for (int i = 3; i < rows.size(); i++) {           // rows[2] is the item header
            String[] itemRow = rows.get(i);
            try {
                if (get(itemRow, 0).isEmpty()) continue;
                Category category = Category.valueOf(get(itemRow, 2).toUpperCase());
                double price = Double.parseDouble(get(itemRow, 3));
                int stock = get(itemRow, 4).isEmpty() ? 0 : Integer.parseInt(get(itemRow, 4));
                if (price <= 0 || stock < 0) throw new IllegalArgumentException("price/stock out of range");
                MenuItem item = new MenuItem(itemIds.getAsInt(), id, get(itemRow, 0), get(itemRow, 1), category, price, stock);
                item.setAvailable(flag(get(itemRow, 5)));
                for (String portionSize : get(itemRow, 6).split(";")) {
                    if (!portionSize.isBlank()) item.getPortionSizes().add(portionSize.trim());
                }
                for (String addOnEntry : get(itemRow, 7).split(";")) {
                    int colon = addOnEntry.lastIndexOf(':');
                    if (colon > 0) {
                        String addOnName = addOnEntry.substring(0, colon).trim();
                        double addOnPrice = Double.parseDouble(addOnEntry.substring(colon + 1).trim());
                        item.getAddOns().add(new AddOn(addOnName, addOnPrice));
                    }
                }
                if (!restaurant.addMenuItem(item)) {
                    System.err.println("! " + file.getFileName() + ": duplicate item '" + get(itemRow, 0) + "' skipped");
                }
            } catch (IllegalArgumentException e) {
                System.err.println("! " + file.getFileName() + " row " + (i + 1) + " skipped: " + e.getMessage());
            }
        }
        return restaurant;
    }

    public void register(int id, Path file) { files.put(id, file); }

    public void save(Restaurant restaurant) throws IOException {
        Files.createDirectories(folder);
        Path target = folder.resolve(String.format("%03d_%s.csv", restaurant.getId(), slug(restaurant.getName())));
        StringBuilder sb = new StringBuilder();
        sb.append(RESTAURANT_HEADER).append('\n');
        sb.append(line(String.valueOf(restaurant.getId()), restaurant.getOwnerEmail(), restaurant.getName(),
                restaurant.getDescription(), restaurant.getAddress(),
                String.valueOf(restaurant.supports(OrderType.DINE_IN)),
                String.valueOf(restaurant.supports(OrderType.TAKEOUT)),
                String.valueOf(restaurant.supports(OrderType.DELIVERY)))).append("\n\n");
        sb.append(ITEM_HEADER).append('\n');
        for (MenuItem item : restaurant.listAlphabetical()) {
            sb.append(line(item.getName(), item.getDescription(), item.getCategory().name(),
                    String.valueOf(item.getPrice()), String.valueOf(item.getStock()), String.valueOf(item.isAvailable()),
                    String.join(";", item.getPortionSizes()), addOns(item))).append('\n');
        }
        Files.writeString(target, sb.toString());
        Path previousFile = files.put(restaurant.getId(), target);
        if (previousFile != null && !previousFile.equals(target)) Files.deleteIfExists(previousFile);
    }

    public void delete(Restaurant restaurant) throws IOException {
        Path file = files.remove(restaurant.getId());
        if (file != null) Files.deleteIfExists(file);
    }

    private String addOns(MenuItem item) {
        List<String> entries = new ArrayList<>();
        for (AddOn addOn : item.getAddOns()) entries.add(addOn.getName() + ":" + addOn.getPrice());
        return String.join(";", entries);
    }

    private static String line(String... fields) { return Csv.formatLine(fields); }

    private static String get(String[] row, int index) { return index < row.length ? row[index] : ""; }

    private static boolean flag(String value) { return value.isEmpty() || Boolean.parseBoolean(value); }

    private static String slug(String name) {
        String cleaned = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return cleaned.isEmpty() ? "restaurant" : cleaned;
    }
}