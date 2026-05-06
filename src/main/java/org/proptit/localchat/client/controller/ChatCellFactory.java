package org.proptit.localchat.client.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Callback;
import org.proptit.localchat.common.models.ChatGroup;
import org.proptit.localchat.common.models.User;

import java.util.Map;
import java.util.Set;

/**
 * Factories for creating custom list cells for chat UI
 */
public class ChatCellFactory {
    
    /**
     * Creates cell factory for online people list (horizontal)
     */
    public static Callback<ListView<String>, ListCell<String>> createOnlinePeopleCellFactory(
            Map<String, User> userMap, Set<Integer> selectedUsers) {
        return param -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.equals(ChatUiConstants.LABEL_NO_ONE_ONLINE)) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setGraphic(createOnlinePeopleCell(item, userMap));
                    setText(null);
                }
            }
        };
    }
    
    /**
     * Creates individual cell for online people list
     */
    private static VBox createOnlinePeopleCell(String item, Map<String, User> userMap) {
        VBox root = new VBox(ChatUiConstants.SPACING_MEDIUM);
        root.setAlignment(Pos.CENTER);
        root.setPrefWidth(75);
        
        StackPane avatarContainer = new StackPane();
        avatarContainer.setMaxSize(52, 52);
        StackPane avatar = ChatAvatarManager.createAvatarPane(userMap.get(item), 52);
        ChatAvatarManager.addOnlineIndicator(avatar, false);
        avatarContainer.getChildren().add(avatar);
        
        String nickname = extractNickname(item);
        if (nickname.length() > ChatUiConstants.CHAT_NAME_MAX_LENGTH) {
            nickname = nickname.substring(0, ChatUiConstants.CHAT_NAME_SUFFIX) + "...";
        }
        
        Label nickLabel = new Label(nickname);
        nickLabel.setTextFill(Color.web(ChatUiConstants.COLOR_TEXT_PRIMARY));
        nickLabel.setFont(Font.font("System", ChatUiConstants.FONT_SIZE_SMALL));
        nickLabel.setAlignment(Pos.CENTER);
        
        root.getChildren().addAll(avatarContainer, nickLabel);
        return root;
    }
    
    /**
     * Creates cell factory for chat list (vertical)
     */
    public static Callback<ListView<String>, ListCell<String>> createChatListCellFactory(
            Map<String, User> userMap, Map<String, ChatGroup> groupMap, 
            Set<Integer> unreadMessages, Set<Integer> onlineUserIds, boolean isGroupMode) {
        return param -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.equals(ChatUiConstants.LABEL_NO_CONVERSATIONS)) {
                    setGraphic(null);
                    setText(null);
                    setTooltip(null);
                } else if (item.equals(ChatUiConstants.LABEL_ANNOUNCEMENT)) {
                    setGraphic(createAnnouncementCell(unreadMessages));
                    setText(null);
                } else if (isGroupMode) {
                    setGraphic(createGroupCell(item, groupMap, unreadMessages));
                    setText(null);
                } else {
                    setGraphic(createUserCell(item, userMap, unreadMessages, onlineUserIds));
                    setTooltip(new Tooltip(extractUsername(item)));
                }
            }
        };
    }
    
    /**
     * Creates announcement cell
     */
    private static HBox createAnnouncementCell(Set<Integer> unreadMessages) {
        HBox root = new HBox(ChatUiConstants.SPACING_XLARGE);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(ChatUiConstants.PADDING_MEDIUM, ChatUiConstants.PADDING_XLARGE, 
                                    ChatUiConstants.PADDING_MEDIUM, ChatUiConstants.PADDING_XLARGE));
        
        Label icon = new Label(ChatUiConstants.LABEL_EMOJI_ANNOUNCEMENT);
        icon.setStyle("-fx-font-size: " + ChatUiConstants.FONT_SIZE_EMOJI + "px; -fx-text-fill: white;");
        
        VBox textInfo = new VBox(ChatUiConstants.SPACING_SMALL);
        textInfo.setAlignment(Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(ChatUiConstants.LABEL_ANNOUNCEMENT);
        nameLabel.setTextFill(Color.web(ChatUiConstants.COLOR_ORANGE));
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, ChatUiConstants.FONT_SIZE_MEDIUM));
        textInfo.getChildren().add(nameLabel);
        
        if (unreadMessages.contains(0)) {
            Label newMsgLabel = new Label(ChatUiConstants.LABEL_NEW_MESSAGE);
            newMsgLabel.setFont(Font.font("System", FontWeight.BOLD, ChatUiConstants.FONT_SIZE_TINY));
            newMsgLabel.setTextFill(Color.WHITE);
            textInfo.getChildren().add(newMsgLabel);
        }
        
        root.getChildren().addAll(icon, textInfo);
        return root;
    }
    
    /**
     * Creates group chat cell
     */
    private static HBox createGroupCell(String item, Map<String, ChatGroup> groupMap, Set<Integer> unreadMessages) {
        HBox root = new HBox(ChatUiConstants.SPACING_XLARGE);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(ChatUiConstants.PADDING_MEDIUM, ChatUiConstants.PADDING_XLARGE, 
                                    ChatUiConstants.PADDING_MEDIUM, ChatUiConstants.PADDING_XLARGE));
        
        ChatGroup group = groupMap.get(item);
        String groupName = extractGroupName(item);
        
        StackPane avatar = ChatAvatarManager.createAvatarPane(null, ChatUiConstants.AVATAR_SIZE_MEDIUM);
        ChatAvatarManager.setDefaultAvatar(avatar, (javafx.scene.shape.Circle) ((StackPane)avatar).getChildren().get(0), 
                                          groupName, ChatUiConstants.FONT_SIZE_SMALL);
        
        VBox textInfo = new VBox(ChatUiConstants.SPACING_SMALL);
        textInfo.setAlignment(Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(groupName);
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, ChatUiConstants.FONT_SIZE_MEDIUM));
        textInfo.getChildren().add(nameLabel);
        
        if (group != null && unreadMessages.contains(-group.getId())) {
            Label newMsgLabel = new Label(ChatUiConstants.LABEL_NEW_MESSAGE);
            newMsgLabel.setFont(Font.font("System", FontWeight.BOLD, ChatUiConstants.FONT_SIZE_TINY));
            newMsgLabel.setTextFill(Color.WHITE);
            textInfo.getChildren().add(newMsgLabel);
        }
        
        root.getChildren().addAll(avatar, textInfo);
        return root;
    }
    
    /**
     * Creates user cell
     */
    private static HBox createUserCell(String item, Map<String, User> userMap, 
                                      Set<Integer> unreadMessages, Set<Integer> onlineUserIds) {
        HBox root = new HBox(ChatUiConstants.SPACING_XLARGE);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(ChatUiConstants.PADDING_MEDIUM, ChatUiConstants.PADDING_XLARGE, 
                                    ChatUiConstants.PADDING_MEDIUM, ChatUiConstants.PADDING_XLARGE));
        
        User user = userMap.get(item);
        String nickname = extractNickname(item);
        
        StackPane avatar = ChatAvatarManager.createAvatarPane(user, ChatUiConstants.AVATAR_SIZE_MEDIUM);
        
        if (user != null && onlineUserIds.contains(user.getId())) {
            ChatAvatarManager.addOnlineIndicator(avatar, true);
        }
        
        VBox textInfo = new VBox(ChatUiConstants.SPACING_SMALL);
        textInfo.setAlignment(Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(nickname);
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, ChatUiConstants.FONT_SIZE_MEDIUM));
        
        if (user != null && unreadMessages.contains(user.getId())) {
            nameLabel.setTextFill(Color.web(ChatUiConstants.COLOR_PURPLE));
            Label newMsgLabel = new Label(ChatUiConstants.LABEL_NEW_MESSAGE);
            newMsgLabel.setFont(Font.font("System", FontWeight.BOLD, ChatUiConstants.FONT_SIZE_TINY));
            newMsgLabel.setTextFill(Color.WHITE);
            textInfo.getChildren().add(newMsgLabel);
        }
        
        textInfo.getChildren().add(0, nameLabel);
        root.getChildren().addAll(avatar, textInfo);
        return root;
    }
    
    // Helper methods
    
    private static String extractNickname(String item) {
        return item.contains("(@") ? item.substring(0, item.indexOf("(@")).trim() : item;
    }
    
    private static String extractUsername(String item) {
        if (item.contains("(@") && item.contains(")")) {
            int start = item.indexOf("(@") + 2;
            int end = item.indexOf(")");
            return item.substring(start, end);
        }
        return item;
    }
    
    private static String extractGroupName(String item) {
        return item.contains("@") ? item.substring(item.indexOf("@") + 1).trim() : item;
    }
}
