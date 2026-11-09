package com.hsb.mkss1.adapters.gui;

import com.hsb.mkss1.adapters.OrderFormatter;
import com.hsb.mkss1.adapters.UserInterface;
import com.hsb.mkss1.usecases.input.AddLineItemInputData;
import com.hsb.mkss1.usecases.input.FinishOrderInputData;
import com.hsb.mkss1.usecases.input.OrderInputBoundary;
import com.hsb.mkss1.usecases.output.LineItemOutputData;
import com.hsb.mkss1.usecases.output.OrderOutputData;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Swing CONTROLLER + PRESENTER (ring 3).
 * Button handlers = controller (call the OrderInputBoundary);
 * the OrderOutputBoundary methods = presenter (update the widgets from OrderOutputData).
 * The widgets are never refreshed by asking the service/repository: the use case pushes the new state.
 */
public class GUI implements UserInterface {
    private OrderInputBoundary inputBoundary;
    private long currentOrderId;

    private final DefaultListModel<String> cartModel = new DefaultListModel<>();
    private final JLabel totalLabel = new JLabel();
    private final JTextField productName = new JTextField();
    private final JTextField productPrice = new JTextField();
    private final JTextField productQuantity = new JTextField();
    private JFrame frame;

    @Override
    public void setInputBoundary(OrderInputBoundary inputBoundary) {
        this.inputBoundary = Objects.requireNonNull(inputBoundary, "Input boundary must not be null");
    }

    @Override
    public void run() {
        Objects.requireNonNull(inputBoundary, "Call setInputBoundary() before run()");
        SwingUtilities.invokeLater(this::buildAndShow); // Swing code must run on the event thread
    }

    private void buildAndShow() {
        frame = new JFrame("Order System");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JList<String> cartList = new JList<>(cartModel);
        JScrollPane cartPane = new JScrollPane(cartList);
        cartPane.setBorder(BorderFactory.createTitledBorder("Shopping cart"));

        JPanel center = new JPanel(new BorderLayout());
        center.add(cartPane, BorderLayout.CENTER);
        center.add(totalLabel, BorderLayout.SOUTH);

        JPanel productPanel = new JPanel(new GridLayout(4, 2, 4, 4));
        productPanel.setBorder(BorderFactory.createTitledBorder("Add product"));
        productPanel.add(new JLabel("Name"));
        productPanel.add(productName);
        productPanel.add(new JLabel("Unit price (cents)"));
        productPanel.add(productPrice);
        productPanel.add(new JLabel("Quantity"));
        productPanel.add(productQuantity);
        productPanel.add(new JLabel());
        JButton addProductButton = new JButton("Add product");
        addProductButton.addActionListener(e -> addLineItem());
        productPanel.add(addProductButton);

        JButton finishButton = new JButton("Finish order");
        finishButton.addActionListener(e -> inputBoundary.finishOrder(new FinishOrderInputData(currentOrderId)));

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.add(productPanel);
        bottom.add(finishButton);

        frame.getContentPane().setLayout(new BorderLayout(8, 8));
        frame.getContentPane().add(center, BorderLayout.CENTER);
        frame.getContentPane().add(bottom, BorderLayout.SOUTH);

        frame.setSize(420, 450);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        inputBoundary.startOrder(); // widgets exist now -> presentOrderStarted() can fill them
    }

    // ---------- Controller part ----------

    private void addLineItem() {
        int price;
        int quantity;
        try {
            price = parseInt(productPrice, "Unit price");
            quantity = parseInt(productQuantity, "Quantity");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage()); // pure input-format error: no need to bother the use case
            return;
        }
        inputBoundary.addLineItem(new AddLineItemInputData(currentOrderId, productName.getText(), price, quantity));
    }

    private static int parseInt(JTextField field, String label) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a whole number.");
        }
    }

    // ---------- Presenter part (OrderOutputBoundary) ----------

    @Override
    public void presentOrderStarted(OrderOutputData order) {
        currentOrderId = order.orderId();
        refreshCart(order); // empty cart for the new order
    }

    @Override
    public void presentLineItemAdded(OrderOutputData order) {
        productName.setText("");
        productPrice.setText("");
        productQuantity.setText("");
        refreshCart(order);
    }

    @Override
    public void presentOrderFinished(OrderOutputData order) {
        JTextArea summary = new JTextArea(OrderFormatter.formatSummary(order));
        summary.setEditable(false);
        summary.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JOptionPane.showMessageDialog(frame, summary, "Order summary", JOptionPane.INFORMATION_MESSAGE);
        inputBoundary.startOrder(); // after the summary is dismissed: new order, cart is cleared
    }

    @Override
    public void presentError(String message) {
        showError(message);
    }

    private void refreshCart(OrderOutputData order) {
        cartModel.clear();
        for (LineItemOutputData item : order.items()) { // in the order the items were added
            cartModel.addElement(OrderFormatter.formatItem(item));
        }
        totalLabel.setText("Total: " + OrderFormatter.formatPrice(order.lumpSumInCents()));
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
