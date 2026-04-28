package pharmacy.ui;

import pharmacy.model.CartItem;
import pharmacy.model.Medicine;
import pharmacy.model.Transaction;
import pharmacy.service.PharmacyService;
import pharmacy.service.TransactionService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Swing-based UI for the Pharmacy Management System.
 * Features: table sorting, keyboard shortcuts, undo, status bar,
 * expiry date tracking, low-stock warnings, search-highlight, total value display,
 * multi-item cart/basket with checkout, real-time input validation.
 */
public class PharmacyApp extends JFrame {
    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final int NEAR_EXPIRY_DAYS = 30;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Validation colors
    private static final Color FIELD_ERROR_BG = new Color(255, 245, 245);
    private static final Color FIELD_ERROR_BORDER = new Color(220, 80, 80);
    private static final Color FIELD_VALID_BORDER = new Color(56, 161, 105);
    private static final Color ERROR_TEXT = new Color(197, 68, 79);

    // Design System Colors - Soft Blue/Gray Medical Dashboard
    private static final Color PRIMARY_DARK = new Color(44, 62, 80);
    private static final Color PRIMARY = new Color(71, 103, 137);
    private static final Color PRIMARY_LIGHT = new Color(127, 163, 198);
    private static final Color ACCENT = new Color(86, 119, 156);
    private static final Color BACKGROUND = new Color(243, 245, 248);
    private static final Color CARD_BG = new Color(255, 255, 255);
    private static final Color TEXT_PRIMARY = new Color(40, 44, 52);
    private static final Color TEXT_SECONDARY = new Color(120, 130, 140);
    private static final Color BORDER_LIGHT = new Color(218, 224, 232);
    private static final Color SUCCESS = new Color(56, 161, 105);
    private static final Color WARNING = new Color(221, 173, 1);
    private static final Color DANGER = new Color(197, 68, 79);
    private static final Color TABLE_ROW_ALT = new Color(246, 248, 252);
    private static final Color TABLE_HEADER_BG = new Color(55, 75, 96);
    private static final Color STATUS_BAR_BG = new Color(44, 52, 63);

    // Input fields
    private JTextField medicineNameField;
    private JTextField quantityField;
    private JTextField priceField;
    private JTextField expiryField;

    // Validation labels
    private JLabel nameErrorLabel;
    private JLabel qtyErrorLabel;
    private JLabel priceErrorLabel;
    private JLabel expiryErrorLabel;

    // Inventory table
    private JTable medicineTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private JLabel statusLabel;

    // Cart
    private DefaultTableModel cartTableModel;
    private JTable cartTable;
    private JLabel cartTotalLabel;
    private JLabel cartItemCountLabel;
    private final List<CartItem> cartItems = new ArrayList<>();

    // Services
    private PharmacyService pharmacyService;
    private TransactionService transactionService;

    public PharmacyApp() {
        super("Pharmacy Management System");
        String filePath = askForFilePath();
        if (filePath == null) {
            JOptionPane.showMessageDialog(null,
                    "No file selected. Exiting application.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
            return;
        }

        try {
            pharmacyService = new PharmacyService(filePath);
            transactionService = new TransactionService(filePath);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(null,
                    "Failed to load data: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }

        initializeUI();
        setupKeyboardShortcuts();
        setupInputValidation();
        refreshTable();
        updateStatusBar();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 750));
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initializeUI() {
        Container container = getContentPane();
        container.setLayout(new BorderLayout(0, 0));
        container.setBackground(BACKGROUND);
        ((JComponent) container).setBorder(new EmptyBorder(16, 16, 16, 16));

        // --- Header Panel (Top) ---
        container.add(createHeaderPanel(), BorderLayout.NORTH);

        // --- Main Content (Center) — inventory table + cart side panel ---
        JPanel mainContent = new JPanel(new BorderLayout(0, 12));
        mainContent.setOpaque(false);

        // Input panel above the table
        mainContent.add(createInputPanel(), BorderLayout.NORTH);

        // Center: inventory table (left) + cart panel (right) in a split
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(700);
        splitPane.setResizeWeight(0.65);
        splitPane.setBorder(null);
        splitPane.setDividerSize(6);
        splitPane.setOpaque(false);

        splitPane.setLeftComponent(createInventoryPanel());
        splitPane.setRightComponent(createCartPanel());

        mainContent.add(splitPane, BorderLayout.CENTER);
        container.add(mainContent, BorderLayout.CENTER);

        // --- Bottom: buttons + status bar ---
        JPanel southPanel = new JPanel(new BorderLayout(0, 0));
        southPanel.setOpaque(false);
        southPanel.add(createButtonPanel(), BorderLayout.CENTER);
        southPanel.add(createStatusBar(), BorderLayout.SOUTH);
        container.add(southPanel, BorderLayout.SOUTH);
    }

    // =========================================================================
    // Panel Builders
    // =========================================================================

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PRIMARY_DARK);
        headerPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel titleLabel = new JLabel("\u2695 Pharmacy Management System");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Inventory Control & Tracking");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(170, 185, 200));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(subtitleLabel);

        headerPanel.add(textPanel, BorderLayout.WEST);

        JLabel versionLabel = new JLabel("v2.0");
        versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        versionLabel.setForeground(new Color(140, 160, 175));
        headerPanel.add(versionLabel, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createInputPanel() {
        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.setOpaque(false);
        wrapperPanel.setBorder(new EmptyBorder(0, 0, 4, 0));

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(CARD_BG);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, BORDER_LIGHT),
                new EmptyBorder(12, 20, 8, 20)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 10, 3, 10);
        gbc.anchor = GridBagConstraints.WEST;

        Font labelFont = new Font("Segoe UI", Font.PLAIN, 12);
        Font fieldFont = new Font("Segoe UI", Font.PLAIN, 13);

        // Row 0: Labels
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;

        JLabel nameLabel = new JLabel("Medicine Name *");
        nameLabel.setFont(labelFont);
        nameLabel.setForeground(TEXT_SECONDARY);
        gbc.gridx = 0;
        inputPanel.add(nameLabel, gbc);

        JLabel qtyLabel = new JLabel("Quantity *");
        qtyLabel.setFont(labelFont);
        qtyLabel.setForeground(TEXT_SECONDARY);
        gbc.gridx = 1;
        inputPanel.add(qtyLabel, gbc);

        JLabel priceLabel = new JLabel("Price ($) *");
        priceLabel.setFont(labelFont);
        priceLabel.setForeground(TEXT_SECONDARY);
        gbc.gridx = 2;
        inputPanel.add(priceLabel, gbc);

        JLabel expiryLabel = new JLabel("Expiry Date");
        expiryLabel.setFont(labelFont);
        expiryLabel.setForeground(TEXT_SECONDARY);
        gbc.gridx = 3;
        inputPanel.add(expiryLabel, gbc);

        // Row 1: Fields
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        medicineNameField = createValidatedField(18, fieldFont);
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        inputPanel.add(medicineNameField, gbc);

        quantityField = createValidatedField(8, fieldFont);
        gbc.gridx = 1;
        gbc.weightx = 0.3;
        inputPanel.add(quantityField, gbc);

        priceField = createValidatedField(8, fieldFont);
        gbc.gridx = 2;
        gbc.weightx = 0.3;
        inputPanel.add(priceField, gbc);

        expiryField = createValidatedField(10, fieldFont);
        expiryField.setToolTipText("Format: yyyy-MM-dd (optional)");
        gbc.gridx = 3;
        gbc.weightx = 0.3;
        inputPanel.add(expiryField, gbc);

        // Add button (spans rows 1-2)
        JButton addButton = createStyledButton("+  Add", PRIMARY);
        addButton.addActionListener(e -> handleAddMedicine());
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.gridheight = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.insets = new Insets(3, 20, 3, 0);
        inputPanel.add(addButton, gbc);

        // Row 2: Error labels
        gbc.gridheight = 1;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 10, 4, 10);

        nameErrorLabel = createErrorLabel();
        gbc.gridx = 0;
        inputPanel.add(nameErrorLabel, gbc);

        qtyErrorLabel = createErrorLabel();
        gbc.gridx = 1;
        inputPanel.add(qtyErrorLabel, gbc);

        priceErrorLabel = createErrorLabel();
        gbc.gridx = 2;
        inputPanel.add(priceErrorLabel, gbc);

        expiryErrorLabel = createErrorLabel();
        gbc.gridx = 3;
        inputPanel.add(expiryErrorLabel, gbc);

        wrapperPanel.add(inputPanel, BorderLayout.CENTER);
        return wrapperPanel;
    }

    private JTextField createValidatedField(int columns, Font font) {
        JTextField field = new JTextField(columns);
        field.setFont(font);
        field.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, BORDER_LIGHT),
                new EmptyBorder(8, 10, 8, 10)
        ));
        return field;
    }

    private JLabel createErrorLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        label.setForeground(ERROR_TEXT);
        return label;
    }

    private JScrollPane createInventoryPanel() {
        String[] columnNames = {"Name", "Quantity", "Price ($)", "Expiry Date", "Days Left", "Status"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1) return Integer.class;
                if (columnIndex == 2) return Double.class;
                if (columnIndex == 4) return Integer.class;
                return String.class;
            }
        };
        medicineTable = new JTable(tableModel);
        medicineTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        medicineTable.getTableHeader().setReorderingAllowed(false);
        medicineTable.setRowHeight(32);
        medicineTable.setIntercellSpacing(new Dimension(0, 0));
        medicineTable.setShowGrid(false);
        medicineTable.setBackground(CARD_BG);
        medicineTable.setSelectionBackground(PRIMARY_LIGHT);
        medicineTable.setSelectionForeground(Color.WHITE);

        JTableHeader header = medicineTable.getTableHeader();
        header.setBackground(TABLE_HEADER_BG);
        header.setForeground(Color.WHITE);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 13f));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40));
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);

        medicineTable.getColumnModel().getColumn(0).setPreferredWidth(180);
        medicineTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        medicineTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        medicineTable.getColumnModel().getColumn(3).setPreferredWidth(110);
        medicineTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        medicineTable.getColumnModel().getColumn(5).setPreferredWidth(120);

        rowSorter = new TableRowSorter<>(tableModel);
        medicineTable.setRowSorter(rowSorter);

        medicineTable.setDefaultRenderer(Object.class, new InventoryRenderer());
        medicineTable.setDefaultRenderer(Integer.class, new InventoryRenderer());
        medicineTable.setDefaultRenderer(Double.class, new InventoryRenderer());

        JScrollPane scrollPane = new JScrollPane(medicineTable);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createEmptyBorder(),
                        " Inventory",
                        javax.swing.border.TitledBorder.LEFT,
                        javax.swing.border.TitledBorder.TOP,
                        new Font("Segoe UI", Font.BOLD, 14),
                        PRIMARY_DARK
                ),
                BorderFactory.createCompoundBorder(
                        new MatteBorder(1, 1, 1, 1, BORDER_LIGHT),
                        new EmptyBorder(8, 0, 0, 0)
                )
        ));
        scrollPane.getViewport().setBackground(CARD_BG);
        return scrollPane;
    }

    private JPanel createCartPanel() {
        JPanel cartPanel = new JPanel(new BorderLayout(0, 8));
        cartPanel.setBackground(CARD_BG);
        cartPanel.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, BORDER_LIGHT),
                new EmptyBorder(12, 12, 12, 12)
        ));

        // Cart header
        JLabel cartTitle = new JLabel("Shopping Cart");
        cartTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cartTitle.setForeground(PRIMARY_DARK);

        cartItemCountLabel = new JLabel("0 items");
        cartItemCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cartItemCountLabel.setForeground(TEXT_SECONDARY);

        JPanel cartHeader = new JPanel(new BorderLayout());
        cartHeader.setOpaque(false);
        cartHeader.add(cartTitle, BorderLayout.WEST);
        cartHeader.add(cartItemCountLabel, BorderLayout.EAST);
        cartPanel.add(cartHeader, BorderLayout.NORTH);

        // Cart table
        String[] cartColumns = {"Medicine", "Qty", "Unit $", "Subtotal"};
        cartTableModel = new DefaultTableModel(cartColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1) return Integer.class;
                if (columnIndex == 2 || columnIndex == 3) return Double.class;
                return String.class;
            }
        };
        cartTable = new JTable(cartTableModel);
        cartTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cartTable.setRowHeight(28);
        cartTable.setShowGrid(false);
        cartTable.setIntercellSpacing(new Dimension(0, 0));
        cartTable.setBackground(CARD_BG);
        cartTable.setSelectionBackground(new Color(232, 240, 254));
        cartTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JTableHeader cartHeader2 = cartTable.getTableHeader();
        cartHeader2.setBackground(new Color(236, 240, 245));
        cartHeader2.setForeground(TEXT_PRIMARY);
        cartHeader2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        cartHeader2.setPreferredSize(new Dimension(0, 32));

        cartTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        cartTable.getColumnModel().getColumn(1).setPreferredWidth(40);
        cartTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        cartTable.getColumnModel().getColumn(3).setPreferredWidth(70);

        // Right-align numeric columns
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        cartTable.getColumnModel().getColumn(1).setCellRenderer(rightRenderer);
        cartTable.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);
        cartTable.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);

        JScrollPane cartScroll = new JScrollPane(cartTable);
        cartScroll.setBorder(new MatteBorder(1, 0, 1, 0, BORDER_LIGHT));
        cartPanel.add(cartScroll, BorderLayout.CENTER);

        // Cart footer: total + buttons
        JPanel cartFooter = new JPanel(new BorderLayout(0, 8));
        cartFooter.setOpaque(false);

        cartTotalLabel = new JLabel("Total: $0.00");
        cartTotalLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        cartTotalLabel.setForeground(PRIMARY_DARK);
        cartTotalLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        cartFooter.add(cartTotalLabel, BorderLayout.NORTH);

        // Cart action buttons
        JPanel cartButtons = new JPanel(new GridLayout(2, 2, 6, 6));
        cartButtons.setOpaque(false);

        JButton addToCartBtn = createStyledButton("+ Add to Cart", new Color(0, 150, 136));
        addToCartBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        addToCartBtn.addActionListener(e -> handleAddToCart());

        JButton removeFromCartBtn = createStyledButton("- Remove", DANGER);
        removeFromCartBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        removeFromCartBtn.addActionListener(e -> handleRemoveFromCart());

        JButton clearCartBtn = createStyledButton("Clear Cart", new Color(120, 130, 140));
        clearCartBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        clearCartBtn.addActionListener(e -> handleClearCart());

        JButton checkoutBtn = createStyledButton("Checkout", SUCCESS);
        checkoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        checkoutBtn.addActionListener(e -> handleCheckout());

        cartButtons.add(addToCartBtn);
        cartButtons.add(removeFromCartBtn);
        cartButtons.add(clearCartBtn);
        cartButtons.add(checkoutBtn);

        cartFooter.add(cartButtons, BorderLayout.CENTER);
        cartPanel.add(cartFooter, BorderLayout.SOUTH);

        return cartPanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 12));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(8, 0, 8, 0));

        JButton removeButton = createStyledButton("X  Remove", DANGER);
        removeButton.addActionListener(e -> handleRemoveMedicine());

        JButton searchButton = createStyledButton("?  Search", PRIMARY);
        searchButton.addActionListener(e -> handleSearchMedicine());

        JButton updateQtyButton = createStyledButton("#  Update Qty", ACCENT);
        updateQtyButton.addActionListener(e -> handleUpdateQuantity());

        JButton updatePriceButton = createStyledButton("$  Update Price", ACCENT);
        updatePriceButton.addActionListener(e -> handleUpdatePrice());

        JButton sortButton = createStyledButton("=  Sort by Name", PRIMARY_DARK);
        sortButton.addActionListener(e -> handleSort());

        JButton filterButton = createStyledButton(">  Filter by Qty", PRIMARY_DARK);
        filterButton.addActionListener(e -> handleFilter());

        JButton undoButton = createStyledButton("<  Undo", new Color(120, 130, 140));
        undoButton.addActionListener(e -> handleUndo());

        JButton summaryButton = createStyledButton("*  Summary", SUCCESS);
        summaryButton.addActionListener(e -> handleSummary());

        JButton checkExpiryButton = createStyledButton("!  Check Expiry", WARNING);
        checkExpiryButton.addActionListener(e -> handleCheckExpiry());

        JButton removeExpiredButton = createStyledButton("X  Remove Expired", DANGER);
        removeExpiredButton.addActionListener(e -> handleRemoveExpired());

        JButton sellButton = createStyledButton("Sell", new Color(0, 150, 136));
        sellButton.addActionListener(e -> handleSell());

        JButton restockButton = createStyledButton("Restock", new Color(63, 81, 181));
        restockButton.addActionListener(e -> handleRestock());

        JButton historyButton = createStyledButton("History", PRIMARY_DARK);
        historyButton.addActionListener(e -> handleTransactionHistory());

        buttonPanel.add(removeButton);
        buttonPanel.add(searchButton);
        buttonPanel.add(updateQtyButton);
        buttonPanel.add(updatePriceButton);
        buttonPanel.add(sellButton);
        buttonPanel.add(restockButton);
        buttonPanel.add(sortButton);
        buttonPanel.add(filterButton);
        buttonPanel.add(undoButton);
        buttonPanel.add(summaryButton);
        buttonPanel.add(checkExpiryButton);
        buttonPanel.add(removeExpiredButton);
        buttonPanel.add(historyButton);

        return buttonPanel;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setBorder(new EmptyBorder(10, 18, 10, 18));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        Color darker = bgColor.darker();
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(darker);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    private JPanel createStatusBar() {
        JPanel statusBarPanel = new JPanel(new BorderLayout());
        statusBarPanel.setBackground(STATUS_BAR_BG);
        statusBarPanel.setBorder(new EmptyBorder(10, 16, 10, 16));

        JLabel indicatorLabel = new JLabel("\u2022");
        indicatorLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        indicatorLabel.setForeground(SUCCESS);
        indicatorLabel.setBorder(new EmptyBorder(0, 0, 0, 8));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(200, 200, 200));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(indicatorLabel);
        leftPanel.add(statusLabel);
        statusBarPanel.add(leftPanel, BorderLayout.WEST);

        JLabel quickInfo = new JLabel("Ctrl+F: search | Del: remove | Ctrl+Z: undo | Ctrl+B: add to cart");
        quickInfo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        quickInfo.setForeground(new Color(150, 150, 150));
        statusBarPanel.add(quickInfo, BorderLayout.EAST);

        return statusBarPanel;
    }

    // =========================================================================
    // Input Validation
    // =========================================================================

    private void setupInputValidation() {
        addFieldValidator(medicineNameField, nameErrorLabel, this::validateName);
        addFieldValidator(quantityField, qtyErrorLabel, this::validateQuantity);
        addFieldValidator(priceField, priceErrorLabel, this::validatePrice);
        addFieldValidator(expiryField, expiryErrorLabel, this::validateExpiry);
    }

    private void addFieldValidator(JTextField field, JLabel errorLabel, FieldValidator validator) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { validate(); }
            @Override
            public void removeUpdate(DocumentEvent e) { validate(); }
            @Override
            public void changedUpdate(DocumentEvent e) { validate(); }

            private void validate() {
                String text = field.getText().trim();
                if (text.isEmpty()) {
                    resetFieldStyle(field, errorLabel);
                    return;
                }
                String error = validator.validate(text);
                if (error != null) {
                    setFieldError(field, errorLabel, error);
                } else {
                    setFieldValid(field, errorLabel);
                }
            }
        });
    }

    private String validateName(String text) {
        if (text.length() < 2) {
            return "At least 2 characters";
        }
        if (!text.matches("[a-zA-Z0-9 \\-]+")) {
            return "Letters, numbers, spaces, hyphens only";
        }
        return null;
    }

    private String validateQuantity(String text) {
        try {
            int qty = Integer.parseInt(text);
            if (qty < 0) return "Cannot be negative";
            return null;
        } catch (NumberFormatException e) {
            return "Must be a whole number";
        }
    }

    private String validatePrice(String text) {
        try {
            double price = Double.parseDouble(text);
            if (price < 0) return "Cannot be negative";
            return null;
        } catch (NumberFormatException e) {
            return "Must be a valid number";
        }
    }

    private String validateExpiry(String text) {
        try {
            LocalDate.parse(text, DATE_FORMAT);
            return null;
        } catch (DateTimeParseException e) {
            return "Format: yyyy-MM-dd";
        }
    }

    private void setFieldError(JTextField field, JLabel errorLabel, String message) {
        field.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, FIELD_ERROR_BORDER),
                new EmptyBorder(8, 10, 8, 10)
        ));
        field.setBackground(FIELD_ERROR_BG);
        errorLabel.setText(message);
    }

    private void setFieldValid(JTextField field, JLabel errorLabel) {
        field.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, FIELD_VALID_BORDER),
                new EmptyBorder(8, 10, 8, 10)
        ));
        field.setBackground(Color.WHITE);
        errorLabel.setText(" ");
    }

    private void resetFieldStyle(JTextField field, JLabel errorLabel) {
        field.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, BORDER_LIGHT),
                new EmptyBorder(8, 10, 8, 10)
        ));
        field.setBackground(Color.WHITE);
        errorLabel.setText(" ");
    }

    @FunctionalInterface
    private interface FieldValidator {
        String validate(String text);
    }

    // =========================================================================
    // Keyboard Shortcuts
    // =========================================================================

    private void setupKeyboardShortcuts() {
        JRootPane rootPane = getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        // Enter in input fields -> Add
        medicineNameField.addActionListener(e -> handleAddMedicine());
        quantityField.addActionListener(e -> handleAddMedicine());
        priceField.addActionListener(e -> handleAddMedicine());
        expiryField.addActionListener(e -> handleAddMedicine());

        // Delete key -> Remove selected
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "remove");
        actionMap.put("remove", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (medicineTable.getSelectedRow() >= 0) {
                    handleRemoveMedicine();
                }
            }
        });

        // Ctrl+F -> Search
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "search");
        actionMap.put("search", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleSearchMedicine();
            }
        });

        // Ctrl+Z -> Undo
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK), "undo");
        actionMap.put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleUndo();
            }
        });

        // Ctrl+B -> Add selected to cart
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.CTRL_DOWN_MASK), "addToCart");
        actionMap.put("addToCart", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleAddToCart();
            }
        });
    }

    // =========================================================================
    // Cart Handlers
    // =========================================================================

    private void handleAddToCart() {
        String selected = getSelectedMedicineName();
        if (selected == null) {
            selected = showInputDialog("Enter the medicine name to add to cart:");
        }
        if (selected == null || selected.trim().isEmpty()) {
            return;
        }

        Medicine medicine = pharmacyService.searchMedicine(selected);
        if (medicine == null) {
            showError("Medicine '" + selected + "' not found.");
            return;
        }

        // Calculate how much of this medicine is already in the cart
        int alreadyInCart = 0;
        for (CartItem item : cartItems) {
            if (item.getMedicineName().equalsIgnoreCase(medicine.getName())) {
                alreadyInCart += item.getQuantity();
            }
        }
        int available = medicine.getQuantity() - alreadyInCart;
        if (available <= 0) {
            showError("All stock of '" + medicine.getName() + "' is already in the cart.");
            return;
        }

        String qtyStr = showInputDialog(
                "Add '" + medicine.getName() + "' to cart\n"
                + "Available: " + available + " (stock: " + medicine.getQuantity()
                + ", in cart: " + alreadyInCart + ")\nQuantity:");
        if (qtyStr == null || qtyStr.trim().isEmpty()) {
            return;
        }

        try {
            int qty = Integer.parseInt(qtyStr.trim());
            if (qty <= 0) {
                showError("Quantity must be positive.");
                return;
            }
            if (qty > available) {
                showError("Only " + available + " available (stock: " + medicine.getQuantity()
                        + ", already in cart: " + alreadyInCart + ").");
                return;
            }

            CartItem item = new CartItem(medicine.getName(), qty, medicine.getPrice());
            cartItems.add(item);
            refreshCart();
        } catch (NumberFormatException e) {
            showError("Quantity must be a whole number.");
        }
    }

    private void handleRemoveFromCart() {
        int selectedRow = cartTable.getSelectedRow();
        if (selectedRow < 0) {
            showError("Select an item in the cart to remove.");
            return;
        }
        cartItems.remove(selectedRow);
        refreshCart();
    }

    private void handleClearCart() {
        if (cartItems.isEmpty()) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Clear all " + cartItems.size() + " items from the cart?",
                "Clear Cart", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            cartItems.clear();
            refreshCart();
        }
    }

    private void handleCheckout() {
        if (cartItems.isEmpty()) {
            showError("Cart is empty. Add items before checkout.");
            return;
        }

        // Build confirmation message
        double grandTotal = cartItems.stream().mapToDouble(CartItem::getLineTotal).sum();
        StringBuilder sb = new StringBuilder();
        sb.append("Checkout Summary\n");
        sb.append("-".repeat(35)).append("\n");
        for (CartItem item : cartItems) {
            sb.append(String.format("%-20s x%-3d  $%,.2f%n",
                    item.getMedicineName(), item.getQuantity(), item.getLineTotal()));
        }
        sb.append("-".repeat(35)).append("\n");
        sb.append(String.format("TOTAL: $%,.2f%n", grandTotal));
        sb.append("\nProceed with sale?");

        int confirm = JOptionPane.showConfirmDialog(this, sb.toString(),
                "Confirm Checkout", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            // Batch sell via service (atomic)
            pharmacyService.sellMultiple(cartItems);

            // Log each item as a separate transaction
            LocalDateTime now = LocalDateTime.now();
            List<Transaction> saleTransactions = new ArrayList<>();
            for (CartItem item : cartItems) {
                Transaction t = new Transaction(Transaction.Type.SALE,
                        item.getMedicineName(), item.getQuantity(), item.getUnitPrice(), now);
                transactionService.recordTransaction(t);
                saleTransactions.add(t);
            }

            refreshTable();
            updateStatusBar();

            // Show consolidated receipt
            showCartReceipt(saleTransactions, grandTotal);

            // Clear cart
            cartItems.clear();
            refreshCart();
        } catch (IllegalArgumentException e) {
            showError("Checkout failed: " + e.getMessage());
        }
    }

    private void showCartReceipt(List<Transaction> transactions, double grandTotal) {
        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(44)).append("\n");
        sb.append("          PHARMACY RECEIPT\n");
        sb.append("=".repeat(44)).append("\n\n");
        sb.append(String.format("Date:       %s%n", transactions.get(0).getFormattedTimestamp()));
        sb.append(String.format("Items:      %d%n", transactions.size()));
        sb.append("-".repeat(44)).append("\n");
        sb.append(String.format("%-20s %5s %8s %8s%n", "Medicine", "Qty", "Unit", "Total"));
        sb.append("-".repeat(44)).append("\n");

        for (Transaction t : transactions) {
            sb.append(String.format("%-20s %5d %8.2f %8.2f%n",
                    t.getMedicineName(), t.getQuantity(), t.getUnitPrice(), t.getTotalAmount()));
        }

        sb.append("-".repeat(44)).append("\n");
        sb.append(String.format("%26s  GRAND TOTAL: $%,.2f%n", "", grandTotal));
        sb.append("=".repeat(44)).append("\n");
        sb.append("\nThank you for your purchase!");

        JTextArea receiptArea = new JTextArea(sb.toString());
        receiptArea.setEditable(false);
        receiptArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        receiptArea.setBackground(new Color(255, 255, 245));
        receiptArea.setCaretPosition(0);
        receiptArea.setBorder(new EmptyBorder(12, 16, 12, 16));

        JScrollPane scrollPane = new JScrollPane(receiptArea);
        scrollPane.setPreferredSize(new Dimension(480, 380));
        JOptionPane.showMessageDialog(this, scrollPane, "Sale Receipt", JOptionPane.PLAIN_MESSAGE);
    }

    private void refreshCart() {
        cartTableModel.setRowCount(0);
        double total = 0;
        for (CartItem item : cartItems) {
            cartTableModel.addRow(new Object[]{
                    item.getMedicineName(), item.getQuantity(),
                    item.getUnitPrice(), item.getLineTotal()
            });
            total += item.getLineTotal();
        }
        cartTotalLabel.setText(String.format("Total: $%,.2f", total));
        cartItemCountLabel.setText(cartItems.size() + " item" + (cartItems.size() != 1 ? "s" : ""));
    }

    // =========================================================================
    // Inventory Action Handlers
    // =========================================================================

    private void handleAddMedicine() {
        String name = medicineNameField.getText().trim();
        String quantityText = quantityField.getText().trim();
        String priceText = priceField.getText().trim();
        String expiryText = expiryField.getText().trim();

        // Run all validators and highlight errors
        boolean valid = true;
        if (name.isEmpty()) {
            setFieldError(medicineNameField, nameErrorLabel, "Required");
            valid = false;
        } else {
            String nameErr = validateName(name);
            if (nameErr != null) { setFieldError(medicineNameField, nameErrorLabel, nameErr); valid = false; }
        }
        if (quantityText.isEmpty()) {
            setFieldError(quantityField, qtyErrorLabel, "Required");
            valid = false;
        } else {
            String qtyErr = validateQuantity(quantityText);
            if (qtyErr != null) { setFieldError(quantityField, qtyErrorLabel, qtyErr); valid = false; }
        }
        if (priceText.isEmpty()) {
            setFieldError(priceField, priceErrorLabel, "Required");
            valid = false;
        } else {
            String priceErr = validatePrice(priceText);
            if (priceErr != null) { setFieldError(priceField, priceErrorLabel, priceErr); valid = false; }
        }
        if (!expiryText.isEmpty()) {
            String expiryErr = validateExpiry(expiryText);
            if (expiryErr != null) { setFieldError(expiryField, expiryErrorLabel, expiryErr); valid = false; }
        }

        if (!valid) {
            return;
        }

        int quantity = Integer.parseInt(quantityText);
        double price = Double.parseDouble(priceText);

        LocalDate expiryDate = null;
        if (!expiryText.isEmpty()) {
            expiryDate = LocalDate.parse(expiryText, DATE_FORMAT);
        }

        try {
            Medicine medicine = new Medicine(name, quantity, price, expiryDate);
            boolean added = pharmacyService.addMedicine(medicine);
            if (added) {
                clearInputFields();
                refreshTable();
                updateStatusBar();
                showInfo("Medicine '" + name + "' added successfully.");
            } else {
                showError("A medicine named '" + name + "' already exists.");
            }
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private void handleRemoveMedicine() {
        String selected = getSelectedMedicineName();
        if (selected == null) {
            selected = showInputDialog("Enter the medicine name to remove:");
        }
        if (selected == null || selected.trim().isEmpty()) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Remove '" + selected + "' from inventory?",
                "Confirm Removal", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        boolean removed = pharmacyService.removeMedicine(selected);
        if (removed) {
            refreshTable();
            updateStatusBar();
            showInfo("Medicine '" + selected + "' removed.");
        } else {
            showError("Medicine '" + selected + "' not found.");
        }
    }

    private void handleSearchMedicine() {
        String name = showInputDialog("Enter the medicine name to search:");
        if (name == null || name.trim().isEmpty()) {
            return;
        }

        int index = pharmacyService.indexOf(name);
        if (index >= 0) {
            int viewRow = medicineTable.convertRowIndexToView(index);
            if (viewRow >= 0) {
                medicineTable.setRowSelectionInterval(viewRow, viewRow);
                medicineTable.scrollRectToVisible(medicineTable.getCellRect(viewRow, 0, true));
            }
        } else {
            showError("Medicine '" + name + "' not found.");
        }
    }

    private void handleUpdateQuantity() {
        String selected = getSelectedMedicineName();
        if (selected == null) {
            selected = showInputDialog("Enter the medicine name to update:");
        }
        if (selected == null || selected.trim().isEmpty()) {
            return;
        }

        String quantityStr = showInputDialog("Enter new quantity for '" + selected + "':");
        if (quantityStr == null || quantityStr.trim().isEmpty()) {
            return;
        }

        try {
            int newQuantity = Integer.parseInt(quantityStr.trim());
            boolean updated = pharmacyService.updateMedicineQuantity(selected, newQuantity);
            if (updated) {
                refreshTable();
                updateStatusBar();
                showInfo("Quantity updated for '" + selected + "'.");
            } else {
                showError("Medicine '" + selected + "' not found.");
            }
        } catch (NumberFormatException e) {
            showError("Quantity must be a whole number.");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private void handleUpdatePrice() {
        String selected = getSelectedMedicineName();
        if (selected == null) {
            selected = showInputDialog("Enter the medicine name to update price:");
        }
        if (selected == null || selected.trim().isEmpty()) {
            return;
        }

        String priceStr = showInputDialog("Enter new price for '" + selected + "':");
        if (priceStr == null || priceStr.trim().isEmpty()) {
            return;
        }

        try {
            double newPrice = Double.parseDouble(priceStr.trim());
            boolean updated = pharmacyService.updateMedicinePrice(selected, newPrice);
            if (updated) {
                refreshTable();
                updateStatusBar();
                showInfo("Price updated for '" + selected + "'.");
            } else {
                showError("Medicine '" + selected + "' not found.");
            }
        } catch (NumberFormatException e) {
            showError("Price must be a valid number.");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private void handleSort() {
        pharmacyService.sortMedicinesByName();
        refreshTable();
    }

    private void handleFilter() {
        String input = showInputDialog("Enter minimum quantity to filter:");
        if (input == null || input.trim().isEmpty()) {
            return;
        }
        try {
            int minQuantity = Integer.parseInt(input.trim());
            List<Medicine> filtered = pharmacyService.filterMedicinesByQuantity(minQuantity);
            populateTable(filtered);
        } catch (NumberFormatException e) {
            showError("Quantity must be a whole number.");
        }
    }

    private void handleUndo() {
        String desc = pharmacyService.getUndoDescription();
        if (desc == null) {
            showInfo("Nothing to undo.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Undo: " + desc + "?",
                "Confirm Undo", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        pharmacyService.undo();
        refreshTable();
        updateStatusBar();
        showInfo("Undo successful.");
    }

    private void handleSummary() {
        int count = pharmacyService.getCount();
        double totalValue = pharmacyService.getTotalInventoryValue();
        List<Medicine> medicines = pharmacyService.getMedicines();

        long expiredCount = medicines.stream().filter(Medicine::isExpired).count();
        long nearExpiryCount = medicines.stream().filter(m -> m.isNearExpiry(NEAR_EXPIRY_DAYS)).count();
        long lowStockCount = medicines.stream().filter(m -> m.getQuantity() < LOW_STOCK_THRESHOLD).count();

        String message = String.format(
                "Inventory Summary\n\n" +
                "Total items: %d\n" +
                "Total value: $%,.2f\n\n" +
                "Low stock (< %d): %d items\n" +
                "Near expiry (<= %d days): %d items\n" +
                "Expired: %d items",
                count, totalValue, LOW_STOCK_THRESHOLD, lowStockCount,
                NEAR_EXPIRY_DAYS, nearExpiryCount, expiredCount);
        showInfo(message);
    }

    private void handleCheckExpiry() {
        List<Medicine> medicines = pharmacyService.getMedicines();
        List<Medicine> expired = new ArrayList<>();
        List<Medicine> nearExpiry = new ArrayList<>();

        for (Medicine m : medicines) {
            if (m.isExpired()) {
                expired.add(m);
            } else if (m.isNearExpiry(NEAR_EXPIRY_DAYS)) {
                nearExpiry.add(m);
            }
        }

        if (expired.isEmpty() && nearExpiry.isEmpty()) {
            showInfo("All medicines are within safe expiry dates.");
            return;
        }

        StringBuilder sb = new StringBuilder("Expiry Report\n");
        sb.append("=".repeat(40)).append("\n\n");

        if (!expired.isEmpty()) {
            sb.append("EXPIRED (").append(expired.size()).append("):\n");
            for (Medicine m : expired) {
                long daysAgo = ChronoUnit.DAYS.between(m.getExpiryDate(), LocalDate.now());
                sb.append("  - ").append(m.getName())
                  .append("  (expired ").append(daysAgo).append(" days ago)\n");
            }
            sb.append("\n");
        }

        if (!nearExpiry.isEmpty()) {
            sb.append("NEAR EXPIRY within ").append(NEAR_EXPIRY_DAYS).append(" days (").append(nearExpiry.size()).append("):\n");
            for (Medicine m : nearExpiry) {
                long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), m.getExpiryDate());
                sb.append("  - ").append(m.getName())
                  .append("  (").append(daysLeft).append(" days left)\n");
            }
        }

        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        textArea.setCaretPosition(0);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(420, 300));
        JOptionPane.showMessageDialog(this, scrollPane, "Expiry Check", JOptionPane.WARNING_MESSAGE);
    }

    private void handleRemoveExpired() {
        List<Medicine> medicines = pharmacyService.getMedicines();
        long expiredCount = medicines.stream().filter(Medicine::isExpired).count();

        if (expiredCount == 0) {
            showInfo("No expired medicines to remove.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("The following ").append(expiredCount).append(" expired medicine(s) will be removed:\n\n");
        for (Medicine m : medicines) {
            if (m.isExpired()) {
                sb.append("  - ").append(m.getName())
                  .append("  (expired: ").append(m.getExpiryDate().format(DATE_FORMAT)).append(")\n");
            }
        }
        sb.append("\nProceed?");

        int confirm = JOptionPane.showConfirmDialog(this, sb.toString(),
                "Remove Expired Medicines", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        int removed = pharmacyService.removeExpiredMedicines();
        refreshTable();
        updateStatusBar();
        showInfo(removed + " expired medicine(s) removed. Use Undo (Ctrl+Z) to restore.");
    }

    private void handleSell() {
        String selected = getSelectedMedicineName();
        if (selected == null) {
            selected = showInputDialog("Enter the medicine name to sell:");
        }
        if (selected == null || selected.trim().isEmpty()) {
            return;
        }

        Medicine medicine = pharmacyService.searchMedicine(selected);
        if (medicine == null) {
            showError("Medicine '" + selected + "' not found.");
            return;
        }

        String qtyStr = showInputDialog("Sell '" + medicine.getName() + "' (available: " + medicine.getQuantity() + ")\nEnter quantity to sell:");
        if (qtyStr == null || qtyStr.trim().isEmpty()) {
            return;
        }

        try {
            int qty = Integer.parseInt(qtyStr.trim());
            double unitPrice = medicine.getPrice();
            Medicine sold = pharmacyService.sellMedicine(medicine.getName(), qty);
            if (sold != null) {
                Transaction transaction = new Transaction(
                        Transaction.Type.SALE, medicine.getName(), qty, unitPrice, LocalDateTime.now());
                transactionService.recordTransaction(transaction);
                refreshTable();
                updateStatusBar();
                showReceipt(transaction);
            }
        } catch (NumberFormatException e) {
            showError("Quantity must be a whole number.");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private void handleRestock() {
        String selected = getSelectedMedicineName();
        if (selected == null) {
            selected = showInputDialog("Enter the medicine name to restock:");
        }
        if (selected == null || selected.trim().isEmpty()) {
            return;
        }

        Medicine medicine = pharmacyService.searchMedicine(selected);
        if (medicine == null) {
            showError("Medicine '" + selected + "' not found. Add it first via the Add panel.");
            return;
        }

        String qtyStr = showInputDialog("Restock '" + medicine.getName() + "' (current stock: " + medicine.getQuantity() + ")\nEnter quantity to add:");
        if (qtyStr == null || qtyStr.trim().isEmpty()) {
            return;
        }

        try {
            int qty = Integer.parseInt(qtyStr.trim());
            boolean restocked = pharmacyService.restockMedicine(medicine.getName(), qty);
            if (restocked) {
                Transaction transaction = new Transaction(
                        Transaction.Type.RESTOCK, medicine.getName(), qty, medicine.getPrice(), LocalDateTime.now());
                transactionService.recordTransaction(transaction);
                refreshTable();
                updateStatusBar();
                showInfo("Restocked " + qty + " units of '" + medicine.getName() + "'. New stock: " + medicine.getQuantity() + ".");
            }
        } catch (NumberFormatException e) {
            showError("Quantity must be a whole number.");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private void handleTransactionHistory() {
        List<Transaction> transactions = transactionService.getTransactions();
        if (transactions.isEmpty()) {
            showInfo("No transactions recorded yet.");
            return;
        }

        String[] columns = {"Type", "Medicine", "Qty", "Unit Price", "Total", "Date/Time"};
        Object[][] data = new Object[transactions.size()][6];
        for (int i = 0; i < transactions.size(); i++) {
            Transaction t = transactions.get(i);
            data[i][0] = t.getType().name();
            data[i][1] = t.getMedicineName();
            data[i][2] = t.getQuantity();
            data[i][3] = String.format("$%.2f", t.getUnitPrice());
            data[i][4] = String.format("$%.2f", t.getTotalAmount());
            data[i][5] = t.getFormattedTimestamp();
        }

        JTable historyTable = new JTable(data, columns);
        historyTable.setEnabled(false);
        historyTable.setRowHeight(28);
        historyTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        historyTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        historyTable.getTableHeader().setBackground(TABLE_HEADER_BG);
        historyTable.getTableHeader().setForeground(Color.WHITE);
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        historyTable.getColumnModel().getColumn(2).setPreferredWidth(50);
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        historyTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        historyTable.getColumnModel().getColumn(5).setPreferredWidth(140);

        JScrollPane scrollPane = new JScrollPane(historyTable);
        scrollPane.setPreferredSize(new Dimension(600, 350));

        JOptionPane.showMessageDialog(this, scrollPane,
                "Transaction History (" + transactions.size() + " records)", JOptionPane.PLAIN_MESSAGE);
    }

    private void showReceipt(Transaction transaction) {
        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(40)).append("\n");
        sb.append("         PHARMACY RECEIPT\n");
        sb.append("=".repeat(40)).append("\n\n");
        sb.append(String.format("Date:       %s%n", transaction.getFormattedTimestamp()));
        sb.append(String.format("Type:       %s%n", transaction.getType().name()));
        sb.append("-".repeat(40)).append("\n");
        sb.append(String.format("Medicine:   %s%n", transaction.getMedicineName()));
        sb.append(String.format("Quantity:   %d%n", transaction.getQuantity()));
        sb.append(String.format("Unit Price: $%.2f%n", transaction.getUnitPrice()));
        sb.append("-".repeat(40)).append("\n");
        sb.append(String.format("TOTAL:      $%.2f%n", transaction.getTotalAmount()));
        sb.append("=".repeat(40)).append("\n");

        JTextArea receiptArea = new JTextArea(sb.toString());
        receiptArea.setEditable(false);
        receiptArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        receiptArea.setBackground(new Color(255, 255, 245));
        receiptArea.setCaretPosition(0);
        receiptArea.setBorder(new EmptyBorder(12, 16, 12, 16));

        JScrollPane scrollPane = new JScrollPane(receiptArea);
        scrollPane.setPreferredSize(new Dimension(380, 300));

        JOptionPane.showMessageDialog(this, scrollPane, "Sale Receipt", JOptionPane.PLAIN_MESSAGE);
    }

    // =========================================================================
    // Table Helpers
    // =========================================================================

    private void refreshTable() {
        populateTable(pharmacyService.getMedicines());
    }

    private void populateTable(List<Medicine> medicines) {
        tableModel.setRowCount(0);
        for (Medicine m : medicines) {
            String expiryStr = m.getExpiryDate() != null ? m.getExpiryDate().format(DATE_FORMAT) : "";
            Object daysLeft = getDaysLeft(m);
            String status = getStatusText(m);
            tableModel.addRow(new Object[]{m.getName(), m.getQuantity(), m.getPrice(), expiryStr, daysLeft, status});
        }
    }

    private Object getDaysLeft(Medicine m) {
        if (m.getExpiryDate() == null) {
            return null;
        }
        long days = ChronoUnit.DAYS.between(LocalDate.now(), m.getExpiryDate());
        return (int) days;
    }

    private String getStatusText(Medicine m) {
        StringBuilder sb = new StringBuilder();
        if (m.isExpired()) {
            sb.append("EXPIRED");
        } else if (m.isNearExpiry(NEAR_EXPIRY_DAYS)) {
            sb.append("Near Expiry");
        }
        if (m.getQuantity() < LOW_STOCK_THRESHOLD) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("Low Stock");
        }
        return sb.toString();
    }

    private String getSelectedMedicineName() {
        int viewRow = medicineTable.getSelectedRow();
        if (viewRow >= 0) {
            int modelRow = medicineTable.convertRowIndexToModel(viewRow);
            return (String) tableModel.getValueAt(modelRow, 0);
        }
        return null;
    }

    // =========================================================================
    // Status Bar
    // =========================================================================

    private void updateStatusBar() {
        String path = pharmacyService.getFilePath();
        int count = pharmacyService.getCount();
        double total = pharmacyService.getTotalInventoryValue();
        String undoInfo = pharmacyService.getUndoDescription() != null ? " | Undo: " + pharmacyService.getUndoDescription() : "";
        statusLabel.setText(String.format("File: %s  |  Items: %d  |  Total Value: $%,.2f%s",
                path, count, total, undoInfo));
    }

    // =========================================================================
    // UI Helpers
    // =========================================================================

    private void clearInputFields() {
        medicineNameField.setText("");
        quantityField.setText("");
        priceField.setText("");
        expiryField.setText("");
        resetFieldStyle(medicineNameField, nameErrorLabel);
        resetFieldStyle(quantityField, qtyErrorLabel);
        resetFieldStyle(priceField, priceErrorLabel);
        resetFieldStyle(expiryField, expiryErrorLabel);
        medicineNameField.requestFocusInWindow();
    }

    private String showInputDialog(String message) {
        return JOptionPane.showInputDialog(this, message);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    // =========================================================================
    // Custom Renderer for Low Stock / Expiry Warnings
    // =========================================================================

    private class InventoryRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            setBorder(new EmptyBorder(8, 12, 8, 12));
            setFont(new Font("Segoe UI", Font.PLAIN, 13));

            if (!isSelected) {
                if (row % 2 == 1) {
                    c.setBackground(TABLE_ROW_ALT);
                } else {
                    c.setBackground(CARD_BG);
                }

                int modelRow = table.convertRowIndexToModel(row);
                String status = (String) tableModel.getValueAt(modelRow, 5);

                if (status.contains("EXPIRED")) {
                    c.setBackground(new Color(255, 205, 210));
                    c.setForeground(new Color(183, 28, 28));
                } else if (status.contains("Near Expiry")) {
                    c.setBackground(new Color(255, 243, 224));
                    c.setForeground(new Color(230, 81, 0));
                } else if (status.contains("Low Stock")) {
                    c.setBackground(new Color(255, 249, 196));
                    c.setForeground(new Color(245, 124, 0));
                } else {
                    c.setForeground(TEXT_PRIMARY);
                }
            } else {
                c.setForeground(Color.WHITE);
            }

            if (column == 1 || column == 2 || column == 4) {
                setHorizontalAlignment(SwingConstants.RIGHT);
            } else {
                setHorizontalAlignment(SwingConstants.LEFT);
            }

            return c;
        }
    }

    // =========================================================================
    // File Selection
    // =========================================================================

    private String askForFilePath() {
        Object[] options = {"Open Existing", "Create New"};
        int choice = JOptionPane.showOptionDialog(null,
                "Would you like to open an existing CSV file or create a new inventory?",
                "Pharmacy Management System",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        if (choice == 0) {
            return openExistingFile();
        } else if (choice == 1) {
            return createNewFile();
        }
        return null;
    }

    private String openExistingFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Medicine CSV File");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile().getAbsolutePath();
        }
        return null;
    }

    private String createNewFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Create New CSV File");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));
        int result = fileChooser.showSaveDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            String path = file.getAbsolutePath();
            if (!path.endsWith(".csv")) {
                path += ".csv";
                file = new File(path);
            }
            try {
                file.createNewFile();
                return path;
            } catch (IOException e) {
                showError("Could not create file: " + e.getMessage());
            }
        }
        return null;
    }

    // =========================================================================
    // Entry Point
    // =========================================================================

    public static void main(String[] args) {
        try {
            UIManager.put("control", BACKGROUND);
            UIManager.put("nimbusBase", PRIMARY_DARK);
            UIManager.put("nimbusBlueGrey", new Color(120, 140, 160));
            UIManager.put("nimbusFocus", PRIMARY_LIGHT);
            UIManager.put("nimbusSelectedText", Color.WHITE);
            UIManager.put("nimbusSelectionBackground", PRIMARY);
            UIManager.put("text", TEXT_PRIMARY);
            UIManager.put("textHighlight", PRIMARY_LIGHT);
            UIManager.put("textHighlightText", Color.WHITE);
            UIManager.put("textInactiveText", TEXT_SECONDARY);
            UIManager.put("info", CARD_BG);
            UIManager.put("menu", CARD_BG);
            UIManager.put("scrollbar", BORDER_LIGHT);
            UIManager.put("TitledBorder.border", BORDER_LIGHT);
            UIManager.put("OptionPane.background", CARD_BG);
            UIManager.put("Panel.background", CARD_BG);
            UIManager.put("Button.background", PRIMARY);
            UIManager.put("Button.textForeground", Color.WHITE);

            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // Fall back to default L&F silently
        }

        SwingUtilities.invokeLater(PharmacyApp::new);
    }
}
