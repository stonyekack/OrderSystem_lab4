package com.hsb.mkss1.usecases;

import com.hsb.mkss1.entities.LineItem;
import com.hsb.mkss1.entities.Order;
import com.hsb.mkss1.entities.OrderRepository;
import com.hsb.mkss1.usecases.input.AddLineItemInputData;
import com.hsb.mkss1.usecases.input.FinishOrderInputData;
import com.hsb.mkss1.usecases.input.OrderInputBoundary;
import com.hsb.mkss1.usecases.output.LineItemOutputData;
import com.hsb.mkss1.usecases.output.OrderOutputBoundary;
import com.hsb.mkss1.usecases.output.OrderOutputData;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * USE CASE INTERACTOR (ring 2, replaces OrderService): application-specific business rules.
 * Depends only on entities and on abstractions (OrderRepository, OrderOutputBoundary).
 * It creates the LineItems itself (the UI only supplies raw input data) and never
 * touches the console or Swing: all output goes through the OutputBoundary.
 */
public class OrderInteractor implements OrderInputBoundary {
    private final OrderRepository repository;
    private final OrderOutputBoundary presenter;

    public OrderInteractor(OrderRepository repository, OrderOutputBoundary presenter) {
        this.repository = Objects.requireNonNull(repository, "Repository must not be null");
        this.presenter = Objects.requireNonNull(presenter, "Presenter must not be null");
    }

    @Override
    public void startOrder() {
        Order order = repository.insert(new Order());
        presenter.presentOrderStarted(toOutputData(order));
    }

    @Override
    public void addLineItem(AddLineItemInputData input) {
        OrderOutputData result;
        try {
            Order order = loadOrder(input.orderId());
            order.addItem(new LineItem(input.name(), input.unitPriceInCents(), input.quantity()));
            repository.update(order);
            result = toOutputData(order);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            presenter.presentError(exception.getMessage());
            return;
        }
        presenter.presentLineItemAdded(result);
    }

    @Override
    public void finishOrder(FinishOrderInputData input) {
        OrderOutputData result;
        try {
            Order order = loadOrder(input.orderId());
            order.finish();
            repository.update(order);
            result = toOutputData(order);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            presenter.presentError(exception.getMessage());
            return;
        }
        presenter.presentOrderFinished(result);
    }

    private Order loadOrder(long orderId) {
        return repository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown order id: " + orderId));
    }

    /** Entity -> output data (so that no entity leaves the use case ring). */
    private static OrderOutputData toOutputData(Order order) {
        List<LineItemOutputData> items = new ArrayList<>();
        for (LineItem item : order.getItems()) {
            items.add(new LineItemOutputData(item.getName(), item.getUnitPriceInCents(),
                    item.getQuantity(), item.getTotalPriceInCents()));
        }
        return new OrderOutputData(order.getId(), items, order.getLumpSum(), order.getCheckoutAt());
    }
}
