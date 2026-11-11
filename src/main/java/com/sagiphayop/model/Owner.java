package com.sagiphayop.model;

/** A pet owner (or adopter). userId is set when the owner has a PET_OWNER login account. */
public record Owner(int id, Integer userId, String fullName, String contactNo,
                    String address, String barangayName, String status) { }
