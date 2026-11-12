package features;

import data.structure.AVLTree;
import data.structure.HeapSort;
import data.structure.LinkedListQueue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Restaurant {
    private final int id;
    private int ownerId;
    private String ownerEmail = "";
    private String name;
    private String description;
    private String address;
    private boolean supportsDineIn = true;
    private boolean supportsTakeout = true;
    private boolean supportsDelivery = true;

    private final AVLTree<String, MenuItem> nameIndex = new AVLTree<>();
    private final AVLTree<PriceKey, MenuItem> priceIndex = new AVLTree<>();

    private final LinkedListQueue<Order> kitchenQueue = new LinkedListQueue<>();
    private final LinkedListQueue<Order> dispatchQueue = new LinkedListQueue<>();
    private final List<SalesRecord> salesRecords = new ArrayList<>();

    private double totalSales;
    private int currentCustomers;
    private int totalCustomers;

    public Restaurant(int id, int ownerId, String name, String description, String address) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.address = address;
    }

    public boolean addMenuItem(MenuItem item) {
        String key = item.getName().toLowerCase();
        if (nameIndex.contains(key)) return false;
        nameIndex.insert(key, item);
        priceIndex.insert(new PriceKey(item.getPrice(), item.getName()), item);
        return true;
    }

    public boolean removeMenuItem(String name) {
        MenuItem item = nameIndex.search(name.toLowerCase());
        if (item == null) return false;
        nameIndex.delete(name.toLowerCase());
        priceIndex.delete(new PriceKey(item.getPrice(), item.getName()));
        return true;
    }

    public boolean editMenuItem(String oldName, String newName, String description, Category category, double price) {
        MenuItem item = nameIndex.search(oldName.toLowerCase());
        if (item == null) return false;
        if (!oldName.equalsIgnoreCase(newName) && nameIndex.contains(newName.toLowerCase())) return false;
        nameIndex.delete(oldName.toLowerCase());
        priceIndex.delete(new PriceKey(item.getPrice(), item.getName()));
        item.setName(newName);
        item.setDescription(description);
        item.setCategory(category);
        item.setPrice(price);
        nameIndex.insert(newName.toLowerCase(), item);
        priceIndex.insert(new PriceKey(price, newName), item);
        return true;
    }

    public boolean updatePrice(String name, double newPrice) {
        MenuItem item = nameIndex.search(name.toLowerCase());
        if (item == null) return false;
        return editMenuItem(item.getName(), item.getName(), item.getDescription(), item.getCategory(), newPrice);
    }

    public MenuItem findByName(String name) { return nameIndex.search(name.toLowerCase()); }

    public List<MenuItem> searchByPrefix(String prefix) {
        String p = prefix.toLowerCase();
        return nameIndex.range(p, p + '\uffff');
    }

    public List<MenuItem> searchByName(String keyword) {
        String k = keyword.toLowerCase();
        List<MenuItem> out = new ArrayList<>();
        for (MenuItem m : nameIndex.inOrder()) if (m.getName().toLowerCase().contains(k)) out.add(m);
        return out;
    }

    public List<MenuItem> listAlphabetical() { return nameIndex.inOrder(); }
    public List<MenuItem> listByPriceAscending() { return priceIndex.inOrder(); }
    public List<MenuItem> listByPriceDescending() { return priceIndex.reverseInOrder(); }

    public List<MenuItem> filterByCategory(Category category) {
        List<MenuItem> out = new ArrayList<>();
        for (MenuItem m : nameIndex.inOrder()) if (m.getCategory() == category) out.add(m);
        return out;
    }

    public List<MenuItem> filterByPriceRange(double min, double max) {
        return priceIndex.range(new PriceKey(min, ""), new PriceKey(max, "\uffff"));
    }

    public List<MenuItem> listByPopularity() {
        return HeapSort.sortDescending(nameIndex.inOrder(),
                Comparator.comparingInt(MenuItem::getTotalSales).thenComparingDouble(MenuItem::getTotalRevenue));
    }

    public void addRevenue(double amount) { totalSales += amount; }
    public void subtractRevenue(double amount) { totalSales = Math.max(0, totalSales - amount); }
    public void addSalesRecord(SalesRecord r) { salesRecords.add(r); }
    public void customerArrived() { currentCustomers++; }
    public void customerLeft(boolean served) {
        currentCustomers = Math.max(0, currentCustomers - 1);
        if (served) totalCustomers++;
    }

    public boolean supports(OrderType type) {
        switch (type) {
            case DINE_IN: return supportsDineIn;
            case TAKEOUT: return supportsTakeout;
            default: return supportsDelivery;
        }
    }

    public int getId() {
        return id;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail == null ? "" : ownerEmail;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
    public void setSupportedTypes(boolean dineIn, boolean takeout, boolean delivery) {
        supportsDineIn = dineIn;
        supportsTakeout = takeout;
        supportsDelivery = delivery;
    }

    public LinkedListQueue<Order> getKitchenQueue() {
        return kitchenQueue;
    }

    public LinkedListQueue<Order> getDispatchQueue() {
        return dispatchQueue;
    }

    public List<SalesRecord> getSalesRecords() {
        return salesRecords;
    }

    public double getTotalSales() {
        return totalSales;
    }

    public int getCurrentCustomers() {
        return currentCustomers;
    }

    public int getTotalCustomers() {
        return totalCustomers;
    }

    public int getMenuSize() {
        return nameIndex.size();
    }

    @Override
    public String toString() {
        return String.format("#%d %s - %s (%s)", id, name, description, address);
    }
}