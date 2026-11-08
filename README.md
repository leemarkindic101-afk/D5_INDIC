# StudentConsoleApp (SagipHayop: Stray Animal Rescue and Rabies Vaccination System)

Maven project, Java 17+, SQLite.

## Run in IntelliJ IDEA
1. File > Open > select this folder (the one containing pom.xml) > Open as Project.
2. Wait for Maven to finish importing.
3. Set File > Project Structure > Project SDK to JDK 17 or newer.
4. Run `src/main/java/com/sagiphayop/Main.java`. `students.db` is created in the project root.

## Login
Default admin: `admin` / `admin123`. New registrations stay PENDING until an admin approves them.

## Roles / statuses
Roles: ADMIN, VET_TECHNICIAN, PET_OWNER. Account statuses: PENDING, APPROVED.

## Audit log (Deliverable 8)
Table `activity_logs` (id, user_id, action_type, description, timestamp), FK to users(id) ON DELETE CASCADE.

| Action type | Triggered by |
|---|---|
| LOGIN / LOGOUT | Authentication.login() |
| USER_APPROVED | Admin > Approve User Account |
| USER_UPDATED | Admin > Update User Role and Status |
| ANIMAL_REGISTERED | Vet Technician > Register Animal |
| VACCINATION_RECORDED | Vet Technician > Record Rabies Vaccination (core business process) |
| ANIMAL_STATUS_UPDATED | Vet Technician > Update Animal Status (lifecycle change) |
| LOGS_CLEARED | Admin > Audit Logs > Clear All Logs |

View them: Admin menu > 5. View System Audit Logs.
