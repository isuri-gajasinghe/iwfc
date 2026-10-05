package com.iwfc.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/** A fault or automatic preventative task with one forward-only lifecycle. */
public class MaintenanceRequest {
    public enum Urgency { LOW, MEDIUM, HIGH }
    public enum Status { PENDING, ASSIGNED, COMPLETED }

    private static int nextId = 1;
    private final String requestId;
    private final Equipment equipment;
    private final Instructor reporter;
    private final String description;
    private final Urgency urgency;
    private final boolean preventative;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Status status = Status.PENDING;
    private String assignedTo;
    private Administrator assignedBy;
    private Administrator completedBy;

    public MaintenanceRequest(Equipment equipment, Instructor reporter, String description, Urgency urgency) {
        this(equipment, Objects.requireNonNull(reporter, "Reporting instructor is required"),
                description, urgency, false);
    }

    private MaintenanceRequest(Equipment equipment, Instructor reporter, String description,
                               Urgency urgency, boolean preventative) {
        this.equipment = Objects.requireNonNull(equipment, "Equipment is required");
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Issue description is required.");
        }
        this.description = description.trim();
        this.urgency = Objects.requireNonNull(urgency, "Urgency is required");
        this.reporter = reporter;
        this.preventative = preventative;
        this.requestId = "MR-" + nextId++;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = createdAt;
    }

    static MaintenanceRequest preventative(Equipment equipment) {
        return new MaintenanceRequest(equipment, null,
                "Preventative service due after " + Equipment.MAINTENANCE_THRESHOLD_HOURS
                        + " hours since last service.", Urgency.MEDIUM, true);
    }

    /** Only a pending task can be assigned; the administrator is retained for attribution. */
    public void assign(Administrator administrator, String assignee) {
        Objects.requireNonNull(administrator, "Assigning administrator is required");
        if (status != Status.PENDING) throw new IllegalStateException("Only PENDING tasks can be assigned.");
        if (assignee == null || assignee.isBlank()) throw new IllegalArgumentException("Assignee is required.");
        assignedTo = assignee.trim();
        assignedBy = administrator;
        status = Status.ASSIGNED;
        updatedAt = LocalDateTime.now();
    }

    public void complete(Administrator administrator) {
        Objects.requireNonNull(administrator, "Completing administrator is required");
        if (status != Status.ASSIGNED) throw new IllegalStateException("Only ASSIGNED tasks can be completed.");
        completedBy = administrator;
        status = Status.COMPLETED;
        updatedAt = LocalDateTime.now();
        equipment.finishMaintenance();
    }

    public String getRequestId() { return requestId; }
    public Equipment getEquipment() { return equipment; }
    public Instructor getReporter() { return reporter; }
    public String getDescription() { return description; }
    public Urgency getUrgency() { return urgency; }
    public Status getStatus() { return status; }
    public String getAssignedTo() { return assignedTo; }
    public Administrator getAssignedBy() { return assignedBy; }
    public Administrator getCompletedBy() { return completedBy; }
    public LocalDate getRequestDate() { return createdAt.toLocalDate(); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public boolean isPreventative() { return preventative; }

    @Override
    public String toString() {
        return String.format("[%s] %s | %s | %s | %s | Reporter: %s | Assignee: %s",
                requestId, equipment.getName(), urgency, status, description,
                preventative ? "System (preventative)" : reporter.getName(),
                assignedTo == null ? "Unassigned" : assignedTo);
    }
}