# Pharmacy Management System

A desktop inventory management application for pharmacies built with Java Swing. Track medicines, manage expiry dates, process sales, and maintain transaction history with a clean, medical-themed interface.

![Java](https://img.shields.io/badge/Java-8%2B-blue)

---

## Features

### Inventory Management
- **Add, remove, search, and update medicines** — modify quantity and price as needed
- **Expiry date tracking** with automatic color-coded warnings:
  - Red: Expired medicines
  - Orange: Near expiry (within 30 days)
  - Yellow: Low stock (5 or fewer units)
- **Days Left column** — auto-computed days remaining until expiry
- **Check Expiry report** — dedicated dialog showing all expired and near-expiry items
- **Bulk remove expired medicines** — clean up outdated inventory in one action

### Sales and Checkout
- **Single-item sales** — quick sell with automatic stock deduction
- **Multi-item cart/basket** — add multiple medicines to a cart before checkout
- **Receipt generation** — printable receipt showing all items, quantities, and totals
- **Restock existing medicines** — increase inventory levels with transaction logging

### Data and Reporting
- **Transaction history** — view all sales and restocks in a sortable table dialog
- **Sort by name** — alphabetical ordering via clickable table headers
- **Filter by minimum quantity** — hide well-stocked items to focus on low inventory
- **Inventory summary** — overview showing total items, inventory value, low stock count, near-expiry count, and expired count

### User Experience
- **Single-level undo** (Ctrl+Z) — revert the last mutating operation
- **Real-time input validation** — visual feedback with red borders and error hints
- **Column sorting** — click any table header to sort
- **Status bar** — displays file path, item count, total inventory value, and undo availability
- **File chooser** — open existing CSV or create new inventory file at startup
- **Nimbus Look and Feel** with custom blue/gray medical dashboard theme
- **Keyboard shortcuts** — efficient navigation without reaching for the mouse

---

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Java 8+ |
| UI Framework | Swing (Nimbus Look and Feel) |
| Persistence | CSV files |
| Date/Time | `java.time.LocalDate`, `LocalDateTime` |
| Collections | Streams, lambdas |

**Zero external dependencies** — everything runs with just the JDK.

---

## Project Structure

```
src/
└── pharmacy/
    ├── model/
    │   ├── Medicine.java         — Medicine POJO with expiry tracking
    │   ├── Transaction.java      — Immutable sale/restock record
    │   └── CartItem.java         — Immutable cart line item
    ├── service/
    │   ├── PharmacyService.java   — Inventory CRUD, undo, CSV persistence
    │   └── TransactionService.java — Transaction logging to CSV
    └── ui/
        └── PharmacyApp.java       — Swing UI (main entry point)
```

---

## Prerequisites

- **JDK 8 or higher** — required for `LocalDate`, streams, and lambda expressions
- Command line access for compilation and execution

---

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd Pharmacy-Management-System
```

### 2. Compile

```bash
javac -d out src/pharmacy/model/Medicine.java src/pharmacy/model/Transaction.java src/pharmacy/model/CartItem.java src/pharmacy/service/PharmacyService.java src/pharmacy/service/TransactionService.java src/pharmacy/ui/PharmacyApp.java
```

### 3. Run

```bash
java -cp out pharmacy.ui.PharmacyApp
```

On startup, a file chooser will appear. Select an existing CSV inventory file or create a new one.

---

## Usage

### Adding a New Medicine

1. Fill in the **Name**, **Quantity**, **Price**, and optional **Expiry Date** (yyyy-MM-dd format)
2. Press **Enter** or click the **Add Medicine** button
3. Valid inputs show green borders, errors show red borders with hints

### Selling a Medicine

1. Select a medicine from the inventory table
2. Click **Sell**, enter the quantity, and confirm to process a quick sale, or
3. Click **Add to Cart** (or press Ctrl+B) to add to the basket for multi-item checkout
4. In the cart panel, click **Checkout** to complete the sale and generate a receipt

### Restocking

1. Select the medicine to restock
2. Click **Restock** and enter the quantity to add
3. The transaction is logged automatically

### Managing Expiry

1. Click **Check Expiry** to see a report of expired and near-expiry medicines
2. Click **Remove Expired** to bulk-delete all expired items

### Searching and Filtering

- Press **Ctrl+F** to open the search dialog and locate a medicine by name (highlights the row in the table)
- Click **Filter by Qty** to show only items above a given stock level
- Click any table header to sort by that column

### Undo

- Press **Ctrl+Z** to undo the last operation (add, remove, update, or restock)
- The status bar shows what action can be undone

---

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| Enter | Add medicine (when input fields focused) |
| Delete | Remove selected medicine |
| Ctrl+F | Search for medicine |
| Ctrl+Z | Undo last operation |
| Ctrl+B | Add selected medicine to cart |

---

## Architecture

The application follows a simple layered architecture:

### Model Layer (`pharmacy.model`)
- **Medicine** — Core entity with name, quantity, price, and optional expiry date
- **Transaction** — Immutable record of sales and restocks
- **CartItem** — Immutable line item for the shopping cart

### Service Layer (`pharmacy.service`)
- **PharmacyService** — Business logic for inventory operations, undo support, and CSV persistence
- **TransactionService** — Transaction logging and retrieval

### UI Layer (`pharmacy.ui`)
- **PharmacyApp** — Main Swing application with tables, forms, dialogs, and event handling

The service layer has no dependencies on the UI. All errors flow through return values or exceptions, making the business logic testable and reusable.

---

## Data Storage

### Inventory CSV Format

```
name,quantity,price,expiryDate
```

- **name** — medicine name (string)
- **quantity** — stock level (integer)
- **price** — unit price (decimal)
- **expiryDate** — optional, yyyy-MM-dd format

The expiry date column is optional for backward compatibility with older CSV files.

### Transaction Log CSV Format

```
TYPE,name,qty,price,timestamp
```

- **TYPE** — SALE or RESTOCK
- **name** — medicine name
- **qty** — quantity involved
- **price** — unit price at time of transaction
- **timestamp** — yyyy-MM-dd HH:mm:ss format

The transaction log file is auto-derived from the inventory file path. If your inventory is `medicines.csv`, the log becomes `medicines_transactions.csv` in the same directory.

---

## License

MIT License — feel free to use, modify, and distribute.
