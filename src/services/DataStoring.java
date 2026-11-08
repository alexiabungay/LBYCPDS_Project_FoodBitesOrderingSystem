package services;

import features.User;
import features.Restaurant;
import features.Order;
import java.util.ArrayList;
import java.util.List;

public class DataStoring {
    private static final List<User> users = new ArrayList<>();
    private static final List<Restaurant> restaurants = new ArrayList<>();
    private static final List<Order> orders = new ArrayList<>();

    private static int orderIdCounter = 1000;
    public static List<User> getUsers() { return users; }
    public static List<Restaurant> getRestaurants() { return restaurants; }
    public static List<Order> getOrders() { return orders; }

    public static synchronized int generateNextOrderId() {
        return ++orderIdCounter;
    }
}
