package pharmacy.service;

import pharmacy.model.Transaction;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages transaction logging — persists sales and restocks to a CSV file.
 * Transaction log file is derived from the inventory file path (same directory, _transactions suffix).
 */
public class TransactionService {
    private static final DateTimeFormatter CSV_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String logFilePath;
    private final List<Transaction> transactions;

    /**
     * Creates a TransactionService that stores logs alongside the inventory file.
     * If inventoryFilePath is "medicines.csv", log becomes "medicines_transactions.csv".
     */
    public TransactionService(String inventoryFilePath) {
        this.logFilePath = deriveLogPath(inventoryFilePath);
        this.transactions = new ArrayList<>();
        loadTransactions();
    }

    private String deriveLogPath(String inventoryPath) {
        if (inventoryPath.toLowerCase().endsWith(".csv")) {
            return inventoryPath.substring(0, inventoryPath.length() - 4) + "_transactions.csv";
        }
        return inventoryPath + "_transactions.csv";
    }

    /**
     * Records a transaction and appends it to the log file.
     */
    public void recordTransaction(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null.");
        }
        transactions.add(transaction);
        appendToLog(transaction);
    }

    /**
     * Returns all transactions (newest first).
     */
    public List<Transaction> getTransactions() {
        List<Transaction> reversed = new ArrayList<>(transactions);
        Collections.reverse(reversed);
        return reversed;
    }

    /**
     * Returns the total number of recorded transactions.
     */
    public int getCount() {
        return transactions.size();
    }

    /**
     * Returns the path of the transaction log file.
     */
    public String getLogFilePath() {
        return logFilePath;
    }

    // --- CSV Persistence ---

    private void loadTransactions() {
        File file = new File(logFilePath);
        if (!file.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split(",", 5);
                if (parts.length < 5) {
                    System.err.println("Warning: Skipping malformed transaction line " + lineNumber);
                    continue;
                }
                try {
                    Transaction.Type type = Transaction.Type.valueOf(parts[0].trim());
                    String name = parts[1].trim();
                    int qty = Integer.parseInt(parts[2].trim());
                    double price = Double.parseDouble(parts[3].trim());
                    LocalDateTime timestamp = LocalDateTime.parse(parts[4].trim(), CSV_FORMAT);
                    transactions.add(new Transaction(type, name, qty, price, timestamp));
                } catch (IllegalArgumentException | DateTimeParseException e) {
                    System.err.println("Warning: Skipping invalid transaction line " + lineNumber + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load transaction log: " + e.getMessage());
        }
    }

    private void appendToLog(Transaction t) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, true))) {
            writer.write(t.getType().name() + ","
                    + t.getMedicineName() + ","
                    + t.getQuantity() + ","
                    + t.getUnitPrice() + ","
                    + t.getTimestamp().format(CSV_FORMAT));
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write transaction log: " + e.getMessage(), e);
        }
    }
}
