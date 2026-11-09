package com.hsb.mkss1.usecases.output;

/** OUTPUT DATA: one line of an order, as plain values (the presenter never sees the LineItem entity). */
public record LineItemOutputData(String name, int unitPriceInCents, int quantity, int totalPriceInCents) { }
