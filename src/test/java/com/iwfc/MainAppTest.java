package com.iwfc;

import org.junit.jupiter.api.Test;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import static org.junit.jupiter.api.Assertions.*;

/** Resource and entry-point checks; JavaFX window startup is verified separately. */
class MainAppTest {
    @Test void javafxLoadsSeededTabsAndRunsEquipmentBookingAndMaintenanceActions() throws Exception {
        java.util.concurrent.CompletableFuture<Void> finished = new java.util.concurrent.CompletableFuture<>();
        javafx.application.Platform.startup(() -> {
            try {
                javafx.application.Platform.setImplicitExit(false);
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/iwfc/MainView.fxml"));
                javafx.scene.Parent root = loader.load();
                javafx.scene.Scene scene = new javafx.scene.Scene(root, 1100, 850);
                root.applyCss();
                root.layout();
                javafx.scene.control.TabPane tabs = (javafx.scene.control.TabPane) root.lookup(".tab-pane");
                assertEquals(java.util.List.of("Dashboard", "Users", "Equipment", "Sessions", "Maintenance"),
                        tabs.getTabs().stream().map(javafx.scene.control.Tab::getText).toList());
                // Tab content is queried directly because unselected tabs are not in the scene graph.
                javafx.scene.Parent users = (javafx.scene.Parent) ((javafx.scene.control.ScrollPane) tabs.getTabs().get(1).getContent()).getContent();
                javafx.scene.Parent inventory = (javafx.scene.Parent) ((javafx.scene.control.ScrollPane) tabs.getTabs().get(2).getContent()).getContent();
                javafx.scene.Parent sessions = (javafx.scene.Parent) ((javafx.scene.control.ScrollPane) tabs.getTabs().get(3).getContent()).getContent();
                javafx.scene.Parent maintenance = (javafx.scene.Parent) ((javafx.scene.control.ScrollPane) tabs.getTabs().get(4).getContent()).getContent();

                @SuppressWarnings("unchecked")
                javafx.scene.control.ComboBox<String> role = (javafx.scene.control.ComboBox<String>) users.lookup("#userRole");
                role.setValue("Instructor");
                text(users, "userName", "New Instructor"); text(users, "userEmail", "new-instructor@test.com");
                text(users, "userExtra", "Fitness"); click(users, "registerUser");
                assertEquals(2, combo(sessions, "sessionInstructor").getItems().size());

                combo(inventory, "equipmentAdmin").getSelectionModel().selectFirst();
                text(inventory, "equipmentId", "UI-EQ"); text(inventory, "equipmentName", "UI Bike");
                text(inventory, "equipmentCategory", "Cardio"); text(inventory, "equipmentLocation", "Studio C");
                click(inventory, "addEquipment");
                @SuppressWarnings("unchecked")
                javafx.scene.control.ListView<com.iwfc.model.Equipment> items =
                        (javafx.scene.control.ListView<com.iwfc.model.Equipment>) inventory.lookup("#equipmentList");
                com.iwfc.model.Equipment bike = items.getItems().stream().filter(e -> e.getEquipmentId().equals("UI-EQ")).findFirst().orElseThrow();
                items.getSelectionModel().select(bike);
                text(inventory, "equipmentName", "Edited Bike"); click(inventory, "editEquipment");
                assertEquals("Edited Bike", bike.getName());
                text(inventory, "equipmentUsage", "10"); click(inventory, "addEquipmentUsage");
                assertEquals(10, bike.getUsageHours());

                text(sessions, "sessionId", "UI-S"); text(sessions, "sessionTitle", "UI Circuit");
                text(sessions, "sessionStudio", "Studio C"); text(sessions, "sessionTime", "12:00");
                combo(sessions, "sessionInstructor").getSelectionModel().selectLast();
                @SuppressWarnings("unchecked")
                javafx.scene.control.ListView<com.iwfc.model.Equipment> allocation =
                        (javafx.scene.control.ListView<com.iwfc.model.Equipment>) sessions.lookup("#sessionEquipment");
                allocation.getSelectionModel().select(bike); click(sessions, "createSession");
                combo(sessions, "bookingMember").getSelectionModel().selectFirst();
                combo(sessions, "bookingSession").getSelectionModel().selectLast(); click(sessions, "bookSession");
                assertEquals(1, combo(sessions, "cancellationBooking").getItems().size());
                combo(sessions, "cancellationBooking").getSelectionModel().selectFirst(); click(sessions, "cancelBooking");
                assertTrue(combo(sessions, "cancellationBooking").getItems().isEmpty());

                combo(maintenance, "maintenanceReporter").getSelectionModel().selectFirst();
                combo(maintenance, "maintenanceEquipment").getSelectionModel().selectLast();
                text(maintenance, "maintenanceDescription", "UI fault"); click(maintenance, "submitMaintenance");
                combo(maintenance, "maintenanceAdmin").getSelectionModel().selectFirst();
                combo(maintenance, "assignmentRequest").getSelectionModel().selectLast();
                text(maintenance, "maintenanceAssignee", "Staff"); click(maintenance, "assignMaintenance");
                combo(maintenance, "completionRequest").getSelectionModel().selectFirst(); click(maintenance, "completeMaintenance");
                assertTrue(bike.isUsable());
                @SuppressWarnings("unchecked")
                javafx.scene.control.ListView<String> log = (javafx.scene.control.ListView<String>) maintenance.lookup("#notificationLog");
                assertTrue(log.getItems().get(0).contains("COMPLETED"));
                click(inventory, "deactivateEquipment"); assertFalse(bike.isActive());
                assertNotNull(scene.getRoot());
                finished.complete(null);
            } catch (Throwable error) { finished.completeExceptionally(error); }
        });
        try { finished.get(30, java.util.concurrent.TimeUnit.SECONDS); }
        finally { javafx.application.Platform.exit(); }
    }

    private static void text(javafx.scene.Parent root, String id, String value) {
        ((javafx.scene.control.TextField) root.lookup("#" + id)).setText(value);
    }
    private static void click(javafx.scene.Parent root, String id) {
        ((javafx.scene.control.Button) root.lookup("#" + id)).fire();
    }
    private static javafx.scene.control.ComboBox<?> combo(javafx.scene.Parent root, String id) {
        return (javafx.scene.control.ComboBox<?>) root.lookup("#" + id);
    }

    @Test void fxmlShellDeclaresControllerAndRequiredInjectionFields() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/com/iwfc/MainView.fxml")) {
            assertNotNull(stream);
            var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
            assertEquals("com.iwfc.MainController", document.getDocumentElement().getAttribute("fx:controller"));
            var panes = document.getElementsByTagName("TabPane");
            assertEquals(1, panes.getLength());
            assertEquals("mainTabPane", panes.item(0).getAttributes().getNamedItem("fx:id").getNodeValue());
            assertNotNull(MainController.class.getDeclaredField("statusLabel"));
        }
    }

    @Test void stylesheetIsPackagedAndDefinesFormStyling() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/com/iwfc/styles.css")) {
            assertNotNull(stream);
            String css = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(css.contains(".form-panel"));
            assertTrue(css.contains(".btn-primary"));
        }
    }

    @Test void launcherHasPublicStaticMainAndApplicationStart() throws Exception {
        int modifiers = Main.class.getMethod("main", String[].class).getModifiers();
        assertTrue(Modifier.isPublic(modifiers));
        assertTrue(Modifier.isStatic(modifiers));
        assertEquals(javafx.application.Application.class, MainApp.class.getSuperclass());
        assertNotNull(MainApp.class.getMethod("start", javafx.stage.Stage.class));
    }
}
