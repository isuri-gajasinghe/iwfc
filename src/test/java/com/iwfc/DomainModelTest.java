package com.iwfc;

import com.iwfc.model.*;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@DisplayName("Domain Model – Tests")
class DomainModelTest {

    private Administrator admin;
    private Instructor    instructor;
    private Member        member1;
    private Member        member2;
    private Equipment     equipment;
    private Session       session;

    @BeforeEach
    void setUp() throws Exception {
        admin      = new Administrator("Alice Admin",  "alice@iwfc.com", "555-0001", "ADMIN-001");
        instructor = new Instructor("Bob Trainer",     "bob@iwfc.com",   "555-0002", "Yoga");
        member1    = new Member("Carol User",          "carol@iwfc.com", "555-0003", MembershipType.PREMIUM);
        member2    = new Member("Dave User",           "dave@iwfc.com",  "555-0004", MembershipType.BASIC);
        equipment  = new Equipment("EQ-001", "Treadmill", "Cardio", "Main floor");
        session    = new Session("S-001", "Morning Yoga",
                         instructor, java.time.LocalDate.now().plusDays(1).atTime(9, 0), 60, 2, "Studio A", List.of());
    }


    @Test
    @DisplayName("getRole() returns the correct label for each user subtype")
    void testPolymorphicGetRole() throws Exception {
        assertEquals("Administrator", admin.getRole());
        assertEquals("Instructor",    instructor.getRole());
        assertEquals("Member",        member1.getRole());
    }

    @Test
    @DisplayName("User[] array can hold any subtype (runtime polymorphism)")
    void testPolymorphismViaArray() throws Exception {
        User[] users        = { admin, instructor, member1 };
        String[] expected   = { "Administrator", "Instructor", "Member" };

        for (int i = 0; i < users.length; i++) {
            assertEquals(expected[i], users[i].getRole(),
                    "Role mismatch at index " + i);
        }
    }

    @Test
    @DisplayName("getSummary() returns a non-empty string for every subtype")
    void testPolymorphicGetSummary() throws Exception {
        for (User u : new User[]{ admin, instructor, member1 }) {
            assertNotNull(u.getSummary());
            assertFalse(u.getSummary().isBlank(), "Summary should not be blank");
        }
    }


    @Test
    @DisplayName("Subclasses inherit name and email from User")
    void testInheritedFields() throws Exception {
        assertEquals("Alice Admin", admin.getName());
        assertEquals("bob@iwfc.com", instructor.getEmail());
        assertEquals("555-0003",     member1.getPhone());
    }

    @Test
    @DisplayName("User.equals() is based on system-assigned id")
    void testUserEquality() throws Exception {
        assertNotEquals(admin, instructor);
        assertNotEquals(member1, member2);
        assertEquals(admin, admin);
    }


    @Test
    @DisplayName("Enrolment succeeds when the session has capacity")
    void testEnrolmentSuccess() throws Exception {
        assertTrue(session.enroll(member1));
        assertEquals(1, session.getCurrentEnrollmentCount());
        assertEquals(1, session.getAvailableSpots());
    }

    @Test
    @DisplayName("Session prevents duplicate enrolment")
    void testDuplicateEnrolment() throws Exception {
        session.enroll(member1);
        assertFalse(session.enroll(member1), "Duplicate enrolment must be rejected");
    }

    @Test
    @DisplayName("Session becomes unavailable when full")
    void testSessionFull() throws Exception {
        session.enroll(member1);
        session.enroll(member2);
        assertFalse(session.isAvailable(), "Session should be full");
    }

    @Test
    @DisplayName("Cancelled session is not available for enrolment")
    void testCancelledSessionUnavailable() throws Exception {
        session.cancel();
        assertFalse(session.isAvailable());
        assertEquals(SessionStatus.CANCELLED, session.getStatus());
    }

    @Test
    @DisplayName("Session status transitions: SCHEDULED → IN_PROGRESS → COMPLETED")
    void testSessionStatusTransitions() throws Exception {
        assertEquals(SessionStatus.SCHEDULED,   session.getStatus());
        session.start();
        assertEquals(SessionStatus.IN_PROGRESS, session.getStatus());
        session.complete();
        assertEquals(SessionStatus.COMPLETED,   session.getStatus());
    }


    @Test
    @DisplayName("New booking has CONFIRMED status")
    void testBookingInitialStatus() throws Exception {
        session.enroll(member1);
        Booking booking = new Booking("B-001", member1, session);
        member1.addBooking(booking);

        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals(1, member1.getBookings().size());
    }

    @Test
    @DisplayName("Cancelling a booking updates status and removes member from session")
    void testBookingCancellation() throws Exception {
        session.enroll(member1);
        Booking booking = new Booking("B-002", member1, session);
        member1.addBooking(booking);

        assertTrue(member1.cancelBooking("B-002"));
        assertEquals(BookingStatus.CANCELLED,  booking.getStatus());
        assertEquals(0, session.getCurrentEnrollmentCount());
    }

    @Test
    @DisplayName("Cancelling a non-existent booking returns false")
    void testCancelNonExistentBooking() throws Exception {
        assertFalse(member1.cancelBooking("INVALID-ID"));
    }

    @Test
    @DisplayName("markAttended() transitions booking from CONFIRMED to ATTENDED")
    void testMarkAttended() throws Exception {
        session.enroll(member1);
        Booking booking = new Booking("B-003", member1, session);
        booking.markAttended();
        assertEquals(BookingStatus.ATTENDED, booking.getStatus());
    }


    @Test
    @DisplayName("New equipment starts AVAILABLE")
    void testEquipmentInitialStatus() throws Exception {
        assertEquals(EquipmentStatus.AVAILABLE, equipment.getStatus());
    }

    @Test
    @DisplayName("Requesting maintenance sets status to IN_MAINTENANCE")
    void testMaintenanceRequestChangesStatus() throws Exception {
        equipment.requestMaintenance(instructor, "Belt worn out", MaintenanceRequest.Urgency.MEDIUM);
        assertEquals(EquipmentStatus.IN_MAINTENANCE, equipment.getStatus());
        assertEquals(1, equipment.getMaintenanceHistory().size());
    }

    @Test
    @DisplayName("Multiple maintenance requests are all recorded")
    void testMultipleMaintenanceRequests() throws Exception {
        equipment.requestMaintenance(instructor, "Issue 1", MaintenanceRequest.Urgency.MEDIUM);
        MaintenanceRequest first = equipment.getMaintenanceHistory().get(0);
        admin.assignMaintenance(first, "Staff");
        admin.completeMaintenance(first);
        equipment.requestMaintenance(instructor, "Issue 2", MaintenanceRequest.Urgency.MEDIUM);
        assertEquals(2, equipment.getMaintenanceHistory().size());
    }


    @Test
    @DisplayName("Administrator can assign a pending maintenance request")
    void testAdminAssignsMaintenance() throws Exception {
        MaintenanceRequest req = equipment.requestMaintenance(instructor, "Motor fault", MaintenanceRequest.Urgency.MEDIUM);
        assertEquals(MaintenanceRequest.Status.PENDING, req.getStatus());

        admin.assignMaintenance(req, "Staff");
        assertEquals(MaintenanceRequest.Status.ASSIGNED, req.getStatus());
        assertEquals("Alice Admin", req.getAssignedBy().getName());
        assertEquals(1, admin.getManagedRequests().size());
    }

    @Test
    @DisplayName("Administrator cannot reassign an already assigned request")
    void testAdminCannotReassign() throws Exception {
        MaintenanceRequest req = equipment.requestMaintenance(instructor, "Screen broken", MaintenanceRequest.Urgency.MEDIUM);
        admin.assignMaintenance(req, "Staff");
        assertThrows(IllegalStateException.class, () -> admin.assignMaintenance(req, "Staff"));
    }


    @Test
    @DisplayName("Member.getBookings() returns an unmodifiable list")
    void testBookingsListIsUnmodifiable() throws Exception {
        List<Booking> bookings = member1.getBookings();
        assertThrows(UnsupportedOperationException.class, () -> bookings.add(null));
    }

    @Test
    @DisplayName("Session.getEnrolledMembers() returns an unmodifiable list")
    void testEnrolledMembersListIsUnmodifiable() throws Exception {
        List<Member> enrolled = session.getEnrolledMembers();
        assertThrows(UnsupportedOperationException.class, () -> enrolled.add(null));
    }

    @Test
    @DisplayName("Equipment.getMaintenanceHistory() returns an unmodifiable list")
    void testMaintenanceHistoryIsUnmodifiable() throws Exception {
        List<MaintenanceRequest> history = equipment.getMaintenanceHistory();
        assertThrows(UnsupportedOperationException.class, () -> history.add(null));
    }


    @Test
    @DisplayName("MembershipType enum has correct monthly fees")
    void testMembershipTypeFees() throws Exception {
        assertEquals(30.0,  MembershipType.BASIC.getMonthlyFee(),   0.001);
        assertEquals(60.0,  MembershipType.PREMIUM.getMonthlyFee(), 0.001);
        assertEquals(100.0, MembershipType.VIP.getMonthlyFee(),     0.001);
    }

    @Test
    @DisplayName("Member membership type can be upgraded")
    void testMembershipUpgrade() throws Exception {
        assertEquals(MembershipType.PREMIUM, member1.getMembershipType());
        member1.setMembershipType(MembershipType.VIP);
        assertEquals(MembershipType.VIP, member1.getMembershipType());
    }


    @Test
    @DisplayName("Instructor session list tracks added sessions")
    void testInstructorSessions() throws Exception {
        instructor.addSession(session);
        assertEquals(1, instructor.getSessions().size());
        assertEquals("Morning Yoga", instructor.getSessions().get(0).getTitle());
    }

    @Test
    @DisplayName("Instructor.getSessions() returns an unmodifiable list")
    void testInstructorSessionsUnmodifiable() throws Exception {
        List<Session> sessions = instructor.getSessions();
        assertThrows(UnsupportedOperationException.class, () -> sessions.add(null));
    }

    @Test void sessionEquipmentIsDefensivelyCopied() {
        java.util.ArrayList<Equipment> source = new java.util.ArrayList<>(List.of(equipment));
        Session allocated = new Session("ALLOC", "Allocated", instructor,
                java.time.LocalDate.now().plusDays(1).atTime(12, 0), 60, 5, "Studio B", source);
        source.clear();
        assertEquals(List.of(equipment), allocated.getEquipment());
        assertThrows(UnsupportedOperationException.class, () -> allocated.getEquipment().clear());
    }

    @Test void administratorManagedHistoryIsUnmodifiable() {
        MaintenanceRequest request = equipment.requestMaintenance(instructor, "Fault", MaintenanceRequest.Urgency.LOW);
        admin.assignMaintenance(request, "Staff");
        assertThrows(UnsupportedOperationException.class, () -> admin.getManagedRequests().clear());
    }

    @Test void urgencySupportsAllRequiredValues() {
        assertArrayEquals(new MaintenanceRequest.Urgency[]{MaintenanceRequest.Urgency.LOW,
                MaintenanceRequest.Urgency.MEDIUM, MaintenanceRequest.Urgency.HIGH}, MaintenanceRequest.Urgency.values());
    }

    @Test void directLifecycleStillRejectsInvalidTransitions() {
        MaintenanceRequest request = equipment.requestMaintenance(instructor, "Fault", MaintenanceRequest.Urgency.LOW);
        assertThrows(IllegalStateException.class, () -> request.complete(admin));
        request.assign(admin, "Staff");
        assertThrows(IllegalStateException.class, () -> request.assign(admin, "Other"));
        request.complete(admin);
        assertThrows(IllegalStateException.class, () -> request.assign(admin, "Other"));
    }

    @Test void openMaintenanceCannotBeBypassedByMarkAvailable() {
        equipment.requestMaintenance(instructor, "Fault", MaintenanceRequest.Urgency.LOW);
        assertThrows(IllegalStateException.class, equipment::markAvailable);
        assertEquals(EquipmentStatus.IN_MAINTENANCE, equipment.getStatus());
    }

    @Test void equipmentConstructorRejectsMissingLocationAndId() {
        assertThrows(IllegalArgumentException.class, () -> new Equipment("E", "Machine", "Cardio", null));
        assertThrows(IllegalArgumentException.class, () -> new Equipment(" ", "Machine", "Cardio", "Lobby"));
    }

    @Test void modelSessionRequiresPositiveCapacityAndDuration() {
        assertThrows(IllegalArgumentException.class, () -> new Session("E", "Bad", instructor,
                java.time.LocalDate.now().plusDays(1).atTime(12, 0), 0, 5, "B", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Session("E", "Bad", instructor,
                java.time.LocalDate.now().plusDays(1).atTime(12, 0), 60, 0, "B", List.of()));
    }

    @Test void blankProfilesAndNullMembershipAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> member1.setName(" "));
        assertThrows(IllegalArgumentException.class, () -> instructor.setSpecialization(null));
        assertThrows(NullPointerException.class, () -> member1.setMembershipType(null));
    }

    @Test void cancelledSessionCannotBeRestartedAfterResourceRelease() {
        session.cancel();
        assertThrows(IllegalStateException.class, session::start);
        assertThrows(IllegalStateException.class, session::complete);
        assertEquals(SessionStatus.CANCELLED, session.getStatus());
    }
}

