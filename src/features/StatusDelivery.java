package features;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StatusDelivery {
    private final OrderStatus status;
    private final LocalDateTime timestamp = LocalDateTime.now();

    public StatusDelivery(OrderStatus status) {
        this.status = status;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "  " + status;
    }
}