package features;

import java.util.ArrayList;
import java.util.List;

public class Order {
    private final int id;
    private final int customerId;
    private final int restaurantId;
    private final List<OrderItem> items = new ArrayList<>();
    private final DeliveryDetails details;
    private final Bill bill;
    private Payment payment;
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;
    private final List<StatusDelivery> history = new ArrayList<>();
    private int riderId = -1;

    public Order(int id, int customerId, int restaurantId, List<CartItem> cartItems,
                 DeliveryDetails details, Bill bill) {
        this.id = id;
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        for (CartItem ci : cartItems) items.add(new OrderItem(ci));
        this.details = details;
        this.bill = bill;
        history.add(new StatusDelivery(status));
    }

    public void setStatus(OrderStatus newStatus) {
        this.status = newStatus;
        history.add(new StatusDelivery(newStatus));
    }

    public int getId() {
        return id;
    }

    public int getCustomerId() {
        return customerId;
    }

    public int getRestaurantId() {
        return restaurantId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public DeliveryDetails getDetails() {
        return details;
    }

    public Bill getBill() {
        return bill;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public List<StatusDelivery> getHistory() {
        return history;
    }

    public int getRiderId() {
        return riderId;
    }

    public void setRiderId(int riderId) {
        this.riderId = riderId;
    }

    @Override
    public String toString() {
        return String.format("Order #%d | %s | %s | PHP %.2f | %s", id, details.getType(), status,
                bill.getTotal(), items.size() + " item line(s)");
    }
}