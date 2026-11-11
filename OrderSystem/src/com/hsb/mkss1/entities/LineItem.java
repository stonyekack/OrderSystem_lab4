package com.hsb.mkss1.entities;

/**
 * ENTITY (ring 1): one line of an order (product name, unit price, quantity).
 * Replaces Item / Product / Service / MergeableItem of Lab 3.
 * Immutable; the enterprise-wide validation rules live here, so an invalid LineItem can never exist.
 */
public final class LineItem {
    private final String name;
    private final int unitPriceInCents;
    private final int quantity;

    public LineItem(String name, int unitPriceInCents, int quantity) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        if (unitPriceInCents < 0 || quantity <= 0) {
            throw new IllegalArgumentException("Price must be nonnegative and quantity must be positive.");
        }
        if ((long) unitPriceInCents * quantity > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Line item total exceeds the supported price range.");
        }
        this.name = name.trim();
        this.unitPriceInCents = unitPriceInCents;
        this.quantity = quantity;
    }

    public String getName() { return name; }
    public int getUnitPriceInCents() { return unitPriceInCents; }
    public int getQuantity() { return quantity; }

    public int getTotalPriceInCents() {
        return unitPriceInCents * quantity;
    }

    /** Two lines can be merged if they describe the same product at the same unit price. */
    public boolean canMergeWith(LineItem other) {
        return other != null && name.equals(other.name) && unitPriceInCents == other.unitPriceInCents;
    }

    /** Returns a new line with the summed quantity; neither input is modified. */
    public LineItem mergeWith(LineItem other) {
        if (!canMergeWith(other)) {
            throw new IllegalArgumentException("Line items must have the same name and unit price");
        }
        long combinedQuantity = (long) quantity + other.quantity;
        if (combinedQuantity > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Combined quantity exceeds the supported range.");
        }
        return new LineItem(name, unitPriceInCents, (int) combinedQuantity);
    }

    @Override
    public String toString() {
        return quantity + " * " + name;
    }
}
