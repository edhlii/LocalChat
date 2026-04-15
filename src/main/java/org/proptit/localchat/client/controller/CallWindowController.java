package org.proptit.localchat.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.proptit.localchat.common.models.User;

import java.util.function.Consumer;

public class CallWindowController {
    @FXML
    private Button btnAddParticipant;
    @FXML
    private Button btnCamera;
    @FXML
    private Button btnChat;
    @FXML
    private Button btnEndCall;
    @FXML
    private Button btnMic;
    @FXML
    private Button btnShareScreen;
    @FXML
    private Button btnToggleView;
    @FXML
    private Label callStatusLabel;
    @FXML
    private Label contactNameLabel;
    @FXML
    private StackPane localPreviewPane;
    @FXML
    private Pane remoteVideoPane;
    private User selectedConversationUser;
    private Runnable onEndCall;
    private Consumer<Boolean> onMuteChanged;
    private boolean micMuted;

    @FXML
    private void initialize() {
        if (btnEndCall != null) {
            btnEndCall.setOnAction(event -> onEndCallButtonClick());
        }
        if (btnMic != null) {
            btnMic.setOnAction(event -> onMicButtonClick());
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

    public void setOnEndCall(Runnable onEndCall) {
        this.onEndCall = onEndCall;
    }

    public void setOnMuteChanged(Consumer<Boolean> onMuteChanged) {
        this.onMuteChanged = onMuteChanged;
    }

    public void updateCallStatus(String statusText) {
        if (callStatusLabel != null) {
            callStatusLabel.setText(statusText);
        }
    }

    public void setMicMuted(boolean muted) {
        micMuted = muted;
        if (btnMic != null) {
            btnMic.setText(micMuted ? "Unmute" : "Mute");
        }
    }

    @FXML
    private void onMicButtonClick() {
        setMicMuted(!micMuted);
        if (onMuteChanged != null) {
            onMuteChanged.accept(micMuted);
        }
    }

    @FXML
    private void onEndCallButtonClick() {
        if (onEndCall != null) {
            onEndCall.run();
            return;
        }

        if (btnEndCall != null && btnEndCall.getScene() != null) {
            Stage stage = (Stage) btnEndCall.getScene().getWindow();
            stage.close();
        }
    }

}
