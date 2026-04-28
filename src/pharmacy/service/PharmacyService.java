package pharmacy.service;

import pharmacy.model.Medicine;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import pharmacy.model.CartItem;

/**
 * Manages the pharmacy inventory — adding, removing, searching, and persisting medicines.
 * This class has NO UI dependency. All errors are reported via return values or exceptions.
 * Supports single-level undo for the last mutating operation.
 */
public class PharmacyService {
    private final List<Medicine> medicines;
    private final String filePath;

    // Undo support — snapshot of the entire list before the last mutation
    private List<Medicine> undoSnapshot;
    private String undoDescription;

    public PharmacyService(String filePath) {
        this.filePath = filePath;
        this.medicines = new ArrayList<>();
        this.undoSnapshot = null;
        this.undoDescription = null;
        loadMedicinesFromCSV();
    }

    // --- Undo Support ---

    private void saveUndoSnapshot(String description) {
        undoSnapshot = new ArrayList<>();
        for (Medicine m : medicines) {
            undoSnapshot.add(new Medicine(m.getName(), m.getQuantity(), m.getPrice(), m.getExpiryDate()));
        }
        undoDescription = description;
    }

    /**
     * Returns a description of what undo will revert, or null if nothing to undo.
     */
    public String getUndoDescription() {
        return undoDescription;
    }

    /**
     * Reverts the last mutating operation.
     * @return true if undo was performed, false if nothing to undo.
     */
    public boolean undo() {
        if (undoSnapshot == null) {
            return false;
        }
        medicines.clear();
        medicines.addAll(undoSnapshot);
        undoSnapshot = null;
        undoDescription = null;
        saveToCSV();
        return true;
    }

    // --- CRUD Operations ---

    /**
     * Adds a medicine to the inventory.
     * @return true if added successfully, false if a medicine with the same name already exists.
     */
    public boolean addMedicine(Medicine medicine) {
        if (medicine == null) {
            throw new IllegalArgumentException("Medicine cannot be null.");
        }
        if (searchMedicine(medicine.getName()) != null) {
            return false;
        }
        saveUndoSnapshot("Add '" + medicine.getName() + "'");
        medicines.add(medicine);
        saveToCSV();
        return true;
    }

    /**
     * Removes a medicine by name.
     * @return true if removed, false if not found.
     */
    public boolean removeMedicine(String medicineName) {
        if (medicineName == null || medicineName.trim().isEmpty()) {
            return false;
        }
        Medicine target = searchMedicine(medicineName);
        if (target == null) {
            return false;
        }
        saveUndoSnapshot("Remove '" + target.getName() + "'");
        medicines.removeIf(m -> m.getName().equalsIgnoreCase(medicineName.trim()));
        saveToCSV();
        return true;
    }

    /**
     * Searches for a medicine by name (case-insensitive).
     * @return the Medicine if found, null otherwise.
     */
    public Medicine searchMedicine(String medicineName) {
        if (medicineName == null || medicineName.trim().isEmpty()) {
            return null;
        }
        return medicines.stream()
                .filter(m -> m.getName().equalsIgnoreCase(medicineName.trim()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Updates the quantity of an existing medicine.
     * @return true if updated, false if medicine not found.
     */
    public boolean updateMedicineQuantity(String medicineName, int newQuantity) {
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }
        Medicine medicine = searchMedicine(medicineName);
        if (medicine == null) {
            return false;
        }
        saveUndoSnapshot("Update quantity of '" + medicine.getName() + "' from " + medicine.getQuantity() + " to " + newQuantity);
        medicine.setQuantity(newQuantity);
        saveToCSV();
        return true;
    }

    /**
     * Updates the price of an existing medicine.
     * @return true if updated, false if medicine not found.
     */
    public boolean updateMedicinePrice(String medicineName, double newPrice) {
        if (newPrice < 0) {
            throw new IllegalArgumentException("Price cannot be negative.");
        }
        Medicine medicine = searchMedicine(medicineName);
        if (medicine == null) {
            return false;
        }
        saveUndoSnapshot("Update price of '" + medicine.getName() + "' from " + String.format("%.2f", medicine.getPrice()) + " to " + String.format("%.2f", newPrice));
        medicine.setPrice(newPrice);
        saveToCSV();
        return true;
    }

    /**
     * Sells (dispenses) a quantity of a medicine, decreasing stock.
     * @param medicineName the medicine to sell
     * @param quantity the number of units to sell (must be positive)
     * @return the Medicine that was sold (for receipt generation), or null if not found
     * @throws IllegalArgumentException if quantity is invalid or exceeds available stock
     */
    public Medicine sellMedicine(String medicineName, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Sell quantity must be positive.");
        }
        Medicine medicine = searchMedicine(medicineName);
        if (medicine == null) {
            return null;
        }
        if (medicine.getQuantity() < quantity) {
            throw new IllegalArgumentException(
                    "Insufficient stock. Available: " + medicine.getQuantity() + ", requested: " + quantity);
        }
        saveUndoSnapshot("Sell " + quantity + " of '" + medicine.getName() + "'");
        medicine.setQuantity(medicine.getQuantity() - quantity);
        saveToCSV();
        return medicine;
    }

    /**
     * Restocks (adds to) the quantity of an existing medicine.
     * @param medicineName the medicine to restock
     * @param quantity the number of units to add (must be positive)
     * @return true if restocked, false if medicine not found
     * @throws IllegalArgumentException if quantity is not positive
     */
    public boolean restockMedicine(String medicineName, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Restock quantity must be positive.");
        }
        Medicine medicine = searchMedicine(medicineName);
        if (medicine == null) {
            return false;
        }
        saveUndoSnapshot("Restock " + quantity + " of '" + medicine.getName() + "'");
        medicine.setQuantity(medicine.getQuantity() + quantity);
        saveToCSV();
        return true;
    }

    // --- Query Operations ---

    /**
     * Returns a copy of the medicines list.
     */
    public List<Medicine> getMedicines() {
        return new ArrayList<>(medicines);
    }

    /**
     * Sorts medicines by name alphabetically (case-insensitive).
     */
    public void sortMedicinesByName() {
        medicines.sort(Comparator.comparing(m -> m.getName().toLowerCase()));
    }

    /**
     * Filters medicines that have at least the given minimum quantity.
     */
    public List<Medicine> filterMedicinesByQuantity(int minQuantity) {
        return medicines.stream()
                .filter(m -> m.getQuantity() >= minQuantity)
                .collect(Collectors.toList());
    }

    /**
     * Returns the number of medicines in inventory.
     */
    public int getCount() {
        return medicines.size();
    }

    /**
     * Returns the total inventory value (sum of quantity * price for each medicine).
     */
    public double getTotalInventoryValue() {
        return medicines.stream()
                .mapToDouble(m -> m.getQuantity() * m.getPrice())
                .sum();
    }

    /**
     * Returns the file path this service is using.
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Returns the index of a medicine by name (case-insensitive), or -1 if not found.
     */
    public int indexOf(String medicineName) {
        if (medicineName == null || medicineName.trim().isEmpty()) {
            return -1;
        }
        for (int i = 0; i < medicines.size(); i++) {
            if (medicines.get(i).getName().equalsIgnoreCase(medicineName.trim())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Removes all expired medicines from inventory.
     * @return the number of medicines removed.
     */
    public int removeExpiredMedicines() {
        long expiredCount = medicines.stream().filter(Medicine::isExpired).count();
        if (expiredCount == 0) {
            return 0;
        }
        saveUndoSnapshot("Remove " + expiredCount + " expired medicine(s)");
        int before = medicines.size();
        medicines.removeIf(Medicine::isExpired);
        int removed = before - medicines.size();
        saveToCSV();
        return removed;
    }

    /**
     * Sells multiple medicines in a single batch (for cart/basket checkout).
     * Validates all items BEFORE modifying any stock — atomic: either all succeed or none.
     * @param cartItems list of CartItem (medicineName + quantity)
     * @return list of CartItems that were sold (same as input on success)
     * @throws IllegalArgumentException if any item fails validation (not found, insufficient stock)
     */
    public List<CartItem> sellMultiple(List<CartItem> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty.");
        }

        // Phase 1: Validate ALL items before modifying anything
        for (CartItem item : cartItems) {
            Medicine medicine = searchMedicine(item.getMedicineName());
            if (medicine == null) {
                throw new IllegalArgumentException("Medicine '" + item.getMedicineName() + "' not found in inventory.");
            }
            // Calculate total requested for this medicine across all cart items
            int totalRequested = cartItems.stream()
                    .filter(c -> c.getMedicineName().equalsIgnoreCase(item.getMedicineName()))
                    .mapToInt(CartItem::getQuantity)
                    .sum();
            if (medicine.getQuantity() < totalRequested) {
                throw new IllegalArgumentException(
                        "Insufficient stock for '" + medicine.getName() + "'. Available: "
                        + medicine.getQuantity() + ", requested: " + totalRequested);
            }
        }

        // Phase 2: All validated — apply changes
        saveUndoSnapshot("Cart checkout (" + cartItems.size() + " items)");
        for (CartItem item : cartItems) {
            Medicine medicine = searchMedicine(item.getMedicineName());
            medicine.setQuantity(medicine.getQuantity() - item.getQuantity());
        }
        saveToCSV();
        return cartItems;
    }

    // --- CSV Persistence ---

    private void loadMedicinesFromCSV() {
        File file = new File(filePath);
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
                String[] parts = line.split(",");
                if (parts.length < 3) {
                    System.err.println("Warning: Skipping malformed line " + lineNumber + ": " + line);
                    continue;
                }
                try {
                    String name = parts[0].trim();
                    int quantity = Integer.parseInt(parts[1].trim());
                    double price = Double.parseDouble(parts[2].trim());
                    LocalDate expiry = null;
                    if (parts.length >= 4 && !parts[3].trim().isEmpty()) {
                        try {
                            expiry = LocalDate.parse(parts[3].trim());
                        } catch (DateTimeParseException e) {
                            System.err.println("Warning: Invalid expiry date on line " + lineNumber + ", ignoring: " + parts[3].trim());
                        }
                    }
                    medicines.add(new Medicine(name, quantity, price, expiry));
                } catch (IllegalArgumentException e) {
                    System.err.println("Warning: Skipping invalid line " + lineNumber + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load medicines from CSV: " + e.getMessage(), e);
        }
    }

    private void saveToCSV() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Medicine medicine : medicines) {
                String expiry = medicine.getExpiryDate() != null ? medicine.getExpiryDate().toString() : "";
                writer.write(medicine.getName() + "," + medicine.getQuantity() + "," + medicine.getPrice() + "," + expiry);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save medicines to CSV: " + e.getMessage(), e);
        }
    }
}
