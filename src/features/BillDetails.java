package features;

public class BillDetails {
    private final String description;
    private final double amount;

    public BillDetails(String description, double amount) {
        this.description = description;
        this.amount = amount;
    }

    public String getDescription() { return description; }
    public double getAmount() { return amount; }
}