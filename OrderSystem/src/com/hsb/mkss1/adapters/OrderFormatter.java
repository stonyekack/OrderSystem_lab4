package com.hsb.mkss1.adapters;

import com.hsb.mkss1.usecases.output.LineItemOutputData;
import com.hsb.mkss1.usecases.output.OrderOutputData;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Presentation helpers shared by CLI and GUI (price format, sorting, summary text).
 * Works on the use case's OUTPUT DATA, not on entities.
 */
public final class OrderFormatter {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private OrderFormatter() { }

    public static String formatPrice(int priceInCents) {
        int euros = priceInCents / 100;
        int cents = priceInCents % 100;
        return euros + "." + (cents < 10 ? "0" : "") + cents + " EUR";
    }

    public static List<LineItemOutputData> sortByPrice(OrderOutputData order) {
        List<LineItemOutputData> sorted = new ArrayList<>(order.items());
        sorted.sort(Comparator.comparingInt(LineItemOutputData::totalPriceInCents));
        return sorted;
    }

    public static String formatItem(LineItemOutputData item) {
        return item.quantity() + " * " + item.name() + " = " + formatPrice(item.totalPriceInCents());
    }

    /** Summary of a finished order: items sorted by price, sum and checkout date. */
    public static String formatSummary(OrderOutputData order) {
        StringBuilder sb = new StringBuilder("=== Order Summary (sorted by price) ===\n");
        for (LineItemOutputData item : sortByPrice(order)) {
            sb.append(formatItem(item)).append('\n');
        }
        sb.append("Sum: ").append(formatPrice(order.lumpSumInCents())).append('\n');
        if (order.checkoutAt() != null) {
            sb.append("Checkout at: ").append(order.checkoutAt().format(DATE_FORMAT)).append('\n');
        }
        return sb.toString();
    }
}
