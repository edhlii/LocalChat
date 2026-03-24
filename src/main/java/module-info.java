module org.proptit.localchat {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.proptit.localchat to javafx.fxml;
    exports org.proptit.localchat;
}