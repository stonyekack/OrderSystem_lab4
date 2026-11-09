package com.hsb.mkss1;

import com.hsb.mkss1.adapters.OrderFormatter;
import com.hsb.mkss1.adapters.persistence.InMemoryOrderRepository;
import com.hsb.mkss1.entities.LineItem;
import com.hsb.mkss1.entities.Order;
import com.hsb.mkss1.entities.OrderRepository;
import com.hsb.mkss1.usecases.OrderInteractor;
import com.hsb.mkss1.usecases.input.AddLineItemInputData;
import com.hsb.mkss1.usecases.input.FinishOrderInputData;
import com.hsb.mkss1.usecases.output.LineItemOutputData;
import com.hsb.mkss1.usecases.output.OrderOutputBoundary;
import com.hsb.mkss1.usecases.output.OrderOutputData;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Dependency-free tests: run main() and check the output (exit code 1 on failure). */
public class OrderSystemTests {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        // ================= Ring 1: Entities =================
        test("LineItem: total price, name is trimmed", () -> {
            LineItem item = new LineItem("  Pen ", 150, 3);
            check(item.getName().equals("Pen") && item.getTotalPriceInCents() == 450);
        });
        test("LineItem: invalid values are rejected", () -> {
            expectThrows(IllegalArgumentException.class, () -> new LineItem(" ", 100, 1));
            expectThrows(IllegalArgumentException.class, () -> new LineItem(null, 100, 1));
            expectThrows(IllegalArgumentException.class, () -> new LineItem("Pen", -1, 1));
            expectThrows(IllegalArgumentException.class, () -> new LineItem("Pen", 100, 0));
        });
        test("Order: same name and unit price are merged, different price is not", () -> {
            Order order = new Order();
            order.addItem(new LineItem("Pen", 150, 2));
            order.addItem(new LineItem("Pen", 150, 3));
            order.addItem(new LineItem("Pen", 200, 1));
            check(order.getItems().size() == 2);
            check(order.getItems().get(0).getQuantity() == 5);
            check(order.getLumpSum() == 5 * 150 + 200);
        });
        test("Order: finish() sets checkout time; empty or finished orders cannot be finished", () -> {
            Order empty = new Order();
            expectThrows(IllegalStateException.class, empty::finish);
            Order order = new Order();
            order.addItem(new LineItem("Pen", 100, 1));
            order.finish();
            check(order.isFinished() && order.getCheckoutAt() != null);
            expectThrows(IllegalStateException.class, order::finish);
            expectThrows(IllegalStateException.class, () -> order.addItem(new LineItem("Pen", 100, 1)));
        });

        // ================= Ring 3: Repository implementation =================
        test("Repository: insert assigns increasing ids", () -> {
            OrderRepository repo = new InMemoryOrderRepository();
            Order a = repo.insert(new Order());
            Order b = repo.insert(new Order());
            check(a.getId() != null && b.getId() != null && b.getId() > a.getId());
        });
        test("Repository: insert rejects an order that already has an id", () -> {
            OrderRepository repo = new InMemoryOrderRepository();
            Order stored = repo.insert(new Order());
            expectThrows(IllegalArgumentException.class, () -> repo.insert(stored));
        });
        test("Repository: findById returns a copy; changes are stored only by update()", () -> {
            OrderRepository repo = new InMemoryOrderRepository();
            long id = repo.insert(new Order()).getId();
            Order copy = repo.findById(id).orElseThrow();
            copy.addItem(new LineItem("Pen", 100, 1));
            check(repo.findById(id).orElseThrow().isEmpty());
            repo.update(copy);
            check(!repo.findById(id).orElseThrow().isEmpty());
        });
        test("Repository: update of an unknown order fails", () -> {
            OrderRepository repo = new InMemoryOrderRepository();
            Order other = new InMemoryOrderRepository().insert(new Order()); // id 1 but not in 'repo'
            expectThrows(IllegalArgumentException.class, () -> repo.update(other));
            expectThrows(IllegalArgumentException.class, () -> repo.update(new Order()));
        });
        test("Repository: findById of an unknown id is empty", () ->
                check(new InMemoryOrderRepository().findById(42).isEmpty()));
        test("Repository: findAll returns all orders, deleteAll removes them", () -> {
            OrderRepository repo = new InMemoryOrderRepository();
            repo.insert(new Order());
            repo.insert(new Order());
            check(repo.findAll().size() == 2);
            repo.deleteAll();
            check(repo.findAll().isEmpty());
        });
        test("Repository: concurrent inserts get unique ids", () -> {
            OrderRepository repo = new InMemoryOrderRepository();
            ExecutorService pool = Executors.newFixedThreadPool(8);
            List<Future<Long>> futures = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                Callable<Long> task = () -> repo.insert(new Order()).getId();
                futures.add(pool.submit(task));
            }
            Set<Long> ids = new HashSet<>();
            for (Future<Long> f : futures) ids.add(f.get());
            pool.shutdown();
            check(ids.size() == 200 && repo.findAll().size() == 200);
        });

        // ================= Ring 2: Use case interactor (with a fake presenter) =================
        test("Interactor: startOrder stores an empty order and presents its id", () -> {
            OrderRepository repo = new InMemoryOrderRepository();
            RecordingPresenter presenter = new RecordingPresenter();
            new OrderInteractor(repo, presenter).startOrder();
            check(presenter.started.size() == 1);
            OrderOutputData data = presenter.started.get(0);
            check(data.items().isEmpty() && repo.findById(data.orderId()).isPresent());
        });
        test("Interactor: addLineItem stores, merges and presents the new state", () -> {
            RecordingPresenter p = new RecordingPresenter();
            OrderInteractor interactor = new OrderInteractor(new InMemoryOrderRepository(), p);
            interactor.startOrder();
            long id = p.started.get(0).orderId();
            interactor.addLineItem(new AddLineItemInputData(id, "Pen", 150, 2));
            interactor.addLineItem(new AddLineItemInputData(id, "Pen", 150, 3));
            interactor.addLineItem(new AddLineItemInputData(id, "Book", 500, 1));
            check(p.errors.isEmpty() && p.added.size() == 3);
            OrderOutputData last = p.added.get(2);
            check(last.items().size() == 2 && last.lumpSumInCents() == 5 * 150 + 500);
        });
        test("Interactor: two orders are independent (multiple clients)", () -> {
            RecordingPresenter p = new RecordingPresenter();
            OrderInteractor interactor = new OrderInteractor(new InMemoryOrderRepository(), p);
            interactor.startOrder();
            interactor.startOrder();
            long a = p.started.get(0).orderId();
            long b = p.started.get(1).orderId();
            interactor.addLineItem(new AddLineItemInputData(a, "Pen", 100, 1));
            interactor.addLineItem(new AddLineItemInputData(b, "Book", 500, 1));
            check(p.added.get(0).lumpSumInCents() == 100 && p.added.get(1).lumpSumInCents() == 500);
        });
        test("Interactor: finishOrder presents the finished order and stores it", () -> {
            OrderRepository repo = new InMemoryOrderRepository();
            RecordingPresenter p = new RecordingPresenter();
            OrderInteractor interactor = new OrderInteractor(repo, p);
            interactor.startOrder();
            long id = p.started.get(0).orderId();
            interactor.addLineItem(new AddLineItemInputData(id, "Pen", 100, 1));
            interactor.finishOrder(new FinishOrderInputData(id));
            check(p.finished.size() == 1 && p.finished.get(0).isFinished());
            check(repo.findById(id).orElseThrow().isFinished());
        });
        test("Interactor: errors go to presentError (empty/finished order, bad input, unknown id)", () -> {
            RecordingPresenter p = new RecordingPresenter();
            OrderInteractor interactor = new OrderInteractor(new InMemoryOrderRepository(), p);
            interactor.startOrder();
            long id = p.started.get(0).orderId();
            interactor.finishOrder(new FinishOrderInputData(id));                       // empty
            interactor.addLineItem(new AddLineItemInputData(999, "Pen", 100, 1));        // unknown id
            interactor.addLineItem(new AddLineItemInputData(id, " ", 100, 1));           // blank name
            interactor.addLineItem(new AddLineItemInputData(id, "Pen", -1, 1));          // negative price
            interactor.addLineItem(new AddLineItemInputData(id, "Pen", 100, 1));         // ok
            interactor.finishOrder(new FinishOrderInputData(id));                       // ok
            interactor.finishOrder(new FinishOrderInputData(id));                       // already finished
            interactor.addLineItem(new AddLineItemInputData(id, "Pen", 100, 1));         // finished -> frozen
            check(p.errors.size() == 6);
            check(p.added.size() == 1 && p.finished.size() == 1);
        });
        test("Interactor works with ANY repository implementation (dependency inversion)", () -> {
            Map<Long, Order> map = new HashMap<>();
            OrderRepository fake = new OrderRepository() { // no InMemoryOrderRepository involved at all
                public Order insert(Order o) { Order s = o.copyWithId(map.size() + 1L); map.put(s.getId(), s); return s.copy(); }
                public void update(Order o) { map.put(o.getId(), o.copy()); }
                public Optional<Order> findById(long id) { return Optional.ofNullable(map.get(id)).map(Order::copy); }
                public List<Order> findAll() { return new ArrayList<>(map.values()); }
                public void deleteAll() { map.clear(); }
            };
            RecordingPresenter p = new RecordingPresenter();
            OrderInteractor interactor = new OrderInteractor(fake, p);
            interactor.startOrder();
            interactor.addLineItem(new AddLineItemInputData(p.started.get(0).orderId(), "Pen", 100, 2));
            check(p.errors.isEmpty() && map.get(1L).getLumpSum() == 200);
        });

        // ================= Ring 3: Presentation helpers =================
        test("OrderFormatter.formatPrice", () -> {
            check(OrderFormatter.formatPrice(5).equals("0.05 EUR"));
            check(OrderFormatter.formatPrice(1234).equals("12.34 EUR"));
            check(OrderFormatter.formatPrice(1200).equals("12.00 EUR"));
        });
        test("OrderFormatter.sortByPrice sorts ascending and leaves the input untouched", () -> {
            OrderOutputData order = new OrderOutputData(1, List.of(
                    new LineItemOutputData("Expensive", 1000, 1, 1000),
                    new LineItemOutputData("Cheap", 10, 1, 10)), 1010, null);
            List<LineItemOutputData> sorted = OrderFormatter.sortByPrice(order);
            check(sorted.get(0).totalPriceInCents() == 10);
            check(order.items().get(0).totalPriceInCents() == 1000);
        });
        test("OrderFormatter.formatSummary contains items, sum and checkout date", () -> {
            RecordingPresenter p = new RecordingPresenter();
            OrderInteractor interactor = new OrderInteractor(new InMemoryOrderRepository(), p);
            interactor.startOrder();
            long id = p.started.get(0).orderId();
            interactor.addLineItem(new AddLineItemInputData(id, "Pen", 150, 2));
            interactor.finishOrder(new FinishOrderInputData(id));
            String summary = OrderFormatter.formatSummary(p.finished.get(0));
            check(summary.contains("2 * Pen = 3.00 EUR") && summary.contains("Sum: 3.00 EUR")
                    && summary.contains("Checkout at: "));
        });

        // ================= Architecture: the dependency rule =================
        test("Dependency rule: entities import nothing from use cases / adapters / UI", () ->
                check(forbiddenImports("entities", "com.hsb.mkss1.usecases", "com.hsb.mkss1.adapters",
                        "javax.swing", "java.awt", "java.io").isEmpty()));
        test("Dependency rule: use cases import nothing from adapters / UI and never print", () -> {
            check(forbiddenImports("usecases", "com.hsb.mkss1.adapters", "javax.swing", "java.awt").isEmpty());
            check(forbiddenText("usecases", "System.out", "System.in").isEmpty());
        });
        test("Dependency rule: controllers/presenters (CLI, GUI) do not use entities or repositories", () ->
                check(forbiddenImports("adapters/console", "com.hsb.mkss1.entities", "adapters.persistence").isEmpty()
                        && forbiddenImports("adapters/gui", "com.hsb.mkss1.entities", "adapters.persistence").isEmpty()));

        System.out.println("\n" + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    // ---------- test doubles & helpers ----------

    /** Fake presenter: records everything the interactor reports. */
    private static class RecordingPresenter implements OrderOutputBoundary {
        final List<OrderOutputData> started = new ArrayList<>();
        final List<OrderOutputData> added = new ArrayList<>();
        final List<OrderOutputData> finished = new ArrayList<>();
        final List<String> errors = new ArrayList<>();

        public void presentOrderStarted(OrderOutputData o) { started.add(o); }
        public void presentLineItemAdded(OrderOutputData o) { added.add(o); }
        public void presentOrderFinished(OrderOutputData o) { finished.add(o); }
        public void presentError(String message) { errors.add(message); }
    }

    private static List<Path> sourceFiles(String subPackage) throws IOException {
        Path dir = Paths.get("src/com/hsb/mkss1/" + subPackage);
        if (!Files.isDirectory(dir)) dir = Paths.get("OrderSystem/src/com/hsb/mkss1/" + subPackage);
        try (Stream<Path> files = Files.walk(dir)) {
            return files.filter(f -> f.toString().endsWith(".java")).collect(Collectors.toList());
        }
    }

    /** Import lines of the given sub-package that mention one of the forbidden prefixes. */
    private static List<String> forbiddenImports(String subPackage, String... forbidden) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : sourceFiles(subPackage)) {
            for (String line : Files.readAllLines(file)) {
                if (!line.startsWith("import ")) continue;
                for (String f : forbidden) {
                    if (line.contains(f)) violations.add(file.getFileName() + ": " + line);
                }
            }
        }
        return violations;
    }

    private static List<String> forbiddenText(String subPackage, String... forbidden) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : sourceFiles(subPackage)) {
            String content = Files.readString(file);
            for (String f : forbidden) {
                if (content.contains(f)) violations.add(file.getFileName() + ": " + f);
            }
        }
        return violations;
    }

    private interface Body { void run() throws Exception; }

    private static void test(String name, Body body) {
        try {
            body.run();
            passed++;
            System.out.println("PASS  " + name);
        } catch (Throwable t) {
            failed++;
            System.out.println("FAIL  " + name + " -> " + t);
        }
    }

    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("check failed");
    }

    private static void expectThrows(Class<? extends Throwable> type, Body body) {
        try {
            body.run();
        } catch (Throwable t) {
            if (type.isInstance(t)) return;
            throw new AssertionError("expected " + type.getSimpleName() + " but got " + t);
        }
        throw new AssertionError("expected " + type.getSimpleName() + " but nothing was thrown");
    }
}
