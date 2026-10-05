package com.iwfc.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An Instructor conducts fitness {@link Session}s at the IWFC.
 *
 * <p><b>Inheritance:</b> extends {@link User}.
 * <p><b>Generics in practice:</b> sessions are stored as {@code List<Session>}.
 */
public class Instructor extends User {

    private String specialization;

    /** Sessions assigned to this instructor. */
    private final List<Session> sessions = new ArrayList<>();

    /**
     * @param name           full name
     * @param email          contact e-mail
     * @param phone          contact phone
     * @param specialization fitness specialization, e.g. "Yoga", "HIIT"
     */
    public Instructor(String name, String email, String phone, String specialization) {
        super(name, email, phone);
        this.specialization = requireText(specialization, "Specialization");
    }

    @Override
    public String getRole() { return "Instructor"; }

    @Override
    public String getSummary() {
        return String.format("Instructor %s | Specialization: %s | Sessions scheduled: %d",
                getName(), specialization, sessions.size());
    }

    /**
     * Assigns a session to this instructor's schedule.
     *
     * @param session the session to add (ignored if null)
     */
    public void addSession(Session session) {
        if (session != null) sessions.add(session);
    }

    /**
     * Removes a session from the instructor's schedule.
     *
     * @param session the session to remove
     * @return {@code true} if the session was found and removed
     */
    public boolean removeSession(Session session) {
        return sessions.remove(session);
    }

    public String getSpecialization() { return specialization; }

    public void setSpecialization(String specialization) {
        this.specialization = requireText(specialization, "Specialization");
    }

    /** Returns an unmodifiable view to protect internal state. */
    public List<Session> getSessions() {
        return Collections.unmodifiableList(sessions);
    }
}
