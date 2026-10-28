# SagipHayop: Stray Animal Rescue and Rabies Vaccination System

Maven project, Java 17+, SQLite (sqlite-jdbc declared in pom.xml).

## Run
- IntelliJ: open the folder (it detects pom.xml), let Maven import, run `com.sagiphayop.Main`.
- Terminal: `mvn compile exec:java -Dexec.mainClass=com.sagiphayop.Main`
  (`exec:java` shares stdin, so the Scanner works in the terminal.)

## Default admin (auto-seeded)
username `admin` / password `admin123`

Roles: ADMIN, VET_TECHNICIAN, PET_OWNER. Statuses: PENDING, APPROVED.
