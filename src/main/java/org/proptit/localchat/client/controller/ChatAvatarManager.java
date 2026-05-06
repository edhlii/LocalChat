package org.proptit.localchat.client.controller;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.proptit.localchat.common.models.User;

import java.io.ByteArrayInputStream;

/**
 * Manages avatar creation and rendering for chat UI
 */
public class ChatAvatarManager {
    
    /**
     * Creates an avatar pane with user's image or initials
     */
    public static StackPane createAvatarPane(User user, int diameter) {
        StackPane pane = new StackPane();
        String name = user != null ? user.getNickname() : "?";
        Circle circle = createAvatarCircle(diameter);
        
        if (user != null && user.getAvatar() != null && user.getAvatar().length > 0) {
            try {
                Image img = new Image(new ByteArrayInputStream(user.getAvatar()));
                if (!img.isError()) {
                    circle.setFill(new javafx.scene.paint.ImagePattern(img));
                    pane.getChildren().add(circle);
                    return pane;
                }
            } catch (Exception e) {
                // Fall through to default avatar
            }
        }
        
        setDefaultAvatar(pane, circle, name, calculateFontSize(diameter));
        return pane;
    }
    
    /**
     * Creates a simple avatar circle with stroke styling
     */
    private static Circle createAvatarCircle(int diameter) {
        Circle circle = new Circle(diameter / 2.0, Color.web(ChatUiConstants.COLOR_DARK_BG));
        circle.setStroke(Color.WHITE);
        circle.setStrokeWidth(diameter >= 20 ? ChatUiConstants.STROKE_WIDTH_AVATAR : ChatUiConstants.STROKE_WIDTH_AVATAR_SMALL);
        return circle;
    }
    
    /**
     * Sets default avatar with initials
     */
    public static void setDefaultAvatar(StackPane stack, Circle circle, String name, int fontSize) {
        stack.getChildren().clear();
        circle.setFill(Color.web(ChatUiConstants.COLOR_DARK_BG));
        
        String initial = (name == null || name.isEmpty()) ? "?" : name.substring(0, 1).toUpperCase();
        Text initialText = new Text(initial);
        initialText.setFill(Color.WHITE);
        initialText.setFont(Font.font("System", FontWeight.BOLD, fontSize));
        
        stack.getChildren().addAll(circle, initialText);
    }
    
    /**
     * Adds online indicator dot to avatar
     */
    public static void addOnlineIndicator(StackPane avatarPane, boolean isSmall) {
        Circle onlineDot = new Circle(
            isSmall ? ChatUiConstants.ONLINE_DOT_SMALL_RADIUS : ChatUiConstants.ONLINE_DOT_RADIUS,
            Color.web(ChatUiConstants.COLOR_ONLINE)
        );
        onlineDot.setStroke(Color.web(isSmall ? ChatUiConstants.COLOR_DARKER_BG : ChatUiConstants.COLOR_BORDER));
        onlineDot.setStrokeWidth(isSmall ? ChatUiConstants.STROKE_WIDTH_ONLINE_DOT_SMALL : ChatUiConstants.STROKE_WIDTH_ONLINE_DOT);
        
        javafx.geometry.Pos alignment = javafx.geometry.Pos.BOTTOM_RIGHT;
        StackPane.setAlignment(onlineDot, alignment);
        avatarPane.getChildren().add(onlineDot);
    }
    
    /**
     * Calculates appropriate font size based on avatar diameter
     */
    private static int calculateFontSize(int diameter) {
        if (diameter <= 16) return ChatUiConstants.FONT_SIZE_TINY;
        if (diameter <= 20) return ChatUiConstants.FONT_SIZE_SMALL;
        return ChatUiConstants.FONT_SIZE_MEDIUM;
    }
}
