package com.iwfc;

import com.iwfc.model.*;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@DisplayName("Design Patterns – Tests")
class DesignPatternsTest {

    /** Fresh facade for every test – prevents shared state interference. */
    private IWFCFacade facade;

    @BeforeEach
    void setUp() throws Exception {
        facade = new IWFCFacade();
    }

    // Section A: Factory Pattern

    @Test
    @DisplayName("Factory creates an Administrator from type string")
    void factoryCreatesAdmin() throws Exception {
        User user = UserFactory.createUser("ADMIN", "Alice", "a@iwfc.com", "555-01", "ADMIN-001");

        assertInstanceOf(Administrator.class, user);
        assertEquals("Administrator", user.getRole());
        assertEquals("Alice", user.getName());
    }

    @Test
    @DisplayName("Factory creates an Instructor from type string")
    void factoryCreatesInstructor() throws Exception {
        User user = UserFactory.createUser("INSTRUCTOR", "Bob", "b@iwfc.com", "555-02", "Yoga");

        assertInstanceOf(Instructor.class, user);
        assertEquals("Instructor", user.getRole());
        assertEquals("Yoga", ((Instructor) user).getSpecialization());
    }

    @Test
    @DisplayName("Factory creates a Member from type string with correct membership tier")
    void factoryCreatesMember() throws Exception {
        User user = UserFactory.createUser("MEMBER", "Carol", "c@iwfc.com", "555-03", "PREMIUM");

        assertInstanceOf(Member.class, user);
        assertEquals("Member", user.getRole());
        assertEquals(MembershipType.PREMIUM, ((Member) user).getMembershipType());
    }

    @Test
    @DisplayName("Factory is case-insensitive for type strings")
    void factoryIsCaseInsensitive() throws Exception {
        User admin = UserFactory.createUser("admin",      "A", "a@t.com", "1", "CODE");
        User instr = UserFactory.createUser("Instructor", "B", "b@t.com", "2", "HIIT");
        User memb  = UserFactory.createUser("member",     "C", "c@t.com", "3", "VIP");

        assertEquals("Administrator", admin.getRole());
        assertEquals("Instructor",    instr.getRole());
        assertEquals("Member",        memb.getRole());
    }

    @Test
    @DisplayName("Factory throws IllegalArgumentException for an unknown type")
    void factoryThrowsForUnknownType() throws Exception {
        assertThrows(IllegalArgumentException.class, () ->
                UserFactory.createUser("ALIEN", "X", "x@t.com", "0", "extra"));
    }

    @Test
    @DisplayName("Factory-created users demonstrate runtime polymorphism")
    void factoryPolymorphism() throws Exception {
        User[] users   = {
                UserFactory.createUser("ADMIN",      "A", "a@t.com", "1", "CODE"),
                UserFactory.createUser("INSTRUCTOR", "B", "b@t.com", "2", "HIIT"),
                UserFactory.createUser("MEMBER",     "C", "c@t.com", "3", "BASIC")
        };
        String[] roles = { "Administrator", "Instructor", "Member" };

        for (int i = 0; i < users.length; i++) {
            assertEquals(roles[i], users[i].getRole(),
                    "Polymorphic getRole() failed at index " + i);
        }
    }

    // Section B: Facade Pattern

    @Test
    @DisplayName("Facade registers all three user types in the in-memory store")
    void facadeRegistersUsers() throws Exception {
        facade.registerAdmin("Alice",  "a@iwfc.com", "1", "ADMIN-001");
        facade.registerInstructor("Bob", "b@iwfc.com", "2", "Yoga");
        facade.registerMember("Carol", "c@iwfc.com", "3", MembershipType.BASIC);

        assertEquals(3, facade.getUsers().size());
    }

    @Test
    @DisplayName("Facade adds equipment to the in-memory store")
    void facadeAddsEquipment() throws Exception {
        Equipment eq = addEquipment("EQ-001", "Treadmill", "Cardio");

        assertNotNull(eq);
        assertEquals(1, facade.getAllEquipment().size());
        assertEquals(EquipmentStatus.AVAILABLE, eq.getStatus());
    }

    @Test
    @DisplayName("Facade creates a session and automatically wires it to the instructor")
    void facadeCreatesSession() throws Exception {
        Instructor instructor = facade.registerInstructor("Bob", "b@iwfc.com", "2", "Yoga");
        Session session = facade.createSession("S-001", "Morning Yoga", instructor,
                java.time.LocalDate.now().plusDays(1).atTime(9, 0), 60, 10, "Studio A", List.of());

        assertEquals(1, facade.getSessions().size());
        assertEquals(1, instructor.getSessions().size());
        assertSame(session, instructor.getSessions().get(0));
    }

    @Test
    @DisplayName("Facade books a session for a member in a single call")
    void facadeBookSession() throws Exception {
        Instructor instructor = facade.registerInstructor("Bob", "b@iwfc.com", "2", "Yoga");
        Member     member     = facade.registerMember("Carol", "c@iwfc.com", "3", MembershipType.PREMIUM);
        Session    session    = facade.createSession("S-001", "Evening Yoga", instructor,
                java.time.LocalDate.now().plusDays(1).atTime(9, 0), 60, 10, "Studio A", List.of());

        Booking booking = facade.bookSession(member, session);

        assertNotNull(booking);
        assertEquals(BookingStatus.CONFIRMED,  booking.getStatus());
        assertEquals(1, facade.getBookings().size());
        assertEquals(1, member.getBookings().size());
        assertEquals(1, session.getCurrentEnrollmentCount());
    }

    @Test
    @DisplayName("Facade throws InvalidBookingException when session is full")
    void facadeBookingFailsWhenFull() throws Exception {
        Instructor instructor = facade.registerInstructor("Bob", "b@iwfc.com", "2", "HIIT");
        Member m1 = facade.registerMember("M1", "m1@iwfc.com", "1", MembershipType.BASIC);
        Member m2 = facade.registerMember("M2", "m2@iwfc.com", "2", MembershipType.BASIC);
        Session session = facade.createSession("S-002", "Packed Session", instructor,
                java.time.LocalDate.now().plusDays(1).atTime(9, 0), 30, 1, "Studio A", List.of());   // capacity = 1

        facade.bookSession(m1, session);   // succeeds

        // Second booking must throw, not return null
        assertThrows(InvalidBookingException.class, () -> facade.bookSession(m2, session));
    }

    @Test
    @DisplayName("Facade getUsersByRole filters by role using streams")
    void facadeGetUsersByRole() throws Exception {
        facade.registerAdmin("Alice", "a@iwfc.com", "1", "ADMIN-001");
        facade.registerInstructor("Bob", "b@iwfc.com", "2", "Pilates");
        facade.registerMember("Carol", "c@iwfc.com", "3", MembershipType.VIP);
        facade.registerMember("Dave",  "d@iwfc.com", "4", MembershipType.BASIC);

        assertEquals(1, facade.getUsersByRole("Administrator").size());
        assertEquals(1, facade.getUsersByRole("Instructor").size());
        assertEquals(2, facade.getUsersByRole("Member").size());
    }

    @Test
    @DisplayName("Facade query lists are unmodifiable")
    void facadeListsAreUnmodifiable() throws Exception {
        assertThrows(UnsupportedOperationException.class,
                () -> facade.getUsers().add(null));
        assertThrows(UnsupportedOperationException.class,
                () -> facade.getSessions().add(null));
    }

    // Section C: Observer Pattern

    @Test
    @DisplayName("Observer is notified with 'CREATED' when a maintenance request is submitted")
    void observerNotifiedOnCreation() throws Exception {
        Equipment    eq     = addEquipment("EQ-001", "Treadmill", "Cardio");
        List<String> events = new ArrayList<>();

        facade.addObserver((request, eventType) -> events.add(eventType));

        reportFault(eq, "Belt worn out");

        assertEquals(1, events.size());
        assertEquals("CREATED", events.get(0));
    }

    @Test
    @DisplayName("Observer is notified with 'ASSIGNED' when a request is assigned")
    void observerNotifiedOnAssignment() throws Exception {
        Administrator admin  = facade.registerAdmin("Alice", "a@iwfc.com", "1", "ADMIN-001");
        Equipment     eq     = addEquipment("EQ-002", "Bike", "Cardio");
        List<String>  events = new ArrayList<>();

        facade.addObserver((req, e) -> events.add(e));

        MaintenanceRequest request = reportFault(eq, "Broken pedal");
        facade.assignMaintenanceRequest(admin, request, "Staff");   // admin is a User at call-site

        assertEquals(2, events.size());
        assertEquals("CREATED",  events.get(0));
        assertEquals("ASSIGNED", events.get(1));
    }

    @Test
    @DisplayName("Multiple observers all receive the same notification")
    void multipleObserversNotified() throws Exception {
        Equipment    eq   = addEquipment("EQ-003", "Rowing Machine", "Cardio");
        List<String> log1 = new ArrayList<>();
        List<String> log2 = new ArrayList<>();

        facade.addObserver((req, e) -> log1.add(e));
        facade.addObserver((req, e) -> log2.add(e));

        reportFault(eq, "Seat broken");

        assertEquals(1, log1.size());
        assertEquals(1, log2.size());
        assertEquals("CREATED", log1.get(0));
        assertEquals("CREATED", log2.get(0));
    }

    @Test
    @DisplayName("Removed observer no longer receives notifications")
    void removedObserverNotNotified() throws Exception {
        Equipment    eq     = addEquipment("EQ-004", "Dumbbell Rack", "Weights");
        List<String> events = new ArrayList<>();

        MaintenanceObserver observer = (req, e) -> events.add(e);
        facade.addObserver(observer);
        facade.removeObserver(observer);

        reportFault(eq, "Damaged frame");

        assertTrue(events.isEmpty(), "Removed observer must not be notified");
    }

    @Test
    @DisplayName("Observer receives the correct MaintenanceRequest object")
    void observerReceivesCorrectRequest() throws Exception {
        Equipment          eq       = addEquipment("EQ-005", "Leg Press", "Strength");
        MaintenanceRequest[] captured = new MaintenanceRequest[1];

        facade.addObserver((request, eventType) -> captured[0] = request);

        reportFault(eq, "Hydraulic leak");

        assertNotNull(captured[0]);
        assertEquals("Hydraulic leak", captured[0].getDescription());
        assertSame(eq, captured[0].getEquipment());
        assertFalse(captured[0].getStatus() == MaintenanceRequest.Status.ASSIGNED);
    }

    @Test
    @DisplayName("No observers registered: maintenance submission still works without error")
    void noObserversNoException() throws Exception {
        Equipment eq = addEquipment("EQ-006", "Cable Machine", "Strength");
        assertDoesNotThrow(() -> reportFault(eq, "Cable frayed"));
    }

    private Equipment addEquipment(String id, String name, String category) throws Exception {
        User actor = facade.findUserByEmail("inventory@iwfc.com").orElse(null);
        if (actor == null) actor = facade.registerAdmin("Inventory Admin", "inventory@iwfc.com", "1", "INV");
        return facade.addEquipment(actor, id, name, category, "Main floor");
    }

    @Test void observerReceivesCompletedStateAndPayload() throws Exception {
        Equipment item = addEquipment("E", "Machine", "Cardio");
        MaintenanceRequest request = reportFault(item, "Fault");
        User admin = facade.findUserByEmail("inventory@iwfc.com").orElseThrow();
        List<MaintenanceRequest.Status> states = new ArrayList<>();
        facade.addObserver((r,e) -> states.add(r.getStatus()));
        facade.assignMaintenanceRequest(admin, request, "Staff");
        facade.completeMaintenanceRequest(admin, request);
        assertEquals(List.of(MaintenanceRequest.Status.ASSIGNED, MaintenanceRequest.Status.COMPLETED), states);
    }

    @Test void observerCanUnsubscribeDuringDelivery() throws Exception {
        Equipment item = addEquipment("E", "Machine", "Cardio");
        List<String> events = new ArrayList<>();
        MaintenanceObserver[] listener = new MaintenanceObserver[1];
        listener[0] = (r,e) -> { events.add("first"); facade.removeObserver(listener[0]); };
        facade.addObserver(listener[0]);
        facade.addObserver((r,e) -> events.add("second"));
        reportFault(item, "Fault");
        reportFault(item, "Another fault");
        assertEquals(List.of("first", "second", "second"), events);
    }

    @Test void duplicateObserverRegistrationDoesNotRepeatEvents() throws Exception {
        Equipment item = addEquipment("E", "Machine", "Cardio");
        List<String> events = new ArrayList<>();
        MaintenanceObserver listener = (r,e) -> events.add(e);
        facade.addObserver(listener);
        facade.addObserver(listener);
        reportFault(item, "Fault");
        assertEquals(List.of("CREATED"), events);
    }

    @Test void allFacadeStoreViewsAreProtected() throws Exception {
        Equipment item = addEquipment("E", "Machine", "Cardio");
        reportFault(item, "Fault");
        assertThrows(UnsupportedOperationException.class, () -> facade.getAllEquipment().clear());
        assertThrows(UnsupportedOperationException.class, () -> facade.getBookings().add(null));
        assertThrows(UnsupportedOperationException.class, () -> facade.getMaintenanceRequests().clear());
        assertThrows(UnsupportedOperationException.class, () -> facade.getUsersByRole("Instructor").clear());
    }

    private MaintenanceRequest reportFault(Equipment item, String description) throws Exception {
        Instructor reporter = facade.getUsers().stream().filter(Instructor.class::isInstance)
                .map(Instructor.class::cast).findFirst().orElse(null);
        if (reporter == null) reporter = facade.registerInstructor("Reporter", "reporter@iwfc.com", "2", "Fitness");
        return facade.submitMaintenanceRequest(reporter, item, description, MaintenanceRequest.Urgency.MEDIUM);
    }
}
