package com.cinema.payment.utils;

public class CardUtils {
    private CardUtils() {
    }

    public static String extractLastFour(String cardNumber) {
        if (cardNumber == null || cardNumber.isBlank()) {
            return null;
        }
        String d = cardNumber.replaceAll("\\D", "");
        if (d.length() < 4) {
            return d;
        }
        return d.substring(d.length() - 4);
    }
}
