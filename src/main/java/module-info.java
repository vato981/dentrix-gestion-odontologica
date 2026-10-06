module org.example.demofx {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.base;

    opens org.example.demofx to javafx.fxml, javafx.base;
    exports org.example.demofx;
}
