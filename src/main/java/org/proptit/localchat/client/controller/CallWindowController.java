package org.proptit.localchat.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

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

}
