package com.hsb.mkss1.usecases.output;

import java.time.LocalDateTime;
import java.util.List;

/** OUTPUT DATA: snapshot of an order handed to the presenter. checkoutAt is null while the order is open. */
public record OrderOutputData(long orderId, List<LineItemOutputData> items, int lumpSumInCents,
                              LocalDateTime checkoutAt) {
    public OrderOutputData {
        items = List.copyOf(items);
    }

    public boolean isFinished() {
        return checkoutAt != null;
    }
}
