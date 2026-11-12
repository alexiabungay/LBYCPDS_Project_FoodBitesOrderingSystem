package features;

import java.util.ArrayList;
import java.util.List;

public class CartItem {
    private final MenuItem item;
    private int quantity;
    private final String portion;
    private final List<AddOn> addOns;

    public CartItem(MenuItem item, int quantity, String portion, List<AddOn> addOns) {
        this.item = item;
        this.quantity = quantity;
        this.portion = portion == null ? "" : portion;
        this.addOns = addOns == null ? new ArrayList<>() : new ArrayList<>(addOns);
    }

    public double getUnitPrice() {
        double total = item.getPrice();
        for (AddOn a : addOns)
            total += a.getPrice();
        return total;
    }

    public double getLineTotal() {
        return getUnitPrice() * quantity;
    }

    public boolean sameAs(MenuItem other, String otherPortion, List<AddOn> otherAddOns) {
        if (item.getId() != other.getId())
            return false;
        if (!portion.equals(otherPortion == null ? "" : otherPortion))
            return false;
        if (addOns.size() != otherAddOns.size())
            return false;
        for (AddOn otherAddOn : otherAddOns) {
            boolean found = false;
            for (AddOn addOn : addOns)
                if (addOn.getName().equals(otherAddOn.getName()))
                    found = true;
            if (!found)
                return false;
        }
        return true;
    }

    public MenuItem getItem() {
        return item;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getPortion() {
        return portion;
    }

    public List<AddOn> getAddOns() {
        return addOns;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(String.format("%dx %s", quantity, item.getName()));
        if (!portion.isEmpty())
            sb.append(" [").append(portion).append("]");
        if (!addOns.isEmpty()) {
            sb.append(" + ");
            for (int i = 0; i < addOns.size(); i++) {
                if (i > 0)
                    sb.append(", ");
                sb.append(addOns.get(i).getName());
            }
        }
        return sb.append(String.format("  = PHP %.2f", getLineTotal())).toString();
    }
}