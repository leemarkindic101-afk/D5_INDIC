# StudentConsoleApp (SagipHayop: Stray Animal Rescue and Rabies Vaccination System)

Maven project, Java 17+, SQLite.

## Run in IntelliJ IDEA
1. File > Open > select this folder (the one containing pom.xml) > Open as Project.
2. Wait for Maven to finish importing (bottom-right progress bar).
3. Set File > Project Structure > Project SDK to JDK 17 or newer.
4. Open `src/main/java/com/sagiphayop/Main.java` and click the green Run arrow.
   `students.db` is created automatically in the project root.

## Login
Default admin: `admin` / `admin123`. New registrations are PENDING until an admin approves them.

## Roles / statuses
Roles: ADMIN, VET_TECHNICIAN, PET_OWNER. Statuses: PENDING, APPROVED.
