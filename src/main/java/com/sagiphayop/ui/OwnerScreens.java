package com.sagiphayop.ui;

import com.sagiphayop.model.Barangay;
import com.sagiphayop.model.Owner;
import com.sagiphayop.model.User;
import com.sagiphayop.util.InputUtil;
import com.sagiphayop.util.Lookups;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** CREATE feature 1 (register pet owner) and READ feature 1 (search owners). */
class OwnerScreens extends Screens {
    private static final Pattern CONTACT = Pattern.compile("^[0-9+()\\- ]{7,20}$");

    OwnerScreens(AppContext ctx, User user) {
        super(ctx, user);
    }

    /**
     * Registers a pet owner. Pass the user's own ID when a pet owner creates their own profile;
     * pass null when staff register someone (staff may then optionally link a login account).
     * Returns the saved owner, or null if cancelled or failed.
     */
    Owner registerOwner(Integer selfUserId) {
        System.out.println("\n--- Register Pet Owner ---");
        String fullName = InputUtil.readRequired(in, "Full name (0 to cancel): ");
        if (fullName == null) {
            return cancelled();
        }
        String contact = InputUtil.readMatching(in, "Contact number (0 to cancel): ", CONTACT,
                "Enter a valid contact number (7-20 characters: digits, + - ( ) and spaces).");
        if (contact == null) {
            return cancelled();
        }
        String address = InputUtil.readRequired(in, "Street / address (0 to cancel): ");
        if (address == null) {
            return cancelled();
        }
        Barangay barangay = pickBarangay();
        if (barangay == null) {
            return null;
        }
        Integer userId = selfUserId != null ? selfUserId : askLoginLink();

        int id = ctx.ownerDao().addOwner(userId, fullName, contact, address, barangay.id());
        if (id < 0) {
            System.out.println("Registration failed. Please try again.");
            return null;
        }
        System.out.println("SUCCESS: Owner #" + id + " (" + fullName + ") registered in " + barangay.name() + ".");
        log("OWNER_REGISTERED", "Registered Owner #" + id + " (" + fullName + "), Barangay " + barangay.name());
        return ctx.ownerDao().findById(id).orElse(null);
    }

    /** Lets staff choose (or add) a barangay. Returns null if cancelled. */
    Barangay pickBarangay() {
        return pick("Barangay", ctx.barangayDao().getAll(), Barangay::id, Tables::barangays,
                (b, kw) -> contains(b.name(), kw), this::addBarangay);
    }

    private Barangay addBarangay() {
        String name = InputUtil.readRequired(in, "New barangay name (0 to cancel): ");
        if (name == null) {
            return cancelled();
        }
        Barangay barangay = ctx.barangayDao().findOrCreate(name);
        if (barangay == null) {
            System.out.println("Could not add barangay. Please try again.");
        } else {
            System.out.println("Barangay \"" + barangay.name() + "\" is ready to use.");
        }
        return barangay;
    }

    /** Optional: tie the new owner to an existing, unlinked PET_OWNER login. Blank skips. */
    private Integer askLoginLink() {
        while (true) {
            String username = InputUtil.readOptional(in,
                    "Link to a PET_OWNER login? Enter username (blank to skip): ");
            if (username.isEmpty()) {
                return null;
            }
            Optional<User> found = ctx.userDao().findByUsername(username);
            if (found.isEmpty()) {
                System.out.println("No account named '" + username + "'.");
            } else if (!"PET_OWNER".equals(found.get().role())) {
                System.out.println("That account is " + found.get().role() + ", not PET_OWNER.");
            } else if (ctx.ownerDao().findByUserId(found.get().id()).isPresent()) {
                System.out.println("That account already has an owner profile.");
            } else {
                return found.get().id();
            }
        }
    }

    /**
     * Chooses an owner from the list. When {@code allowNew} is true the user may type N to register one.
     * Returns null if cancelled.
     */
    Owner pickOwner(List<Owner> pool, boolean allowNew) {
        return pick("Owner", pool, Owner::id, Tables::owners,
                (o, kw) -> contains(o.fullName(), kw) || contains(o.barangayName(), kw)
                        || contains(o.contactNo(), kw),
                allowNew ? () -> registerOwner(null) : null);
    }

    void searchOwners() {
        System.out.println("\n--- Search / Filter Owners (press Enter to skip a filter) ---");
        String name = InputUtil.readOptional(in, "Name contains: ");
        String barangay = InputUtil.readOptional(in, "Barangay contains: ");
        String status = InputUtil.readChoice(in, "Status " + Lookups.OWNER_STATUSES + " (Enter = any): ",
                Lookups.OWNER_STATUSES, "");
        List<Owner> owners = ctx.ownerDao().search(name, barangay, status == null ? "" : status);
        if (owners.isEmpty()) {
            System.out.println("No owners match those filters.");
        } else {
            Tables.owners(owners);
        }
    }
}
