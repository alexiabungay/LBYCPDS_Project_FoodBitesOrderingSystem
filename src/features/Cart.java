package features;

import java.util.ArrayList;
import java.util.List;

public class Cart {
    private final ArrayList<CartItem> items = new ArrayList<>();
    private int restaurantId = -1;

    public void addItem(MenuItem item, int quantity, String portion, List<AddOn> addOns) {
        if (quantity <= 0)
            throw new IllegalArgumentException("Quantity must be at least 1.");
        if (!item.canOrder(quantity))
            throw new IllegalArgumentException(item.getName() + " is not available in that quantity.");
        if (!items.isEmpty() && restaurantId != item.getRestaurantId())
            throw new IllegalArgumentException("Your cart has items from another restaurant. Clear it first.");
        List<AddOn> extras = addOns == null ? new ArrayList<>() : addOns;
        for (CartItem currentItem : items) {
            if (currentItem.sameAs(item, portion, extras)) {
                int newQty = currentItem.getQuantity() + quantity;
                if (!item.canOrder(newQty))
                    throw new IllegalArgumentException("Not enough stock for " + item.getName() + ".");
                currentItem.setQuantity(newQty);
                return;
            }
        }
        items.add(new CartItem(item, quantity, portion, extras));
        restaurantId = item.getRestaurantId();
    }

    public void updateQuantity(int index, int quantity) {
        CartItem currentItem = get(index);
        if (quantity <= 0) { removeItem(index);
            return;
        }
        if (!currentItem.getItem().canOrder(quantity))
            throw new IllegalArgumentException("Not enough stock for " + currentItem.getItem().getName() + ".");
        currentItem.setQuantity(quantity);
    }

    public void removeItem(int index) {
        get(index);
        items.remove(index);
        if (items.isEmpty()) restaurantId = -1;
    }

    public void clear() {
        items.clear(); restaurantId = -1;
    }

    public List<String> validateAvailability() {
        List<String> problems = new ArrayList<>();
        for (CartItem currentItem : items) {
            if (!currentItem.getItem().canOrder(currentItem.getQuantity()))
                problems.add(currentItem.getItem().getName() + " is no longer available in the quantity requested.");
        }
        return problems;
    }

    public double getSubtotal() {
        double total = 0;
        for (CartItem currentItem : items)
            total += currentItem.getLineTotal();
        return total;
    }

    private CartItem get(int index) {
        if (index < 0 || index >= items.size())
            throw new IllegalArgumentException("No such cart line.");
        return items.get(index);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public List<CartItem> getItems() {
        return items;
    }

    public int getRestaurantId() {
        return restaurantId;
    }
}