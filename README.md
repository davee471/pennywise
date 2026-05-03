# PennyWise

**PennyWise** is an in-development lightweight, high-performance personal finance manager built with **Kotlin Multiplatform** and **Compose**. It focuses on "under the hood" systems logic to provide users with a reactive, secure, and data-driven budgeting experience.

## Key Features

*   **Reactive Budgeting API:** A custom-built internal API that orchestrates financial data between a local SQLite database and a reactive UI layer using the Single Source of Truth (SSOT) pattern.
*   **Dynamic Daily Limits:** Implements automated spending calculations that adjust in real-time based on current allowance, spent-today metrics, and negative-balance coercion logic.
*   **Multi-Layered Security:** Integrated PIN authentication system featuring a state-driven verification and setup flow to protect user financial data.
*   **Asynchronous Logic Orchestration:** Engineered lifecycle-aware data streams to manage background database operations, ensuring the main UI thread remains responsive while synchronizing financial state updates.
*   **Consistent Error Handling:** Uses a functional `Result` wrapper across all controllers to ensure type-safe error propagation and application stability.

## Tech Stack

*   **Language:** Kotlin
*   **UI Framework:** Compose Multiplatform
*   **Architecture:** Model-View-Controller (MVC) with Reactive State Management
*   **Local Storage:** SQLite
*   **Build System:** Gradle

## Architectural Highlights

### **The Controller API Layer**
The core of PennyWise is the `BudgetController`, which acts as a **Facade API**. It abstracts the complexity of database transactions and business logic into simple, observable properties

### **Defensive Programming**
The application is designed to handle edge cases gracefully:
* **Overspending**: Daily limits are coerced to $0.0$ if the budget is exceeded, maintaining a clean UI while preserving mathematical accuracy in the backend.
* **Cycle Transitions**: Automated detection and handling of expired budget cycles and final-day warnings.

## How to Run

1.  Clone the repository:
```bash
git clone https://github.com/davee471/pennywise.git
```
2.  Open the project in **Android Studio** or **IntelliJ IDEA**.
3.  Ensure you have the latest **Kotlin** and **Compose** plugins installed.
4.  Run the application on an Android Emulator or Desktop (JVM) target.
