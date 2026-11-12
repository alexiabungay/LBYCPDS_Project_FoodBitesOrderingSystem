package features;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Customer extends User {
    private String address = "";
    private final Cart cart = new Cart();
    private final List<Integer> orderIds = new ArrayList<>();

    public Customer(int id, String name, String email, String phone, String password) {
        super(id, name, email, phone, password, UserRole.CUSTOMER);
    }

    @Override
    public List<String> getAllowedActions() {
        return Arrays.asList("Browse menus", "Search and filter", "Manage cart", "Place order",
                "Pay", "Track order");
    }

    @Override
    public String getRoleDescription() {
        return "Customer";
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Cart getCart() {
        return cart;
    }

    public List<Integer> getOrderIds() {
        return orderIds;
    }
}