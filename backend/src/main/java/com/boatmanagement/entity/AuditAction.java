package com.boatmanagement.entity;

/**
 * Enum representing every auditable action in the system.
 *
 * Convention: RESOURCE_VERB — keeps actions sortable and groupable by resource.
 * Add new entries here as new resources or operations are introduced.
 */
public enum AuditAction {

    // --- Boat CRUD ---
    BOAT_CREATE,
    BOAT_READ,
    BOAT_LIST,
    BOAT_UPDATE,
    BOAT_DELETE,

    // --- Access failures (authorization) ---
    ACCESS_DENIED
}
