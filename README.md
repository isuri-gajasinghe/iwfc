# IWFC Management System

Java 21 / JavaFX desktop prototype for CMP7001 PRAC1. It manages users, gym equipment, session reservations and maintenance tasks using in-memory collections.

## Roles and features

- **Administrator:** adds, edits and deactivates equipment, records usage, and assigns/completes maintenance tasks. These facade operations require a registered Administrator.
- **Instructor:** leads sessions and reports equipment faults. Fault reporting requires a registered Instructor and records that reporter.
- **Member:** books available sessions and cancels their own confirmed bookings.

Roles are selected in the demonstration UI. There is no login/authentication system.

### Equipment

Each item has an immutable unique inventory ID, name, category, location, operational status, active flag and cumulative usage hours. Deactivation is separate from OUT_OF_ORDER and prevents further usage recording, allocation and booking with that equipment.

Usage increments must be finite and non-negative. Every 100 recorded hours since the last completed service, the facade creates one preventative maintenance request and broadcasts CREATED. Further increments do not duplicate an open preventative request. Service preserves lifetime hours and resets the maintenance interval only after all outstanding requests have completed. Usage is entered manually; bookings do not automatically calculate hours.

### Sessions and bookings

Sessions store a studio, instructor, start time, duration, capacity and an immutable list of allocated Equipment objects. An empty allocation is allowed for classes needing no tracked equipment.

Scheduling rejects overlapping studio, instructor or equipment reservations. Intervals are half-open, so back-to-back sessions are allowed. Cancelled sessions release scheduling resources. Studios are matched case-insensitively after trimming surrounding whitespace.

Sessions must start in the future, have positive duration/capacity, and fit within the fixed daily operating window **06:00–22:00**, including their end time. Only registered, active, available equipment with no service due can be allocated.

Booking validates registered members/sessions, scheduled state, future time, equipment availability, capacity and duplicate confirmed reservations. Cancellation checks booking ownership and releases capacity. Deactivating or fault-reporting already allocated equipment blocks subsequent enrollment; existing booking history remains.

### Maintenance

Fault reports contain equipment, Instructor reporter, description, urgency (LOW/MEDIUM/HIGH), creation/update timestamps, and task attribution.

The only task lifecycle is **PENDING → ASSIGNED → COMPLETED**. Administrators assign a staff name and complete assigned tasks; invalid transitions and reassignment are rejected. Completion restores equipment availability only when all outstanding tasks are completed and never reactivates deactivated inventory.

The Observer log receives **CREATED**, **ASSIGNED** and **COMPLETED** events from facade operations. Automatic preventative requests are labelled as system requests and have no Instructor reporter.

## Design and interface

- **OOP:** abstract User; Administrator, Instructor and Member inheritance; overridden role/summary methods; runtime dispatch through User references; private state and final IDs.
- **Collections/generics:** typed List/ArrayList stores, protected collection views, defensive equipment-allocation copies, Streams and Optional queries.
- **Simple Factory:** UserFactory centralizes user subtype construction. It is not the GoF Factory Method pattern.
- **Facade:** IWFCFacade coordinates validation and application writes.
- **Observer:** MaintenanceObserver callbacks decouple maintenance events from JavaFX presentation.
- **Checked exceptions:** DuplicateDataException (emails/inventory/session IDs), InvalidBookingException (scheduling/booking/cancellation), UnauthorizedAccessException (role checks). Invalid form values and task transitions also use standard validation exceptions.

The existing five tabs are Dashboard, Users, Equipment, Sessions and Maintenance. FXML defines the window shell; MainController builds the tab controls; CSS provides styling. The UI supports equipment creation/edit/deactivation/usage, studio and multiple-equipment allocation, booking/cancellation, instructor fault reporting, urgency, assignment/completion and notifications. Dropdowns refresh after relevant operations.

All application data, maintenance history and notifications are in memory and are lost on exit. No database or file persistence is used. Startup creates validated demonstration data.

## Requirements and commands

- JDK 21
- Maven 3.8+
- A graphical desktop/display for JavaFX and the JavaFX integration test
- JavaFX 21.0.2 and JUnit 5.10.2, declared in pom.xml

Build and test:

```powershell
mvn clean test
```

Run:

```powershell
mvn javafx:run
```

Run tests without cleaning:

```powershell
mvn test
```

## Verification and source structure

The final clean test run passed **156 tests: 0 failures, 0 errors, 0 skipped**.

| Existing test class | Tests |
|---|---:|
| BusinessLogicTest | 94 |
| DesignPatternsTest | 23 |
| DomainModelTest | 35 |
| MainAppTest | 4 |

Coverage includes role authorization, equipment usage and maintenance thresholds, resource overlap boundaries, operating hours, booking/cancellation, task transitions, Observer events, Factory/Facade regression, polymorphism and collection protection. MainAppTest also starts the JavaFX toolkit, loads FXML and exercises the UI workflow without showing an application window.

Production source contains **21 top-level types: 16 classes, 4 enums and 1 interface**. Two additional enums are nested in MaintenanceRequest (Urgency and Status). The module descriptor is not a class. The four test classes are separate from production counts. No new top-level Java types or test classes were added.

Core files are under src/main/java/com/iwfc and its model package; FXML/CSS are under src/main/resources/com/iwfc; tests are under src/test/java/com/iwfc.
