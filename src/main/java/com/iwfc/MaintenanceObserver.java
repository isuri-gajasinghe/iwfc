package com.iwfc;

import com.iwfc.model.MaintenanceRequest;

/**
 * <b>Behavioural – Observer Pattern (Observer contract)</b>
 *
 * <p>Defines the notification interface that any interested party must implement
 * to receive maintenance events from the {@link IWFCFacade} (the Subject).
 *
 * <p>Declared as a {@link FunctionalInterface} so that observers can be
 * registered concisely with lambda expressions:
 * <pre>{@code
 *   facade.addObserver((request, event) ->
 *       System.out.println("[ALERT] " + event + ": " + request));
 * }</pre>
 */
@FunctionalInterface
public interface MaintenanceObserver {

    /**
     * Called by the {@link IWFCFacade} when a maintenance event occurs.
     *
     * @param request   the {@link MaintenanceRequest} involved in the event
     * @param eventType a short description of what happened,
     *                  {@code "CREATED"}, {@code "ASSIGNED"}, or {@code "COMPLETED"}
     */
    void onMaintenanceEvent(MaintenanceRequest request, String eventType);
}
