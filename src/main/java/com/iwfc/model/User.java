package com.iwfc.model;

/**
 * Abstract base class for every person in the IWFC system.
 *
 * <p><b>Abstraction:</b> hides the common identity fields (id, name, email,
 * phone) behind a clean interface and forces each concrete subclass to declare
 * its own {@link #getRole()} and {@link #getSummary()}.
 *
 * <p><b>Encapsulation:</b> all fields are {@code private}; the auto-generated
 * {@code id} is immutable and only accessible via a getter.
 *
 * <p><b>Polymorphism:</b> any code that holds a {@code User} reference can
 * call {@link #getRole()} and {@link #getSummary()} without knowing the
 * concrete subtype.
 */
public abstract class User {

    // Auto-incremented across all User instances (not thread-safe by design –
    // concurrency is out of scope for this assignment).
    private static int nextId = 1;

    private final int    id;
    private       String name;
    private       String email;
    private       String phone;

    /**
     * Constructs a new User with a system-assigned id.
     *
     * @param name  full name (must not be null)
     * @param email contact e-mail (must not be null)
     * @param phone contact phone number (may be null)
     */
    protected User(String name, String email, String phone) {
        this.id    = nextId++;
        this.name  = requireText(name, "Name");
        this.email = requireText(email, "Email");
        this.phone = phone;
    }

    /** Returns a short role label, e.g. "Member", "Instructor". */
    public abstract String getRole();

    /** Returns a one-line human-readable summary of this user's details. */
    public abstract String getSummary();

    public int    getId()    { return id; }
    public String getName()  { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }

    public void setName(String name) {
        this.name = requireText(name, "Name");
    }

    public void setEmail(String email) {
        this.email = requireText(email, "Email");
    }

    public void setPhone(String phone) { this.phone = phone; }

    protected static String requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required.");
        return value.trim();
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (ID: %d)", getRole(), name, id);
    }

    /** Two Users are equal when they share the same system-assigned id. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
