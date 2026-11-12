package features;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Payment {
    private static int counter = 1;

    private final String referenceNumber;
    private final double amount;
    private final PaymentMethod method;
    private PaymentStatus status = PaymentStatus.PENDING;
    private LocalDateTime timestamp;
    private String message = "";

    public Payment(double amount, PaymentMethod method) {
        this.referenceNumber = String.format("FB-PAY-%05d", counter++);
        this.amount = amount;
        this.method = method;
    }

    public void markSuccess() {
        status = PaymentStatus.SUCCESS; timestamp = LocalDateTime.now(); message = "Payment successful.";
    }

    public void markFailed(String reason) {
        status = PaymentStatus.FAILED; timestamp = LocalDateTime.now(); message = reason;
    }

    public void markCancelled() {
        status = PaymentStatus.CANCELLED; timestamp = LocalDateTime.now(); message = "Payment cancelled.";
    }

    public void markRefunded() {
        status = PaymentStatus.REFUNDED; message = "Payment refunded.";
    }

    public String receipt() {
        if (status != PaymentStatus.SUCCESS && status != PaymentStatus.REFUNDED)
            return "No receipt: " + message;
        return String.format("RECEIPT %s | %s | PHP %.2f | %s | %s", referenceNumber, method, amount, status,
                timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public double getAmount() {
        return amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }
}
