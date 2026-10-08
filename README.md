# 🚗 ParkEase — Smart Parking Management System

<p align="center">
  <b>A modular Java-based parking management system built with OOP, Design Patterns, JDBC and MySQL.</b>
</p>

<p align="center">
  <a href="#-features">Features</a> •
  <a href="#-architecture">Architecture</a> •
  <a href="#-design-patterns">Design Patterns</a> •
  <a href="#-uml-documentation">UML</a> •
  <a href="#-getting-started">Getting Started</a>
</p>

---

## 🎥 Project Demo

The complete **ParkEase CUI workflow** is demonstrated on LinkedIn, including vehicle entry, slot allocation, ticketing, payment, exit processing, reports and other core operations.

> 🔗 **LinkedIn Demo:** Add your LinkedIn post URL here

---

## 📌 Overview

**ParkEase** is a console-based Smart Parking Management System designed to model the core workflows of a real-world parking facility.

The application separates parking, vehicle, ticket, payment, pricing, reservation, monthly-pass, reporting and persistence responsibilities into dedicated components. It uses **Java + JDBC + MySQL** for implementing business operations and persistent storage.

The project focuses on software-engineering fundamentals rather than simply providing a menu-driven application:

- Object-Oriented Design
- Abstraction and Polymorphism
- Separation of Responsibilities
- Strategy-based extensibility
- Factory-based object creation
- Observer-based availability updates
- JDBC persistence
- Relational data management
- Maintainable business workflows

---

## 🎯 Problem Statement

A manual parking system makes it difficult to efficiently manage:

- Available parking spaces
- Vehicle entry and exit
- Ticket generation
- Parking charges
- Payment methods
- Reservations
- Monthly passes
- Lost tickets
- Revenue tracking
- Historical records
- Administrative activities

**ParkEase** provides a centralized software solution for these operations while keeping the core business logic modular and extensible.

---

## ✨ Features

| Module | Capabilities |
|---|---|
| 🚗 Vehicle Management | Bike, Car and Truck support |
| 🅿️ Parking Management | Multi-floor parking and compatible spot allocation |
| 🧠 Parking Strategy | First Available and Nearest Available |
| 🎫 Ticket Management | Ticket generation, active ticket tracking and exit processing |
| 💰 Pricing | Normal, Weekend and Dynamic pricing |
| 💳 Payments | Cash, UPI and Card |
| 🔎 Search | Search vehicles and parking records |
| 📜 History | Parking and vehicle history |
| 📊 Reports | Revenue and statistics |
| 💵 Payment History | Track completed payment transactions |
| 📅 Reservations | Create and view parking reservations |
| 🎟️ Monthly Pass | Create and validate monthly passes |
| ⚠️ Lost Ticket | Lost-ticket processing with penalty |
| 🔐 Administration | Admin authentication |
| 📝 Audit | Audit-log tracking |
| 🗄️ Persistence | MySQL database through JDBC |

---

# 🏗️ Architecture

ParkEase follows a layered, responsibility-oriented design.

```text
┌───────────────────────────────────────────────┐
│              User / Parking Operator          │
└───────────────────────┬───────────────────────┘
                        │
                        ▼
┌───────────────────────────────────────────────┐
│              CUI / Application Layer          │
│                  ParkEase                     │
└───────────────────────┬───────────────────────┘
                        │
                        ▼
┌───────────────────────────────────────────────┐
│               Domain / Business Layer         │
│                                               │
│ Vehicle • ParkingLot • Floor • Spot • Ticket │
│ Reservation • MonthlyPass • Payment • Pricing│
└───────────────────────┬───────────────────────┘
                        │
                        ▼
┌───────────────────────────────────────────────┐
│             Strategy / Service Logic          │
│                                               │
│ Parking Strategy • Pricing • Payment          │
│ Vehicle Factory • Parking Observer            │
└───────────────────────┬───────────────────────┘
                        │
                        ▼
┌───────────────────────────────────────────────┐
│                Persistence Layer              │
│             JDBC / Database Access            │
└───────────────────────┬───────────────────────┘
                        │
                        ▼
                  ┌────────────┐
                  │   MySQL    │
                  └────────────┘
```

### Architectural Goals

- Keep domain objects focused on their responsibilities.
- Make algorithms replaceable through interfaces.
- Avoid hard-coding payment/pricing/parking algorithms.
- Keep persistence behind JDBC/database components.
- Make future GUI/API migration easier without redesigning the domain model.

---

# 🔄 Core Business Workflow

## Vehicle Entry

```text
Vehicle Arrives
      │
      ▼
Read Vehicle Details
      │
      ▼
VehicleFactory
      │
      ▼
Create Vehicle
      │
      ▼
Select ParkingStrategy
      │
      ▼
Find Compatible Spot
      │
      ▼
Allocate Spot
      │
      ▼
Generate ParkingTicket
      │
      ▼
Persist Transaction
```

## Vehicle Exit

```text
Exit Request
      │
      ▼
Find Active Ticket
      │
      ▼
Calculate Parking Duration
      │
      ▼
Apply PricingStrategy
      │
      ▼
Calculate Amount
      │
      ▼
Select PaymentStrategy
      │
      ▼
Process Payment
      │
      ▼
Release Parking Spot
      │
      ▼
Update Ticket + Payment Records
```

---

# 🧠 Design Patterns

ParkEase intentionally uses multiple design patterns to keep the system extensible.

## Strategy Pattern

### Parking Allocation

```text
             ParkingStrategy
                  ▲
          ┌───────┴────────┐
          │                │
 FirstAvailable     NearestAvailable
```

### Pricing

```text
             PricingStrategy
                  ▲
        ┌─────────┼──────────┐
        │         │          │
      Normal    Weekend    Dynamic
```

### Payment

```text
             PaymentStrategy
                  ▲
          ┌───────┼────────┐
          │       │        │
        Cash      UPI     Card
```

**Why?**

A new parking algorithm, pricing model or payment method can be added by implementing the relevant interface without modifying the core workflow.

---

## Factory Pattern

`VehicleFactory` centralizes vehicle creation.

```text
VehicleFactory
      │
      ├──► Bike
      ├──► Car
      └──► Truck
```

This avoids scattering vehicle-construction logic across the application.

---

## Observer Pattern

Parking availability can be observed through the parking observer abstraction.

```text
ParkingFloor
     │
     │ notify
     ▼
ParkingObserver
     │
     ▼
ParkingDisplayBoard
```

This provides a foundation for real-time parking availability displays.

---

# 📐 UML Documentation

Detailed diagrams are maintained separately under:

**`docs/uml/UML.md`**

The UML documentation covers:

1. **Use Case Diagram** — actors and system capabilities
2. **Class Diagram** — domain objects and relationships
3. **Sequence Diagram** — vehicle parking and exit interactions
4. **Activity Diagram** — parking workflow
5. **Component Diagram** — high-level software components
6. **State Model** — parking spot/ticket lifecycle

The diagrams use **Mermaid**, so they can be rendered directly by GitHub-compatible Markdown viewers.

---

# 🧩 Domain Model

```text
                         ┌──────────────┐
                         │   ParkEase   │
                         └──────┬───────┘
                                │
                                ▼
                         ┌──────────────┐
                         │  ParkingLot  │
                         └──────┬───────┘
                                │
                         ┌──────┴──────┐
                         ▼             ▼
                  ┌────────────┐ ┌────────────┐
                  │ParkingFloor│ │ ParkingGate│
                  └─────┬──────┘ └────────────┘
                        │
                        ▼
                 ┌──────────────┐
                 │ ParkingSpot  │
                 └──────┬───────┘
                        │
              ┌─────────┼─────────┐
              ▼         ▼         ▼
            Bike       Car       Truck

Vehicle ───────► ParkingTicket ───────► Payment
                      │
                      ├────► PricingStrategy
                      │
                      └────► ParkingStrategy
```

---

# 🗃️ Persistence

ParkEase uses **MySQL** as the relational database and **JDBC** for Java-to-database communication.

```text
Java Domain / Business Logic
             │
             ▼
        JDBC Layer
             │
             ▼
       MySQL Database
```

The persistence layer supports records related to:

- Vehicles
- Parking spots/floors
- Tickets
- Payments
- Reservations
- Monthly passes
- Audit logs

> Database credentials should always be supplied through environment variables and never committed to source control.

---

# 🧪 Functional Areas

### Parking

- Park a vehicle
- Allocate a compatible spot
- Remove a vehicle
- Release the spot
- Check availability

### Ticketing

- Generate ticket
- Track active ticket
- Close ticket
- Handle lost ticket

### Billing

- Calculate duration
- Apply pricing strategy
- Process payment
- Store payment history

### Operations

- Search vehicle
- View parking history
- View vehicle history
- View revenue
- View statistics
- View payment history

### Membership & Reservation

- Create reservation
- View reservations
- Create monthly pass
- Validate monthly pass

### Administration

- Admin authentication
- Audit log access

---

# 🖥️ CUI

ParkEase currently provides a **Console User Interface (CUI)** for interacting with the complete parking workflow.

Typical operations include:

```text
1.  Park Vehicle
2.  Remove Vehicle
3.  Search Vehicle
4.  Parking History
5.  Parking Availability
6.  Vehicle History
7.  Revenue Report
8.  Statistics
9.  Payment History
10. Create Reservation
11. View Reservations
12. Monthly Pass
13. Parking Strategy
14. Pricing Strategy
15. Lost Ticket
16. Admin / Audit Log
17. Exit
```

> A GUI can be added later as a presentation layer while reusing the existing domain and business logic.

---

# 🛠️ Technology Stack

### Language

- **Java**

### Database

- **MySQL**

### Database Connectivity

- **JDBC**

### Core Concepts

- OOP
- Interfaces
- Abstract Classes
- Inheritance
- Polymorphism
- Encapsulation
- Exception Handling
- Collections
- Enums

### Design Patterns

- Strategy
- Factory
- Observer

### Development Tools

- VS Code
- IntelliJ IDEA
- Eclipse
- MySQL / XAMPP
- Git & GitHub

---

# 📁 Repository Structure

Recommended repository organization:

```text
ParkEase/
│
├── src/
│   └── ParkEase.java
│
├── docs/
│   ├── uml/
│   │   └── UML.md
│   ├── architecture/
│   │   └── architecture.md
│   └── screenshots/
│
├── database/
│   └── README.md
│
├── lib/
│   └── mysql-connector-j.jar
│
├── README.md
├── .gitignore
└── LICENSE
```

---

# ⚙️ Getting Started

## Prerequisites

Install:

- Java JDK
- MySQL Server / XAMPP
- Git
- VS Code, IntelliJ IDEA or Eclipse

Verify Java:

```bash
java -version
javac -version
```

---

## Database Configuration

Set credentials using environment variables.

### Windows PowerShell

```powershell
$env:PARKING_DB_USERNAME="root"
$env:PARKING_DB_PASSWORD="your_password"
```

For a blank MySQL password:

```powershell
$env:PARKING_DB_PASSWORD=""
```

### Linux / macOS

```bash
export PARKING_DB_USERNAME=root
export PARKING_DB_PASSWORD=your_password
```

---

## Run on Windows

```powershell
.\run.bat
```

## Run on Linux / macOS

```bash
chmod +x run.sh
./run.sh
```

---

# 🔒 Security Notes

Do not commit:

```text
.env
database passwords
API keys
personal credentials
IDE secrets
```

Recommended `.gitignore` entries:

```gitignore
*.class
.env
.vscode/
.idea/
*.iml
target/
bin/
out/
```

---

# 📊 Engineering Practices Demonstrated

This project demonstrates practical application of:

- **Separation of Concerns**
- **Abstraction**
- **Polymorphism**
- **Interface-driven design**
- **Loose coupling**
- **Extensibility through Strategy Pattern**
- **Centralized object creation**
- **Database persistence**
- **Business workflow modelling**
- **Auditability**
- **Error handling**

---

# 🚀 Future Roadmap

- [ ] Modern Java GUI
- [ ] Split monolithic source into packages
- [ ] Spring Boot REST API
- [ ] React frontend
- [ ] JWT authentication
- [ ] Online payment gateway
- [ ] QR-based ticketing
- [ ] Real-time parking availability
- [ ] Parking-slot visualization
- [ ] EV charging management
- [ ] Docker support
- [ ] Cloud deployment
- [ ] Unit testing with JUnit
- [ ] Integration testing
- [ ] CI/CD pipeline

---

# 🎓 What This Project Demonstrates

From a Java/software-engineering perspective, ParkEase demonstrates the ability to:

> **Model a real-world problem → identify domain entities → separate responsibilities → apply appropriate design patterns → persist data → implement business workflows → document the system using UML.**

This makes the project suitable as a portfolio demonstration of **Core Java, OOP, SQL/JDBC and software design fundamentals**.

---

# 👨‍💻 Author

### Badal Borude

**Java Developer | Software Development | SQL | Spring Boot**

GitHub: `badalborude04`

---

## ⭐ Support

If you find this project useful or interesting, consider giving the repository a ⭐.

**Built with Java ☕ and a focus on clean software design.**
