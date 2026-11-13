package services;

import features.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

public class RestaurantStorage {
    private static final String RESTAURANT_HEADER = "restaurant_id,owner_email,name,description,address,dine_in,takeout,delivery";
    private static final String ITEM_HEADER = "item_name,description,category,price,stock,available,portion_sizes,add_ons";

    private static final int COLUMN_RESTAURANT_ID = 0;
    private static final int COLUMN_OWNER_EMAIL = 1;
    private static final int COLUMN_RESTAURANT_NAME = 2;
    private static final int COLUMN_RESTAURANT_DESCRIPTION = 3;
    private static final int COLUMN_RESTAURANT_ADDRESS = 4;
    private static final int COLUMN_DINE_IN = 5;
    private static final int COLUMN_TAKEOUT = 6;
    private static final int COLUMN_DELIVERY = 7;

    private static final int COLUMN_ITEM_NAME = 0;
    private static final int COLUMN_ITEM_DESCRIPTION = 1;
    private static final int COLUMN_ITEM_CATEGORY = 2;
    private static final int COLUMN_ITEM_PRICE = 3;
    private static final int COLUMN_ITEM_STOCK = 4;
    private static final int COLUMN_ITEM_AVAILABLE = 5;
    private static final int COLUMN_ITEM_PORTION_SIZES = 6;
    private static final int COLUMN_ITEM_ADD_ONS = 7;

    private final Path folder;
    private final Map<Integer, Path> filesByRestaurantId = new HashMap<>();

    public RestaurantStorage(String folderName) {
        this.folder = Paths.get(folderName);
    }

    public List<Path> listFiles() {
        List<Path> csvFiles = new ArrayList<>();
        if (!Files.isDirectory(folder)) {
            return csvFiles;
        }

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(folder, "*.csv")) {
            for (Path csvFile : directoryStream) {
                csvFiles.add(csvFile);
            }
        } catch (IOException exception) {
            System.err.println("! Could not read " + folder + ": " + exception.getMessage());
        }

        Collections.sort(csvFiles);
        return csvFiles;
    }

    private List<List<String>> readRows(Path filePath) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        boolean isFirstLine = true;

        for (String line : Files.readAllLines(filePath, StandardCharsets.UTF_8)) {
            if (isFirstLine) {
                line = line.replace("\uFEFF", "");
                isFirstLine = false;
            }

            boolean isBlankLine = line.trim().isEmpty() || line.replace(",", "").trim().isEmpty();
            if (isBlankLine) {
                continue;
            }
            rows.add(Csv.parseLine(line));
        }
        return rows;
    }

    public int readId(Path filePath) {
        try {
            List<List<String>> rows = readRows(filePath);
            if (rows.size() < 2) {
                return 0;
            }
            return Integer.parseInt(get(rows.get(1), COLUMN_RESTAURANT_ID));
        } catch (IOException | NumberFormatException exception) {
            return 0;
        }
    }

    public Restaurant read(Path filePath, int restaurantId, IntSupplier menuItemIds) throws IOException {
        List<List<String>> rows = readRows(filePath);
        if (rows.size() < 2) {
            throw new IOException("file needs a header row and a restaurant row");
        }

        List<String> restaurantRow = rows.get(1);
        if (get(restaurantRow, COLUMN_RESTAURANT_NAME).isEmpty()) {
            throw new IOException("restaurant name is empty");
        }

        Restaurant restaurant = new Restaurant(restaurantId, -1,
                get(restaurantRow, COLUMN_RESTAURANT_NAME),
                get(restaurantRow, COLUMN_RESTAURANT_DESCRIPTION),
                get(restaurantRow, COLUMN_RESTAURANT_ADDRESS));
        restaurant.setOwnerEmail(get(restaurantRow, COLUMN_OWNER_EMAIL));
        restaurant.setSupportedTypes(
                flag(get(restaurantRow, COLUMN_DINE_IN)),
                flag(get(restaurantRow, COLUMN_TAKEOUT)),
                flag(get(restaurantRow, COLUMN_DELIVERY)));

        for (int i = 3; i < rows.size(); i++) {
            List<String> itemRow = rows.get(i);
            try {
                String itemName = get(itemRow, COLUMN_ITEM_NAME);
                if (itemName.isEmpty()) {
                    continue;
                }

                Category category = Category.valueOf(get(itemRow, COLUMN_ITEM_CATEGORY).toUpperCase());
                double price = Double.parseDouble(get(itemRow, COLUMN_ITEM_PRICE));

                String stockText = get(itemRow, COLUMN_ITEM_STOCK);
                int stock = 0;
                if (!stockText.isEmpty()) {
                    stock = Integer.parseInt(stockText);
                }
                if (price <= 0 || stock < 0) {
                    throw new IllegalArgumentException("price/stock out of range");
                }

                MenuItem menuItem = new MenuItem(menuItemIds.getAsInt(), restaurantId, itemName,
                        get(itemRow, COLUMN_ITEM_DESCRIPTION), category, price, stock);
                menuItem.setAvailable(flag(get(itemRow, COLUMN_ITEM_AVAILABLE)));
                readPortionSizes(menuItem, get(itemRow, COLUMN_ITEM_PORTION_SIZES));
                readAddOns(menuItem, get(itemRow, COLUMN_ITEM_ADD_ONS));

                if (!restaurant.addMenuItem(menuItem)) {
                    System.err.println("Error: " + filePath.getFileName() + " duplicate item '" + itemName + "' skipped");
                }
            } catch (IllegalArgumentException exception) {
                System.err.println("Error: " + filePath.getFileName() + " row " + (i + 1) + " skipped: " + exception.getMessage());
            }
        }
        return restaurant;
    }

    private void readPortionSizes(MenuItem menuItem, String portionSizesText) {
        for (String portionSize : portionSizesText.split(";")) {
            if (!portionSize.trim().isEmpty()) {
                menuItem.getPortionSizes().add(portionSize.trim());
            }
        }
    }

    private void readAddOns(MenuItem menuItem, String addOnsText) {
        for (String addOnText : addOnsText.split(";")) {
            int colonPosition = addOnText.lastIndexOf(':');
            if (colonPosition > 0) {
                String addOnName = addOnText.substring(0, colonPosition).trim();
                double addOnPrice = Double.parseDouble(addOnText.substring(colonPosition + 1).trim());
                menuItem.getAddOns().add(new AddOn(addOnName, addOnPrice));
            }
        }
    }

    public void register(int restaurantId, Path filePath) {
        filesByRestaurantId.put(restaurantId, filePath);
    }

    public void save(Restaurant restaurant) throws IOException {
        Files.createDirectories(folder);
        Path targetFile = folder.resolve(String.format("%03d_%s.csv", restaurant.getId(), slug(restaurant.getName())));

        StringBuilder csvContent = new StringBuilder();
        csvContent.append(RESTAURANT_HEADER).append('\n');
        csvContent.append(Csv.join(Arrays.asList(
                String.valueOf(restaurant.getId()),
                restaurant.getOwnerEmail(),
                restaurant.getName(),
                restaurant.getDescription(),
                restaurant.getAddress(),
                String.valueOf(restaurant.supports(OrderType.DINE_IN)),
                String.valueOf(restaurant.supports(OrderType.TAKEOUT)),
                String.valueOf(restaurant.supports(OrderType.DELIVERY))))).append("\n\n");

        csvContent.append(ITEM_HEADER).append('\n');
        for (MenuItem menuItem : restaurant.listAlphabetical()) {
            csvContent.append(Csv.join(Arrays.asList(
                    menuItem.getName(),
                    menuItem.getDescription(),
                    menuItem.getCategory().name(),
                    String.valueOf(menuItem.getPrice()),
                    String.valueOf(menuItem.getStock()),
                    String.valueOf(menuItem.isAvailable()),
                    String.join(";", menuItem.getPortionSizes()),
                    addOnsToText(menuItem)))).append('\n');
        }

        Files.write(targetFile, csvContent.toString().getBytes(StandardCharsets.UTF_8));

        Path previousFile = filesByRestaurantId.put(restaurant.getId(), targetFile);
        if (previousFile != null && !previousFile.equals(targetFile)) {
            Files.deleteIfExists(previousFile);
        }
    }

    public void delete(Restaurant restaurant) throws IOException {
        Path restaurantFile = filesByRestaurantId.remove(restaurant.getId());
        if (restaurantFile != null) {
            Files.deleteIfExists(restaurantFile);
        }
    }

    private String addOnsToText(MenuItem menuItem) {
        List<String> addOnTexts = new ArrayList<>();
        for (AddOn addOn : menuItem.getAddOns()) {
            addOnTexts.add(addOn.getName() + ":" + addOn.getPrice());
        }
        return String.join(";", addOnTexts);
    }

    private static String get(List<String> row, int columnIndex) {
        if (columnIndex < row.size()) {
            return row.get(columnIndex);
        }
        return "";
    }

    private static boolean flag(String text) {
        return text.isEmpty() || Boolean.parseBoolean(text);
    }

    private static String slug(String name) {
        String slugText = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (slugText.isEmpty()) {
            return "restaurant";
        }
        return slugText;
    }
}