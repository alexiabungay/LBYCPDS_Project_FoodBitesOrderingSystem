package features;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BusinessOwner extends User {
    private final List<Integer> restaurantIds = new ArrayList<>();

    public BusinessOwner(int id, String name, String email, String phone, String password) {
        super(id, name, email, phone, password, UserRole.BUSINESS_OWNER);
    }

    @Override
    public List<String> getAllowedActions() {
        return Arrays.asList("Manage restaurant details", "Manage menu and prices", "Manage stock",
                "View orders", "Record cancellations and refunds", "View sales reports");
    }

    @Override
    public String getRoleDescription() {
        return "Business Owner";
    }

    public List<Integer> getRestaurantIds() {
        return restaurantIds;
    }
}