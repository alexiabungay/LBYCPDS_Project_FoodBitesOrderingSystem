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

    private void editCart(Customer customer) {
        printCart(customer.getCart());
        if (customer.getCart().isEmpty()) {
            return;
        }
        System.out.println("0. Back");
        System.out.println("1. Change quantity");
        System.out.println("2. Remove item");
        System.out.println("3. Clear cart");
        int editChoice = readInt("Choice: ");
        switch (editChoice) {
            case 1:
                int quantityLineNumber = readInt("Line number: ") - 1;
                int newQuantity = readInt("New quantity: ");
                customer.getCart().updateQuantity(quantityLineNumber, newQuantity);
                break;
            case 2:
                int removeLineNumber = readInt("Line number: ") - 1;
                customer.getCart().removeItem(removeLineNumber);
                break;
            case 3:
                customer.getCart().clear();
                break;
            default:
                return;
        }
        printCart(customer.getCart());
    }

    private void checkout(Customer customer) {
        printCart(customer.getCart());
        if (customer.getCart().isEmpty()) {
            return;
        }
        System.out.println("Fulfillment");
        System.out.println("\t1. Dine-in pre-order");
        System.out.println("\t2. Takeout pickup");
        System.out.println("\t3. Delivery");
        int fulfillmentChoice = readInt("Choice: ");
        OrderType orderType = fulfillmentChoice == 1 ? OrderType.DINE_IN : fulfillmentChoice == 2 ? OrderType.TAKEOUT : OrderType.DELIVERY;
        String address = "";
        String preferredTime = "";
        if (orderType == OrderType.DELIVERY) {
            address = readLine("Delivery address [" + customer.getAddress() + "]: ");
            if (address.isEmpty()) {
                address = customer.getAddress();
            }
        }
        preferredTime = readLine("Preferred time (optional): ");
        String specialInstructions = readLine("Special instructions (optional): ");
        String promoCode = readLine("Promo code (optional): ");
        System.out.println("\nBILL\n" + orders.previewBill(customer, orderType, promoCode));
        System.out.println("Payment");
        System.out.println("\t1. Cash on delivery");
        System.out.println("\t2. Cash at counter");
        System.out.println("\t3. Digital");
        int paymentChoice = readInt("Choice: ");
        PaymentMethod paymentMethod = paymentChoice == 1 ? PaymentMethod.CASH_ON_DELIVERY : paymentChoice == 2 ? PaymentMethod.CASH_AT_COUNTER : PaymentMethod.DIGITAL;
        String confirmAnswer = readLine("Confirm order? (y/n): ");
        if (!confirmAnswer.equalsIgnoreCase("y")) {
            System.out.println("Order not placed.");
            return;
        }

        DeliveryDetails deliveryDetails = new DeliveryDetails(orderType, address, preferredTime, specialInstructions);
        Order placedOrder = orders.checkout(customer, deliveryDetails, paymentMethod, promoCode, null);
        System.out.println(placedOrder.getPayment().receipt());
        if (placedOrder.getStatus() == OrderStatus.FAILED) {
            System.out.println("Payment failed, order was not confirmed.");
        } else {
            int queuePosition = orders.getQueuePosition(placedOrder);
            System.out.println("Order #" + placedOrder.getId() + " confirmed! Your position in the queue: " + queuePosition);
        }
    }

    private void printOrders(List<Order> orderList) {
        if (orderList.isEmpty()) {
            System.out.println("No orders.");
        }
        for (Order order : orderList) {
            System.out.println("  " + order);
        }
    }

    private void guard(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException | IllegalStateException | SecurityException exception) {
            System.out.println("! " + exception.getMessage());
        }
    }

    private void customerMenu(Customer customer) {
        System.out.println("1. Browse/search menus");
        System.out.println("2. Add to cart");
        System.out.println("3. View/edit cart");
        System.out.println("4. Checkout");
        System.out.println("5. Track an order");
        System.out.println("6. My orders");
        System.out.println("7. Cancel an order");
        System.out.println("8. Logout");
        guard(() -> {
            int customerChoice = readInt("Choice: ");
            switch (customerChoice) {
                case 1:
                    browse();
                    break;
                case 2:
                    addToCart(customer);
                    break;
                case 3:
                    editCart(customer);
                    break;
                case 4:
                    checkout(customer);
                    break;
                case 5:
                    int trackOrderId = readInt("Order ID: ");
                    System.out.println(orders.trackOrder(customer, trackOrderId));
                    break;
                case 6:
                    for (Order order : orders.getOrdersOf(customer)) {
                        System.out.println(order);
                    }
                    break;
                case 7:
                    int cancelOrderId = readInt("Order ID: ");
                    System.out.println(orders.cancelOrder(customer, cancelOrderId));
                    break;
                case 8:
                    authentication.logout();
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        });
    }

    private void employeeMenu(Employee employee) {
        System.out.println("1. View kitchen queue");
        System.out.println("2. Mark order as preparing front order");
        System.out.println("3. Mark order as ready");
        System.out.println("4. Mark order as done");
        System.out.println("5. Logout");
        guard(() -> {
            int employeeChoice = readInt("Choice: ");
            switch (employeeChoice) {
                case 1:
                    List<Order> kitchenQueue = orders.viewKitchenQueue(employee);
                    if (kitchenQueue.isEmpty()) {
                        System.out.println("Kitchen queue is empty.");
                    }
                    int queuePosition = 1;
                    for (Order queuedOrder : kitchenQueue) {
                        System.out.println("  " + (queuePosition++) + ". " + queuedOrder);
                    }
                    break;
                case 2:
                    System.out.println("Preparing: " + orders.startPreparing(employee));
                    break;
                case 3:
                    System.out.println("Ready: " + orders.markReady(employee));
                    break;
                case 4:
                    int completeOrderId = readInt("Order ID: ");
                    System.out.println("Done: " + orders.completeOrder(employee, completeOrderId));
                    break;
                case 5:
                    authentication.logout();
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        });
    }

    private void riderMenu(DeliveryRider rider) {
        System.out.println("1. View pending deliveries");
        System.out.println("2. Accept next delivery");
        System.out.println("3. Confirm delivered");
        System.out.println("4. Report failed delivery");
        System.out.println("5. Logout");
        guard(() -> {
            int riderChoice = readInt("Choice: ");
            switch (riderChoice) {
                case 1:
                    List<Order> pendingDeliveries = orders.pendingDeliveries();
                    if (pendingDeliveries.isEmpty()) {
                        System.out.println("No pending deliveries.");
                    }
                    for (Order pendingOrder : pendingDeliveries) {
                        System.out.println("  " + pendingOrder + " - " + pendingOrder.getDetails().getAddress());
                    }
                    break;
                case 2:
                    System.out.println("Out for delivery: " + orders.acceptNextDelivery(rider));
                    break;
                case 3:
                    int deliveredOrderId = readInt("Order ID: ");
                    System.out.println(orders.confirmDelivery(rider, deliveredOrderId, true));
                    break;
                case 4:
                    int failedOrderId = readInt("Order ID: ");
                    System.out.println(orders.confirmDelivery(rider, failedOrderId, false));
                    break;
                case 5:
                    authentication.logout();
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        });
    }

    private void managerMenu(User user) {
        System.out.println("1. Add restaurant");
        System.out.println("2. Edit restaurant");
        System.out.println("3. Remove restaurant");
        System.out.println("4. Add menu item");
        System.out.println("5. Edit menu item");
        System.out.println("6. Update price");
        System.out.println("7. Remove menu item");
        System.out.println("8. Set availability");
        System.out.println("9. Update stock");
        System.out.println("10. View active orders");
        System.out.println("11. View completed orders");
        System.out.println("12. Cancel order");
        System.out.println("13. Refund order");
        System.out.println("14. Sales report");
        System.out.println("15. Inventory report");
        System.out.println("16. Best sellers");
        System.out.println("17. Rank restaurants ");
        System.out.println("18. View restaurants");
        System.out.println("19. Logout");
        guard(() -> {
            int restaurantId;
            int managerChoice = readInt("Choice: ");
            switch (managerChoice) {
                case 1:
                    String newRestaurantName = readLine("Name: ");
                    String newRestaurantDescription = readLine("Description: ");
                    String newRestaurantAddress = readLine("Address: ");
                    Restaurant createdRestaurant = menu.createRestaurant(user, newRestaurantName, newRestaurantDescription, newRestaurantAddress);
                    System.out.println("Created: " + createdRestaurant);
                    break;
                case 2:
                    restaurantId = readInt("Restaurant ID: ");
                    String updatedName = readLine("Name: ");
                    String updatedDescription = readLine("Description: ");
                    String updatedAddress = readLine("Address: ");
                    menu.updateRestaurant(user, restaurantId, updatedName, updatedDescription, updatedAddress);
                    break;
                case 3:
                    restaurantId = readInt("Restaurant ID: ");
                    menu.removeRestaurant(user, restaurantId);
                    break;
                case 4:
                    restaurantId = readInt("Restaurant ID: ");
                    String newItemName = readLine("Name: ");
                    String newItemDescription = readLine("Description: ");
                    Category newItemCategory = readCategory();
                    double newItemPrice = readDouble("Price: ");
                    int newItemStock = readInt("Stock: ");
                    MenuItem addedItem = menu.addItem(user, restaurantId, newItemName, newItemDescription, newItemCategory, newItemPrice, newItemStock);
                    System.out.println("Added: " + addedItem);
                    break;
                case 5:
                    restaurantId = readInt("Restaurant ID: ");
                    String currentItemName = readLine("Current name: ");
                    String newName = readLine("New name: ");
                    String editedDescription = readLine("Description: ");
                    Category editedCategory = readCategory();
                    double editedPrice = readDouble("Price: ");
                    menu.editItem(user, restaurantId, currentItemName, newName, editedDescription, editedCategory, editedPrice);
                    break;
                case 6:
                    restaurantId = readInt("Restaurant ID: ");
                    String priceItemName = readLine("Item name: ");
                    double newPrice = readDouble("New price: ");
                    menu.updatePrice(user, restaurantId, priceItemName, newPrice);
                    break;
                case 7:
                    restaurantId = readInt("Restaurant ID: ");
                    String removeItemName = readLine("Item name: ");
                    menu.removeItem(user, restaurantId, removeItemName);
                    break;
                case 8:
                    restaurantId = readInt("Restaurant ID: ");
                    String availabilityItemName = readLine("Item name: ");
                    String availabilityAnswer = readLine("Available? (y/n): ");
                    menu.setAvailability(user, restaurantId, availabilityItemName, availabilityAnswer.equalsIgnoreCase("y"));
                    break;
                case 9:
                    restaurantId = readInt("Restaurant ID: ");
                    String stockItemName = readLine("Item name: ");
                    int newStock = readInt("New stock: ");
                    menu.updateStock(user, restaurantId, stockItemName, newStock);
                    break;
                case 10:
                    restaurantId = readInt("Restaurant ID: ");
                    printOrders(orders.listOrders(user, restaurantId, true));
                    break;
                case 11:
                    restaurantId = readInt("Restaurant ID: ");
                    printOrders(orders.listOrders(user, restaurantId, false));
                    break;
                case 12:
                    int cancelOrderId = readInt("Order ID: ");
                    System.out.println(orders.cancelOrder(user, cancelOrderId));
                    break;
                case 13:
                    int refundOrderId = readInt("Order ID: ");
                    System.out.println(orders.refundOrder(user, refundOrderId));
                    break;
                case 14:
                    restaurantId = readInt("Restaurant ID: ");
                    System.out.println("1. Daily  2. Weekly  3. Monthly");
                    int periodChoice = readInt("Choice: ");
                    ReportType reportType = periodChoice == 1 ? ReportType.DAILY : periodChoice == 2 ? ReportType.WEEKLY : ReportType.MONTHLY;
                    System.out.println(reports.generate(user, restaurantId, reportType, LocalDate.now()));
                    break;
                case 15:
                    restaurantId = readInt("Restaurant ID: ");
                    System.out.println(reports.inventoryReport(user, restaurantId));
                    break;
                case 16:
                    restaurantId = readInt("Restaurant ID: ");
                    Restaurant bestSellerRestaurant = menu.getRestaurant(restaurantId);
                    int bestSellerRank = 1;
                    for (MenuItem bestSellingItem : reports.bestSellers(bestSellerRestaurant)) {
                        System.out.printf("  %d. %-22s %d sold%n", bestSellerRank++, bestSellingItem.getName(), bestSellingItem.getTotalSales());
                    }
                    break;
                case 17:
                    int restaurantRank = 1;
                    for (Restaurant rankedRestaurant : reports.rankRestaurants(user)) {
                        System.out.printf("  %d. %-22s PHP %.2f%n", restaurantRank++, rankedRestaurant.getName(), rankedRestaurant.getTotalSales());
                    }
                    break;
                case 18:
                    for (Restaurant restaurantToShow : menu.browseRestaurants()) {
                        System.out.println(restaurantToShow);
                    }
                    break;
                case 19:
                    authentication.logout();
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        });
    }
}