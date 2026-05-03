package org.proptit.localchat.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.proptit.localchat.common.models.User;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CallWindowController {
    @FXML
    private Button btnAddParticipant;
    @FXML
    private Button btnCamera;
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
    private Label participantsTitleLabel;
    @FXML
    private Label remoteVideoPlaceholderLabel;
    @FXML
    private StackPane localPreviewPane;
    @FXML
    private Pane remoteVideoPane;
    @FXML
    private ImageView remoteScreenImageView;
    @FXML
    private ImageView localPreviewImageView;
    @FXML
    private Label localPreviewPlaceholderLabel;
    @FXML
    private VBox participantListBox;
    private User selectedConversationUser;
    private Runnable onEndCall;
    private Consumer<Boolean> onMuteChanged;
    private Consumer<Boolean> onScreenShareChanged;
    private Consumer<Boolean> onVideoChanged;
    private boolean micMuted;
    private boolean screenSharing;
    private boolean videoActive;
    private boolean videoAvailable;
    private final List<String> currentParticipants = new ArrayList<>();

    @FXML
    private void initialize() {
        if (btnEndCall != null) {
            btnEndCall.setOnAction(event -> onEndCallButtonClick());
        }
        if (btnMic != null) {
            btnMic.setOnAction(event -> onMicButtonClick());
        }
        if (btnCamera != null) {
            btnCamera.setOnAction(event -> onCameraButtonClick());
        }
        if (btnShareScreen != null) {
            btnShareScreen.setOnAction(event -> onShareScreenButtonClick());
        }
    }

    public void init(User selectedConversationUser) {
        this.selectedConversationUser = selectedConversationUser;
        String peerDisplayName = selectedConversationUser != null
                ? selectedConversationUser.getNickname()
                : "Unknown";

        if (contactNameLabel != null) {
            contactNameLabel.setText(peerDisplayName);
        }
        updateCallParticipants(java.util.Arrays.asList("You", peerDisplayName));
    }

    public void updateCallParticipants(List<String> participantLabels) {
        currentParticipants.clear();
        if (participantLabels != null) {
            for (String label : participantLabels) {
                if (label != null && !label.isBlank()) {
                    currentParticipants.add(label);
                }
            }
        }

        if (participantsTitleLabel != null) {
            participantsTitleLabel.setText("Participants (" + currentParticipants.size() + ")");
        }
        if (participantListBox != null) {
            participantListBox.getChildren().clear();
            if (currentParticipants.isEmpty()) {
                Label emptyLabel = new Label("No participants yet");
                emptyLabel.setTextFill(javafx.scene.paint.Color.web("#b9c8e8"));
                participantListBox.getChildren().add(emptyLabel);
                return;
            }

            for (String label : currentParticipants) {
                participantListBox.getChildren().add(createParticipantRow(label));
            }
        }
    }

    private HBox createParticipantRow(String participantLabel) {
        HBox row = new HBox(8.0);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        javafx.scene.shape.Circle statusDot = new javafx.scene.shape.Circle(4.0);
        statusDot.setStroke(javafx.scene.paint.Color.TRANSPARENT);
        statusDot.setFill(javafx.scene.paint.Color.web("#32d296"));

        Label nameLabel = new Label(participantLabel);
        nameLabel.setTextFill(javafx.scene.paint.Color.web("#d7e3ff"));
        nameLabel.setWrapText(true);

        row.getChildren().addAll(statusDot, nameLabel);
        return row;
    }

    public void setOnEndCall(Runnable onEndCall) {
        this.onEndCall = onEndCall;
    }

    public void setOnMuteChanged(Consumer<Boolean> onMuteChanged) {
        this.onMuteChanged = onMuteChanged;
    }

    public void setOnScreenShareChanged(Consumer<Boolean> onScreenShareChanged) {
        this.onScreenShareChanged = onScreenShareChanged;
    }

    public void setOnVideoChanged(Consumer<Boolean> onVideoChanged) {
        this.onVideoChanged = onVideoChanged;
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

    public void setScreenSharingActive(boolean sharing) {
        screenSharing = sharing;
        if (btnShareScreen != null) {
            btnShareScreen.setText(screenSharing ? "Stop Share" : "Share Screen");
        }
    }

    public void setVideoCallAvailable(boolean available) {
        videoAvailable = available;
        if (btnCamera != null) {
            btnCamera.setDisable(!available);
            btnCamera.setText(available ? (videoActive ? "Stop Camera" : "Camera") : "Camera");
        }
    }

    public void setVideoCallActive(boolean active) {
        videoActive = active;
        if (btnCamera != null) {
            btnCamera.setText(videoActive ? "Stop Camera" : "Camera");
        }
    }

    public void showRemoteScreenFrame(byte[] frameBytes) {
        if (frameBytes == null || frameBytes.length == 0) {
            return;
        }

        if (remoteScreenImageView != null) {
            remoteScreenImageView.setImage(new Image(new ByteArrayInputStream(frameBytes)));
            remoteScreenImageView.setVisible(true);
        }
        if (remoteVideoPlaceholderLabel != null) {
            remoteVideoPlaceholderLabel.setText("Remote video stream");
            remoteVideoPlaceholderLabel.setVisible(false);
        }
    }

    public void showRemoteVideoFrame(byte[] frameBytes) {
        showRemoteScreenFrame(frameBytes);
    }

    public void showLocalVideoFrame(byte[] frameBytes) {
        if (frameBytes == null || frameBytes.length == 0) {
            return;
        }

        if (localPreviewImageView != null) {
            localPreviewImageView.setImage(new Image(new ByteArrayInputStream(frameBytes)));
            localPreviewImageView.setVisible(true);
        }
        if (localPreviewPlaceholderLabel != null) {
            localPreviewPlaceholderLabel.setVisible(false);
        }
    }

    public void clearRemoteScreenFrame() {
        if (remoteScreenImageView != null) {
            remoteScreenImageView.setImage(null);
            remoteScreenImageView.setVisible(false);
        }
        if (remoteVideoPlaceholderLabel != null) {
            remoteVideoPlaceholderLabel.setText("Remote video stream");
            remoteVideoPlaceholderLabel.setVisible(true);
        }
    }

    public void clearLocalVideoFrame() {
        if (localPreviewImageView != null) {
            localPreviewImageView.setImage(null);
            localPreviewImageView.setVisible(false);
        }
        if (localPreviewPlaceholderLabel != null) {
            localPreviewPlaceholderLabel.setText("Camera preview");
            localPreviewPlaceholderLabel.setVisible(true);
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
    private void onShareScreenButtonClick() {
        setScreenSharingActive(!screenSharing);
        if (onScreenShareChanged != null) {
            onScreenShareChanged.accept(screenSharing);
        }
    }

    @FXML
    private void onCameraButtonClick() {
        if (!videoAvailable) {
            return;
        }

        setVideoCallActive(!videoActive);
        if (onVideoChanged != null) {
            onVideoChanged.accept(videoActive);
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
