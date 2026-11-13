package services;

import features.Payment;
import features.PaymentMethod;

import java.util.Random;

public class PaymentService {
    private final Random random = new Random();

    public Payment process(double amount, PaymentMethod method, Boolean forcedResult) {
        Payment payment = new Payment(amount, method);

        boolean isSuccessful;
        if (forcedResult != null) {
            isSuccessful = forcedResult;
        } else if (method == PaymentMethod.DIGITAL) {
            isSuccessful = random.nextInt(100) >= 10;
        } else {
            isSuccessful = true;
        }

        if (isSuccessful) {
            payment.markSuccess();
        } else {
            payment.markFailed("Payment failed. Please try again or use another payment method.");
        }
        return payment;
    }
}
