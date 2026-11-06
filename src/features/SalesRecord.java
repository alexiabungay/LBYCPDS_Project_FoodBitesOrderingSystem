package features;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SalesRecord {
    private final int orderId;
    private final RecordType type;
    private final LocalDate date;
    private final double amount;
    private final List<OrderItem> items;

    public SalesRecord(int orderId, RecordType type, LocalDate date, double amount, List<OrderItem> items) {
        this.orderId = orderId;
        this.type = type;
        this.date = date;
        this.amount = amount;
        this.items = new ArrayList<>(items);
    }

    public int getOrderId() { return orderId; }
    public RecordType getType() { return type; }
    public LocalDate getDate() { return date; }
    public double getAmount() { return amount; }
    public List<OrderItem> getItems() { return items; }
}