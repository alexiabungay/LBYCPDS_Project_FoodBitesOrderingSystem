package features;

import java.util.ArrayList;
import java.util.List;

public class Bill {
    private final List<BillDetails> lines = new ArrayList<>();
    private double subtotal;
    private double tax;
    private double serviceCharge;
    private double deliveryFee;
    private double discount;
    private String promoCode = "";

    public void addLine(BillDetails line) {
        lines.add(line);
    }

    public double getTotal() {
        return subtotal + tax + serviceCharge + deliveryFee - discount;
    }

    public List<BillDetails> getLines() {
        return lines;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getTax() {
        return tax;
    }

    public void setTax(double tax) {
        this.tax = tax;
    }

    public double getServiceCharge() {
        return serviceCharge;
    }

    public void setServiceCharge(double serviceCharge) {
        this.serviceCharge = serviceCharge;
    }

    public double getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(double deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (BillDetails l : lines)
            sb.append(String.format("  %-34s PHP %9.2f%n", l.getDescription(), l.getAmount()));
        sb.append(String.format("  %-34s PHP %9.2f", "TOTAL", getTotal()));
        return sb.toString();
    }
}