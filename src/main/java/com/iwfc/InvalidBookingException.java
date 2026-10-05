package com.iwfc;

/**
 * Checked exception thrown when a booking operation is invalid.
 *
 * <p>Examples: session is full, session has been cancelled, member already
 * has a confirmed booking for the same session, member or session is not
 * registered in the system.
 *
 * <p>Being a <em>checked</em> exception forces callers to handle booking
 * failures explicitly rather than silently receiving {@code null}.
 */
public class InvalidBookingException extends Exception {

    /**
     * Constructs the exception with a descriptive message.
     *
     * @param message explanation of why the booking is invalid
     */
    public InvalidBookingException(String message) {
        super(message);
    }

    /**
     * Constructs the exception with a message and an underlying cause.
     *
     * @param message explanation of why the booking is invalid
     * @param cause   the underlying exception that triggered this one
     */
    public InvalidBookingException(String message, Throwable cause) {
        super(message, cause);
    }
}
