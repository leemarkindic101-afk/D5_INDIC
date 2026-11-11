package com.sagiphayop.util;

import java.util.List;
import java.util.regex.Pattern;

/** Allowed values for every coded column, shared by the menus and validation. */
public final class Lookups {
    private Lookups() { }

    public static final List<String> ROLES = List.of("ADMIN", "VET_TECHNICIAN", "PET_OWNER");
    public static final List<String> USER_STATUSES = List.of("APPROVED", "PENDING");

    public static final List<String> SPECIES = List.of("DOG", "CAT", "OTHER");
    public static final List<String> SEXES = List.of("MALE", "FEMALE", "UNKNOWN");

    /** Statuses a technician may pick when registering an animal. */
    public static final List<String> REGISTER_STATUSES = List.of("STRAY", "IN_SHELTER", "OWNED");
    /** Statuses selectable by hand; ADOPTED is only reachable through an adoption record. */
    public static final List<String> MANUAL_STATUSES =
            List.of("STRAY", "IN_SHELTER", "OWNED", "RELEASED", "DECEASED");
    public static final List<String> ANIMAL_STATUSES =
            List.of("STRAY", "IN_SHELTER", "OWNED", "ADOPTED", "RELEASED", "DECEASED");

    public static final List<String> RESCUE_OUTCOMES = List.of("IN_SHELTER", "ADOPTED", "RELEASED", "DECEASED");
    /** Outcomes that close an open rescue without an adoption. */
    public static final List<String> CLOSING_OUTCOMES = List.of("RELEASED", "DECEASED");

    public static final List<String> OWNER_STATUSES = List.of("ACTIVE", "INACTIVE");

    public static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_.-]{3,20}$");
    public static final String USERNAME_RULE =
            "Username must be 3-20 characters: letters, digits, '.', '_' or '-' (no spaces).";
}
