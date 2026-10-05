package com.iwfc;

import com.iwfc.model.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/** Five-tab JavaFX client. All inventory, booking and maintenance writes use the facade. */
public class MainController {
    @FXML private TabPane mainTabPane;
    @FXML private Label statusLabel;
    private final IWFCFacade facade = new IWFCFacade();
    private final ObservableList<String> notificationLog = FXCollections.observableArrayList();
    private final ObservableList<User> userItems = FXCollections.observableArrayList();
    private final ObservableList<Equipment> equipItems = FXCollections.observableArrayList();
    private final ObservableList<Session> sessionItems = FXCollections.observableArrayList();
    private final ObservableList<MaintenanceRequest> maintItems = FXCollections.observableArrayList();
    private final List<ComboBox<User>> adminSelectors = new ArrayList<>();
    private Label dashboardCounts;
    private ListView<Equipment> equipListView;
    private ListView<Equipment> allocationList;
    private ComboBox<Instructor> sessionInstructor;
    private ComboBox<Instructor> reportingInstructor;
    private ComboBox<Equipment> maintenanceEquipment;
    private ComboBox<Member> bookingMember;
    private ComboBox<Session> bookingSession;
    private ComboBox<Booking> cancellationBooking;
    private ComboBox<MaintenanceRequest> assignmentRequest;
    private ComboBox<MaintenanceRequest> completionRequest;

    @FXML
    public void initialize() {
        registerObserver();
        seedSampleData();
        mainTabPane.getTabs().addAll(buildDashboardTab(), buildUsersTab(), buildEquipmentTab(),
                buildSessionsTab(), buildMaintenanceTab());
        refreshAll();
        statusLabel.setText("IWFC ready. Sample data loaded.");
    }

    private void registerObserver() {
        facade.addObserver((request, event) -> {
            notificationLog.add(0, String.format("[%s] %s | %s | %s",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    event, request.getEquipment().getName(), request.getDescription()));
            refreshAll();
        });
    }

    private void seedSampleData() {
        try {
            Administrator alice = facade.registerAdmin("Alice Admin", "alice@iwfc.com", "555-0001", "ADMIN-001");
            Instructor bob = facade.registerInstructor("Bob Instructor", "bob@iwfc.com", "555-0002", "Yoga & Pilates");
            facade.registerMember("Carol Member", "carol@iwfc.com", "555-0003", MembershipType.PREMIUM);
            facade.registerMember("Dave Member", "dave@iwfc.com", "555-0004", MembershipType.BASIC);
            Equipment treadmill = facade.addEquipment(alice, "EQ-001", "Treadmill", "Cardio", "Cardio floor");
            Equipment rower = facade.addEquipment(alice, "EQ-002", "Rowing Machine", "Cardio", "Studio B");
            Equipment weights = facade.addEquipment(alice, "EQ-003", "Dumbbell Rack", "Strength", "Studio A");
            facade.addEquipmentUsage(alice, treadmill, 25);
            facade.createSession("S-001", "Morning Yoga", bob, LocalDate.now().plusDays(1).atTime(9, 0),
                    60, 5, "Studio A", List.of(weights));
            facade.createSession("S-002", "HIIT", bob, LocalDate.now().plusDays(2).atTime(7, 30),
                    45, 8, "Studio B", List.of(rower));
            facade.submitMaintenanceRequest(bob, treadmill, "Belt worn; needs replacement", MaintenanceRequest.Urgency.HIGH);
        } catch (Exception e) {
            throw new IllegalStateException("Sample data could not be initialized.", e);
        }
    }

    private Tab buildDashboardTab() {
        dashboardCounts = new Label();
        Button refresh = button("Refresh Dashboard", "refreshDashboard", () -> "Dashboard refreshed.");
        return tab("Dashboard", panel("System Overview", dashboardCounts,
                new Label("Operating hours: 06:00–22:00"),
                new Label("Preventative service: every 100 recorded usage hours"),
                refresh));
    }

    private Tab buildUsersTab() {
        ComboBox<String> role = combo(List.of("Administrator", "Instructor", "Member"), "userRole");
        TextField name = field("Full name", "userName");
        TextField email = field("Email", "userEmail");
        TextField phone = field("Phone", "userPhone");
        TextField extra = field("Admin code / specialization / BASIC, PREMIUM, VIP", "userExtra");
        Button register = button("Register User", "registerUser", () -> {
            if (role.getValue() == null) throw new IllegalArgumentException("Select a role.");
            switch (role.getValue()) {
                case "Administrator" -> facade.registerAdmin(name.getText(), email.getText(), phone.getText(), extra.getText());
                case "Instructor" -> facade.registerInstructor(name.getText(), email.getText(), phone.getText(), extra.getText());
                default -> facade.registerMember(name.getText(), email.getText(), phone.getText(),
                        MembershipType.valueOf(extra.getText().trim().toUpperCase(java.util.Locale.ROOT)));
            }
            return "User registered.";
        });
        return tab("Users", columns(panel("Register User", row("Role", role), row("Name", name),
                row("Email", email), row("Phone", phone), row("Role details", extra), register),
                panel("Registered Users", new ListView<>(userItems))));
    }

    private Tab buildEquipmentTab() {
        ComboBox<User> actor = adminSelector("equipmentAdmin");
        TextField id = field("EQ-004", "equipmentId");
        TextField name = field("Equipment name", "equipmentName");
        TextField category = field("Category", "equipmentCategory");
        TextField location = field("Location", "equipmentLocation");
        TextField hours = field("Non-negative hours to add", "equipmentUsage");
        equipListView = new ListView<>(equipItems);
        equipListView.setId("equipmentList");
        equipListView.setPrefHeight(340);
        equipListView.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
            if (item != null) {
                id.setText(item.getEquipmentId());
                name.setText(item.getName());
                category.setText(item.getCategory());
                location.setText(item.getLocation());
            }
        });
        Button add = button("Add Equipment", "addEquipment", () -> {
            facade.addEquipment(actor.getValue(), id.getText(), name.getText(), category.getText(), location.getText());
            return "Equipment added.";
        });
        Button edit = button("Save Selected Equipment", "editEquipment", () -> {
            facade.editEquipment(actor.getValue(), equipListView.getSelectionModel().getSelectedItem(),
                    name.getText(), category.getText(), location.getText());
            return "Equipment updated. ID remains unchanged.";
        });
        Button deactivate = button("Deactivate Selected", "deactivateEquipment", () -> {
            facade.deactivateEquipment(actor.getValue(), equipListView.getSelectionModel().getSelectedItem());
            return "Equipment deactivated.";
        });
        Button usage = button("Add Usage Hours", "addEquipmentUsage", () -> {
            facade.addEquipmentUsage(actor.getValue(), equipListView.getSelectionModel().getSelectedItem(),
                    Double.parseDouble(hours.getText().trim()));
            return "Usage recorded. Service alerts appear in Maintenance.";
        });
        return tab("Equipment", columns(panel("Manage Equipment", row("Administrator", actor),
                row("ID (add only)", id), row("Name", name), row("Category", category), row("Location", location),
                add, edit, deactivate, row("Usage increment", hours), usage),
                panel("Inventory: location, hours, status and active state", equipListView)));
    }

    private Tab buildSessionsTab() {
        TextField id = field("S-003", "sessionId");
        TextField title = field("Class title", "sessionTitle");
        TextField studio = field("Studio C", "sessionStudio");
        DatePicker date = new DatePicker(LocalDate.now().plusDays(1));
        date.setId("sessionDate");
        TextField time = field("09:00 (HH:mm)", "sessionTime");
        time.setText("09:00");
        TextField duration = field("Minutes", "sessionDuration"); duration.setText("60");
        TextField capacity = field("Capacity", "sessionCapacity"); capacity.setText("10");
        sessionInstructor = combo(List.of(), "sessionInstructor");
        allocationList = new ListView<>();
        allocationList.setId("sessionEquipment");
        allocationList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        allocationList.setPrefHeight(100);
        Button create = button("Create Session", "createSession", () -> {
            if (date.getValue() == null) throw new IllegalArgumentException("Select a date.");
            LocalDateTime start = date.getValue().atTime(LocalTime.parse(time.getText().trim()));
            facade.createSession(id.getText(), title.getText(), sessionInstructor.getValue(), start,
                    Integer.parseInt(duration.getText().trim()), Integer.parseInt(capacity.getText().trim()),
                    studio.getText(), List.copyOf(allocationList.getSelectionModel().getSelectedItems()));
            return "Session created.";
        });
        TableView<Session> table = new TableView<>(sessionItems);
        table.setId("sessionTable");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getColumns().add(column("ID", "sessionId"));
        table.getColumns().add(column("Title", "title"));
        table.getColumns().add(column("Start", "startTime"));
        table.getColumns().add(column("Studio", "studio"));
        table.getColumns().add(column("Instructor", "instructor"));
        table.getColumns().add(column("Equipment", "equipmentNames"));
        table.getColumns().add(column("Status", "status"));
        table.getColumns().add(column("Capacity", "maxCapacity"));
        table.getColumns().add(column("Enrolled", "currentEnrollmentCount"));
        table.setPrefHeight(250);
        bookingMember = combo(List.of(), "bookingMember");
        bookingSession = combo(List.of(), "bookingSession");
        cancellationBooking = combo(List.of(), "cancellationBooking");
        bookingMember.valueProperty().addListener((obs, old, member) -> refreshCancellations());
        Button book = button("Book Session", "bookSession", () -> {
            Booking booking = facade.bookSession(bookingMember.getValue(), bookingSession.getValue());
            return "Booking confirmed: " + booking.getBookingId();
        });
        Button cancel = button("Cancel Booking", "cancelBooking", () -> {
            Booking booking = cancellationBooking.getValue();
            if (booking == null) throw new IllegalArgumentException("Select a confirmed booking.");
            facade.cancelBooking(bookingMember.getValue(), booking.getBookingId());
            return "Booking cancelled; capacity released.";
        });
        return tab("Sessions", columns(panel("Create Session", row("ID", id), row("Title", title),
                row("Instructor", sessionInstructor), row("Studio", studio), row("Date", date),
                row("Start time", time), row("Duration", duration), row("Capacity", capacity),
                new Label("Equipment (Ctrl-click for multiple; optional)"), allocationList, create),
                panel("Schedule and Bookings", table, row("Member", bookingMember),
                        row("Session", bookingSession), book, row("Confirmed booking", cancellationBooking), cancel)));
    }

    private Tab buildMaintenanceTab() {
        reportingInstructor = combo(List.of(), "maintenanceReporter");
        maintenanceEquipment = combo(List.of(), "maintenanceEquipment");
        ComboBox<MaintenanceRequest.Urgency> urgency = combo(List.of(MaintenanceRequest.Urgency.values()), "maintenanceUrgency");
        urgency.setValue(MaintenanceRequest.Urgency.MEDIUM);
        TextField description = field("Describe the issue", "maintenanceDescription");
        Button submit = button("Report Fault", "submitMaintenance", () -> {
            facade.submitMaintenanceRequest(reportingInstructor.getValue(), maintenanceEquipment.getValue(),
                    description.getText(), urgency.getValue());
            return "Fault reported.";
        });
        ComboBox<User> admin = adminSelector("maintenanceAdmin");
        assignmentRequest = combo(List.of(), "assignmentRequest");
        completionRequest = combo(List.of(), "completionRequest");
        TextField assignee = field("Staff name", "maintenanceAssignee");
        Button assign = button("Assign Task", "assignMaintenance", () -> {
            facade.assignMaintenanceRequest(admin.getValue(), assignmentRequest.getValue(), assignee.getText());
            return "Task assigned.";
        });
        Button complete = button("Complete Task", "completeMaintenance", () -> {
            facade.completeMaintenanceRequest(admin.getValue(), completionRequest.getValue());
            return "Task completed.";
        });
        ListView<MaintenanceRequest> requests = new ListView<>(maintItems);
        requests.setId("maintenanceList"); requests.setPrefHeight(270);
        ListView<String> log = new ListView<>(notificationLog);
        log.setId("notificationLog"); log.setPrefHeight(200);
        return tab("Maintenance", columns(panel("Fault Reporting", row("Instructor", reportingInstructor),
                row("Equipment", maintenanceEquipment), row("Urgency", urgency), row("Description", description),
                submit, sectionLabel("Task Management"), row("Administrator", admin),
                row("Pending task", assignmentRequest), row("Assignee", assignee), assign,
                row("Assigned task", completionRequest), complete),
                panel("Requests: urgency, status, reporter and assignee", requests,
                        sectionLabel("Maintenance Notifications"), log)));
    }

    private void refreshAll() {
        Equipment selectedEquipment = equipListView == null ? null : equipListView.getSelectionModel().getSelectedItem();
        userItems.setAll(facade.getUsers());
        equipItems.setAll(facade.getAllEquipment());
        if (equipListView != null && selectedEquipment != null) {
            equipListView.getSelectionModel().select(selectedEquipment);
        }
        sessionItems.setAll(facade.getSessions());
        maintItems.setAll(facade.getMaintenanceRequests());
        List<User> admins = facade.getUsersByRole("Administrator");
        adminSelectors.forEach(c -> replaceItems(c, admins));
        List<Instructor> instructors = facade.getUsers().stream().filter(Instructor.class::isInstance)
                .map(Instructor.class::cast).toList();
        replaceItems(sessionInstructor, instructors);
        replaceItems(reportingInstructor, instructors);
        replaceItems(maintenanceEquipment, facade.getAllEquipment().stream().filter(Equipment::isActive).toList());
        replaceItems(bookingMember, facade.getUsers().stream().filter(Member.class::isInstance).map(Member.class::cast).toList());
        replaceItems(bookingSession, facade.getSessions().stream().filter(Session::isAvailable).toList());
        replaceItems(assignmentRequest, maintItems.stream().filter(r -> r.getStatus() == MaintenanceRequest.Status.PENDING).toList());
        replaceItems(completionRequest, maintItems.stream().filter(r -> r.getStatus() == MaintenanceRequest.Status.ASSIGNED).toList());
        if (allocationList != null) {
            List<Equipment> selected = List.copyOf(allocationList.getSelectionModel().getSelectedItems());
            allocationList.getItems().setAll(facade.getAllEquipment().stream().filter(Equipment::isUsable).toList());
            selected.forEach(e -> allocationList.getSelectionModel().select(e));
        }
        refreshCancellations();
        if (equipListView != null) equipListView.refresh();
        if (dashboardCounts != null) dashboardCounts.setText(String.format(
                "Users: %d    Equipment: %d    Sessions: %d    Maintenance requests: %d",
                userItems.size(), equipItems.size(), sessionItems.size(), maintItems.size()));
    }

    private void refreshCancellations() {
        if (bookingMember == null) return;
        Member member = bookingMember.getValue();
        replaceItems(cancellationBooking, member == null ? List.of() : member.getBookings().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED).toList());
    }

    private <T> void replaceItems(ComboBox<T> combo, List<T> items) {
        if (combo == null) return;
        T selected = combo.getValue();
        combo.getItems().setAll(items);
        combo.setValue(selected != null && items.contains(selected) ? selected : null);
    }

    private ComboBox<User> adminSelector(String id) {
        ComboBox<User> combo = combo(List.of(), id);
        adminSelectors.add(combo);
        return combo;
    }

    private <T> ComboBox<T> combo(List<T> values, String id) {
        ComboBox<T> combo = new ComboBox<>(FXCollections.observableArrayList(values));
        combo.setId(id); combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
    }

    private TextField field(String prompt, String id) {
        TextField field = new TextField();
        field.setPromptText(prompt); field.setId(id);
        return field;
    }

    private Button button(String text, String id, Callable<String> action) {
        Button button = new Button(text);
        button.setId(id); button.getStyleClass().add("btn-primary");
        button.setOnAction(event -> {
            try {
                String message = action.call();
                refreshAll();
                statusLabel.setText(message);
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Operation failed"); alert.setHeaderText(null);
                alert.setContentText(e.getMessage() == null ? "Check the selected values." : e.getMessage());
                alert.showAndWait();
            }
        });
        return button;
    }

    private <T> TableColumn<Session, T> column(String title, String property) {
        TableColumn<Session, T> column = new TableColumn<>(title);
        column.setCellValueFactory(new PropertyValueFactory<>(property));
        return column;
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text); label.getStyleClass().add("section-title");
        return label;
    }

    private VBox panel(String title, javafx.scene.Node... controls) {
        VBox panel = new VBox(9);
        panel.getStyleClass().add("form-panel");
        panel.setPadding(new Insets(16)); panel.setPrefWidth(420);
        panel.getChildren().add(sectionLabel(title));
        panel.getChildren().addAll(controls);
        return panel;
    }

    private HBox row(String text, javafx.scene.Node control) {
        Label label = new Label(text); label.setMinWidth(125);
        HBox row = new HBox(8, label, control); HBox.setHgrow(control, Priority.ALWAYS);
        return row;
    }

    private HBox columns(VBox left, VBox right) {
        HBox box = new HBox(16, left, right);
        box.setPadding(new Insets(16)); HBox.setHgrow(right, Priority.ALWAYS);
        return box;
    }

    private Tab tab(String title, javafx.scene.Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        Tab tab = new Tab(title, scroll); tab.setClosable(false);
        return tab;
    }
}
