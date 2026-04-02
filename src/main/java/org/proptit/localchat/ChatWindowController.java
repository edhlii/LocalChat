package org.proptit.localchat;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ChatWindowController {

    @FXML
    private TextArea messageInput;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private Button sendMessageButton;

    @FXML
    private VBox vboxMessage;

    @FXML
    public void initialize() {
        vboxMessage.heightProperty().addListener((observable, oldValue, newValue) -> {
            scrollPane.setVvalue((Double) newValue);
        });
    }

    @FXML
    void onSendButtonClick(ActionEvent event) {
        String messageText = messageInput.getText();

        // Kiểm tra xem tin nhắn có rỗng không
        if (!messageText.trim().isEmpty()) {
            // Thêm tin nhắn của bạn (bên phải)
            addMessageToScreen(messageText, true);

            // Xóa nội dung trong ô nhập liệu sau khi gửi
            messageInput.clear();
        }
    }

    /**
     * Hàm dùng để tạo bong bóng chat và nhét vào VBox
     * @param text Nội dung tin nhắn
     * @param isMe Nếu là true -> Căn lề phải (Mình gửi). Nếu là false -> Căn lề trái (Người khác gửi)
     */
    private void addMessageToScreen(String text, boolean isMe) {
        // 1. Tạo một Label chứa nội dung tin nhắn
        Label lblMessage = new Label(text);
        lblMessage.setWrapText(true); // Cho phép xuống dòng nếu tin nhắn quá dài

        // Thêm CSS để Label trông giống bong bóng chat
        if (isMe) {
            lblMessage.setStyle("-fx-background-color: #0084FF; -fx-text-fill: white; -fx-background-radius: 15px; -fx-padding: 8px 12px;");
        } else {
            lblMessage.setStyle("-fx-background-color: #E4E6EB; -fx-text-fill: black; -fx-background-radius: 15px; -fx-padding: 8px 12px;");
        }

        // 2. Bọc Label vào trong một HBox
        HBox hboxContainer = new HBox(lblMessage);
        // Padding để các bong bóng không dính sát vào mép màn hình
        hboxContainer.setPadding(new Insets(5, 10, 5, 10));

        // 3. Căn lề cho HBox (Trái hoặc Phải)
        if (isMe) {
            hboxContainer.setAlignment(Pos.CENTER_RIGHT); // Mình gửi thì nằm bên phải
        } else {
            hboxContainer.setAlignment(Pos.CENTER_LEFT);  // Bạn gửi thì nằm bên trái
        }

        // 4. Thêm HBox (chứa Label) vào VBox chính
        vboxMessage.getChildren().add(hboxContainer);
    }

}
