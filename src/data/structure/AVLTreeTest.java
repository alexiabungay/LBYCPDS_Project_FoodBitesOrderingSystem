package data.structure;

import java.util.LinkedList;
import java.util.Queue;

public class AVLTreeTest {

    static class MenuItem {
        int id;
        String name;
        double price;

        MenuItem(int id, String name, double price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }

        @Override
        public String toString() {
            return String.format("[%d] %-20s PHP %.2f", id, name, price);
        }
    }

    public static void main(String[] args) {

        String[] names = {"Sisig", "Bacsilog", "1pc Chicken Joy", "Mang-Inasal Paa", "Chicken Burger McDo"};
        int[] prices = {190, 120, 110, 180, 100};

        AVLTree<Integer, MenuItem> menu = new AVLTree<>();

        System.out.println("=== INSERT menu items ===");
        for (int i = 0; i < names.length; i++) {
            int id = i + 1;
            menu.insert(id, new MenuItem(id, names[i], prices[i]));
        }

        System.out.println("Size: " + menu.size() + " | Height: " + menu.height());
        printList("Menu (in-order, by ID)", menu.inOrder());

        System.out.println("\n=== SEARCH ===");
        int[] searchIds = {3, 99};
        for (int id : searchIds) {
            MenuItem found = menu.search(id);
            System.out.println("Search " + id + ": " + (found != null ? found : "Not found"));
        }
        System.out.println("Contains 3? " + menu.contains(3));
        System.out.println("Contains 200? " + menu.contains(200));

        System.out.println("\n=== EDIT ===");
        System.out.println("Before: " + menu.search(2));
        menu.insert(2, new MenuItem(2, "Bacsilog w/ Egg", 130.00));
        System.out.println("After : " + menu.search(2));
        System.out.println("Size unchanged: " + menu.size());
        printList("Menu after edit", menu.inOrder());

        printList("\nRange query (IDs 2 to 4)", menu.range(2, 4));

        printList("\nMenu (reverse in-order)", menu.reverseInOrder());

        System.out.println("\n=== ORDER QUEUE ===");
        Queue<Integer> orderQueue = new LinkedList<>();
        int[] incomingOrders = {1, 3, 4, 5, 2};
        for (int id : incomingOrders) {
            orderQueue.add(id);
            System.out.println("Enqueued order for item " + id);
        }

        System.out.println("\n=== DELETE ===");
        System.out.println("Delete 4 (Mang-Inasal Paa): " + menu.delete(4));
        System.out.println("Delete 99 (not in menu): " + menu.delete(99));
        System.out.println("Delete 2 (Bacsilog w/ Egg): " + menu.delete(2));
        System.out.println("Size: " + menu.size() + " | Height: " + menu.height());
        printList("Menu after delete", menu.inOrder());

        System.out.println("\n=== PROCESSING QUEUE ===");
        double total = 0;
        while (!orderQueue.isEmpty()) {
            int id = orderQueue.poll();
            MenuItem item = menu.search(id);
            if (item != null) {
                System.out.println("Served : " + item);
                total += item.price;
            } else {
                System.out.println("Skipped: item " + id + " is no longer available");
            }
        }
        System.out.printf("Total bill: PHP %.2f%n", total);
        System.out.println("Queue empty? " + orderQueue.isEmpty());
    }

    private static void printList(String title, java.util.List<MenuItem> items) {
        System.out.println(title + ":");
        for (MenuItem item : items) {
            System.out.println("  " + item);
        }
    }
}