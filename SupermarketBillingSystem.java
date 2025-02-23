import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.sql.*;
import javax.swing.*;

public class SupermarketBillingSystem extends JFrame {
    private Connection connection;
    private JTextArea billArea;
    private JTextField productField, quantityField, totalField, discountField;
    private JComboBox<String> productComboBox;
    private JButton addButton, refreshButton, applyDiscountButton, printButton;
    private double totalAmount = 0.0;
    private double discount = 0.0;

    // Constructor: Setup JDBC connection and GUI
    public SupermarketBillingSystem() {
        // Establish JDBC connection
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/supermarketbilling", "root", "bhuvan007");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database connection failed.");
            System.exit(1);
        }

        // Set up the GUI
        setTitle("Supermarket Billing System");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel for product selection
        JPanel productPanel = new JPanel();
        productPanel.setLayout(new GridLayout(1, 2));
        productPanel.add(new JLabel("Product:"));
        productComboBox = new JComboBox<>();
        loadProducts();
        productPanel.add(productComboBox);
        add(productPanel, BorderLayout.NORTH);

        // Panel for user input
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(5, 2));

        inputPanel.add(new JLabel("Product ID:"));
        productField = new JTextField();
        inputPanel.add(productField);

        inputPanel.add(new JLabel("Quantity:"));
        quantityField = new JTextField();
        inputPanel.add(quantityField);

        inputPanel.add(new JLabel("Total:"));
        totalField = new JTextField();
        totalField.setEditable(false);
        inputPanel.add(totalField);

        inputPanel.add(new JLabel("Discount (%):"));
        discountField = new JTextField();
        inputPanel.add(discountField);

        add(inputPanel, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonPanel = new JPanel();
        addButton = new JButton("Add to Bill");
        refreshButton = new JButton("Refresh Inventory");
        applyDiscountButton = new JButton("Apply Discount");
        printButton = new JButton("Print Bill");
        buttonPanel.add(addButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(applyDiscountButton);
        buttonPanel.add(printButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Text area to display the bill
        billArea = new JTextArea();
        billArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(billArea);
        add(scrollPane, BorderLayout.EAST);

        // Button actions
        addButton.addActionListener(e -> addToBill());
        refreshButton.addActionListener(e -> refreshInventory());
        applyDiscountButton.addActionListener(e -> applyDiscount());
        printButton.addActionListener(e -> printBill());
    }

    // Load products into the combo box
    private void loadProducts() {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT product_name FROM Products")) {
            while (resultSet.next()) {
                productComboBox.addItem(resultSet.getString("product_name"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading products.");
        }
    }

    private void addToBill() {
        try {
            int productId = Integer.parseInt(productField.getText());
            int quantity = Integer.parseInt(quantityField.getText());

            // Query product details
            String query = "SELECT product_name, price, quantity FROM Products WHERE product_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, productId);
                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    String productName = resultSet.getString("product_name");
                    double price = resultSet.getDouble("price");
                    int availableQuantity = resultSet.getInt("quantity");

                    if (availableQuantity >= quantity) {
                        double cost = price * quantity;
                        totalAmount += cost;

                        // Update inventory
                        String updateQuery = "UPDATE Products SET quantity = quantity - ? WHERE product_id = ?";
                        try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                            updateStmt.setInt(1, quantity);
                            updateStmt.setInt(2, productId);
                            updateStmt.executeUpdate();
                        }

                        // Display product in the bill area
                        billArea.append("Product: " + productName + ", Quantity: " + quantity + ", Price: " + cost + "\n");

                        // Update total
                        totalField.setText(String.valueOf(totalAmount));
                    } else {
                        JOptionPane.showMessageDialog(this, "Insufficient stock for this product.");
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Product not found.");
                }
            }
        } catch (SQLException | NumberFormatException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error while adding product to the bill.");
        }
    }

    private void refreshInventory() {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM Products")) {
            billArea.setText("Inventory:\n");
            billArea.append("ID | Product Name | Price | Quantity\n");
            while (resultSet.next()) {
                int id = resultSet.getInt("product_id");
                String name = resultSet.getString("product_name");
                double price = resultSet.getDouble("price");
                int quantity = resultSet.getInt("quantity");

                billArea.append(id + " | " + name + " | " + price + " | " + quantity + "\n");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void applyDiscount() {
        try {
            discount = Double.parseDouble(discountField.getText());
            totalAmount -= (totalAmount * discount / 100);
            totalField.setText(String.valueOf(totalAmount));
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Invalid discount value.");
        }
    }

    private void printBill() {
        BillPrintWindow printWindow = new BillPrintWindow(billArea.getText());
        printWindow.setVisible(true);
    }

    // Main method to run the application
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}

// BillPrintWindow class
class BillPrintWindow extends JFrame {
    private JTextArea billTextArea;

    public BillPrintWindow(String billText) {
        setTitle("Print Bill");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create a text area to display the bill
        billTextArea = new JTextArea(billText);
        billTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(billTextArea);
        add(scrollPane, BorderLayout.CENTER);

        // Print button
        JButton printButton = new JButton("Print");
        printButton.addActionListener(new PrintAction(billText));
        add(printButton, BorderLayout.SOUTH);
    }

    private class PrintAction implements ActionListener {
        private String billText;

        public PrintAction(String billText) {
            this.billText = billText;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                PrinterJob job = PrinterJob.getPrinterJob();
                job.setPrintable(new BillPrintable(billText));
                if (job.printDialog()) {
                    job.print();
                }
            } catch (PrinterException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(BillPrintWindow.this, "Error printing the bill.");
            }
        }
    }

    private class BillPrintable implements Printable {
        private String billText;

        public BillPrintable(String billText) {
            this.billText = billText;
        }

        @Override
        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
            if (pageIndex > 0) {
                return NO_SUCH_PAGE;
            }

            Graphics2D g2d = (Graphics2D) graphics;
            g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

            Font font = new Font("Monospaced", Font.PLAIN, 12);
            g2d.setFont(font);

            String[] lines = billText.split("\n");
            int y = 0;
            for (String line : lines) {
                y += g2d.getFontMetrics().getHeight();
                g2d.drawString(line, 0, y);
            }

            return PAGE_EXISTS;
        }
    }
}

// LoginFrame class
class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private Connection connection;

    public LoginFrame() {
        setTitle("Login");
        setSize(400, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Establish JDBC connection
        try {
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/supermarketbilling", "root", "bhuvan007");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Create a panel for the login form
        JPanel loginPanel = new JPanel();
        loginPanel.setLayout(new GridLayout(3, 2));

        // Add labels and fields for username and password
        loginPanel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        loginPanel.add(usernameField);

        loginPanel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        loginPanel.add(passwordField);

        // Add a login button
        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            if (username.equals("admin") && password.equals("password")) { // Simple login check
                dispose();
                new SupermarketBillingSystem().setVisible(true);
            } else {
                JOptionPane.showMessageDialog(null, "Invalid username or password");
            }
        });
        loginPanel.add(loginButton);

        add(loginPanel, BorderLayout.CENTER);
    }
}