import features.*;
import features.Order;
import services.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class FoodBitesApp {
    private final Scanner scanner = new Scanner(System.in);
    private final DataStoring store = new DataStoring();
    private final Authentication authentication = new Authentication(store);
    private final Menu menu = new Menu(store);
    private final services.OrderService orders = new services.OrderService(store, new BillCalculator(), new PaymentService());
    private final services.SalesReport reports = new services.SalesReport(store);

    private String readLine(String prompt) {
        System.out.print(prompt);
        if (scanner.hasNextLine()) {
            return scanner.nextLine().trim();
        } else {
            return "";
        }
    }

    private int readInt(String prompt) {
        while (true) {
            if (!scanner.hasNextLine()) {
                System.out.println();
                System.exit(0);
            }
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException exception) {
                System.out.println("Please enter an integer.");
            }
        }
    }

    private double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException exception) {
                System.out.println("Please enter a number.");
            }
        }
    }

    private Category readCategory() {
        System.out.println("Category");
        System.out.println("\t1. Meal");
        System.out.println("\t2. Snack");
        System.out.println("\t3. Dessert");
        System.out.println("\t4. Drink");
        int categoryChoice = readInt("Choice: ");
        return categoryChoice == 2 ? Category.SNACK : categoryChoice == 3 ? Category.DESSERT : categoryChoice == 4 ? Category.DRINK : Category.MEAL;
    }

    private void register() {
        System.out.println("User Role");
        System.out.println("\t1. Customer");
        System.out.println("\t2. Delivery rider");
        System.out.println("\t3. Business owner");
        System.out.println("\t4. Employee");
        int roleChoice = readInt("Choice: ");

        UserRole role = switch (roleChoice) {
            case 2 -> UserRole.DELIVERY_RIDER;
            case 3 -> UserRole.BUSINESS_OWNER;
            case 4 -> UserRole.EMPLOYEE;
            default -> UserRole.CUSTOMER;
        };

        int restaurantId = -1;
        if (role == UserRole.EMPLOYEE) {
            restaurantId = readInt("Restaurant ID you work for: ");
        }
        String name = readLine("Name: ");
        String email = readLine("Email: ");
        String phone = readLine("Phone: ");
        String password = readLine("Password: ");
        User newUser = authentication.register(name, email, phone, password, role, restaurantId);
        System.out.println("Registered " + newUser.getName() + ". You can now log in.");
    }

    private void login() {
        String email = readLine("Email: ");
        String password = readLine("Password: ");
        User loggedInUser = authentication.login(email, password);
        System.out.println("Welcome, " + loggedInUser.getName() + "!");
    }

    private void printCategory(Map<Category, List<MenuItem>> groupedItems) {
        for (Map.Entry<Category, List<MenuItem>> entry : groupedItems.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            System.out.println("== " + entry.getKey() + " ==");
            for (MenuItem menuItem : entry.getValue()) {
                printItem(menuItem);
            }
        }
    }

    private void printItems(List<MenuItem> items) {
        if (items.isEmpty()) {
            System.out.println("No items match your search.");
        }
        for (MenuItem menuItem : items) {
            printItem(menuItem);
        }
    }

    private void printItem(MenuItem menuItem) {
        System.out.println("  " + menuItem);
        String sizesText = menuItem.getPortionSizes().isEmpty() ? "" : " | Sizes: " + menuItem.getPortionSizes();
        String addOnsText = menuItem.getAddOns().isEmpty() ? "" : " | Add-ons: " + menuItem.getAddOns();
        System.out.println("       " + menuItem.getDescription() + sizesText + addOnsText);
    }

    private void browse() {
        List<Restaurant> restaurantList = menu.browseRestaurants();
        for (Restaurant restaurant : restaurantList) {
            System.out.println(restaurant);
        }
        int restaurantId = readInt("Restaurant ID to open (Press 0 to go back): ");
        if (restaurantId == 0) {
            return;
        }
        Restaurant restaurant = menu.getRestaurant(restaurantId);
        while (true) {
            System.out.println("\n" + restaurant.getName());
            System.out.println("\t0. Back");
            System.out.println("\t1. View full menu");
            System.out.println("\t2. Search by name");
            System.out.println("\t3. Filter by category");
            System.out.println("\t4. Filter by price range");
            System.out.println("\t5. Sort A to Z");
            System.out.println("\t6. Sort lowest to highest price");
            System.out.println("\t7. Sort highest to lowest price");
            System.out.println("\t8. Popularity");
            int menuChoice = readInt("Choice: ");

            if (menuChoice == 0) {
                return;
            }
            List<MenuItem> result = null;
            switch (menuChoice) {
                case 1:
                    printCategory(menu.browseMenu(restaurant));
                    continue;
                case 2:
                    String searchText = readLine("Search: ");
                    result = menu.search(restaurant, searchText);
                    break;
                case 3:
                    Category category = readCategory();
                    result = menu.filterByCategory(restaurant, category);
                    break;
                case 4:
                    double minPrice = readDouble("Min price: ");
                    double maxPrice = readDouble("Max price: ");
                    result = menu.filterByPrice(restaurant, minPrice, maxPrice);
                    break;
                case 5:
                    result = menu.sort(restaurant, SortFeature.ALPHABETICAL);
                    break;
                case 6:
                    result = menu.sort(restaurant, SortFeature.PRICE_LOW_TO_HIGH);
                    break;
                case 7:
                    result = menu.sort(restaurant, SortFeature.PRICE_HIGH_TO_LOW);
                    break;
                case 8:
                    result = menu.sort(restaurant, SortFeature.POPULARITY);
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
            if (result != null) {
                printItems(result);
            }
        }
    }

    private void addToCart(Customer customer) {
        for (Restaurant restaurant : menu.browseRestaurants()) {
            System.out.println(restaurant);
        }
        int restaurantId = readInt("Restaurant ID: ");
        Restaurant restaurant = menu.getRestaurant(restaurantId);
        printItems(restaurant.listAlphabetical());
        String itemName = readLine("Item name: ");
        MenuItem item = restaurant.findByName(itemName);
        if (item == null) {
            throw new IllegalArgumentException("Item not found.");
        }
        int quantity = readInt("Quantity: ");
        String portion = "";
        if (!item.getPortionSizes().isEmpty()) {
            portion = readLine("Portion " + item.getPortionSizes() + ": ");
        }
        List<AddOn> chosenAddOns = new ArrayList<>();
        for (AddOn addOn : item.getAddOns()) {
            String addOnAnswer = readLine("Add " + addOn + "? (y/n): ");
            if (addOnAnswer.equalsIgnoreCase("y")) {
                chosenAddOns.add(addOn);
            }
        }
        customer.getCart().addItem(item, quantity, portion, chosenAddOns);
        String subtotalText = String.format("%.2f", customer.getCart().getSubtotal());
        System.out.println("Added. Cart subtotal: PHP " + subtotalText);
    }

    private void printCart(Cart cart) {
        if (cart.isEmpty()) {
            System.out.println("Your cart is empty.");
            return;
        }
        int lineNumber = 1;
        for (CartItem cartItem : cart.getItems()) {
            System.out.println("  " + (lineNumber++) + ". " + cartItem);
        }
        System.out.printf("  Running subtotal: PHP %.2f%n", cart.getSubtotal());
    }
}