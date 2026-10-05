package com.iwfc.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Administrator role with an attributed history of managed maintenance tasks. */
public class Administrator extends User {
    private final String adminCode;
    private final List<MaintenanceRequest> managedRequests = new ArrayList<>();

    public Administrator(String name, String email, String phone, String adminCode) {
        super(name, email, phone);
        if (adminCode == null || adminCode.isBlank()) throw new IllegalArgumentException("Admin code is required.");
        this.adminCode = adminCode.trim();
    }

    @Override public String getRole() { return "Administrator"; }

    @Override public String getSummary() {
        return String.format("Administrator %s | Code: %s | Managed tasks: %d",
                getName(), adminCode, managedRequests.size());
    }

    public void assignMaintenance(MaintenanceRequest request, String assignee) {
        Objects.requireNonNull(request, "Maintenance request is required").assign(this, assignee);
        if (!managedRequests.contains(request)) managedRequests.add(request);
    }

    public void completeMaintenance(MaintenanceRequest request) {
        Objects.requireNonNull(request, "Maintenance request is required").complete(this);
        if (!managedRequests.contains(request)) managedRequests.add(request);
    }

    public String getAdminCode() { return adminCode; }

    public List<MaintenanceRequest> getManagedRequests() {
        return Collections.unmodifiableList(managedRequests);
    }
}