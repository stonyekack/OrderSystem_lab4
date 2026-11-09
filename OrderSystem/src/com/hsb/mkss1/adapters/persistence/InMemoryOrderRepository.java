package com.hsb.mkss1.adapters.persistence;

import com.hsb.mkss1.entities.Order;
import com.hsb.mkss1.entities.OrderRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

/** REPOSITORY (ring 3, gateway): in-memory implementation of the entities.OrderRepository interface. */
public class InMemoryOrderRepository implements OrderRepository {
    // Thread-safe and sorted by id, so parallel clients are safe.
    private final Map<Long, Order> store = new ConcurrentSkipListMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    @Override
    public Order insert(Order order) {
        Objects.requireNonNull(order, "Order must not be null");
        if (order.getId() != null) {
            throw new IllegalArgumentException("Order already has an id; use update() instead.");
        }
        Order stored = order.copyWithId(nextId.getAndIncrement());
        store.put(stored.getId(), stored);
        return stored.copy(); // never expose the internal instance
    }

    @Override
    public void update(Order order) {
        Objects.requireNonNull(order, "Order must not be null");
        Long id = order.getId();
        if (id == null || !store.containsKey(id)) {
            throw new IllegalArgumentException("Unknown order id: " + id);
        }
        store.put(id, order.copy()); // store a copy, so later changes by the caller have no effect
    }

    @Override
    public Optional<Order> findById(long id) {
        Order stored = store.get(id);
        return Optional.ofNullable(stored).map(Order::copy);
    }

    @Override
    public List<Order> findAll() {
        List<Order> result = new ArrayList<>();
        for (Order stored : store.values()) {
            result.add(stored.copy());
        }
        return result;
    }

    @Override
    public void deleteAll() {
        store.clear();
    }
}
