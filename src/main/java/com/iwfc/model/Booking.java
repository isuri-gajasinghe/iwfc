package com.iwfc.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Records a {@link Member}'s reservation for a specific {@link Session}.
 *
 * <p>A Booking is the link between a member and a session.  When cancelled,
 * it both updates its own status and removes the member from the session's
 * enrolment list – demonstrating <b>object collaboration</b>.
 */
public class Booking {

    private final String        bookingId;
    private final Member        member;
    private final Session       session;
    private final LocalDateTime bookingTime;
    private       BookingStatus status;

    /**
     * Creates a new confirmed booking.
     *
     * @param bookingId unique identifier, e.g. "B-001"
     * @param member    the member making the booking
     * @param session   the session being booked
     */
    public Booking(String bookingId, Member member, Session session) {
        this.bookingId   = Objects.requireNonNull(bookingId);
        this.member      = Objects.requireNonNull(member);
        this.session     = Objects.requireNonNull(session);
        this.bookingTime = LocalDateTime.now();
        this.status      = BookingStatus.CONFIRMED;
    }

    /**
     * Cancels this booking and removes the member from the session.
     *
     * @return {@code true} if the booking was confirmed and is now cancelled;
     *         {@code false} if it was already cancelled or attended
     */
    public boolean cancel() {
        if (status != BookingStatus.CONFIRMED) return false;
        status = BookingStatus.CANCELLED;
        session.removeEnrollment(member);
        return true;
    }

    /** Marks this booking as attended (called after the session ends). */
    public void markAttended() {
        if (status == BookingStatus.CONFIRMED) status = BookingStatus.ATTENDED;
    }

    public String        getBookingId()   { return bookingId; }
    public Member        getMember()      { return member; }
    public Session       getSession()     { return session; }
    public LocalDateTime getBookingTime() { return bookingTime; }
    public BookingStatus getStatus()      { return status; }

    @Override
    public String toString() {
        return String.format("Booking[%s] %s → '%s' [%s]",
                bookingId, member.getName(), session.getTitle(), status);
    }
}
