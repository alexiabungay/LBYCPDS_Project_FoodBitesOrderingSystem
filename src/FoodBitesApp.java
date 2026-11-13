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
}