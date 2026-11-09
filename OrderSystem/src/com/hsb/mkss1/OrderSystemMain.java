package com.hsb.mkss1;

import com.hsb.mkss1.adapters.UserInterface;
import com.hsb.mkss1.adapters.console.CLI;
import com.hsb.mkss1.adapters.gui.GUI;
import com.hsb.mkss1.adapters.persistence.InMemoryOrderRepository;
import com.hsb.mkss1.entities.OrderRepository;
import com.hsb.mkss1.usecases.OrderInteractor;

/**
 * Composition root ("Main" component, outermost ring): the only place that knows all concrete classes
 * and wires them together. Dependencies at source level point inwards, wiring happens here.
 */
public class OrderSystemMain {
    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0].trim().toLowerCase() : "cli";
        UserInterface ui;
        switch (mode) {
            case "cli":
                ui = new CLI();
                break;
            case "gui":
                ui = new GUI();
                break;
            default:
                System.err.println("Usage: java com.hsb.mkss1.OrderSystemMain [cli|gui]");
                System.exit(1);
                return;
        }
        OrderRepository repository = new InMemoryOrderRepository();
        ui.setInputBoundary(new OrderInteractor(repository, ui)); // ui is controller AND presenter
        ui.run();
    }
}
