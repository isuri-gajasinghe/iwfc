package com.iwfc;

import com.iwfc.model.*;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Facade for validated application operations and in-memory stores.
 * Also acts as the Observer subject for maintenance lifecycle events.
 * This desktop prototype uses registered role objects rather than login sessions.
 */
public class IWFCFacade {
    public static final LocalTime OPENING_TIME = LocalTime.of(6, 0);
    public static final LocalTime CLOSING_TIME = LocalTime.of(22, 0);

    private final List<User> users = new ArrayList<>();
    private final List<Equipment> equipment = new ArrayList<>();
    private final List<Session> sessions = new ArrayList<>();
    private final List<Booking> bookings = new ArrayList<>();
    private final List<MaintenanceObserver> observers = new ArrayList<>();
    private int bookingCounter = 1;

    public void addObserver(MaintenanceObserver observer) {
        if (observer != null && !observers.contains(observer)) observers.add(observer);
    }
    public void removeObserver(MaintenanceObserver observer) { observers.remove(observer); }

    private void notifyObservers(MaintenanceRequest request, String eventType) {
        // Snapshot allows a listener to unsubscribe itself during delivery.
        for (MaintenanceObserver observer : List.copyOf(observers)) {
            observer.onMaintenanceEvent(request, eventType);
        }
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required.");
        return value.trim();
    }

    private Administrator requireAdmin(User actor, String operation) throws UnauthorizedAccessException {
        if (!(actor instanceof Administrator admin) || !users.contains(actor)) {
            throw new UnauthorizedAccessException(actor == null ? "Unknown" : actor.getName(),
                    operation, "Registered Administrator");
        }
        return admin;
    }

    private Instructor requireInstructor(User actor) throws UnauthorizedAccessException {
        if (!(actor instanceof Instructor instructor) || !users.contains(actor)) {
            throw new UnauthorizedAccessException(actor == null ? "Unknown" : actor.getName(),
                    "report equipment faults", "Registered Instructor");
        }
        return instructor;
    }

    private void requireEquipment(Equipment item) {
        if (item == null || !equipment.contains(item)) {
            throw new IllegalArgumentException("Equipment must be registered in this system.");
        }
    }

    private void requireRequest(MaintenanceRequest request) {
        if (request == null || !equipment.contains(request.getEquipment())
                || !request.getEquipment().getMaintenanceHistory().contains(request)) {
            throw new IllegalArgumentException("Maintenance request must belong to this system.");
        }
    }

    private void checkEmailUnique(String email) throws DuplicateDataException {
        if (users.stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email))) {
            throw new DuplicateDataException("User", email);
        }
    }

    public Administrator registerAdmin(String name, String email, String phone, String adminCode)
            throws DuplicateDataException {
        email = requireText(email, "Email");
        checkEmailUnique(email);
        Administrator user = UserFactory.createAdmin(name, email, phone, adminCode);
        users.add(user);
        return user;
    }

    public Instructor registerInstructor(String name, String email, String phone, String specialization)
            throws DuplicateDataException {
        email = requireText(email, "Email");
        checkEmailUnique(email);
        Instructor user = UserFactory.createInstructor(name, email, phone, specialization);
        users.add(user);
        return user;
    }

    public Member registerMember(String name, String email, String phone, MembershipType membershipType)
            throws DuplicateDataException {
        email = requireText(email, "Email");
        checkEmailUnique(email);
        Member user = UserFactory.createMember(name, email, phone, membershipType);
        users.add(user);
        return user;
    }

    public Equipment addEquipment(User actor, String id, String name, String category, String location)
            throws DuplicateDataException, UnauthorizedAccessException {
        requireAdmin(actor, "add equipment");
        id = requireText(id, "Equipment ID");
        final String identifier = id;
        if (equipment.stream().anyMatch(e -> e.getEquipmentId().equals(identifier))) {
            throw new DuplicateDataException("Equipment", id);
        }
        Equipment item = new Equipment(id, name, category, location);
        equipment.add(item);
        return item;
    }

    public void editEquipment(User actor, Equipment item, String name, String category, String location)
            throws UnauthorizedAccessException {
        requireAdmin(actor, "edit equipment");
        requireEquipment(item);
        item.edit(name, category, location);
    }

    /** Deactivation retains inventory/history but blocks subsequent use and allocation. */
    public void deactivateEquipment(User actor, Equipment item) throws UnauthorizedAccessException {
        requireAdmin(actor, "deactivate equipment");
        requireEquipment(item);
        item.deactivate();
    }

    /** One system-created request per service interval; fault reports still require an instructor. */
    public void addEquipmentUsage(User actor, Equipment item, double hours) throws UnauthorizedAccessException {
        requireAdmin(actor, "record equipment usage");
        requireEquipment(item);
        item.addUsageHours(hours);
        MaintenanceRequest request = item.requestPreventativeMaintenance();
        if (request != null) notifyObservers(request, "CREATED");
    }

    public Session createSession(String id, String title, Instructor instructor, LocalDateTime startTime,
                                 int duration, int capacity, String studio, List<Equipment> allocated)
            throws DuplicateDataException, InvalidBookingException {
        if (id == null || id.isBlank() || title == null || title.isBlank()
                || studio == null || studio.isBlank()) {
            throw new InvalidBookingException("Session ID, title, and studio are required.");
        }
        id = id.trim();
        final String identifier = id;
        if (sessions.stream().anyMatch(s -> s.getSessionId().equals(identifier))) {
            throw new DuplicateDataException("Session", id);
        }
        if (instructor == null || !users.contains(instructor)) {
            throw new InvalidBookingException("Instructor must be registered.");
        }
        if (capacity <= 0) throw new InvalidBookingException("Session capacity must be positive.");
        LocalDateTime end = validateSchedule(startTime, duration);
        if (allocated == null || allocated.stream().anyMatch(e -> e == null || !equipment.contains(e))) {
            throw new InvalidBookingException("Allocated equipment must be registered.");
        }
        if (allocated.stream().distinct().count() != allocated.size()) {
            throw new InvalidBookingException("An equipment item cannot be allocated twice.");
        }
        if (allocated.stream().anyMatch(e -> !e.isUsable())) {
            throw new InvalidBookingException("Allocated equipment must be active and available, with no service due.");
        }
        String space = studio.trim();
        for (Session existing : sessions) {
            if (existing.getStatus() == SessionStatus.CANCELLED || !existing.overlaps(startTime, end)) continue;
            if (existing.getStudio().equalsIgnoreCase(space)) {
                throw new InvalidBookingException("Studio '" + space + "' is already reserved at that time.");
            }
            if (existing.getInstructor().equals(instructor)) {
                throw new InvalidBookingException("Instructor is already scheduled at that time.");
            }
            if (allocated.stream().anyMatch(existing.getEquipment()::contains)) {
                throw new InvalidBookingException("Equipment is already allocated at that time.");
            }
        }
        Session session = new Session(id, title, instructor, startTime, duration, capacity, space, allocated);
        instructor.addSession(session);
        sessions.add(session);
        return session;
    }

    private LocalDateTime validateSchedule(LocalDateTime start, int duration) throws InvalidBookingException {
        if (start == null || !start.isAfter(LocalDateTime.now())) {
            throw new InvalidBookingException("Session start must be a future date and time.");
        }
        if (duration <= 0) throw new InvalidBookingException("Session duration must be positive.");
        try {
            LocalDateTime end = start.plusMinutes(duration);
            if (!start.toLocalDate().equals(end.toLocalDate()) || start.toLocalTime().isBefore(OPENING_TIME)
                    || end.toLocalTime().isAfter(CLOSING_TIME)) {
                throw new InvalidBookingException("Sessions must fit within operating hours 06:00–22:00.");
            }
            return end;
        } catch (DateTimeException e) {
            throw new InvalidBookingException("Session date/time is invalid.", e);
        }
    }

    public Booking bookSession(Member member, Session session) throws InvalidBookingException {
        if (member == null || !users.contains(member)) {
            throw new InvalidBookingException("Member must be registered.");
        }
        if (session == null || !sessions.contains(session)) {
            throw new InvalidBookingException("Session must be registered.");
        }
        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new InvalidBookingException("Session cannot be booked because it is " + session.getStatus() + ".");
        }
        validateSchedule(session.getStartTime(), session.getDurationMinutes());
        if (session.getEquipment().stream().anyMatch(e -> !e.isUsable())) {
            throw new InvalidBookingException("Session equipment is unavailable or deactivated.");
        }
        if (member.getBookings().stream().anyMatch(b -> b.getSession() == session
                && b.getStatus() == BookingStatus.CONFIRMED)) {
            throw new InvalidBookingException(member.getName() + " already has a confirmed booking for this session.");
        }
        if (session.getCurrentEnrollmentCount() >= session.getMaxCapacity()) {
            throw new InvalidBookingException("Session is full (capacity: " + session.getMaxCapacity() + ").");
        }
        if (!session.enroll(member)) throw new InvalidBookingException("Member cannot be enrolled in this session.");
        Booking booking = new Booking(String.format("B-%03d", bookingCounter++), member, session);
        member.addBooking(booking);
        bookings.add(booking);
        return booking;
    }

    public void cancelBooking(Member member, String bookingId) throws InvalidBookingException {
        if (member == null || !users.contains(member) || bookingId == null) {
            throw new InvalidBookingException("Registered member and booking ID are required.");
        }
        Booking booking = bookings.stream().filter(b -> b.getBookingId().equals(bookingId)).findFirst()
                .orElseThrow(() -> new InvalidBookingException("Booking was not found."));
        if (!booking.getMember().equals(member)) {
            throw new InvalidBookingException("Members may cancel only their own bookings.");
        }
        if (!member.cancelBooking(bookingId)) {
            throw new InvalidBookingException("Only confirmed bookings can be cancelled.");
        }
    }

    public MaintenanceRequest submitMaintenanceRequest(User actor, Equipment item, String description,
                                                       MaintenanceRequest.Urgency urgency)
            throws UnauthorizedAccessException {
        Instructor reporter = requireInstructor(actor);
        requireEquipment(item);
        if (!item.isActive()) throw new IllegalArgumentException("Deactivated equipment cannot receive new fault reports.");
        MaintenanceRequest request = item.requestMaintenance(reporter, description, urgency);
        notifyObservers(request, "CREATED");
        return request;
    }

    public void assignMaintenanceRequest(User actor, MaintenanceRequest request, String assignee)
            throws UnauthorizedAccessException {
        Administrator admin = requireAdmin(actor, "assign maintenance tasks");
        requireRequest(request);
        admin.assignMaintenance(request, assignee);
        notifyObservers(request, "ASSIGNED");
    }

    public void completeMaintenanceRequest(User actor, MaintenanceRequest request)
            throws UnauthorizedAccessException {
        Administrator admin = requireAdmin(actor, "complete maintenance tasks");
        requireRequest(request);
        admin.completeMaintenance(request);
        notifyObservers(request, "COMPLETED");
    }

    public List<User> getUsers() { return Collections.unmodifiableList(users); }
    public List<Equipment> getAllEquipment() { return Collections.unmodifiableList(equipment); }
    public List<Session> getSessions() { return Collections.unmodifiableList(sessions); }
    public List<Booking> getBookings() { return Collections.unmodifiableList(bookings); }
    public List<MaintenanceRequest> getMaintenanceRequests() {
        return equipment.stream().flatMap(e -> e.getMaintenanceHistory().stream()).toList();
    }
    public List<User> getUsersByRole(String role) {
        return users.stream().filter(u -> u.getRole().equalsIgnoreCase(role)).toList();
    }
    public Optional<User> findUserByEmail(String email) {
        return users.stream().filter(u -> u.getEmail().equalsIgnoreCase(email)).findFirst();
    }
    public Optional<Session> findSessionById(String id) {
        return sessions.stream().filter(s -> s.getSessionId().equals(id)).findFirst();
    }
    public Optional<Equipment> findEquipmentById(String id) {
        return equipment.stream().filter(e -> e.getEquipmentId().equals(id)).findFirst();
    }
}