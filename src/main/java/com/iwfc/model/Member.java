package com.iwfc.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A fitness centre Member who can book {@link Session}s.
 *
 * <p><b>Inheritance:</b> extends {@link User}.
 * <p><b>Encapsulation:</b> the internal bookings list is never exposed directly;
 * callers receive an unmodifiable view.
 * <p><b>Generics:</b> bookings stored as {@code List<Booking>}.
 */
public class Member extends User {

    private MembershipType membershipType;
    private final LocalDate joinDate;

    /** Booking history for this member. */
    private final List<Booking> bookings = new ArrayList<>();

    /**
     * @param name           full name
     * @param email          contact e-mail
     * @param phone          contact phone
     * @param membershipType the initial membership tier
     */
    public Member(String name, String email, String phone, MembershipType membershipType) {
        super(name, email, phone);
        this.membershipType = java.util.Objects.requireNonNull(membershipType, "Membership type is required");
        this.joinDate        = LocalDate.now();
    }

    @Override
    public String getRole() { return "Member"; }

    @Override
    public String getSummary() {
        return String.format("Member %s | %s membership | Joined: %s | Bookings: %d",
                getName(), membershipType, joinDate, bookings.size());
    }

    /**
     * Adds a confirmed booking to this member's history.
     *
     * @param booking the booking to record (ignored if null)
     */
    public void addBooking(Booking booking) {
        if (booking != null) bookings.add(booking);
    }

    /**
     * Cancels the booking with the given id.
     *
     * @param bookingId the id of the booking to cancel
     * @return {@code true} if a matching confirmed booking was found and cancelled
     */
    public boolean cancelBooking(String bookingId) {
        return bookings.stream()
                .filter(b -> b.getBookingId().equals(bookingId))
                .findFirst()
                .map(Booking::cancel)
                .orElse(false);
    }

    public MembershipType getMembershipType() { return membershipType; }

    public void setMembershipType(MembershipType membershipType) {
        this.membershipType = java.util.Objects.requireNonNull(membershipType, "Membership type is required");
    }

    public LocalDate getJoinDate() { return joinDate; }

    /** Returns an unmodifiable view to protect internal state. */
    public List<Booking> getBookings() {
        return Collections.unmodifiableList(bookings);
    }
}
