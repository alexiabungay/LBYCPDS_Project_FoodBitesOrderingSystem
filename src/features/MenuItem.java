package features;

import java.util.ArrayList;
import java.util.List;

public class MenuItem {
    private final int id;
    private final int restaurantId;
    private String name;
    private String description;
    private Category category;
    private double price;
    private boolean available = true;
    private int stock;
    private final List<String> portionSizes = new ArrayList<>();
    private final List<AddOn> addOns = new ArrayList<>();
    private int totalSales;
    private double totalRevenue;

    public MenuItem(int id, int restaurantId, String name, String description,
                    Category category, double price, int stock) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.stock = stock;
    }

    public boolean canOrder(int quantity) {
        return available && stock >= quantity;
    }

    public void deductStock(int quantity) {
        stock -= quantity;
        if (stock <= 0) {
            stock = 0;
        }
    }

    public void restock(int qty) {
        stock += qty;
    }

    public void recordSale(int qty, double amount) {
        totalSales += qty; totalRevenue += amount;
    }

    public void reverseSale(int qty, double amount) {
        totalSales = Math.max(0, totalSales - qty);
        totalRevenue = Math.max(0, totalRevenue - amount);
    }

    public int getId() {
        return id;
    }

    public int getRestaurantId() {
        return restaurantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public List<String> getPortionSizes() {
        return portionSizes;
    }

    public List<AddOn> getAddOns() {
        return addOns;
    }

    public int getTotalSales() {
        return totalSales;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    @Override
    public String toString() {
        return String.format("#%d %-22s %-8s PHP %7.2f  stock:%-4d %s",
                id, name, category, price, stock, available && stock > 0 ? "Available" : "Unavailable");
    }
}