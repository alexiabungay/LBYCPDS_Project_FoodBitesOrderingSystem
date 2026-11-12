package features;

public class OrderItem {
    private final MenuItem item;
    private final String name;
    private final double unitPrice;
    private final int quantity;
    private final String details;

    public OrderItem(CartItem currentItem) {
        this.item = currentItem.getItem();
        this.name = currentItem.getItem().getName();
        this.unitPrice = currentItem.getUnitPrice();
        this.quantity = currentItem.getQuantity();
        StringBuilder sb = new StringBuilder();
        if (!currentItem.getPortion().isEmpty())
            sb.append(currentItem.getPortion());
        for (AddOn a : currentItem.getAddOns())
            sb.append(sb.length() > 0 ? ", " : "").append(a.getName());
        this.details = sb.toString();
    }

    public double getLineTotal() {
        return unitPrice * quantity;
    }

    public MenuItem getItem() {
        return item;
    }

    public String getName() {
        return name;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getDetails() {
        return details;
    }

    @Override
    public String toString() {
        return String.format("%dx %-20s %s PHP %.2f", quantity, name,
                details.isEmpty() ? "" : "(" + details + ")", getLineTotal());
    }
}
