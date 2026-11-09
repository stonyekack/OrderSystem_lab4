package com.hsb.mkss1.usecases.input;

/**
 * INPUT BOUNDARY (use case input port): what the outside world (controller) may ask the application.
 * Implemented by the interactor; the controller only knows this interface.
 * Results are NOT returned: they are pushed to the OutputBoundary.
 */
public interface OrderInputBoundary {
    /** Use case "start a new order". */
    void startOrder();

    /** Use case "add a line item to an order". */
    void addLineItem(AddLineItemInputData input);

    /** Use case "finish (check out) an order". */
    void finishOrder(FinishOrderInputData input);
}
