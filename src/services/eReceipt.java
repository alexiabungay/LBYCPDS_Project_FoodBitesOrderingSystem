package services;

import features.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the plain-text e-receipt / bill of an order. The layout follows a printed e-receipt:
 * centered header and section titles, label on the left and value on the right, then customer details,
 * address, order notes and a "Powered by" footer.
 */
public class eReceipt {
    private static final int WIDTH = 46;
    private static final int QTY_WIDTH = 4;
    private static final int AMOUNT_WIDTH = 12;
    private static final int NAME_WIDTH = WIDTH - QTY_WIDTH - AMOUNT_WIDTH - 2;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private eReceipt() {
    }

    private static class ReceiptLine {
        private final String name;
        private final String details;
        private final int quantity;
        private final double amount;

        private ReceiptLine(String name, String details, int quantity, double amount) {
            this.name = name;
            this.details = details;
            this.quantity = quantity;
            this.amount = amount;
        }
    }

    // ---------------------------------------------------------------- public

    // The receipt of a placed order.
    public static String generate(Order order, User customer, Restaurant restaurant) {
        List<ReceiptLine> lines = new ArrayList<>();
        for (OrderItem orderItem : order.getItems()) {
            lines.add(new ReceiptLine(orderItem.getName(), orderItem.getDetails(),
                    orderItem.getQuantity(), orderItem.getLineTotal()));
        }

        Payment payment = order.getPayment();
        LocalDateTime orderedAt = LocalDateTime.now();
        if (payment != null && payment.getTimestamp() != null) {
            orderedAt = payment.getTimestamp();
        }
        return render("E-RECEIPT", String.valueOf(order.getId()), orderedAt, restaurant, customer,
                order.getDetails(), lines, order.getBill(), payment);
    }

    // The bill shown before the customer confirms the order (no order number or payment yet).
    public static String preview(Customer customer, Restaurant restaurant, DeliveryDetails details, Bill bill) {
        List<ReceiptLine> lines = new ArrayList<>();
        for (CartItem cartItem : customer.getCart().getItems()) {
            lines.add(new ReceiptLine(cartItem.getItem().getName(), describe(cartItem),
                    cartItem.getQuantity(), cartItem.getLineTotal()));
        }
        return render("BILL", "Pending", LocalDateTime.now(), restaurant, customer, details, lines, bill, null);
    }

    // ---------------------------------------------------------------- layout

    private static String render(String title, String orderNumber, LocalDateTime orderedAt, Restaurant restaurant,
                                 User customer, DeliveryDetails details, List<ReceiptLine> lines, Bill bill,
                                 Payment payment) {
        StringBuilder out = new StringBuilder();

        // header
        centered(out, "FOOD BITES");
        if (restaurant != null) {
            centered(out, restaurant.getName());
            if (isFilled(restaurant.getAddress())) {
                centered(out, restaurant.getAddress());
            }
        }
        centered(out, title);
        out.append('\n');

        // order info
        row(out, "Order Number", orderNumber);
        row(out, "Date", orderedAt.format(DATE_FORMAT));
        row(out, "Time Ordered", orderedAt.format(TIME_FORMAT).toLowerCase(Locale.ENGLISH));
        row(out, "Order Type", orderTypeLabel(details.getType()));
        out.append('\n');

        // food ordered
        out.append(String.format(Locale.US, "%-" + NAME_WIDTH + "s %" + QTY_WIDTH + "s %" + AMOUNT_WIDTH + "s%n",
                "Item", "Qty", "Amount"));
        out.append(dashes());
        for (ReceiptLine line : lines) {
            List<String> nameLines = wrap(line.name, NAME_WIDTH);
            out.append(String.format(Locale.US, "%-" + NAME_WIDTH + "s %" + QTY_WIDTH + "d %" + AMOUNT_WIDTH + "s%n",
                    nameLines.get(0), line.quantity, money(line.amount)));
            for (int index = 1; index < nameLines.size(); index++) {
                out.append(nameLines.get(index)).append('\n');
            }
            if (isFilled(line.details)) {
                for (String detailLine : wrap(line.details, WIDTH - 2)) {
                    out.append("  ").append(detailLine).append('\n');
                }
            }
        }
        out.append(dashes());

        // totals (the bill lines after the food lines: subtotal, discount, tax, service charge, delivery fee)
        List<BillDetails> billLines = bill.getLines();
        for (int index = lines.size(); index < billLines.size(); index++) {
            row(out, billLines.get(index).getDescription(), money(billLines.get(index).getAmount()));
        }
        row(out, "TOTAL", money(bill.getTotal()));
        if (bill.getDiscountType().isSectorDiscount()) {
            row(out, bill.getDiscountType() + " ID", maskedId(bill.getDiscountIdNumber()));
        }

        // payment
        if (payment != null) {
            out.append('\n');
            centered(out, "Payment");
            out.append('\n');
            row(out, "Method", paymentMethodLabel(payment.getMethod()));
            row(out, "Reference", payment.getReferenceNumber());
            row(out, "Status", paymentStatusLabel(payment.getStatus()));
        }

        // customer
        if (customer != null) {
            out.append('\n');
            centered(out, "Customer Details");
            out.append('\n');
            left(out, customer.getName());
            left(out, customer.getPhone());
            left(out, customer.getEmail());
        }

        // delivery address or pick-up place
        out.append('\n');
        if (details.getType() == OrderType.DELIVERY) {
            centered(out, "Delivery Address");
            left(out, details.getAddress());
        } else if (restaurant != null) {
            centered(out, details.getType() == OrderType.DINE_IN ? "Dine-in At" : "Pickup At");
            left(out, restaurant.getName());
            if (isFilled(restaurant.getAddress())) {
                left(out, restaurant.getAddress());
            }
        }

        // order notes
        if (isFilled(details.getPreferredTime()) || isFilled(details.getSpecialInstructions())) {
            centered(out, "Order Notes");
            if (isFilled(details.getPreferredTime())) {
                left(out, "Preferred time: " + details.getPreferredTime());
            }
            if (isFilled(details.getSpecialInstructions())) {
                left(out, details.getSpecialInstructions());
            }
        }

        // footer
        out.append('\n');
        if (payment == null) {
            centered(out, "Please review your bill and confirm");
            centered(out, "your order.");
        } else {
            centered(out, "Thank you for ordering!");
        }
        centered(out, "Powered by Food Bites Ordering System");
        return out.toString();
    }

    // ---------------------------------------------------------------- helpers

    private static void centered(StringBuilder out, String text) {
        for (String textLine : wrap(text, WIDTH)) {
            int padding = Math.max(0, (WIDTH - textLine.length()) / 2);
            out.append(" ".repeat(padding)).append(textLine).append('\n');
        }
    }

    private static void left(StringBuilder out, String text) {
        for (String textLine : wrap(text, WIDTH)) {
            out.append(textLine).append('\n');
        }
    }

    // label on the left, value on the right edge
    private static void row(StringBuilder out, String label, String value) {
        int gap = WIDTH - label.length() - value.length();
        if (gap < 1) {
            left(out, label);
            out.append(" ".repeat(Math.max(0, WIDTH - value.length()))).append(value).append('\n');
            return;
        }
        out.append(label).append(" ".repeat(gap)).append(value).append('\n');
    }

    private static String dashes() {
        return "-".repeat(WIDTH) + "\n";
    }

    private static List<String> wrap(String text, int width) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String safeText = text == null ? "" : text.trim();

        for (String word : safeText.split("\\s+")) {
            while (word.length() > width) {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }
                result.add(word.substring(0, width));
                word = word.substring(width);
            }
            if (current.length() == 0) {
                current.append(word);
            } else if (current.length() + 1 + word.length() <= width) {
                current.append(' ').append(word);
            } else {
                result.add(current.toString());
                current.setLength(0);
                current.append(word);
            }
        }
        if (current.length() > 0 || result.isEmpty()) {
            result.add(current.toString());
        }
        return result;
    }

    private static boolean isFilled(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private static String money(double amount) {
        String sign = amount < 0 ? "-" : "";
        return sign + "PHP " + String.format(Locale.US, "%,.2f", Math.abs(amount));
    }

    // portion size and upgrades of a cart item, same wording as the saved order item
    private static String describe(CartItem cartItem) {
        StringBuilder details = new StringBuilder();
        if (!cartItem.getPortion().isEmpty()) {
            details.append(cartItem.getPortion());
        }
        for (AddOn addOn : cartItem.getAddOns()) {
            details.append(details.length() > 0 ? ", " : "").append(addOn.getName());
        }
        return details.toString();
    }

    private static String maskedId(String idNumber) {
        if (idNumber == null || idNumber.length() <= 4) {
            return idNumber == null ? "" : idNumber;
        }
        return "*".repeat(idNumber.length() - 4) + idNumber.substring(idNumber.length() - 4);
    }

    private static String orderTypeLabel(OrderType type) {
        switch (type) {
            case DINE_IN:
                return "Dine-in";
            case TAKEOUT:
                return "Takeout";
            default:
                return "Delivery";
        }
    }

    private static String paymentMethodLabel(PaymentMethod method) {
        switch (method) {
            case CASH_ON_DELIVERY:
                return "Cash on delivery";
            case CASH_AT_COUNTER:
                return "Cash at counter";
            default:
                return "Digital";
        }
    }

    private static String paymentStatusLabel(PaymentStatus status) {
        switch (status) {
            case SUCCESS:
                return "Paid";
            case REFUNDED:
                return "Refunded";
            case FAILED:
                return "Failed";
            case CANCELLED:
                return "Cancelled";
            default:
                return "Pending";
        }
    }
}
