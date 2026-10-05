package com.iwfc.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A scheduled fitness session conducted by an {@link Instructor}.
 *
 * <p>A Session manages its own enrolment list (a {@code List<Member>}),
 * enforcing capacity and preventing duplicate enrolments – an example of
 * <b>encapsulation</b> and <b>collections with generics</b>.
 */
public class Session {

    private final String        sessionId;
    private       String        title;
    private final Instructor    instructor;
    private final LocalDateTime startTime;
    private final int           durationMinutes;
    private final int           maxCapacity;
    private final String        studio;
    private final List<Equipment> equipment;
    private       SessionStatus status;

    /** Members currently enrolled in this session. */
    private final List<Member> enrolledMembers = new ArrayList<>();

    /**
     * @param sessionId       unique identifier, e.g. "S-001"
     * @param title           e.g. "Morning Yoga"
     * @param instructor      the instructor running this session
     * @param startTime       scheduled start date/time
     * @param durationMinutes length of the session in minutes
     * @param maxCapacity     maximum number of members allowed
     * @param studio          studio or space identifier
     * @param equipment       equipment allocation, defensively copied (may be empty)
     */
    public Session(String sessionId, String title, Instructor instructor,
                   LocalDateTime startTime, int durationMinutes, int maxCapacity,
                   String studio, List<Equipment> equipment) {
        if (sessionId == null || sessionId.isBlank() || title == null || title.isBlank()) {
            throw new IllegalArgumentException("Session ID and title are required.");
        }
        if (durationMinutes <= 0 || maxCapacity <= 0) {
            throw new IllegalArgumentException("Duration and capacity must be positive.");
        }
        if (studio == null || studio.isBlank()) throw new IllegalArgumentException("Studio is required.");
        this.studio = studio.trim();
        this.equipment = List.copyOf(Objects.requireNonNull(equipment, "Equipment list is required"));
        if (this.equipment.stream().distinct().count() != this.equipment.size()) {
            throw new IllegalArgumentException("An equipment item cannot be allocated twice.");
        }
        if (this.equipment.stream().anyMatch(e -> !e.isUsable())) {
            throw new IllegalArgumentException("Only active, available equipment can be allocated.");
        }
        this.sessionId       = sessionId.trim();
        this.title           = title.trim();
        this.instructor      = Objects.requireNonNull(instructor);
        this.startTime       = Objects.requireNonNull(startTime);
        this.durationMinutes = durationMinutes;
        this.maxCapacity     = maxCapacity;
        this.status          = SessionStatus.SCHEDULED;
    }

    /**
     * Enrols a member in this session.
     *
     * @param member the member to enrol
     * @return {@code true} on success; {@code false} if the session is full,
     *         cancelled, or the member is already enrolled
     */
    public boolean enroll(Member member) {
        if (member == null || !isAvailable() || enrolledMembers.contains(member)) {
            return false;
        }
        enrolledMembers.add(member);
        return true;
    }

    /**
     * Removes a member's enrolment (called when a booking is cancelled).
     *
     * @param member the member to remove
     * @return {@code true} if the member was found and removed
     */
    public boolean removeEnrollment(Member member) {
        return enrolledMembers.remove(member);
    }

    /**
     * @return {@code true} for a future scheduled session with usable resources and spare capacity
     */
    public boolean isAvailable() {
        return status == SessionStatus.SCHEDULED
                && startTime.isAfter(LocalDateTime.now())
                && equipment.stream().allMatch(Equipment::isUsable)
                && enrolledMembers.size() < maxCapacity;
    }

    /** Half-open intervals permit one session to start exactly when another ends. */
    public boolean overlaps(LocalDateTime start, LocalDateTime end) {
        return startTime.isBefore(end) && start.isBefore(getEndTime());
    }

    public LocalDateTime getEndTime() { return startTime.plusMinutes(durationMinutes); }
    public String getStudio() { return studio; }
    public List<Equipment> getEquipment() { return equipment; }
    public String getEquipmentNames() {
        return equipment.stream().map(Equipment::getName).collect(java.util.stream.Collectors.joining(", "));
    }

    /** Transitions the session to {@link SessionStatus#CANCELLED}. */
    public void cancel() {
        if (status == SessionStatus.COMPLETED) throw new IllegalStateException("Completed sessions cannot be cancelled.");
        this.status = SessionStatus.CANCELLED;
    }

    /** Transitions the session to {@link SessionStatus#IN_PROGRESS}. */
    public void start() {
        if (status != SessionStatus.SCHEDULED) throw new IllegalStateException("Only scheduled sessions can start.");
        if (equipment.stream().anyMatch(e -> !e.isUsable())) throw new IllegalStateException("Session equipment is unavailable.");
        this.status = SessionStatus.IN_PROGRESS;
    }

    /** Transitions the session to {@link SessionStatus#COMPLETED}. */
    public void complete() {
        if (status != SessionStatus.IN_PROGRESS) throw new IllegalStateException("Only in-progress sessions can complete.");
        this.status = SessionStatus.COMPLETED;
    }

    public String        getSessionId()      { return sessionId; }
    public String        getTitle()          { return title; }
    public Instructor    getInstructor()     { return instructor; }
    public LocalDateTime getStartTime()      { return startTime; }
    public int           getDurationMinutes(){ return durationMinutes; }
    public int           getMaxCapacity()    { return maxCapacity; }
    public SessionStatus getStatus()         { return status; }

    public int  getCurrentEnrollmentCount() { return enrolledMembers.size(); }
    public int  getAvailableSpots()         { return maxCapacity - enrolledMembers.size(); }

    /** Returns an unmodifiable view to protect internal state. */
    public List<Member> getEnrolledMembers() {
        return Collections.unmodifiableList(enrolledMembers);
    }

    public void setTitle(String title) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Title is required.");
        this.title = title.trim();
    }

    @Override
    public String toString() {
        return String.format("Session[%s] '%s' | %s | %s | %s | %d spots | %s",
                sessionId, title, instructor.getName(), startTime, studio, getAvailableSpots(), status);
    }
}
