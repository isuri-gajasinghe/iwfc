package com.iwfc;

import com.iwfc.model.*;

/**
 * <b>Creational – Factory Pattern</b>
 *
 * <p>Simple static factory for {@link User} subtypes. The facade uses typed
 * creation methods; clients needing a common User reference may use createUser.
 *
 * <p>Uses a Java 21 switch expression to dispatch on the type string, returning
 * the correct concrete subclass through the polymorphic {@link User} reference.
 */
public final class UserFactory {

    /** Utility class – not instantiable. */
    private UserFactory() {}
    // Generic factory method

    /**
     * Creates a {@link User} of the requested type.
     *
     * @param type  one of {@code "ADMIN"}, {@code "INSTRUCTOR"}, {@code "MEMBER"}
     *              (case-insensitive)
     * @param name  full name
     * @param email contact e-mail
     * @param phone contact phone
     * @param extra type-specific parameter:
     *              <ul>
     *                <li>ADMIN → admin code, e.g. {@code "ADMIN-001"}</li>
     *                <li>INSTRUCTOR → specialization, e.g. {@code "Yoga"}</li>
     *                <li>MEMBER → {@link MembershipType} name,
     *                    e.g. {@code "BASIC"}, {@code "PREMIUM"}, {@code "VIP"}</li>
     *              </ul>
     * @return the concrete {@link User} subtype
     * @throws IllegalArgumentException if {@code type} is not recognised
     */
    public static User createUser(String type, String name,
                                  String email, String phone, String extra) {
        if (type == null || type.isBlank() || extra == null) {
            throw new IllegalArgumentException("User type and role details are required.");
        }
        return switch (type.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "ADMIN"      -> new Administrator(name, email, phone, extra);
            case "INSTRUCTOR" -> new Instructor(name, email, phone, extra);
            case "MEMBER"     -> new Member(name, email, phone,
                                     MembershipType.valueOf(extra.trim().toUpperCase(java.util.Locale.ROOT)));
            default -> throw new IllegalArgumentException(
                    "Unknown user type: '" + type + "'. Expected ADMIN, INSTRUCTOR, or MEMBER.");
        };
    }
    // Typed convenience methods (used by IWFCFacade for compile-time safety)

    /** Creates and returns a typed {@link Administrator}. */
    public static Administrator createAdmin(String name, String email,
                                            String phone, String adminCode) {
        return new Administrator(name, email, phone, adminCode);
    }

    /** Creates and returns a typed {@link Instructor}. */
    public static Instructor createInstructor(String name, String email,
                                              String phone, String specialization) {
        return new Instructor(name, email, phone, specialization);
    }

    /** Creates and returns a typed {@link Member}. */
    public static Member createMember(String name, String email,
                                      String phone, MembershipType type) {
        return new Member(name, email, phone, type);
    }
}
