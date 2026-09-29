# MediQ — Medical Appointment and Queue Management System

Full-stack implementation of the SRS you supplied (Coimbatore Institute of Technology,
"Medical Appointment and Queue Management System"), matching the use cases in
`User_Classes_and_Characteristics.docx`, the sequence diagram in `EX_6_SE.docx`, and the
component diagram in `EX9.docx`.

- **Backend**: Java 17, Spring Boot 3, Spring Security + JWT, Spring Data JPA, H2 (dev) / MySQL (prod)
- **Frontend**: Angular 17 (standalone components), Bootstrap 5

## Project layout
```
backend/    Spring Boot REST API (Maven)
frontend/   Angular SPA
```

## Roles implemented
- **Patient** — register/login, search doctors, book/cancel appointments, view live queue
  position & estimated wait, view consultation/prescription history, submit feedback.
- **Doctor** — manage availability (generates bookable slots), view today's queue, call
  next / skip / start / complete, record diagnosis + prescriptions per REQ-CON-01..07.
- **Administrator** — manage doctors, patients, departments; monitor all appointments and
  the live queue; view feedback; generate date-range reports (by status/doctor/department).

Every module traces to a `REQ-*` requirement ID from the SRS (see comments in
`AppointmentService.java`, `QueueService.java`, `DoctorController.java`, `AdminController.java`).

## Run the backend
Requires JDK 17+ and Maven (not pre-installed in this sandbox, so it hasn't been
compiled here — see **Note on verification** below).
```bash
cd backend
mvn spring-boot:run
```
Starts on `http://localhost:8080`, using a file-based H2 database (`backend/data/mediq.mv.db`,
auto-created). Demo accounts are seeded on first run:

| Role  | Email              | Password  |
|-------|--------------------|-----------|
| Admin | admin@mediq.com    | admin123  |
| Doctor| doctor@mediq.com   | doctor123 |
| Patient| patient@mediq.com | patient123|

To switch to MySQL for production: create a database, then in
`backend/src/main/resources/application.properties` comment out the H2 lines and
uncomment the MySQL lines (also set your own credentials and JWT secret).

## Run the frontend
Requires Node.js 18+.
```bash
cd frontend
npm install
npm start
```
Opens `http://localhost:4200` and talks to the API at `http://localhost:8080/api`
(edit the `API` constant in `src/app/core.ts` to change this).

## Note on verification
This sandbox has no internet access to Maven Central and no `javac`, so the Java backend
could not be compiled here — it was written and carefully reviewed by hand, following
standard Spring Boot 3 conventions. The Angular frontend **was** built successfully in
this environment (`ng build` completes with no errors). Please run
`mvn spring-boot:run` (or `mvn -q compile`) on your machine first; if Maven reports any
issue, paste it back to me and I'll fix it immediately.

## Key design notes
- **No double-booking**: `Slot` rows are pessimistically locked on booking (REQ-APT-04).
- **Queue tokens**: generated per appointment (`QNNNN-###`), position & estimated wait are
  computed live from active queue entries ahead of the patient for that doctor/day.
- **State machine**: queue status follows WAITING → CALLED → IN_PROGRESS → COMPLETED, with
  SKIPPED/CANCELLED side paths, enforced server-side (REQ-QUEUE-07).
- **Security**: BCrypt password hashing, stateless JWT auth, role-based route guards on
  both the API (`/api/admin`, `/api/doctor`, `/api/patient`) and the Angular router.
- **Cancellation window**: patients can't cancel within `app.cancel.min-hours-before`
  (default 1h) of the appointment — configurable in `application.properties`.
