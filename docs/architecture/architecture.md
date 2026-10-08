# ParkEase — Architecture Notes

## High-Level Layers

```text
Presentation
    │
    ▼
Application / Use Cases
    │
    ▼
Domain / Business Logic
    │
    ├── Strategies
    ├── Factories
    └── Observers
    │
    ▼
Persistence
    │
    ▼
MySQL
```

## Migration Path

The current CUI can later be replaced or complemented by:

```text
                 ┌─────────────┐
                 │ CUI / GUI   │
                 └──────┬──────┘
                        │
                        ▼
                 ┌─────────────┐
                 │ Application │
                 │   Logic     │
                 └──────┬──────┘
                        │
                        ▼
                 ┌─────────────┐
                 │   Domain    │
                 └──────┬──────┘
                        │
                        ▼
                 ┌─────────────┐
                 │ Persistence │
                 └──────┬──────┘
                        │
                        ▼
                      MySQL
```

This keeps the business model reusable if the presentation layer changes.
