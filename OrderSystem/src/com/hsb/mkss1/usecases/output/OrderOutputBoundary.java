package com.hsb.mkss1.usecases.output;

/**
 * OUTPUT BOUNDARY (use case output port): how the use case reports results and errors.
 * Defined in the use case ring, implemented by the presenter (outer ring)
 * -> the interactor can talk to the UI without depending on it.
 */
public interface OrderOutputBoundary {
    void presentOrderStarted(OrderOutputData order);

    void presentLineItemAdded(OrderOutputData order);

    void presentOrderFinished(OrderOutputData order);

    /** Business or validation error (invalid input, unknown order, finished order, ...). */
    void presentError(String message);
}
