package org.proptit.localchat.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.proptit.localchat.common.models.User;

public class CallWindowController {
    @FXML private Button btnAddParticipant;
    @FXML private Button btnCamera;
    @FXML private Button btnChat;
    @FXML private Button btnEndCall;
    @FXML private Button btnMic;
    @FXML private Button btnShareScreen;
    @FXML private Button btnToggleView;
    @FXML private Label callStatusLabel;
    @FXML private Label contactNameLabel;
    @FXML private StackPane localPreviewPane;
    @FXML private Pane remoteVideoPane;
    private User selectedConversationUser;

    @FXML
    private void initialize() {
        if (btnEndCall != null) {
            btnEndCall.setOnAction(event -> onEndCallButtonClick());
        }
    }

    public void init(User selectedConversationUser) {
        this.selectedConversationUser = selectedConversationUser;

        if (contactNameLabel != null) {
            contactNameLabel.setText(selectedConversationUser != null
                    ? selectedConversationUser.getNickname()
                    : "Unknown");
        }
    }

    @FXML
    private void onEndCallButtonClick() {
        if (btnEndCall != null && btnEndCall.getScene() != null) {
            Stage stage = (Stage) btnEndCall.getScene().getWindow();
            stage.close();
        }
    }

}
