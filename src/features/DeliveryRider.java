package features;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeliveryRider extends User {
    private boolean available = true;
    private final List<Integer> deliveredOrderIds = new ArrayList<>();

    public DeliveryRider(int id, String name, String email, String phone, String password) {
        super(id, name, email, phone, password, UserRole.DELIVERY_RIDER);
    }

    @Override
    public List<String> getAllowedActions() {
        return Arrays.asList("View pending deliveries", "Accept next delivery", "Confirm delivery result");
    }

    @Override
    public String getRoleDescription() {
        return "Delivery Rider";
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public List<Integer> getDeliveredOrderIds() {
        return deliveredOrderIds;
    }
}