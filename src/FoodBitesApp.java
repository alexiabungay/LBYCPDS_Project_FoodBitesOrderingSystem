import features.*;
import features.Order;
import services.*;
import data.structure.AVLTree;
import data.structure.HeapSort;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

public class FoodBitesApp {
    private static final String CANCEL = "0";

    private final Scanner scanner = new Scanner(System.in);
    private final DataStoring store = new DataStoring();
    private final Authentication authentication = new Authentication(store);
    private final Menu menu = new Menu(store);
    private final services.OrderService orders = new services.OrderService(store, new BillCalculator(), new PaymentService());
    private final services.SalesReport reports = new services.SalesReport(store);

    private void screen(String title) {
        ClearScreen.clear();
        System.out.println(title);
    }

    private void pause() {
        ClearScreen.pause(scanner);
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            System.out.println();
            System.exit(0);
        }
        return scanner.nextLine().trim();
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine()) {
                System.out.println();
                System.exit(0);
            }
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException exception) {
                System.out.println("Please enter an integer.");
            }
        }
    }

    private int readChoice(String prompt, int min, int max) {
        while (true) {
            int choice = readInt(prompt);
            if (choice >= min && choice <= max) {
                return choice;
            }
            System.out.println("Please choose a number from " + min + " to " + max + ".");
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

    private String categoryLabel(Category category) {
        String name = category.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }

    private String askField(String title, List<String> accepted, String[] hints, String label,
                            boolean hidden, Function<String, String> check) {
        String error = null;
        while (true) {
            System.out.println("\n" + title + " (Enter 0 to cancel)");
            for (String line : accepted) {
                System.out.println(line);
            }
            if (hints != null) {
                for (String hint : hints) {
                    System.out.println(hint);
                }
            }
            if (error != null) {
                System.out.println("! " + error);
            }
            String input = readLine(label + ": ");
            if (input.equals(CANCEL)) {
                return null;
            }
            error = check.apply(input);
            if (error == null) {
                return input;
            }
        }
    }

    private boolean register() {
        String title = "Register";
        List<String> accepted = new ArrayList<>();

        String[] roleHints = {"User Role", "\t1. Customer", "\t2. Delivery rider", "\t3. Business owner", "\t4. Employee"};
        String roleInput = askField(title, accepted, roleHints, "Choice", false,
                text -> text.matches("[1-4]") ? null : "Please enter a number from 1 to 4.");
        if (roleInput == null) {
            return false;
        }
        UserRole role;
        String roleLabel;
        switch (Integer.parseInt(roleInput)) {
            case 2:
                role = UserRole.DELIVERY_RIDER;
                roleLabel = "Delivery rider";
                break;
            case 3:
                role = UserRole.BUSINESS_OWNER;
                roleLabel = "Business owner";
                break;
            case 4:
                role = UserRole.EMPLOYEE;
                roleLabel = "Employee";
                break;
            default:
                role = UserRole.CUSTOMER;
                roleLabel = "Customer";
        }
        accepted.add("Role: " + roleLabel);

        int restaurantId = -1;
        if (role == UserRole.EMPLOYEE) {
            List<String> restaurantHints = new ArrayList<>();
            restaurantHints.add("Restaurants:");
            for (Restaurant restaurant : menu.browseRestaurants()) {
                restaurantHints.add("  " + restaurant);
            }
            String restaurantInput = askField(title, accepted, restaurantHints.toArray(new String[0]),
                    "Restaurant ID you work for", false, text -> {
                        try {
                            return store.getRestaurants().search(Integer.parseInt(text)) != null
                                    ? null : "No restaurant has that ID.";
                        } catch (NumberFormatException exception) {
                            return "Please enter the restaurant's ID number.";
                        }
                    });
            if (restaurantInput == null) {
                return false;
            }
            restaurantId = Integer.parseInt(restaurantInput);
            accepted.add("Restaurant ID: " + restaurantId);
        }

        String name = askField(title, accepted, null, "Name", false,
                text -> Validator.isNotBlank(text) ? null : "Name is required.");
        if (name == null) {
            return false;
        }
        accepted.add("Name: " + name);

        String email = askField(title, accepted, null, "Email", false, text -> {
            if (!Validator.isValidEmail(text)) {
                return "Invalid email address.";
            }
            if (authentication.isEmailRegistered(text)) {
                return "That email is already registered.";
            }
            return null;
        });
        if (email == null) {
            return false;
        }
        accepted.add("Email: " + email);

        String[] phoneHints = {"(Example: 09171234567 or +639171234567)"};
        String phone = askField(title, accepted, phoneHints, "Phone", false,
                text -> Validator.isValidPhone(text) ? null : "Phone number is invalid.");
        if (phone == null) {
            return false;
        }
        accepted.add("Phone: " + phone);

        String[] passwordHints = {"(At least 8 characters with a letter and a number)"};
        String password = askField(title, accepted, passwordHints, "Password", true,
                text -> Validator.isValidPassword(text) ? null
                        : "Password needs at least 8 characters with a letter and a number.");
        if (password == null) {
            return false;
        }

        User newUser = authentication.register(name, email, phone, password, role, restaurantId);
        screen(title);
        System.out.println("Registered " + newUser.getName() + ". You can now log in.");
        return true;
    }

    private boolean login() {
        String title = "Login";
        List<String> accepted = new ArrayList<>();

        String email = askField(title, accepted, null, "Email", false,
                text -> authentication.isEmailRegistered(text) ? null : "No account is registered with that email.");
        if (email == null) {
            return false;
        }
        accepted.add("Email: " + email);

        askField(title, accepted, null, "Password", true, text -> {
            try {
                authentication.login(email, text);
                return null;
            } catch (IllegalArgumentException exception) {
                return "Incorrect password.";
            }
        });

        User currentUser = authentication.getCurrentUser();
        System.out.println("\nSuccessfully Logged In. Welcome, " + currentUser.getRoleDescription() + " " + currentUser.getName() + "!");

        return false;
    }

    private Category chooseCategory(Restaurant restaurant) {
        Category[] categories = Category.values();
        screen(restaurant.getName() + " - Menu Categories");
        for (int index = 0; index < categories.length; index++) {
            int count = restaurant.filterByCategory(categories[index]).size();
            System.out.printf("\t%d. %-8s (%d item%s)%n", index + 1, categoryLabel(categories[index]),
                    count, count == 1 ? "" : "s");
        }
        System.out.println("\t0. Back");
        while (true) {
            int choice = readInt("Choice: ");
            if (choice == 0) {
                return null;
            }
            if (choice >= 1 && choice <= categories.length) {
                return categories[choice - 1];
            }
            System.out.println("Please choose a number from 0 to " + categories.length + ".");
        }
    }

    private void browseByCategory(Restaurant restaurant) {
        while (true) {
            Category category = chooseCategory(restaurant);
            if (category == null) {
                return;
            }
            screen(restaurant.getName() + " - " + categoryLabel(category));
            printItems(restaurant.filterByCategory(category), false);
            pause();
        }
    }

    private void printItems(List<MenuItem> items, boolean showCategory) {
        if (items.isEmpty()) {
            System.out.println("No items to show.");
            return;
        }
        System.out.println(DisplayMenu.forMenu(items, showCategory));
        System.out.println(items.size() + " item(s)");
    }

    private MenuItem findMenuItem(Restaurant restaurant, String input) {
        if (input.matches("\\d{1,9}")) {
            int itemId = Integer.parseInt(input);
            for (MenuItem menuItem : restaurant.listAlphabetical()) {
                if (menuItem.getId() == itemId) {
                    return menuItem;
                }
            }
        }
        return restaurant.findByName(input);
    }

    private void showItemDetails(Restaurant restaurant) {
        String input = readLine("Item ID or name: ");
        MenuItem item = findMenuItem(restaurant, input);
        if (item == null) {
            System.out.println("Item not found.");
            return;
        }
        screen(item.getName());
        System.out.println("Category : " + categoryLabel(item.getCategory()));
        System.out.printf("Price    : PHP %.2f%n", item.getPrice());
        System.out.println("Stock    : " + item.getStock()
                + (item.isAvailable() && item.getStock() > 0 ? " (Available)" : " (Unavailable)"));
        if (Validator.isNotBlank(item.getDescription())) {
            System.out.println("Details  : " + item.getDescription());
        }
        if (!item.getPortionSizes().isEmpty()) {
            System.out.println("Sizes    : " + String.join(", ", item.getPortionSizes()));
        }
        if (!item.getAddOns().isEmpty()) {
            System.out.println("\nAdd-ons");
            System.out.println(DisplayMenu.forAddOns(item.getAddOns()));
        }
    }

    private void browse() {
        screen("Restaurants");
        for (Restaurant listedRestaurant : menu.browseRestaurants()) {
            System.out.println(listedRestaurant);
        }
        int restaurantId = readInt("Restaurant ID to open (0 to go back): ");
        if (restaurantId == 0) {
            return;
        }
        Restaurant restaurant = menu.getRestaurant(restaurantId);
        while (true) {
            screen(restaurant.getName());
            System.out.println("\t1. Browse by category");
            System.out.println("\t2. Search by name");
            System.out.println("\t3. Filter by price range");
            System.out.println("\t4. Sort A to Z");
            System.out.println("\t5. Sort lowest to highest price");
            System.out.println("\t6. Sort highest to lowest price");
            System.out.println("\t7. Popularity");
            System.out.println("\t8. View item details");
            System.out.println("\t9. Back");
            int menuChoice = readInt("Choice: ");

            if (menuChoice == 9) {
                return;
            }
            ClearScreen.clear();
            List<MenuItem> result = null;
            String heading = "";
            switch (menuChoice) {
                case 1:
                    browseByCategory(restaurant);
                    continue;
                case 2:
                    String searchText = readLine("Search: ");
                    result = menu.search(restaurant, searchText);
                    heading = "Search results for \"" + searchText + "\"";
                    break;
                case 3:
                    double minPrice = readDouble("Min price: ");
                    double maxPrice = readDouble("Max price: ");
                    result = menu.filterByPrice(restaurant, minPrice, maxPrice);
                    heading = String.format("Items from PHP %.2f to PHP %.2f", minPrice, maxPrice);
                    break;
                case 4:
                    result = menu.sort(restaurant, SortFeature.ALPHABETICAL);
                    heading = "All items, A to Z";
                    break;
                case 5:
                    result = menu.sort(restaurant, SortFeature.PRICE_LOW_TO_HIGH);
                    heading = "All items, lowest to highest price";
                    break;
                case 6:
                    result = menu.sort(restaurant, SortFeature.PRICE_HIGH_TO_LOW);
                    heading = "All items, highest to lowest price";
                    break;
                case 7:
                    result = menu.sort(restaurant, SortFeature.POPULARITY);
                    heading = "All items by popularity";
                    break;
                case 8:
                    showItemDetails(restaurant);
                    pause();
                    continue;
                default:
                    System.out.println("Invalid choice.");
                    pause();
                    continue;
            }
            screen(restaurant.getName() + " - " + heading);
            printItems(result, true);
            pause();
        }
    }

    private void printNumberedItems(List<MenuItem> items) {
        if (items.isEmpty()) {
            System.out.println("No items to show.");
            return;
        }
        System.out.println(DisplayMenu.forOrdering(items));
        System.out.println(items.size() + " item(s)");
    }

    private MenuItem chooseItemByNumber(List<MenuItem> items) {
        if (items.isEmpty()) {
            return null;
        }
        while (true) {
            int choice = readInt("Item number (0 to go back): ");
            if (choice == 0) {
                return null;
            }
            if (choice >= 1 && choice <= items.size()) {
                return items.get(choice - 1);
            }
            System.out.println("Please choose a number from 0 to " + items.size() + ".");
        }
    }

    private String choosePortion(MenuItem item) {
        List<String> portionSizes = item.getPortionSizes();
        if (portionSizes.isEmpty()) {
            return "";
        }
        System.out.println("Portion sizes");
        for (int index = 0; index < portionSizes.size(); index++) {
            System.out.printf("\t%d. %s%n", index + 1, portionSizes.get(index));
        }
        int choice = readChoice("Portion (0 for no preference): ", 0, portionSizes.size());
        return choice == 0 ? "" : portionSizes.get(choice - 1);
    }

    private List<AddOn> chooseAddOns(MenuItem item) {
        List<AddOn> chosenAddOns = new ArrayList<>();
        List<AddOn> availableAddOns = item.getAddOns();
        if (availableAddOns.isEmpty()) {
            return chosenAddOns;
        }
        System.out.println("Upgrades / add-ons");
        for (int index = 0; index < availableAddOns.size(); index++) {
            System.out.printf("\t%d. %s%n", index + 1, availableAddOns.get(index));
        }
        System.out.println("\t0. Done choosing upgrades");
        while (true) {
            int choice = readInt("Upgrade number: ");
            if (choice == 0) {
                return chosenAddOns;
            }
            if (choice < 1 || choice > availableAddOns.size()) {
                System.out.println("Please choose a number from 0 to " + availableAddOns.size() + ".");
                continue;
            }
            AddOn addOn = availableAddOns.get(choice - 1);
            if (containsAddOn(chosenAddOns, addOn)) {
                System.out.println("That upgrade is already selected.");
                continue;
            }
            chosenAddOns.add(addOn);
            System.out.println("Added upgrade: " + addOn.getName());
        }
    }

    private boolean containsAddOn(List<AddOn> addOns, AddOn candidate) {
        for (AddOn addOn : addOns) {
            if (addOn.getName().equalsIgnoreCase(candidate.getName())) {
                return true;
            }
        }
        return false;
    }

    private boolean addToCart(Customer customer) {
        screen("Add to Cart");
        for (Restaurant listedRestaurant : menu.browseRestaurants()) {
            System.out.println(listedRestaurant);
        }
        int restaurantId = readInt("Restaurant ID (0 to go back): ");
        if (restaurantId == 0) {
            return false;
        }
        Restaurant restaurant = menu.getRestaurant(restaurantId);

        boolean addedAnyItem = false;
        while (true) {
            Category category = chooseCategory(restaurant);
            if (category == null) {
                break;
            }
            List<MenuItem> items = restaurant.filterByCategory(category);
            screen(restaurant.getName() + " - " + categoryLabel(category));
            printNumberedItems(items);
            if (items.isEmpty()) {
                pause();
                continue;
            }
            MenuItem item = chooseItemByNumber(items);
            if (item == null) {
                continue;
            }
            if (!item.canOrder(1)) {
                System.out.println("! " + item.getName() + " is not available right now.");
                pause();
                continue;
            }

            int quantity;
            while (true) {
                quantity = readInt("Quantity (0 to go back): ");
                if (quantity == 0) {
                    break;
                }
                if (quantity > 0 && item.canOrder(quantity)) {
                    break;
                }
                System.out.println("! Please enter a quantity from 1 to " + item.getStock() + ".");
            }
            if (quantity == 0) {
                continue;
            }

            String portion = choosePortion(item);
            List<AddOn> chosenAddOns = chooseAddOns(item);
            customer.getCart().addItem(item, quantity, portion, chosenAddOns);
            addedAnyItem = true;
            System.out.printf("Added %d x %s. Cart subtotal: PHP %.2f%n",
                    quantity, item.getName(), customer.getCart().getSubtotal());
            if (!readLine("Add another item? (y/n): ").equalsIgnoreCase("y")) {
                break;
            }
        }

        if (!addedAnyItem) {
            return false;
        }
        printCart(customer.getCart());
        return true;
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

        System.out.println("1. Change quantity");
        System.out.println("2. Remove item");
        System.out.println("3. Clear cart");
        System.out.println("4. Back");
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

    private String askDiscountId(DiscountType discountType) {
        while (true) {
            String idNumber = readLine("Enter your " + discountType + " ID number (0 to skip the discount): ");
            if (idNumber.equals(CANCEL)) {
                return null;
            }
            if (Validator.isValidDiscountId(idNumber)) {
                return idNumber.toUpperCase();
            }
            System.out.println("! Invalid ID number. Use 6-20 letters/digits (dashes allowed) with at least one digit.");
        }
    }

    private boolean checkout(Customer customer) {
        screen("Checkout");
        printCart(customer.getCart());
        if (customer.getCart().isEmpty()) {
            return true;
        }
        System.out.println("\nFulfillment");
        System.out.println("\t1. Dine-in Pre Order");
        System.out.println("\t2. Takeout Pickup");
        System.out.println("\t3. Delivery");
        int fulfillmentChoice = readChoice("Choice: ", 1, 3);
        OrderType orderType = fulfillmentChoice == 1 ? OrderType.DINE_IN : fulfillmentChoice == 2 ? OrderType.TAKEOUT : OrderType.DELIVERY;
        String address = "";
        if (orderType == OrderType.DELIVERY) {
            address = readLine("Delivery address [" + customer.getAddress() + "]: ");
            if (address.isEmpty()) {
                address = customer.getAddress();
            }
        }
        String preferredTime = readLine("Preferred time (optional): ");
        String specialInstructions = readLine("Special instructions (optional): ");

        System.out.println("\nDiscount");
        System.out.println("\t1. None");
        System.out.println("\t2. Senior Citizen");
        System.out.println("\t3. PWD");
        int discountChoice = readChoice("Choice: ", 1, 3);
        DiscountType discountType = discountChoice == 2 ? DiscountType.SENIOR_CITIZEN
                : discountChoice == 3 ? DiscountType.PWD : DiscountType.NONE;
        String discountIdNumber = "";
        if (discountType.isSectorDiscount()) {
            discountIdNumber = askDiscountId(discountType);
            if (discountIdNumber == null) {
                discountType = DiscountType.NONE;
                discountIdNumber = "";
                System.out.println("Continuing without the discount.");
            }
        }

        String promoCode = "";
        if (discountType.isSectorDiscount()) {
            System.out.println("Promo codes cannot be combined with the Senior Citizen and PWD discount.");
        } else {
            promoCode = readLine("Promo code (optional): ");
        }

        Bill bill = orders.previewBill(customer, orderType, promoCode, discountType, discountIdNumber);
        DeliveryDetails deliveryDetails = new DeliveryDetails(orderType, address, preferredTime, specialInstructions);
        Restaurant billRestaurant = menu.getRestaurant(customer.getCart().getRestaurantId());
        screen("Bill");
        System.out.println(eReceipt.preview(customer, billRestaurant, deliveryDetails, bill));
        if (discountType.isSectorDiscount()) {
            System.out.println("Please show your valid " + discountType + " ID when you receive your order.");
        }
        System.out.println("\nPayment");
        System.out.println("\t1. Cash on delivery");
        System.out.println("\t2. Cash at counter");
        System.out.println("\t3. Digital");
        int paymentChoice = readChoice("Choice: ", 1, 3);
        PaymentMethod paymentMethod = paymentChoice == 1 ? PaymentMethod.CASH_ON_DELIVERY : paymentChoice == 2 ? PaymentMethod.CASH_AT_COUNTER : PaymentMethod.DIGITAL;
        String confirmAnswer = readLine("Confirm order? (y/n): ");
        if (!confirmAnswer.equalsIgnoreCase("y")) {
            System.out.println("Order not placed.");
            return true;
        }

        Order placedOrder = orders.checkout(customer, deliveryDetails, paymentMethod, promoCode, null,
                discountType, discountIdNumber);
        if (placedOrder.getStatus() == OrderStatus.FAILED) {
            System.out.println(placedOrder.getPayment().receipt());
            System.out.println("Payment failed, order was not confirmed.");
        } else {
            screen("E-Receipt");
            System.out.println(eReceipt.generate(placedOrder, customer, billRestaurant));
            int queuePosition = orders.getQueuePosition(placedOrder);
            System.out.println("Order #" + placedOrder.getId() + " confirmed! Your position in the queue: " + queuePosition);
        }
        return true;
    }

    private void printOrders(List<Order> orderList) {
        if (orderList.isEmpty()) {
            System.out.println("No orders.");
        }
        for (Order order : orderList) {
            System.out.println("  " + order);
        }
    }

    private void guard(BooleanSupplier action) {
        boolean needsPause;
        try {
            needsPause = action.getAsBoolean();
        } catch (IllegalArgumentException | IllegalStateException | SecurityException exception) {
            System.out.println("! " + exception.getMessage());
            needsPause = true;
        }
        if (needsPause) {
            pause();
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
            ClearScreen.clear();
            switch (customerChoice) {
                case 1:
                    browse();
                    return false;
                case 2:
                    return addToCart(customer);
                case 3:
                    editCart(customer);
                    break;
                case 4:
                    return checkout(customer);
                case 5:
                    int trackOrderId = readInt("Order ID: ");
                    System.out.println(orders.trackOrder(customer, trackOrderId));
                    break;
                case 6:
                    List<Order> myOrders = orders.getOrdersOf(customer);
                    if (myOrders.isEmpty()) {
                        System.out.println("No orders yet.");
                    }
                    for (Order order : myOrders) {
                        System.out.println(order);
                    }
                    break;
                case 7:
                    int cancelOrderId = readInt("Order ID: ");
                    System.out.println(orders.cancelOrder(customer, cancelOrderId));
                    break;
                case 8:
                    authentication.logout();
                    return false;
                default:
                    System.out.println("Invalid choice.");
            }
            return true;
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
            ClearScreen.clear();
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
            return employeeChoice != 5;
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
            ClearScreen.clear();
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
            return riderChoice != 5;
        });
    }

    private void addItemUpgrades(User user, int restaurantId, MenuItem item) {
        while (readLine("Add an upgrade/add-on to " + item.getName() + "? (y/n): ").equalsIgnoreCase("y")) {
            String upgradeName = readLine("Upgrade name: ");
            double upgradePrice = readDouble("Upgrade price: ");
            menu.addAddOn(user, restaurantId, item.getName(), new AddOn(upgradeName, upgradePrice));
            System.out.println("Upgrade added: " + upgradeName);
        }
        while (readLine("Add a portion size to " + item.getName() + "? (y/n): ").equalsIgnoreCase("y")) {
            String portionSize = readLine("Portion size: ");
            menu.addPortionSize(user, restaurantId, item.getName(), portionSize);
            System.out.println("Portion size added: " + portionSize);
        }
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
        System.out.println("19. Add item upgrade");
        System.out.println("20. Add portion size");
        System.out.println("21. Logout");
        guard(() -> {
            int restaurantId;
            int managerChoice = readInt("Choice: ");
            ClearScreen.clear();
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
                    addItemUpgrades(user, restaurantId, addedItem);
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
                    restaurantId = readInt("Restaurant ID: ");
                    String upgradeItemName = readLine("Item name: ");
                    String upgradeName = readLine("Upgrade name: ");
                    double upgradePrice = readDouble("Upgrade price: ");
                    menu.addAddOn(user, restaurantId, upgradeItemName, new AddOn(upgradeName, upgradePrice));
                    System.out.println("Upgrade added to " + upgradeItemName + ".");
                    break;
                case 20:
                    restaurantId = readInt("Restaurant ID: ");
                    String portionItemName = readLine("Item name: ");
                    String portionSize = readLine("Portion size: ");
                    menu.addPortionSize(user, restaurantId, portionItemName, portionSize);
                    System.out.println("Portion size added to " + portionItemName + ".");
                    break;
                case 21:
                    authentication.logout();
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
            return managerChoice != 21;
        });
    }

    private void printRestaurantList(List<Restaurant> restaurants) {
        if (restaurants.isEmpty()) {
            System.out.println("No restaurants to show.");
            return;
        }
        for (Restaurant restaurant : restaurants) {
            System.out.println(restaurant);
        }
    }

    private void printSalesRanking(List<Restaurant> restaurants) {
        int salesRank = 1;
        for (Restaurant ranked : restaurants) {
            System.out.printf("  %d. %-22s PHP %.2f%n", salesRank++, ranked.getName(), ranked.getTotalSales());
        }
    }

    private void printBusinessOwners() {
        List<BusinessOwner> owners = menu.browseBusinessOwners();
        if (owners.isEmpty()) {
            System.out.println("(No business owner accounts yet. Register one first.)");
            return;
        }
        System.out.println("Business owners:");
        for (BusinessOwner owner : owners) {
            System.out.println("  " + owner.getName() + " - " + owner.getEmail());
        }
    }

    private void adminMenu(User user){
        System.out.println("1. Add restaurant");
        System.out.println("2. Edit restaurant");
        System.out.println("3. Remove restaurant");
        System.out.println("4. View restaurants by ID");
        System.out.println("5. View restaurants A to Z");
        System.out.println("6. View restaurants by Descending Order of Sales");
        System.out.println("7. View restaurants by Increasing Order of Sales");
        System.out.println("8. View List of Business Owners");
        System.out.println("9. Logout");

        guard(() -> {
            int restaurantId;
            int adminChoice = readInt("Choice: ");
            ClearScreen.clear();
            switch (adminChoice) {
                case 1:
                    String newRestaurantName = readLine("Name: ");
                    String newRestaurantDescription = readLine("Description: ");
                    String newRestaurantAddress = readLine("Address: ");
                    printBusinessOwners();
                    String newRestaurantOwnerEmail = readLine("Owner email: ");
                    Restaurant createdRestaurant = menu.createRestaurant(user, newRestaurantName, newRestaurantDescription, newRestaurantAddress, newRestaurantOwnerEmail);
                    System.out.println("Created: " + createdRestaurant);
                    break;
                case 2:
                    restaurantId = readInt("Restaurant ID: ");
                    String updatedName = readLine("Name: ");
                    String updatedDescription = readLine("Description: ");
                    String updatedAddress = readLine("Address: ");
                    printBusinessOwners();
                    String updatedOwnerEmail = readLine("New owner email (leave blank to keep the current owner): ");
                    menu.updateRestaurant(user, restaurantId, updatedName, updatedDescription, updatedAddress, updatedOwnerEmail);
                    break;
                case 3:
                    restaurantId = readInt("Restaurant ID: ");
                    menu.removeRestaurant(user, restaurantId);
                    break;
                case 4:
                    System.out.println("Restaurants by ID");
                    printRestaurantList(menu.browseRestaurants());
                    break;
                case 5:
                    System.out.println("Restaurants A to Z");
                    printRestaurantList(menu.browseRestaurantsAlphabetical());
                    break;
                case 6:
                    System.out.println("Restaurants by sales (highest first)");
                    printSalesRanking(reports.rankRestaurants(user));
                    break;
                case 7:
                    System.out.println("Restaurants by sales (lowest first)");
                    printSalesRanking(menu.browseRestaurantsBySalesAscending());
                    break;
                case 8:
                    printBusinessOwners();
                    break;
                case 9:
                    authentication.logout();
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
            return adminChoice != 9;
        });
    }

    private void run() {
        int loadedRestaurantCount = store.loadRestaurants();
        store.loadUsers();
        store.loadSalesData();
        if (store.getUsersByEmail().search("admin@foodbites.ph") == null) {
            authentication.createAdministrator("Food Bites Admin", "admin@foodbites.ph", "09170000000", "Admin1234");
        }
        while (true) {
            ClearScreen.clear();
            if (!authentication.isLoggedIn()) {
                System.out.println("FOOD BITES ORDERING SYSTEM");
                System.out.println(loadedRestaurantCount + " restaurant(s) loaded from data/restaurants");
                System.out.println("\nMain Menu");
                System.out.println("\t1. Browse restaurants and menus");
                System.out.println("\t2. Register");
                System.out.println("\t3. Login");
                System.out.println("\t4. Exit");
                int mainChoice = readInt("Choice: ");
                ClearScreen.clear();
                switch (mainChoice) {
                    case 1:
                        guard(() -> {
                            browse();
                            return false;
                        });
                        break;
                    case 2:
                        guard(() -> register());
                        break;
                    case 3:
                        guard(() -> login());
                        break;
                    case 4:
                        System.out.println("Goodbye!");
                        return;
                    default:
                        System.out.println("Invalid choice.");
                        pause();
                }
            } else {
                User currentUser = authentication.getCurrentUser();
                System.out.println("[" + currentUser.getRoleDescription() + "] " + currentUser.getName() + "\n");
                switch (currentUser.getRole()) {
                    case CUSTOMER:
                        customerMenu((Customer) currentUser);
                        break;
                    case EMPLOYEE:
                        employeeMenu((Employee) currentUser);
                        break;
                    case DELIVERY_RIDER:
                        riderMenu((DeliveryRider) currentUser);
                        break;
                    case ADMINISTRATOR:
                        adminMenu(currentUser);
                        break;
                    default:
                        managerMenu(currentUser);
                }
            }
        }
    }

    public static void main(String[] args) {
        new FoodBitesApp().run();
    }
}