package features;

import java.util.Arrays;
import java.util.List;

public class Employee extends User {
    private final int restaurantId;

    public Employee(int id, String name, String email, String phone, String password, int restaurantId) {
        super(id, name, email, phone, password, UserRole.EMPLOYEE);
        this.restaurantId = restaurantId;
    }

    @Override
    public List<String> getAllowedActions() {
        return Arrays.asList("View kitchen queue", "Start preparing", "Mark order ready", "Complete pickup/dine-in orders");
    }

    @Override
    public String getRoleDescription() {
        return "Restaurant Employee";
    }

    public int getRestaurantId() {
        return restaurantId;
    }
}