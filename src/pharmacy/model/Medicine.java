package pharmacy.model;

import java.time.LocalDate;
import java.util.Objects;

public class Medicine {
    private String name;
    private int quantity;
    private double price;
    private LocalDate expiryDate; // nullable — not all inventories track expiry

    public Medicine(String name, int quantity, double price) {
        this(name, quantity, price, null);
    }

    public Medicine(String name, int quantity, double price, LocalDate expiryDate) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Medicine name cannot be null or empty.");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }
        if (price < 0) {
            throw new IllegalArgumentException("Price cannot be negative.");
        }
        this.name = name.trim();
        this.quantity = quantity;
        this.price = price;
        this.expiryDate = expiryDate;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Medicine name cannot be null or empty.");
        }
        this.name = name.trim();
    }

    public void setQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }
        this.quantity = quantity;
    }

    public void setPrice(double price) {
        if (price < 0) {
            throw new IllegalArgumentException("Price cannot be negative.");
        }
        this.price = price;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    /**
     * Returns true if this medicine has an expiry date and it is before today.
     */
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    /**
     * Returns true if this medicine expires within the given number of days from today.
     */
    public boolean isNearExpiry(int days) {
        if (expiryDate == null) return false;
        LocalDate threshold = LocalDate.now().plusDays(days);
        return !isExpired() && !expiryDate.isAfter(threshold);
    }

    @Override
    public String toString() {
        String expiry = expiryDate != null ? expiryDate.toString() : "N/A";
        return String.format("Medicine{name='%s', quantity=%d, price=%.2f, expiry=%s}",
                name, quantity, price, expiry);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Medicine medicine = (Medicine) o;
        return name.equalsIgnoreCase(medicine.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.toLowerCase());
    }
}
