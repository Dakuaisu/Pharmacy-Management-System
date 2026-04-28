package pharmacy.model;

/**
 * Represents a single item in the shopping cart (sale basket).
 * Immutable after construction.
 */
public class CartItem {
    private final String medicineName;
    private final int quantity;
    private final double unitPrice;
    private final double lineTotal;

    public CartItem(String medicineName, int quantity, double unitPrice) {
        if (medicineName == null || medicineName.trim().isEmpty()) {
            throw new IllegalArgumentException("Medicine name cannot be null or empty.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Cart quantity must be positive.");
        }
        if (unitPrice < 0) {
            throw new IllegalArgumentException("Unit price cannot be negative.");
        }
        this.medicineName = medicineName.trim();
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = quantity * unitPrice;
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

    public double getLineTotal() {
        return lineTotal;
    }

    @Override
    public String toString() {
        return String.format("CartItem{medicine='%s', qty=%d, unit=$%.2f, total=$%.2f}",
                medicineName, quantity, unitPrice, lineTotal);
    }
}
