package com.iwfc;

import com.iwfc.model.*;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


@DisplayName("Business Logic and Exception Handling – Tests")
class BusinessLogicTest {

    private IWFCFacade    facade;
    private Administrator admin;
    private Instructor    instructor;
    private Member        member;
    private Equipment     treadmill;
    private Session       yogaSession;

    @BeforeEach
    void setUp() throws Exception {
        facade     = new IWFCFacade();
        admin      = facade.registerAdmin("Alice Admin",   "alice@iwfc.com",   "555-0001", "ADMIN-001");
        instructor = facade.registerInstructor("Bob Train", "bob@iwfc.com",    "555-0002", "Yoga");
        member     = facade.registerMember("Carol Member", "carol@iwfc.com",   "555-0003", MembershipType.PREMIUM);
        treadmill  = facade.addEquipment(admin, "EQ-001", "Treadmill", "Cardio", "Main floor");
        yogaSession = facade.createSession("S-001", "Morning Yoga", instructor,
                          java.time.LocalDate.now().plusDays(1).atTime(9, 0), 60, 3, "Studio A", List.of());
    }

    // Section A: Duplicate Detection

    @Test
    @DisplayName("Registering a user with a duplicate e-mail throws DuplicateDataException")
    void duplicateEmailThrows() throws Exception {
        DuplicateDataException ex = assertThrows(DuplicateDataException.class, () ->
                facade.registerMember("Carol2", "carol@iwfc.com", "555-9999", MembershipType.BASIC));

        assertEquals("User",            ex.getEntityType());
        assertEquals("carol@iwfc.com",  ex.getDuplicateIdentifier());
        assertTrue(ex.getMessage().contains("carol@iwfc.com"));
    }

    @Test
    @DisplayName("Duplicate e-mail check is case-insensitive")
    void duplicateEmailCaseInsensitive() throws Exception {
        assertThrows(DuplicateDataException.class, () ->
                facade.registerAdmin("Alice2", "ALICE@IWFC.COM", "555-9999", "ADMIN-002"));
    }

    @Test
    @DisplayName("Registering different e-mails succeeds without exception")
    void uniqueEmailsSucceed() throws Exception {
        facade.registerMember("Dave",  "dave@iwfc.com",  "555-0004", MembershipType.BASIC);
        facade.registerMember("Eve",   "eve@iwfc.com",   "555-0005", MembershipType.VIP);
        assertEquals(5, facade.getUsers().size());
    }

    @Test
    @DisplayName("Adding equipment with a duplicate ID throws DuplicateDataException")
    void duplicateEquipmentIdThrows() throws Exception {
        DuplicateDataException ex = assertThrows(DuplicateDataException.class, () ->
                facade.addEquipment(admin, "EQ-001", "Other Treadmill", "Cardio", "Main floor"));

        assertEquals("Equipment", ex.getEntityType());
        assertEquals("EQ-001",    ex.getDuplicateIdentifier());
    }

    @Test
    @DisplayName("Adding equipment with different IDs succeeds")
    void uniqueEquipmentIdsSucceed() throws Exception {
        facade.addEquipment(admin, "EQ-002", "Bike",   "Cardio", "Main floor");
        facade.addEquipment(admin, "EQ-003", "Weights", "Strength", "Main floor");
        assertEquals(3, facade.getAllEquipment().size());
    }

    @Test
    @DisplayName("Creating a session with a duplicate ID throws DuplicateDataException")
    void duplicateSessionIdThrows() throws Exception {
        DuplicateDataException ex = assertThrows(DuplicateDataException.class, () ->
                facade.createSession("S-001", "Evening Yoga", instructor,
                        java.time.LocalDate.now().plusDays(2).atTime(9, 0), 45, 5, "Studio A", List.of()));

        assertEquals("Session", ex.getEntityType());
        assertEquals("S-001",   ex.getDuplicateIdentifier());
    }

    // Section B: Booking Validation

    @Test
    @DisplayName("A valid booking succeeds and updates all related objects")
    void validBookingSucceeds() throws Exception {
        Booking booking = facade.bookSession(member, yogaSession);

        assertNotNull(booking);
        assertEquals(BookingStatus.CONFIRMED,   booking.getStatus());
        assertEquals(member,                    booking.getMember());
        assertEquals(yogaSession,               booking.getSession());
        assertTrue(booking.getBookingId().startsWith("B-"));
        assertEquals(1, facade.getBookings().size());
        assertEquals(1, member.getBookings().size());
        assertEquals(1, yogaSession.getCurrentEnrollmentCount());
    }

    @Test
    @DisplayName("Booking a full session throws InvalidBookingException with capacity detail")
    void bookingFullSessionThrows() throws Exception {
        // Fill the 3-capacity session
        Member m2 = facade.registerMember("M2", "m2@iwfc.com", "2", MembershipType.BASIC);
        Member m3 = facade.registerMember("M3", "m3@iwfc.com", "3", MembershipType.BASIC);
        facade.bookSession(member, yogaSession);
        facade.bookSession(m2,     yogaSession);
        facade.bookSession(m3,     yogaSession);

        // 4th member should fail
        Member m4 = facade.registerMember("M4", "m4@iwfc.com", "4", MembershipType.BASIC);
        InvalidBookingException ex = assertThrows(InvalidBookingException.class,
                () -> facade.bookSession(m4, yogaSession));

        assertTrue(ex.getMessage().contains("full"),
                "Exception message should mention 'full'");
    }

    @Test
    @DisplayName("Booking the same session twice throws InvalidBookingException")
    void duplicateBookingThrows() throws Exception {
        facade.bookSession(member, yogaSession);

        InvalidBookingException ex = assertThrows(InvalidBookingException.class,
                () -> facade.bookSession(member, yogaSession));

        assertTrue(ex.getMessage().contains("already"),
                "Exception message should mention 'already'");
    }

    @Test
    @DisplayName("Booking a cancelled session throws InvalidBookingException")
    void bookingCancelledSessionThrows() throws Exception {
        yogaSession.cancel();

        InvalidBookingException ex = assertThrows(InvalidBookingException.class,
                () -> facade.bookSession(member, yogaSession));

        assertTrue(ex.getMessage().contains("CANCELLED"),
                "Exception message should mention the status");
    }

    @Test
    @DisplayName("Booking with a member not in the system throws InvalidBookingException")
    void bookingUnregisteredMemberThrows() throws Exception {
        // Create member via factory directly – never registered in facade
        Member outsider = UserFactory.createMember("Outsider", "out@test.com",
                "000", MembershipType.BASIC);

        assertThrows(InvalidBookingException.class,
                () -> facade.bookSession(outsider, yogaSession));
    }

    @Test
    @DisplayName("Booking a session not in the system throws InvalidBookingException")
    void bookingUnregisteredSessionThrows() throws Exception {
        // Create session directly – not registered in facade
        Session ghostSession = new Session("GHOST", "Ghost Session", instructor,
                java.time.LocalDate.now().plusDays(5).atTime(9, 0), 30, 10, "Studio A", List.of());

        assertThrows(InvalidBookingException.class,
                () -> facade.bookSession(member, ghostSession));
    }

    @Test
    @DisplayName("Cancelled booking allows the member to re-book the same session")
    void reBookingAfterCancelSucceeds() throws Exception {
        Booking first = facade.bookSession(member, yogaSession);
        first.cancel();   // cancels booking and removes from session

        // Member should be able to book again
        Booking second = facade.bookSession(member, yogaSession);
        assertEquals(BookingStatus.CONFIRMED, second.getStatus());
    }

    // Section C: Authorisation

    @Test
    @DisplayName("A Member attempting maintenance approval throws UnauthorizedAccessException")
    void memberCannotAssignMaintenance() throws Exception {
        MaintenanceRequest request = facade.submitMaintenanceRequest(instructor, treadmill, "Belt issue", MaintenanceRequest.Urgency.MEDIUM);

        UnauthorizedAccessException ex = assertThrows(UnauthorizedAccessException.class,
                () -> facade.assignMaintenanceRequest(member, request, "Staff"));

        assertEquals("Carol Member",          ex.getUserName());
        assertEquals("assign maintenance tasks", ex.getOperation());
        assertEquals("Registered Administrator", ex.getRequiredRole());
        assertTrue(ex.getMessage().contains("not authorised"));
    }

    @Test
    @DisplayName("An Instructor attempting maintenance approval throws UnauthorizedAccessException")
    void instructorCannotAssignMaintenance() throws Exception {
        MaintenanceRequest request = facade.submitMaintenanceRequest(instructor, treadmill, "Motor noise", MaintenanceRequest.Urgency.MEDIUM);

        assertThrows(UnauthorizedAccessException.class,
                () -> facade.assignMaintenanceRequest(instructor, request, "Staff"));
    }

    @Test
    @DisplayName("An Administrator can successfully assign a maintenance request")
    void adminCanAssignMaintenance() throws Exception {
        MaintenanceRequest request = facade.submitMaintenanceRequest(instructor, treadmill, "Screen cracked", MaintenanceRequest.Urgency.MEDIUM);
        assertEquals(MaintenanceRequest.Status.PENDING, request.getStatus());

        facade.assignMaintenanceRequest(admin, request, "Staff");
        assertTrue(request.getStatus() == MaintenanceRequest.Status.ASSIGNED);
        assertEquals("Alice Admin", request.getAssignedBy().getName());
    }

    @Test
    @DisplayName("UnauthorizedAccessException message contains user name and required role")
    void unauthorizedExceptionMessage() throws Exception {
        MaintenanceRequest request = facade.submitMaintenanceRequest(instructor, treadmill, "Paint worn", MaintenanceRequest.Urgency.MEDIUM);

        UnauthorizedAccessException ex = assertThrows(UnauthorizedAccessException.class,
                () -> facade.assignMaintenanceRequest(member, request, "Staff"));

        String msg = ex.getMessage();
        assertTrue(msg.contains("Carol Member"),   "Message should contain user name");
        assertTrue(msg.contains("Administrator"),  "Message should contain required role");
    }

    // Section D: Maintenance Workflow

    @Test
    @DisplayName("Submitting a maintenance request sets equipment status to IN_MAINTENANCE")
    void maintenanceRequestSetsStatus() throws Exception {
        assertEquals(EquipmentStatus.AVAILABLE, treadmill.getStatus());

        facade.submitMaintenanceRequest(instructor, treadmill, "Belt worn out", MaintenanceRequest.Urgency.MEDIUM);

        assertEquals(EquipmentStatus.IN_MAINTENANCE, treadmill.getStatus());
        assertEquals(1, treadmill.getMaintenanceHistory().size());
    }

    @Test
    @DisplayName("Observer receives CREATED and ASSIGNED events in order")
    void maintenanceObserverEventsInOrder() throws Exception {
        List<String> events = new ArrayList<>();
        facade.addObserver((req, e) -> events.add(e));

        MaintenanceRequest request = facade.submitMaintenanceRequest(instructor, treadmill, "Hydraulic leak", MaintenanceRequest.Urgency.MEDIUM);
        facade.assignMaintenanceRequest(admin, request, "Staff");

        assertEquals(List.of("CREATED", "ASSIGNED"), events);
    }

    @Test
    @DisplayName("Assigned request is added to the administrator's managed list")
    void assignedRequestTrackedByAdmin() throws Exception {
        MaintenanceRequest request = facade.submitMaintenanceRequest(instructor, treadmill, "Loose bolts", MaintenanceRequest.Urgency.MEDIUM);
        facade.assignMaintenanceRequest(admin, request, "Staff");

        assertEquals(1, admin.getManagedRequests().size());
        assertSame(request, admin.getManagedRequests().get(0));
    }

    // Section E: Optional Query Methods

    @Test
    @DisplayName("findUserByEmail returns the correct user when found")
    void findUserByEmailFound() throws Exception {
        Optional<User> result = facade.findUserByEmail("carol@iwfc.com");

        assertTrue(result.isPresent());
        assertEquals("Carol Member", result.get().getName());
    }

    @Test
    @DisplayName("findUserByEmail is case-insensitive")
    void findUserByEmailCaseInsensitive() throws Exception {
        Optional<User> result = facade.findUserByEmail("CAROL@IWFC.COM");
        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("findUserByEmail returns empty Optional for unknown e-mail")
    void findUserByEmailNotFound() throws Exception {
        Optional<User> result = facade.findUserByEmail("nobody@unknown.com");
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("findSessionById returns the correct session")
    void findSessionByIdFound() throws Exception {
        Optional<Session> result = facade.findSessionById("S-001");

        assertTrue(result.isPresent());
        assertEquals("Morning Yoga", result.get().getTitle());
    }

    @Test
    @DisplayName("findSessionById returns empty Optional for unknown ID")
    void findSessionByIdNotFound() throws Exception {
        assertFalse(facade.findSessionById("UNKNOWN").isPresent());
    }

    @Test
    @DisplayName("findEquipmentById returns the correct equipment")
    void findEquipmentByIdFound() throws Exception {
        Optional<Equipment> result = facade.findEquipmentById("EQ-001");

        assertTrue(result.isPresent());
        assertEquals("Treadmill", result.get().getName());
    }

    // Section F: Integration scenario

    @Test
    @DisplayName("Full workflow: register → book → cancel → re-book → assign maintenance")
    void fullWorkflowIntegration() throws Exception {
        // Register another member
        Member dave = facade.registerMember("Dave", "dave@iwfc.com", "555-0004", MembershipType.VIP);

        // Dave books the yoga session
        Booking booking = facade.bookSession(dave, yogaSession);
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());

        // Dave cancels
        booking.cancel();
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(0, yogaSession.getCurrentEnrollmentCount());

        // Dave re-books after cancellation
        Booking rebook = facade.bookSession(dave, yogaSession);
        assertEquals(BookingStatus.CONFIRMED, rebook.getStatus());

        // Equipment maintenance assigned by admin
        List<String> log = new ArrayList<>();
        facade.addObserver((req, e) -> log.add(e));
        MaintenanceRequest req = facade.submitMaintenanceRequest(instructor, treadmill, "Full service", MaintenanceRequest.Urgency.MEDIUM);
        facade.assignMaintenanceRequest(admin, req, "Staff");

        assertEquals(2, log.size());
        assertTrue(req.getStatus() == MaintenanceRequest.Status.ASSIGNED);
    }

    @Test
    @DisplayName("DuplicateDataException carries correct entity type and identifier")
    void duplicateExceptionFields() throws Exception {
        DuplicateDataException ex = assertThrows(DuplicateDataException.class, () ->
                facade.addEquipment(admin, "EQ-001", "Duplicate", "Cardio", "Main floor"));

        assertEquals("Equipment", ex.getEntityType());
        assertEquals("EQ-001",    ex.getDuplicateIdentifier());
        assertNotNull(ex.getMessage());
        assertFalse(ex.getMessage().isBlank());
    }
private LocalDateTime at(int hour, int minute) {
        return java.time.LocalDate.now().plusDays(1).atTime(hour, minute);
    }
    private Instructor anotherInstructor() throws Exception {
        return facade.registerInstructor("Other", "other" + facade.getUsers().size() + "@test.com", "4", "Fitness");
    }
    private Session resourceSession() throws Exception {
        return facade.createSession("RESOURCE", "Resource Class", instructor, at(12,0), 60, 5, "Studio B", List.of(treadmill));
    }
    private MaintenanceRequest fault() throws Exception {
        return facade.submitMaintenanceRequest(instructor, treadmill, "Motor fault", MaintenanceRequest.Urgency.HIGH);
    }

    @Test void equipmentLocationIsStored() { assertEquals("Main floor", treadmill.getLocation()); }
    @Test void equipmentUsageAccumulates() throws Exception {
        facade.addEquipmentUsage(admin, treadmill, 10.5);
        facade.addEquipmentUsage(admin, treadmill, 4.25);
        assertEquals(14.75, treadmill.getUsageHours(), 0.001);
    }
    @Test void negativeUsageIsRejectedWithoutMutation() {
        assertThrows(IllegalArgumentException.class, () -> facade.addEquipmentUsage(admin, treadmill, -1));
        assertEquals(0, treadmill.getUsageHours());
        assertTrue(treadmill.getMaintenanceHistory().isEmpty());
    }
    @Test void nonFiniteUsageIsRejected() {
        for (double h : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
            assertThrows(IllegalArgumentException.class, () -> facade.addEquipmentUsage(admin, treadmill, h));
        assertEquals(0, treadmill.getUsageHours());
    }
    @Test void belowThresholdDoesNotAlert() throws Exception {
        List<String> events = new ArrayList<>();
        facade.addObserver((r,e) -> events.add(e));
        facade.addEquipmentUsage(admin, treadmill, 99.5);
        assertFalse(treadmill.isMaintenanceDue());
        assertTrue(events.isEmpty());
    }
    @Test void thresholdReachedCreatesOnePreventativeRequest() throws Exception {
        List<String> events = new ArrayList<>();
        facade.addObserver((r,e) -> events.add(e));
        facade.addEquipmentUsage(admin, treadmill, 100);
        assertTrue(treadmill.isMaintenanceDue());
        MaintenanceRequest r = treadmill.getMaintenanceHistory().get(0);
        assertTrue(r.isPreventative());
        assertNull(r.getReporter());
        assertEquals(MaintenanceRequest.Status.PENDING, r.getStatus());
        assertEquals(List.of("CREATED"), events);
        assertFalse(treadmill.isUsable());
    }
    @Test void aboveThresholdDoesNotRepeatAlert() throws Exception {
        List<String> events = new ArrayList<>();
        facade.addObserver((r,e) -> events.add(e));
        facade.addEquipmentUsage(admin, treadmill, 125);
        facade.addEquipmentUsage(admin, treadmill, 10);
        facade.addEquipmentUsage(admin, treadmill, 0);
        assertEquals(135, treadmill.getUsageHours());
        assertEquals(1, treadmill.getMaintenanceHistory().size());
        assertEquals(List.of("CREATED"), events);
    }
    @Test void serviceResetsIntervalButPreservesLifetimeUsage() throws Exception {
        facade.addEquipmentUsage(admin, treadmill, 100);
        MaintenanceRequest r = treadmill.getMaintenanceHistory().get(0);
        facade.assignMaintenanceRequest(admin, r, "Staff");
        facade.completeMaintenanceRequest(admin, r);
        assertEquals(100, treadmill.getUsageHours());
        assertFalse(treadmill.isMaintenanceDue());
        facade.addEquipmentUsage(admin, treadmill, 99);
        assertEquals(1, treadmill.getMaintenanceHistory().size());
        facade.addEquipmentUsage(admin, treadmill, 1);
        assertEquals(200, treadmill.getUsageHours());
        assertEquals(2, treadmill.getMaintenanceHistory().size());
    }
    @Test void equipmentEditKeepsIdentity() throws Exception {
        facade.editEquipment(admin, treadmill, "Updated", "Strength", "Studio C");
        assertEquals("Updated", treadmill.getName());
        assertEquals("Strength", treadmill.getCategory());
        assertEquals("Studio C", treadmill.getLocation());
        assertEquals("EQ-001", treadmill.getEquipmentId());
        assertSame(treadmill, facade.getAllEquipment().get(0));
    }
    @Test void invalidEditIsAtomic() {
        assertThrows(IllegalArgumentException.class, () -> facade.editEquipment(admin, treadmill, "Changed", "Cardio", " "));
        assertEquals("Treadmill", treadmill.getName());
        assertEquals("Main floor", treadmill.getLocation());
    }
    @Test void deactivationIsSeparateFromFaultStatus() throws Exception {
        facade.deactivateEquipment(admin, treadmill);
        assertFalse(treadmill.isActive());
        assertFalse(treadmill.isUsable());
        assertEquals(EquipmentStatus.AVAILABLE, treadmill.getStatus());
        assertThrows(IllegalStateException.class, () -> facade.addEquipmentUsage(admin, treadmill, 1));
    }
    @Test void membersCannotManageEquipment() {
        assertThrows(UnauthorizedAccessException.class, () -> facade.addEquipment(member, "X", "X", "X", "X"));
        assertThrows(UnauthorizedAccessException.class, () -> facade.editEquipment(member, treadmill, "X", "X", "X"));
        assertThrows(UnauthorizedAccessException.class, () -> facade.deactivateEquipment(member, treadmill));
        assertThrows(UnauthorizedAccessException.class, () -> facade.addEquipmentUsage(member, treadmill, 1));
        assertTrue(treadmill.isActive());
    }
    @Test void instructorsCannotManageEquipment() {
        assertThrows(UnauthorizedAccessException.class, () -> facade.addEquipment(instructor, "X", "X", "X", "X"));
        assertThrows(UnauthorizedAccessException.class, () -> facade.editEquipment(instructor, treadmill, "X", "X", "X"));
        assertThrows(UnauthorizedAccessException.class, () -> facade.deactivateEquipment(instructor, treadmill));
    }
    @Test void unregisteredAndNullAdminAreRejected() {
        Administrator outsider = UserFactory.createAdmin("Outside", "outside@test.com", "1", "OUT");
        assertThrows(UnauthorizedAccessException.class, () -> facade.deactivateEquipment(outsider, treadmill));
        assertThrows(UnauthorizedAccessException.class, () -> facade.deactivateEquipment(null, treadmill));
    }
    @Test void foreignEquipmentCannotBeManaged() {
        Equipment outsider = new Equipment("OUT", "Outside", "Cardio", "Lobby");
        assertThrows(IllegalArgumentException.class, () -> facade.editEquipment(admin, outsider, "X", "X", "X"));
        assertThrows(IllegalArgumentException.class, () -> facade.deactivateEquipment(admin, null));
    }
    @Test void blankEquipmentLocationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> facade.addEquipment(admin, "NEW", "New", "Cardio", " "));
        assertEquals(1, facade.getAllEquipment().size());
    }
    @Test void sessionStoresStudioAndMultipleEquipment() throws Exception {
        Equipment bike = facade.addEquipment(admin, "BIKE", "Bike", "Cardio", "Studio B");
        Session s = facade.createSession("MULTI", "Circuit", instructor, at(12,0), 60, 5, "Studio B", List.of(treadmill, bike));
        assertEquals("Studio B", s.getStudio());
        assertEquals(List.of(treadmill, bike), s.getEquipment());
        assertEquals(at(13,0), s.getEndTime());
    }
    @Test void identicalStudioTimesAreRejected() throws Exception {
        resourceSession();
        assertThrows(InvalidBookingException.class, () -> facade.createSession("X", "Other", anotherInstructor(), at(12,0), 60, 5, "Studio B", List.of()));
    }
    @Test void partialStudioOverlapIsRejected() throws Exception {
        resourceSession();
        assertThrows(InvalidBookingException.class, () -> facade.createSession("X", "Other", anotherInstructor(), at(12,30), 60, 5, "Studio B", List.of()));
    }
    @Test void containedStudioIntervalIsRejected() throws Exception {
        resourceSession();
        assertThrows(InvalidBookingException.class, () -> facade.createSession("X", "Other", anotherInstructor(), at(12,15), 15, 5, "Studio B", List.of()));
    }
    @Test void containingStudioIntervalIsRejected() throws Exception {
        resourceSession();
        assertThrows(InvalidBookingException.class, () -> facade.createSession("X", "Other", anotherInstructor(), at(11,30), 120, 5, "Studio B", List.of()));
    }
    @Test void backToBackSessionsAreAllowed() throws Exception {
        resourceSession();
        assertNotNull(facade.createSession("NEXT", "Next", instructor, at(13,0), 60, 5, "Studio B", List.of(treadmill)));
    }
    @Test void sessionEndingAtExistingStartIsAllowed() throws Exception {
        resourceSession();
        assertNotNull(facade.createSession("BEFORE", "Before", instructor, at(11,0), 60, 5, "Studio B", List.of(treadmill)));
    }
    @Test void sameTimeDifferentStudiosAndResourcesIsAllowed() throws Exception {
        resourceSession();
        assertNotNull(facade.createSession("OTHER", "Other", anotherInstructor(), at(12,0), 60, 5, "Studio C", List.of()));
    }
    @Test void studioIdentityIgnoresCaseAndWhitespace() throws Exception {
        resourceSession();
        assertThrows(InvalidBookingException.class, () -> facade.createSession("OTHER", "Other", anotherInstructor(), at(12,0), 60, 5, " studio b ", List.of()));
    }
    @Test void overlappingEquipmentIsRejectedAcrossStudios() throws Exception {
        resourceSession();
        InvalidBookingException ex = assertThrows(InvalidBookingException.class,
                () -> facade.createSession("OTHER", "Other", anotherInstructor(), at(12,30), 60, 5, "Studio C", List.of(treadmill)));
        assertTrue(ex.getMessage().contains("Equipment"));
    }
    @Test void differentEquipmentCanOverlap() throws Exception {
        resourceSession();
        Equipment bike = facade.addEquipment(admin, "BIKE", "Bike", "Cardio", "Studio C");
        assertNotNull(facade.createSession("OTHER", "Other", anotherInstructor(), at(12,30), 60, 5, "Studio C", List.of(bike)));
    }
    @Test void instructorCannotOverlapAcrossStudios() throws Exception {
        resourceSession();
        InvalidBookingException ex = assertThrows(InvalidBookingException.class,
                () -> facade.createSession("OTHER", "Other", instructor, at(12,30), 60, 5, "Studio C", List.of()));
        assertTrue(ex.getMessage().contains("Instructor"));
    }
    @Test void cancelledSessionReleasesResources() throws Exception {
        resourceSession().cancel();
        assertNotNull(facade.createSession("REPLACE", "Replacement", instructor, at(12,0), 60, 5, "Studio B", List.of(treadmill)));
    }
    @Test void conflictRejectionDoesNotChangeStores() throws Exception {
        resourceSession();
        int count = facade.getSessions().size(), scheduleCount = instructor.getSessions().size();
        assertThrows(InvalidBookingException.class, () -> facade.createSession("BAD", "Bad", instructor, at(12,30), 60, 5, "Other", List.of()));
        assertEquals(count, facade.getSessions().size());
        assertEquals(scheduleCount, instructor.getSessions().size());
    }
    @Test void deactivatedEquipmentCannotBeAllocated() throws Exception {
        facade.deactivateEquipment(admin, treadmill);
        assertThrows(InvalidBookingException.class, () -> facade.createSession("OTHER", "Other", instructor, at(12,0), 60, 5, "Studio B", List.of(treadmill)));
    }
    @Test void faultyEquipmentCannotBeAllocated() {
        treadmill.markOutOfOrder();
        assertThrows(InvalidBookingException.class, () -> facade.createSession("OTHER", "Other", instructor, at(12,0), 60, 5, "Studio B", List.of(treadmill)));
    }
    @Test void deactivationAfterAllocationPreventsBooking() throws Exception {
        Session s = resourceSession();
        facade.deactivateEquipment(admin, treadmill);
        assertThrows(InvalidBookingException.class, () -> facade.bookSession(member, s));
        assertEquals(0, s.getCurrentEnrollmentCount());
    }
    @Test void unregisteredEquipmentCannotBeAllocated() {
        Equipment outsider = new Equipment("OUT", "Outside", "Cardio", "Lobby");
        assertThrows(InvalidBookingException.class, () -> facade.createSession("OTHER", "Other", instructor, at(12,0), 60, 5, "Studio B", List.of(outsider)));
    }
    @Test void duplicateAllocationIsRejected() {
        assertThrows(InvalidBookingException.class, () -> facade.createSession("OTHER", "Other", instructor, at(12,0), 60, 5, "Studio B", List.of(treadmill, treadmill)));
    }
    @Test void sessionAtOpeningIsAllowed() throws Exception {
        assertNotNull(facade.createSession("OPEN", "Opening", instructor, at(6,0), 60, 5, "Studio B", List.of()));
    }
    @Test void sessionEndingAtClosingIsAllowed() throws Exception {
        assertNotNull(facade.createSession("CLOSE", "Closing", instructor, at(21,0), 60, 5, "Studio B", List.of()));
    }
    @Test void beforeOpeningIsRejected() {
        assertThrows(InvalidBookingException.class, () -> facade.createSession("EARLY", "Early", instructor, at(5,59), 60, 5, "Studio B", List.of()));
    }
    @Test void startingAfterClosingIsRejected() {
        assertThrows(InvalidBookingException.class, () -> facade.createSession("LATE", "Late", instructor, at(22,1), 60, 5, "Studio B", List.of()));
    }
    @Test void endingAfterClosingIsRejected() {
        assertThrows(InvalidBookingException.class, () -> facade.createSession("LATE", "Late", instructor, at(21,30), 60, 5, "Studio B", List.of()));
    }
    @Test void overnightSessionIsRejected() {
        assertThrows(InvalidBookingException.class, () -> facade.createSession("NIGHT", "Night", instructor, at(21,0), 600, 5, "Studio B", List.of()));
    }
    @Test void pastAndNullTimesAreRejected() {
        assertThrows(InvalidBookingException.class, () -> facade.createSession("PAST", "Past", instructor, LocalDateTime.now().minusDays(1), 60, 5, "Studio B", List.of()));
        assertThrows(InvalidBookingException.class, () -> facade.createSession("NULL", "Null", instructor, null, 60, 5, "Studio B", List.of()));
    }
    @Test void nonPositiveDurationAndCapacityAreRejected() {
        for (int v : new int[]{0, -1}) {
            assertThrows(InvalidBookingException.class, () -> facade.createSession("D", "Bad", instructor, at(12,0), v, 5, "Studio B", List.of()));
            assertThrows(InvalidBookingException.class, () -> facade.createSession("C", "Bad", instructor, at(12,0), 60, v, "Studio B", List.of()));
        }
    }
    @Test void nullResourcesAndBlankStudioAreRejected() {
        assertThrows(InvalidBookingException.class, () -> facade.createSession("X", "X", instructor, at(12,0), 60, 5, " ", List.of()));
        assertThrows(InvalidBookingException.class, () -> facade.createSession("X", "X", instructor, at(12,0), 60, 5, "B", null));
        assertThrows(InvalidBookingException.class, () -> facade.createSession("X", "X", null, at(12,0), 60, 5, "B", List.of()));
    }
    @Test void nullBookingArgumentsUseCheckedException() {
        assertThrows(InvalidBookingException.class, () -> facade.bookSession(null, yogaSession));
        assertThrows(InvalidBookingException.class, () -> facade.bookSession(member, null));
    }
    @Test void completedAndStartedSessionsRejectBookings() {
        yogaSession.start();
        assertThrows(InvalidBookingException.class, () -> facade.bookSession(member, yogaSession));
        yogaSession.complete();
        assertThrows(InvalidBookingException.class, () -> facade.bookSession(member, yogaSession));
    }
    @Test void facadeCancellationReleasesCapacity() throws Exception {
        Booking b = facade.bookSession(member, yogaSession);
        facade.cancelBooking(member, b.getBookingId());
        assertEquals(BookingStatus.CANCELLED, b.getStatus());
        assertEquals(0, yogaSession.getCurrentEnrollmentCount());
        assertNotNull(facade.bookSession(member, yogaSession));
    }
    @Test void cannotCancelAnotherMembersBooking() throws Exception {
        Booking b = facade.bookSession(member, yogaSession);
        Member other = facade.registerMember("Other", "other@test.com", "4", MembershipType.BASIC);
        assertThrows(InvalidBookingException.class, () -> facade.cancelBooking(other, b.getBookingId()));
        assertEquals(BookingStatus.CONFIRMED, b.getStatus());
    }
    @Test void cancellationRejectsUnknownAndCancelledBooking() throws Exception {
        assertThrows(InvalidBookingException.class, () -> facade.cancelBooking(member, "UNKNOWN"));
        Booking b = facade.bookSession(member, yogaSession);
        facade.cancelBooking(member, b.getBookingId());
        assertThrows(InvalidBookingException.class, () -> facade.cancelBooking(member, b.getBookingId()));
    }
    @Test void faultRecordsReporterUrgencyAndPendingState() throws Exception {
        MaintenanceRequest r = fault();
        assertSame(instructor, r.getReporter());
        assertSame(treadmill, r.getEquipment());
        assertEquals("Motor fault", r.getDescription());
        assertEquals(MaintenanceRequest.Urgency.HIGH, r.getUrgency());
        assertEquals(MaintenanceRequest.Status.PENDING, r.getStatus());
        assertEquals(r.getCreatedAt(), r.getUpdatedAt());
    }
    @Test void membersAndAdminsCannotReportFaults() {
        assertThrows(UnauthorizedAccessException.class, () -> facade.submitMaintenanceRequest(member, treadmill, "Fault", MaintenanceRequest.Urgency.LOW));
        assertThrows(UnauthorizedAccessException.class, () -> facade.submitMaintenanceRequest(admin, treadmill, "Fault", MaintenanceRequest.Urgency.LOW));
        assertTrue(treadmill.getMaintenanceHistory().isEmpty());
    }
    @Test void nullAndUnregisteredInstructorCannotReport() {
        Instructor outsider = UserFactory.createInstructor("Outside", "out@test.com", "1", "Yoga");
        assertThrows(UnauthorizedAccessException.class, () -> facade.submitMaintenanceRequest(outsider, treadmill, "Fault", MaintenanceRequest.Urgency.LOW));
        assertThrows(UnauthorizedAccessException.class, () -> facade.submitMaintenanceRequest(null, treadmill, "Fault", MaintenanceRequest.Urgency.LOW));
    }
    @Test void blankDescriptionDoesNotMutateEquipment() {
        assertThrows(IllegalArgumentException.class, () -> facade.submitMaintenanceRequest(instructor, treadmill, " ", MaintenanceRequest.Urgency.LOW));
        assertEquals(EquipmentStatus.AVAILABLE, treadmill.getStatus());
        assertTrue(treadmill.getMaintenanceHistory().isEmpty());
    }
    @Test void nullUrgencyAndEquipmentAreRejected() {
        assertThrows(NullPointerException.class, () -> facade.submitMaintenanceRequest(instructor, treadmill, "Fault", null));
        assertThrows(IllegalArgumentException.class, () -> facade.submitMaintenanceRequest(instructor, null, "Fault", MaintenanceRequest.Urgency.LOW));
    }
    @Test void assignmentRecordsAssigneeAndAdmin() throws Exception {
        MaintenanceRequest r = fault();
        facade.assignMaintenanceRequest(admin, r, "  Maintenance Staff  ");
        assertEquals(MaintenanceRequest.Status.ASSIGNED, r.getStatus());
        assertEquals("Maintenance Staff", r.getAssignedTo());
        assertSame(admin, r.getAssignedBy());
        assertFalse(r.getUpdatedAt().isBefore(r.getCreatedAt()));
    }
    @Test void completionRestoresEquipment() throws Exception {
        MaintenanceRequest r = fault();
        facade.assignMaintenanceRequest(admin, r, "Staff");
        facade.completeMaintenanceRequest(admin, r);
        assertEquals(MaintenanceRequest.Status.COMPLETED, r.getStatus());
        assertSame(admin, r.getCompletedBy());
        assertTrue(treadmill.isUsable());
    }
    @Test void pendingTaskCannotBeCompleted() throws Exception {
        MaintenanceRequest r = fault();
        assertThrows(IllegalStateException.class, () -> facade.completeMaintenanceRequest(admin, r));
        assertEquals(MaintenanceRequest.Status.PENDING, r.getStatus());
    }
    @Test void assignedTaskCannotBeReassigned() throws Exception {
        MaintenanceRequest r = fault();
        facade.assignMaintenanceRequest(admin, r, "Original");
        assertThrows(IllegalStateException.class, () -> facade.assignMaintenanceRequest(admin, r, "Other"));
        assertEquals("Original", r.getAssignedTo());
    }
    @Test void completedTaskCannotChangeAgain() throws Exception {
        MaintenanceRequest r = fault();
        facade.assignMaintenanceRequest(admin, r, "Staff");
        facade.completeMaintenanceRequest(admin, r);
        assertThrows(IllegalStateException.class, () -> facade.assignMaintenanceRequest(admin, r, "Other"));
        assertThrows(IllegalStateException.class, () -> facade.completeMaintenanceRequest(admin, r));
    }
    @Test void blankAssigneeKeepsPendingState() throws Exception {
        MaintenanceRequest r = fault();
        assertThrows(IllegalArgumentException.class, () -> facade.assignMaintenanceRequest(admin, r, " "));
        assertEquals(MaintenanceRequest.Status.PENDING, r.getStatus());
        assertNull(r.getAssignedTo());
    }
    @Test void membersAndInstructorsCannotCompleteTasks() throws Exception {
        MaintenanceRequest r = fault();
        facade.assignMaintenanceRequest(admin, r, "Staff");
        assertThrows(UnauthorizedAccessException.class, () -> facade.completeMaintenanceRequest(member, r));
        assertThrows(UnauthorizedAccessException.class, () -> facade.completeMaintenanceRequest(instructor, r));
        assertEquals(MaintenanceRequest.Status.ASSIGNED, r.getStatus());
    }
    @Test void foreignRequestIsRejected() {
        MaintenanceRequest r = new MaintenanceRequest(treadmill, instructor, "Outside", MaintenanceRequest.Urgency.LOW);
        assertThrows(IllegalArgumentException.class, () -> facade.assignMaintenanceRequest(admin, r, "Staff"));
        assertThrows(IllegalArgumentException.class, () -> facade.completeMaintenanceRequest(admin, null));
    }
    @Test void completionWaitsForAllOpenRequests() throws Exception {
        MaintenanceRequest a = fault(), b = fault();
        facade.assignMaintenanceRequest(admin, a, "A");
        facade.completeMaintenanceRequest(admin, a);
        assertEquals(EquipmentStatus.IN_MAINTENANCE, treadmill.getStatus());
        facade.assignMaintenanceRequest(admin, b, "B");
        facade.completeMaintenanceRequest(admin, b);
        assertEquals(EquipmentStatus.AVAILABLE, treadmill.getStatus());
    }
    @Test void completionDoesNotReactivateEquipment() throws Exception {
        MaintenanceRequest r = fault();
        facade.deactivateEquipment(admin, treadmill);
        facade.assignMaintenanceRequest(admin, r, "Staff");
        facade.completeMaintenanceRequest(admin, r);
        assertFalse(treadmill.isActive());
        assertFalse(treadmill.isUsable());
    }
    @Test void lifecycleEventsAreOrderedAndRejectedChangesSilent() throws Exception {
        List<String> events = new ArrayList<>();
        facade.addObserver((r,e) -> events.add(e));
        MaintenanceRequest r = fault();
        assertThrows(IllegalStateException.class, () -> facade.completeMaintenanceRequest(admin, r));
        facade.assignMaintenanceRequest(admin, r, "Staff");
        assertThrows(IllegalStateException.class, () -> facade.assignMaintenanceRequest(admin, r, "Other"));
        facade.completeMaintenanceRequest(admin, r);
        assertEquals(List.of("CREATED", "ASSIGNED", "COMPLETED"), events);
    }
    @Test void completeResourceBookingMaintenanceWorkflow() throws Exception {
        Session s = resourceSession();
        Booking b = facade.bookSession(member, s);
        facade.cancelBooking(member, b.getBookingId());
        assertNotNull(facade.bookSession(member, s));
        MaintenanceRequest r = fault();
        Member other = facade.registerMember("New", "new@test.com", "3", MembershipType.BASIC);
        assertThrows(InvalidBookingException.class, () -> facade.bookSession(other, s));
        facade.assignMaintenanceRequest(admin, r, "Staff");
        facade.completeMaintenanceRequest(admin, r);
        assertTrue(treadmill.isUsable());
        facade.addEquipmentUsage(admin, treadmill, 100);
        assertTrue(treadmill.getMaintenanceHistory().get(1).isPreventative());
    }
}

