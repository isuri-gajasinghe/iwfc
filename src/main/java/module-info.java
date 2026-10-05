module com.iwfc {
    requires javafx.controls;
    requires javafx.fxml;
    requires static java.xml;

    opens com.iwfc to javafx.fxml;
    // javafx.base needs reflective access to model classes for PropertyValueFactory
    opens com.iwfc.model to javafx.base;

    exports com.iwfc;
    exports com.iwfc.model;
}
