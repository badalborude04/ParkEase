# ParkEase — UML Documentation

> Software-engineering UML documentation for the Smart Parking Management System.

## 1. Use Case Diagram

```mermaid
flowchart LR
    User((Parking User))
    Admin((Admin))

    subgraph ParkEase["ParkEase System"]
        UC1([Park Vehicle])
        UC2([Remove Vehicle])
        UC3([Search Vehicle])
        UC4([Check Availability])
        UC5([View Parking History])
        UC6([Make Payment])
        UC7([Create Reservation])
        UC8([Manage Monthly Pass])
        UC9([Lost Ticket])
        UC10([View Reports])
        UC11([Admin Login])
        UC12([View Audit Logs])
        UC13([Select Parking Strategy])
        UC14([Select Pricing Strategy])
    end

    User --> UC1
    User --> UC2
    User --> UC3
    User --> UC4
    User --> UC5
    User --> UC6
    User --> UC7
    User --> UC8
    User --> UC9

    Admin --> UC11
    Admin --> UC10
    Admin --> UC12
    Admin --> UC13
    Admin --> UC14
```

---

## 2. Class Diagram

```mermaid
classDiagram

class Vehicle {
    <<abstract>>
    -String vehicleNumber
    -VehicleType type
    +getVehicleNumber()
    +getType()
}

class Bike
class Car
class Truck

Vehicle <|-- Bike
Vehicle <|-- Car
Vehicle <|-- Truck

class VehicleFactory {
    +createVehicle(type, number) Vehicle
}

VehicleFactory ..> Vehicle

class ParkingLot {
    -List~ParkingFloor~ floors
    -ParkingStrategy parkingStrategy
    +parkVehicle(vehicle)
    +removeVehicle(ticket)
    +findVehicle(number)
}

class ParkingFloor {
    -int floorNumber
    -List~ParkingSpot~ spots
    +findAvailableSpot(type)
    +allocateSpot(vehicle)
    +releaseSpot(spot)
}

class ParkingSpot {
    <<abstract>>
    -String spotId
    -boolean occupied
    -Vehicle vehicle
    +isAvailable()
    +park(vehicle)
    +release()
}

class BikeSpot
class CarSpot
class TruckSpot

ParkingSpot <|-- BikeSpot
ParkingSpot <|-- CarSpot
ParkingSpot <|-- TruckSpot

ParkingLot "1" o-- "*" ParkingFloor
ParkingFloor "1" o-- "*" ParkingSpot
ParkingSpot --> "0..1" Vehicle

class ParkingTicket {
    -String ticketId
    -Vehicle vehicle
    -LocalDateTime entryTime
    -LocalDateTime exitTime
    -TicketStatus status
    +close()
}

ParkingTicket --> Vehicle

class ParkingStrategy {
    <<interface>>
    +findSpot(lot, vehicle) ParkingSpot
}

class FirstAvailableParkingStrategy
class NearestAvailableParkingStrategy

ParkingStrategy <|.. FirstAvailableParkingStrategy
ParkingStrategy <|.. NearestAvailableParkingStrategy
ParkingLot --> ParkingStrategy

class PricingStrategy {
    <<interface>>
    +calculateFee(ticket) double
}

class NormalPricingStrategy
class WeekendPricingStrategy
class DynamicPricingStrategy

PricingStrategy <|.. NormalPricingStrategy
PricingStrategy <|.. WeekendPricingStrategy
PricingStrategy <|.. DynamicPricingStrategy

class PaymentStrategy {
    <<interface>>
    +pay(amount) boolean
}

class CashPayment
class UPIPayment
class CardPayment

PaymentStrategy <|.. CashPayment
PaymentStrategy <|.. UPIPayment
PaymentStrategy <|.. CardPayment

class ParkingObserver {
    <<interface>>
    +update()
}

class ParkingDisplayBoard
ParkingObserver <|.. ParkingDisplayBoard
ParkingFloor --> ParkingObserver

class Reservation {
    -String reservationId
    -String vehicleNumber
    -LocalDateTime reservationTime
}

class MonthlyPass {
    -String passId
    -String vehicleNumber
    -LocalDate validFrom
    -LocalDate validUntil
}

class Payment {
    -String paymentId
    -double amount
    -PaymentMethod method
}

class Admin {
    -String username
    +authenticate()
}

class AuditLog {
    -String action
    -LocalDateTime timestamp
}

VehicleFactory ..> Vehicle
ParkingTicket --> Payment
Reservation --> Vehicle
MonthlyPass --> Vehicle
Admin --> AuditLog
```

---

## 3. Vehicle Entry — Sequence Diagram

```mermaid
sequenceDiagram
    actor User
    participant App as ParkEase
    participant Factory as VehicleFactory
    participant Lot as ParkingLot
    participant Strategy as ParkingStrategy
    participant Spot as ParkingSpot
    participant Ticket as ParkingTicket
    participant DB as MySQL

    User->>App: Enter vehicle details
    App->>Factory: createVehicle(type, number)
    Factory-->>App: Vehicle
    App->>Lot: parkVehicle(vehicle)
    Lot->>Strategy: findSpot(lot, vehicle)
    Strategy-->>Lot: available spot
    Lot->>Spot: park(vehicle)
    Lot->>Ticket: create ticket
    App->>DB: Save vehicle, spot and ticket
    DB-->>App: Success
    App-->>User: Ticket generated
```

---

## 4. Vehicle Exit — Sequence Diagram

```mermaid
sequenceDiagram
    actor User
    participant App as ParkEase
    participant Ticket as ParkingTicket
    participant Pricing as PricingStrategy
    participant Payment as PaymentStrategy
    participant Spot as ParkingSpot
    participant DB as MySQL

    User->>App: Request vehicle exit
    App->>DB: Find active ticket
    DB-->>App: Ticket
    App->>Ticket: Calculate duration
    App->>Pricing: calculateFee(ticket)
    Pricing-->>App: Amount
    App->>Payment: pay(amount)
    Payment-->>App: Payment success
    App->>Ticket: close()
    App->>Spot: release()
    App->>DB: Update ticket + payment + spot
    DB-->>App: Success
    App-->>User: Exit completed
```

---

## 5. Parking Activity Diagram

```mermaid
flowchart TD
    A([Start]) --> B[Enter Vehicle Details]
    B --> C[Create Vehicle]
    C --> D{Reservation / Monthly Pass?}

    D -->|Yes| E[Validate Eligibility]
    D -->|No| F[Select Parking Strategy]
    E --> F

    F --> G[Find Compatible Spot]
    G --> H{Spot Available?}

    H -->|No| I[Show Parking Full]
    I --> Z([End])

    H -->|Yes| J[Allocate Spot]
    J --> K[Generate Ticket]
    K --> L[Persist Parking Data]
    L --> M[Vehicle Parked]

    M --> N[Exit Requested]
    N --> O[Calculate Duration]
    O --> P[Apply Pricing Strategy]
    P --> Q[Select Payment Method]
    Q --> R[Process Payment]
    R --> S{Payment Successful?}

    S -->|No| Q
    S -->|Yes| T[Release Parking Spot]
    T --> U[Update Database]
    U --> V[Close Ticket]
    V --> Z([End])
```

---

## 6. Component Diagram

```mermaid
flowchart TB
    CUI["Console User Interface"]
    Domain["Domain Model"]
    Strategy["Strategy Components"]
    Factory["Vehicle Factory"]
    Observer["Parking Observer"]
    JDBC["JDBC / Persistence"]
    DB[("MySQL")]

    CUI --> Domain
    Domain --> Strategy
    Domain --> Factory
    Domain --> Observer
    Domain --> JDBC
    JDBC --> DB
```

---

## 7. Parking Spot State

```mermaid
stateDiagram-v2
    [*] --> Available

    Available --> Occupied: Park Vehicle
    Occupied --> Available: Vehicle Exit

    Available --> Reserved: Reservation
    Reserved --> Occupied: Vehicle Entry
    Reserved --> Available: Reservation Cancelled
```

---

## 8. Design Pattern Mapping

| Pattern | Component | Purpose |
|---|---|---|
| Strategy | ParkingStrategy | Slot allocation algorithm |
| Strategy | PricingStrategy | Parking fee calculation |
| Strategy | PaymentStrategy | Payment processing |
| Factory | VehicleFactory | Vehicle object creation |
| Observer | ParkingObserver | Parking availability updates |

---

## 9. Design Principles

### Single Responsibility
Major components focus on one responsibility such as parking, payment, pricing, persistence or vehicle creation.

### Open/Closed
New parking, pricing and payment strategies can be introduced through existing interfaces.

### Dependency on Abstractions
The core workflow depends on strategy interfaces rather than concrete implementations.

### Separation of Concerns
CUI interaction, business logic, domain modelling and database persistence have distinct responsibilities.
