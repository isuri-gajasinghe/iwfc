package com.iwfc;

/**
 * Checked exception thrown when a duplicate entity is added to the system.
 *
 * <p>Thrown when a user with the same e-mail, a piece of equipment with the
 * same ID, or a session with the same ID already exists in the in-memory store.
 *
 * <p>Carries the entity type and the duplicate identifier so the caller can
 * produce a specific error message without parsing the exception text.
 */
public class DuplicateDataException extends Exception {

    private final String entityType;
    private final String duplicateIdentifier;

    /**
     * Constructs the exception with the type and identifier of the duplicate.
     *
     * @param entityType          the kind of entity, e.g. {@code "User"},
     *                            {@code "Equipment"}, {@code "Session"}
     * @param duplicateIdentifier the value that already exists, e.g. an e-mail
     *                            or an equipment/session ID
     */
    public DuplicateDataException(String entityType, String duplicateIdentifier) {
        super(String.format("A %s with identifier '%s' already exists in the system.",
                entityType, duplicateIdentifier));
        this.entityType          = entityType;
        this.duplicateIdentifier = duplicateIdentifier;
    }

    /**
     * Constructs the exception with a free-form message.
     *
     * @param message explanation of the duplication problem
     */
    public DuplicateDataException(String message) {
        super(message);
        this.entityType          = null;
        this.duplicateIdentifier = null;
    }

    /** @return the type of entity that was duplicated */
    public String getEntityType() { return entityType; }

    /** @return the identifier (e-mail, equipment ID, session ID) that clashed */
    public String getDuplicateIdentifier() { return duplicateIdentifier; }
}
