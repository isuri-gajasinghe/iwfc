package com.iwfc.model;

/**
 * Membership tier for a {@link Member}.
 * Encapsulates the display name and associated monthly fee.
 */
public enum MembershipType {
    BASIC("Basic",   30.0),
    PREMIUM("Premium", 60.0),
    VIP("VIP",      100.0);

    private final String displayName;
    private final double monthlyFee;

    MembershipType(String displayName, double monthlyFee) {
        this.displayName = displayName;
        this.monthlyFee  = monthlyFee;
    }

    public String getDisplayName() { return displayName; }
    public double getMonthlyFee()  { return monthlyFee; }

    @Override
    public String toString() { return displayName; }
}
