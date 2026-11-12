package features;

public class DeliveryDetails {
    private final OrderType type;
    private final String address;
    private final String preferredTime;
    private final String specialInstructions;

    public DeliveryDetails(OrderType type, String address, String preferredTime, String specialInstructions) {
        this.type = type;
        this.address = address == null ? "" : address;
        this.preferredTime = preferredTime == null ? "" : preferredTime;
        this.specialInstructions = specialInstructions == null ? "" : specialInstructions;
    }

    public OrderType getType() {
        return type;
    }

    public String getAddress() {
        return address;
    }

    public String getPreferredTime() {
        return preferredTime;
    }

    public String getSpecialInstructions() {
        return specialInstructions;
    }
}