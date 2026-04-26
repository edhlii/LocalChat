package org.proptit.localchat.client.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.proptit.localchat.client.networks.SocketClient;
import org.proptit.localchat.common.enums.TypeDataPacket;
import org.proptit.localchat.common.models.DataPacket;
import org.proptit.localchat.common.models.User;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;


public class UserSettingsController {
    @FXML private Circle circleAvatar;
    @FXML private Label lblUsername;
    @FXML private Label lblNickname;
    @FXML private Label lblError;
    @FXML private TextField txtEditNickname;
    @FXML private Button btnEditNickname;
    @FXML private Label lblRole;
    private ChangePasswordController changePasswordController;

    private User me;
    private SocketClient client;
    private byte[] tempAvatar;

    public User getMe() {
        return me;
    }

    public void setMe(User me) {
        this.me = me;
    }

    public ChangePasswordController getChangePasswordController() {
        return changePasswordController;
    }

    public void init(SocketClient client, User me) {
        this.client = client;
        this.me = me;
        lblUsername.setText(me.getUsername());
        lblNickname.setText(me.getNickname());
        lblRole.setText(me.getRole());
        if (me.getAvatar() != null) {
            circleAvatar.setFill(new ImagePattern(new Image(new ByteArrayInputStream(me.getAvatar()))));
        }
    }
    @FXML
    void onSaveClick(ActionEvent event) {
        me.setNickname(lblNickname.getText());
        if (tempAvatar != null) me.setAvatar(tempAvatar);
        client.sendData(new DataPacket(TypeDataPacket.UPDATE_PROFILE_REQUEST, me));
    }

    @FXML
    void onTogglePassword(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/proptit/localchat/change_password.fxml"));
            Parent root = loader.load();

            changePasswordController  = loader.getController();
            changePasswordController.setup(client, me);

            Stage stage = new Stage();
            stage.setTitle("Change Password");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void onChangeAvatarClick(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh đại diện...");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        File selectedFile = fileChooser.showOpenDialog(circleAvatar.getScene().getWindow());
        if (selectedFile != null) {
            try {
                if (selectedFile.length() > 1024 * 1024) {
                    lblError.setStyle("-fx-text-fill: #FF5C5C;");
                    lblError.setText("Vui lòng chọn ảnh < 1MB...");
                    return;
                }

                tempAvatar = Files.readAllBytes(selectedFile.toPath());
                setAvatarToCircle(tempAvatar);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    @FXML
    void onChangeNicknameClick(ActionEvent event) {
        if (btnEditNickname.getText().equals("Edit")) {

            txtEditNickname.setText(lblNickname.getText());

            lblNickname.setVisible(false);
            lblNickname.setManaged(false);

            txtEditNickname.setVisible(true);
            txtEditNickname.setManaged(true);
            txtEditNickname.requestFocus();

            btnEditNickname.setText("Done");
        } else {

            String newName = txtEditNickname.getText().trim();
            if (!newName.isEmpty()) {
                lblNickname.setText(newName);
            }

            switchToViewMode();
        }
    }
    private void switchToViewMode() {
        txtEditNickname.setVisible(false);
        txtEditNickname.setManaged(false);

        lblNickname.setVisible(true);
        lblNickname.setManaged(true);

        btnEditNickname.setText("Edit");
    }
    private void setAvatarToCircle(byte[] bytes) {
        Image img = new Image(new ByteArrayInputStream(bytes));
        circleAvatar.setFill(new ImagePattern(img));
    }
    public void closeWindow(User updatedUser) {
        Platform.runLater(() -> {
            Stage stage = (Stage) lblNickname.getScene().getWindow();
            stage.close();
        });
    }


}
