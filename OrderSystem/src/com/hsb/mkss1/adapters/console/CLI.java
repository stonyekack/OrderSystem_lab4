package com.hsb.mkss1.adapters.console;

import com.hsb.mkss1.adapters.OrderFormatter;
import com.hsb.mkss1.adapters.UserInterface;
import com.hsb.mkss1.adapters.console.util.Input;
import com.hsb.mkss1.usecases.input.AddLineItemInputData;
import com.hsb.mkss1.usecases.input.FinishOrderInputData;
import com.hsb.mkss1.usecases.input.OrderInputBoundary;
import com.hsb.mkss1.usecases.output.OrderOutputData;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Console CONTROLLER + PRESENTER (ring 3).
 * Controller part: reads input and calls the OrderInputBoundary.
 * Presenter part: implements OrderOutputBoundary and prints what the use case reports.
 * It only knows use case interfaces and data structures - no entities, no repository.
 */
public class CLI implements UserInterface {
    private OrderInputBoundary inputBoundary;
    private long currentOrderId;      // state of the dialogue, not of the business
    private boolean orderFinished;

    @Override
    public void setInputBoundary(OrderInputBoundary inputBoundary) {
        this.inputBoundary = Objects.requireNonNull(inputBoundary, "Input boundary must not be null");
    }

    @Override
    public void run() {
        Objects.requireNonNull(inputBoundary, "Call setInputBoundary() before run()");
        try {
            do {
                orderFinished = false;
                inputBoundary.startOrder();   // answer arrives in presentOrderStarted()
                runOrderMenu();
            } while (askForAnotherOrder());
        } catch (NoSuchElementException exception) {
            // Closing console input exits without finishing an incomplete order.
        }
    }

    // ---------- Controller part ----------

    private void runOrderMenu() {
        while (!orderFinished) {
            printMenu();
            int choice = Input.readInt();
            switch (choice) {
                case 0:
                    inputBoundary.finishOrder(new FinishOrderInputData(currentOrderId));
                    break; // loop ends only if presentOrderFinished() was called
                case 1:
                    addLineItemFromInput();
                    break;
                default:
                    System.out.println("Invalid choice. Please select 0 or 1.");
            }
        }
    }

    private void printMenu() {
        System.out.println("Welcome! What would you like to do?");
        System.out.println("(0) Finish order");
        System.out.println("(1) Order product");
    }

    private void addLineItemFromInput() {
        System.out.println("Name: ");
        String name = Input.readString();
        if (name == null) throw new NoSuchElementException("Input closed");
        System.out.println("Unit price (in cents): ");
        int unitPrice = Input.readInt();
        System.out.println("Quantity: ");
        int quantity = Input.readInt();
        // Only raw values are passed on; the use case creates the LineItem.
        inputBoundary.addLineItem(new AddLineItemInputData(currentOrderId, name, unitPrice, quantity));
    }

    private boolean askForAnotherOrder() {
        while (true) {
            System.out.print("\nWould you like to place another order? (y/n): ");
            String answer = Input.readString();
            if (answer == null) return false;
            switch (answer.trim().toLowerCase(Locale.ROOT)) {
                case "y":
                case "yes":
                    System.out.println("\nStarting a new order...\n");
                    return true;
                case "n":
                case "no":
                    System.out.println("\nGoodbye!");
                    return false;
                default:
                    System.out.println("Invalid choice. Please type 'y' or 'n'.");
            }
        }
    }

    // ---------- Presenter part (OrderOutputBoundary) ----------

    @Override
    public void presentOrderStarted(OrderOutputData order) {
        currentOrderId = order.orderId();
    }

    @Override
    public void presentLineItemAdded(OrderOutputData order) {
        System.out.println("Added. Current total: " + OrderFormatter.formatPrice(order.lumpSumInCents()) + "\n");
    }

    @Override
    public void presentOrderFinished(OrderOutputData order) {
        System.out.println("\n" + OrderFormatter.formatSummary(order));
        orderFinished = true;
    }

    @Override
    public void presentError(String message) {
        System.out.println(message);
    }
}
