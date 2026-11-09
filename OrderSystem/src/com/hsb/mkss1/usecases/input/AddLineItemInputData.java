package com.hsb.mkss1.usecases.input;

/** INPUT DATA: plain data the controller hands to the use case. Contains raw values, no entities. */
public record AddLineItemInputData(long orderId, String name, int unitPriceInCents, int quantity) { }
