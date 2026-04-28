package pharmacy.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a single inventory transaction (sale or restock).
 */
public class Transaction {
    public enum Type { SALE, RESTOCK }

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Type type;
    private final String medicineName;
    private final int quantity;
    private final double unitPrice;
    private final double totalAmount;
    private final LocalDateTime timestamp;

    public Transaction(Type type, String medicineName, int quantity, double unitPrice, LocalDateTime timestamp) {
        if (medicineName == null || medicineName.trim().isEmpty()) {
            throw new IllegalArgumentException("Medicine name cannot be null or empty.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Transaction quantity must be positive.");
        }
        if (unitPrice < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative.");
        }
        this.type = type;
        this.medicineName = medicineName.trim();
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalAmount = quantity * unitPrice;
        this.timestamp = timestamp;
    }

    public Type getType() {
        return type;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getFormattedTimestamp() {
        return timestamp.format(DISPLAY_FORMAT);
    }

    @Override
    public String toString() {
        return String.format("Transaction{type=%s, medicine='%s', qty=%d, unit=$%.2f, total=$%.2f, time=%s}",
                type, medicineName, quantity, unitPrice, totalAmount, getFormattedTimestamp());
    }
}
