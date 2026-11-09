package com.hsb.mkss1.entities;

import java.util.List;
import java.util.Optional;

/**
 * REPOSITORY INTERFACE (ring 1/2, "RepositoryInterface" in the figure).
 * The use cases depend on this abstraction; the implementation lives in the outer
 * ring (adapters.persistence) -> Dependency Inversion.
 * Implementations hand out COPIES of orders: changes only become visible after update().
 */
public interface OrderRepository {

    /** Stores a new order (id must be null) and returns the stored copy including its new id. */
    Order insert(Order order);

    /** Replaces the stored order with the same id. Throws IllegalArgumentException if unknown. */
    void update(Order order);

    Optional<Order> findById(long id);

    /** All orders, ordered by id. */
    List<Order> findAll();

    void deleteAll();
}
