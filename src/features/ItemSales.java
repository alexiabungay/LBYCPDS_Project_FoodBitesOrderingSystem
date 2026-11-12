package features;

public class ItemSales {
    private final String name;
    private int units;
    private double revenue;

    public ItemSales(String name) {
        this.name = name;
    }

    public void add(int qty, double amount) {
        units += qty; revenue += amount;
    }

    public String getName() {
        return name;
    }

    public int getUnits() {
        return units;
    }

    public double getRevenue() {
        return revenue;
    }
}