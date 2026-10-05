package com.iwfc.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Inventory item with cumulative usage and a maintenance history. */
public class Equipment {
    public static final double MAINTENANCE_THRESHOLD_HOURS = 100.0;

    private final String equipmentId;
    private String name;
    private String category;
    private String location;
    private EquipmentStatus status = EquipmentStatus.AVAILABLE;
    private boolean active = true;
    private double usageHours;
    private double lastServicedUsageHours;
    private final List<MaintenanceRequest> maintenanceHistory = new ArrayList<>();

    public Equipment(String equipmentId, String name, String category, String location) {
        this.equipmentId = requireText(equipmentId, "Equipment ID");
        this.name = requireText(name, "Name");
        this.category = requireText(category, "Category");
        this.location = requireText(location, "Location");
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required.");
        return value.trim();
    }

    /** Validate the whole edit before mutating any field. */
    public void edit(String name, String category, String location) {
        String validName = requireText(name, "Name");
        String validCategory = requireText(category, "Category");
        String validLocation = requireText(location, "Location");
        this.name = validName;
        this.category = validCategory;
        this.location = validLocation;
    }

    /** Usage is recorded manually; total lifetime hours never reset after service. */
    public void addUsageHours(double hours) {
        if (!active) throw new IllegalStateException("Deactivated equipment cannot record usage.");
        if (!Double.isFinite(hours) || hours < 0 || !Double.isFinite(usageHours + hours)) {
            throw new IllegalArgumentException("Usage increment must be a finite non-negative number.");
        }
        usageHours += hours;
    }

    public boolean isMaintenanceDue() {
        return usageHours - lastServicedUsageHours >= MAINTENANCE_THRESHOLD_HOURS;
    }

    public boolean isUsable() {
        return active && status == EquipmentStatus.AVAILABLE && !isMaintenanceDue();
    }

    public void deactivate() { active = false; }

    public MaintenanceRequest requestMaintenance(Instructor reporter, String description,
                                                  MaintenanceRequest.Urgency urgency) {
        if (reporter == null) throw new IllegalArgumentException("Reporting instructor is required.");
        return recordRequest(new MaintenanceRequest(this, reporter, description, urgency));
    }

    /** Automatic usage alerts are system requests, so they have no reporting instructor. */
    public MaintenanceRequest requestPreventativeMaintenance() {
        if (!isMaintenanceDue() || maintenanceHistory.stream().anyMatch(
                r -> r.isPreventative() && r.getStatus() != MaintenanceRequest.Status.COMPLETED)) {
            return null;
        }
        return recordRequest(MaintenanceRequest.preventative(this));
    }

    private MaintenanceRequest recordRequest(MaintenanceRequest request) {
        maintenanceHistory.add(request);
        status = EquipmentStatus.IN_MAINTENANCE;
        return request;
    }

    /** Restore availability only after every open request has been completed. */
    public void finishMaintenance() {
        if (maintenanceHistory.stream().anyMatch(r -> r.getStatus() != MaintenanceRequest.Status.COMPLETED)) {
            return;
        }
        lastServicedUsageHours = usageHours;
        status = EquipmentStatus.AVAILABLE;
    }

    public void markAvailable() {
        if (maintenanceHistory.stream().anyMatch(r -> r.getStatus() != MaintenanceRequest.Status.COMPLETED)
                || isMaintenanceDue()) {
            throw new IllegalStateException("Outstanding maintenance must be completed first.");
        }
        status = EquipmentStatus.AVAILABLE;
    }

    public void markOutOfOrder() { status = EquipmentStatus.OUT_OF_ORDER; }
    public String getEquipmentId() { return equipmentId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getLocation() { return location; }
    public EquipmentStatus getStatus() { return status; }
    public boolean isActive() { return active; }
    public double getUsageHours() { return usageHours; }
    public void setName(String name) { this.name = requireText(name, "Name"); }
    public void setCategory(String category) { this.category = requireText(category, "Category"); }
    public void setLocation(String location) { this.location = requireText(location, "Location"); }

    public List<MaintenanceRequest> getMaintenanceHistory() {
        return Collections.unmodifiableList(maintenanceHistory);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | %s | %s | %.1f h | %s%s%s", equipmentId, name,
                category, location, usageHours, status, active ? "" : " | DEACTIVATED",
                isMaintenanceDue() ? " | SERVICE DUE" : "");
    }
}