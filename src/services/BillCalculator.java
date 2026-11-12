package services;

import features.*;

import java.util.HashMap;
import java.util.Map;

public class BillCalculator {
    public static final double TAX_RATE = 0.12;
    public static final double SERVICE_CHARGE_RATE = 0.10;
    public static final double DELIVERY_FEE = 49.00;

    private final Map<String, Double> promos = new HashMap<>();

    public BillCalculator() {
        promos.put("FOODBITES", 0.10);
        promos.put("WELCOME", 0.05);
    }

    public boolean isValidPromo(String code) {
        return code != null && promos.containsKey(code.toUpperCase());
    }

    public Bill compute(Cart cart, OrderType type, String promoCode) {
        Bill bill = new Bill();
        double subtotal = 0;
        for (CartItem currentItem : cart.getItems()) {
            bill.addLine(new BillDetails(currentItem.toString().replaceAll("  = PHP .*$", ""), currentItem.getLineTotal()));
            subtotal += currentItem.getLineTotal();
        }
        bill.setSubtotal(subtotal);
        bill.addLine(new BillDetails("Subtotal", subtotal));

        double discount = 0;
        if (promoCode != null && !promoCode.trim().isEmpty()) {
            if (!isValidPromo(promoCode))
                throw new IllegalArgumentException("Invalid promo code.");
            discount = subtotal * promos.get(promoCode.toUpperCase());
            bill.setPromoCode(promoCode.toUpperCase());
            bill.setDiscount(discount);
            bill.addLine(new BillDetails("Promo " + promoCode.toUpperCase(), -discount));
        }

        double taxable = subtotal - discount;
        double tax = taxable * TAX_RATE;
        bill.setTax(tax);
        bill.addLine(new BillDetails("Tax (12%)", tax));

        if (type == OrderType.DINE_IN) {
            double serviceCharge = taxable * SERVICE_CHARGE_RATE;
            bill.setServiceCharge(serviceCharge);
            bill.addLine(new BillDetails("Service charge (10%)", serviceCharge));
        }
        if (type == OrderType.DELIVERY) {
            bill.setDeliveryFee(DELIVERY_FEE);
            bill.addLine(new BillDetails("Delivery fee", DELIVERY_FEE));
        }
        return bill;
    }
}