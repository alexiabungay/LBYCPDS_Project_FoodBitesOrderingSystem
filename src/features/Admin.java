package features;

import java.util.Arrays;
import java.util.List;

public class Admin extends User {

    public Admin(int id, String name, String email, String phone, String password) {
        super(id, name, email, phone, password, UserRole.ADMINISTRATOR);
    }

    @Override
    public List<String> getAllowedActions() {
        return Arrays.asList("Manage all restaurants and menus", "View all orders", "View all sales reports",
                "Rank restaurants by sales");
    }

    @Override
    public String getRoleDescription() {
        return "Administrator";
    }
}