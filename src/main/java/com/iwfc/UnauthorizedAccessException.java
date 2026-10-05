package com.iwfc;

/**
 * Checked exception thrown when a user attempts an operation that their
 * role does not permit.
 *
 * <p>For example, a {@link com.iwfc.model.Member} or
 * {@link com.iwfc.model.Instructor} attempting to manage equipment or assign
 * maintenance tasks (Administrator-only actions).
 *
 * <p>Carries the user name, operation attempted, and required role so that
 * a meaningful message can be logged or displayed without additional lookups.
 */
public class UnauthorizedAccessException extends Exception {

    private final String userName;
    private final String operation;
    private final String requiredRole;

    /**
     * Constructs the exception with full semantic context.
     *
     * @param userName     name of the user who attempted the operation
     * @param operation    short description of the denied operation
     * @param requiredRole the role that is permitted to perform the operation
     */
    public UnauthorizedAccessException(String userName, String operation, String requiredRole) {
        super(String.format(
                "User '%s' is not authorised to %s. Required role: %s.",
                userName, operation, requiredRole));
        this.userName     = userName;
        this.operation    = operation;
        this.requiredRole = requiredRole;
    }

    /**
     * Constructs the exception with a free-form message.
     *
     * @param message explanation of the authorisation failure
     */
    public UnauthorizedAccessException(String message) {
        super(message);
        this.userName     = null;
        this.operation    = null;
        this.requiredRole = null;
    }

    /** @return the name of the user who was denied access */
    public String getUserName()     { return userName; }

    /** @return the operation the user attempted */
    public String getOperation()    { return operation; }

    /** @return the role required to perform the operation */
    public String getRequiredRole() { return requiredRole; }
}
