package com.sagiphayop.model;

/** Immutable user account. Roles: ADMIN, VET_TECHNICIAN, PET_OWNER. Statuses: PENDING, APPROVED. */
public record User(int id, String username, String role, String status) { }
