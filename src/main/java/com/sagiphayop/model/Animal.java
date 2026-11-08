package com.sagiphayop.model;

/** A stray or owned animal. Status values: STRAY, IN_SHELTER, OWNED, ADOPTED. */
public record Animal(int id, String name, String species, String status) { }
