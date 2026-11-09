package com.hsb.mkss1.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * ENTITY (ring 1): an order consisting of line items.
 * Contains the enterprise business rules (merging, "finished orders are frozen",
 * "empty orders cannot be finished"). Knows nothing about UI, use cases or persistence.
 */
public class Order {
    private final Long id; // assigned by the repository (null until stored)
    private final List<LineItem> items = new ArrayList<>();
    private LocalDateTime checkoutAt;

    public Order() {
        this.id = null;
    }

    private Order(Order other, Long id) {
        this.id = id;
        this.items.addAll(other.items); // shallow copy is enough: LineItem is immutable
        this.checkoutAt = other.checkoutAt;
    }

    /** Independent copy (same id), so callers never share internal state. */
    public Order copy() {
        return new Order(this, id);
    }

    /** Independent copy carrying the given id (used by repositories on insert). */
    public Order copyWithId(long newId) {
        return new Order(this, newId);
    }

    public Long getId() {
        return id;
    }

    public void addItem(LineItem item) {
        Objects.requireNonNull(item, "Cannot add a null item to an order");
        if (isFinished()) {
            throw new IllegalStateException("Order is already finished.");
        }
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).canMergeWith(item)) {
                items.set(i, items.get(i).mergeWith(item));
                return;
            }
        }
        items.add(item);
    }

    public List<LineItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int getLumpSum() {
        int sum = 0;
        for (LineItem item : items) {
            sum += item.getTotalPriceInCents();
        }
        return sum;
    }

    /** Business rule: only a non-empty, not yet finished order can be finished. */
    public void finish() {
        if (isFinished()) {
            throw new IllegalStateException("Order is already finished.");
        }
        if (isEmpty()) {
            throw new IllegalStateException("Your order is empty. Add a product before checkout.");
        }
        this.checkoutAt = LocalDateTime.now();
    }

    public LocalDateTime getCheckoutAt() {
        return checkoutAt;
    }

    public boolean isFinished() {
        return checkoutAt != null;
    }
}
