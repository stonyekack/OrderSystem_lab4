package com.hsb.mkss1.adapters;

import com.hsb.mkss1.usecases.input.OrderInputBoundary;
import com.hsb.mkss1.usecases.output.OrderOutputBoundary;

/**
 * A user interface is a combined CONTROLLER + PRESENTER:
 * it calls the InputBoundary (controller part) and implements the OutputBoundary (presenter part).
 * The input boundary is set after construction because interactor and presenter reference each other.
 */
public interface UserInterface extends OrderOutputBoundary {
    void setInputBoundary(OrderInputBoundary inputBoundary);

    void run();
}
