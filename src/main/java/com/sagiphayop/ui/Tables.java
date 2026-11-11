package com.sagiphayop.ui;

import com.sagiphayop.model.Adoption;
import com.sagiphayop.model.Animal;
import com.sagiphayop.model.Barangay;
import com.sagiphayop.model.Owner;
import com.sagiphayop.model.Rescue;
import com.sagiphayop.model.Vaccination;
import com.sagiphayop.util.TablePrinter;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

/** One place that decides how each record type is laid out as a console table. */
final class Tables {
    private Tables() { }

    static void barangays(List<Barangay> list) {
        TablePrinter.print(List.of("ID", "Barangay"), rows(list, b -> List.of(str(b.id()), b.name())));
    }

    static void owners(List<Owner> list) {
        TablePrinter.print(List.of("ID", "Name", "Contact", "Barangay", "Address", "Status", "Login"),
                rows(list, o -> List.of(str(o.id()), o.fullName(), o.contactNo(), o.barangayName(),
                        o.address(), o.status(), o.userId() == null ? "-" : "Yes")));
    }

    static void animals(List<Animal> list) {
        TablePrinter.print(
                List.of("ID", "Name", "Species", "Breed", "Sex", "Age", "Color", "Status", "Owner"),
                rows(list, a -> List.of(str(a.id()), a.name(), a.species(), a.breed(), a.sex(),
                        a.approxAge(), a.color(), a.status(), a.ownerName())));
    }

    /** Newest dose first; only the newest dose carries a live booster status. */
    static void vaccinationHistory(List<Vaccination> list, LocalDate today) {
        TablePrinter.print(
                List.of("Dose ID", "Vaccine", "Batch No", "Date Given", "Next Due", "Given By", "Booster Status"),
                rows(list, v -> List.of(str(v.id()), v.vaccineName(), v.batchNo(), v.dateGiven(),
                        v.nextDueDate(), v.administeredBy(),
                        v.id() == list.get(0).id() ? v.dueStatus(today) : "Superseded")));
    }

    static void overdue(List<Vaccination> list, LocalDate today) {
        TablePrinter.print(
                List.of("Animal ID", "Animal", "Species", "Owner", "Contact", "Last Dose", "Was Due", "Status"),
                rows(list, v -> List.of(str(v.animalId()), v.animalName().isBlank() ? "unnamed" : v.animalName(),
                        v.species(), v.ownerName(), v.ownerContact(), v.dateGiven(), v.nextDueDate(),
                        v.dueStatus(today))));
    }

    static void rescues(List<Rescue> list) {
        TablePrinter.print(
                List.of("ID", "Animal", "Species", "Barangay", "Rescued", "Location", "Condition", "Outcome", "Handled By"),
                rows(list, r -> List.of(str(r.id()), r.animalLabel(), r.species(), r.barangayName(),
                        r.dateRescued(), r.locationFound(), r.conditionNotes(), r.outcome(), r.handledBy())));
    }

    static void adoptions(List<Adoption> list) {
        TablePrinter.print(
                List.of("ID", "Animal", "Species", "Adopter", "Date Adopted", "Approved By", "Follow-up Notes"),
                rows(list, a -> List.of(str(a.id()), a.animalLabel(), a.species(), a.adopterName(),
                        a.dateAdopted(), a.approvedBy(), a.followUpNotes())));
    }

    private static <T> List<List<String>> rows(List<T> items, Function<T, List<String>> mapper) {
        return items.stream().map(mapper).toList();
    }

    private static String str(int n) {
        return String.valueOf(n);
    }
}
