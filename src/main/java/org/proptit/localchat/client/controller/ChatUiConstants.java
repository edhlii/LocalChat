package org.proptit.localchat.client.controller;

import javafx.scene.paint.Color;

/**
 * Constants for Chat UI styling and colors
 */
public class ChatUiConstants {
    
    // Colors
    public static final String COLOR_PRIMARY = "#AD7BFF";
    public static final String COLOR_DARK_BG = "#2A3042";
    public static final String COLOR_DARKER_BG = "#1E2435";
    public static final String COLOR_TEXT_PRIMARY = "#E4E6EB";
    public static final String COLOR_TEXT_SECONDARY = "#919191";
    public static final String COLOR_ONLINE = "#23A559";
    public static final String COLOR_ORANGE = "#E67E22";
    public static final String COLOR_PURPLE = "#AD7BFF";
    public static final String COLOR_WHITE = "#FFFFFF";
    public static final String COLOR_BORDER = "#0B0F19";
    
    // Message Styles
    public static final String STYLE_MESSAGE_SENT = 
        "-fx-background-color: " + COLOR_PRIMARY + "; " +
        "-fx-text-fill: white; " +
        "-fx-background-radius: 15px; " +
        "-fx-padding: 8px 12px;";
    
    public static final String STYLE_MESSAGE_RECEIVED = 
        "-fx-background-color: " + COLOR_DARKER_BG + "; " +
        "-fx-text-fill: white; " +
        "-fx-background-radius: 15px; " +
        "-fx-padding: 8px 12px;";
    
    public static final String STYLE_FILE_BOX =
        "-fx-background-color: " + COLOR_DARKER_BG + "; " +
        "-fx-background-radius: 10px; " +
        "-fx-padding: 10px; " +
        "-fx-border-color: " + COLOR_DARK_BG + "; " +
        "-fx-border-radius: 10px;";
    
    public static final String STYLE_TIME_LABEL = 
        "-fx-font-size: 10px; " +
        "-fx-text-fill: " + COLOR_TEXT_SECONDARY + ";";
    
    public static final String STYLE_FILE_NAME = 
        "-fx-font-weight: bold; " +
        "-fx-text-fill: white;";
    
    public static final String STYLE_BUTTON_DOWNLOAD =
        "-fx-background-color: " + COLOR_PRIMARY + "; " +
        "-fx-text-fill: black; " +
        "-fx-background-radius: 5px; " +
        "-fx-cursor: hand;";
    
    // Sizes
    public static final int AVATAR_SIZE_SMALL = 16;
    public static final int AVATAR_SIZE_MEDIUM = 20;
    public static final int AVATAR_SIZE_LARGE = 26;
    
    public static final int ONLINE_DOT_RADIUS = 7;
    public static final int ONLINE_DOT_SMALL_RADIUS = 6;
    
    public static final double STROKE_WIDTH_AVATAR = 2;
    public static final double STROKE_WIDTH_AVATAR_SMALL = 1;
    public static final double STROKE_WIDTH_ONLINE_DOT = 2.5;
    public static final double STROKE_WIDTH_ONLINE_DOT_SMALL = 2;
    
    public static final int MESSAGE_MAX_WIDTH = 400;
    public static final int CHAT_NAME_MAX_LENGTH = 10;
    public static final int CHAT_NAME_SUFFIX = 9;
    
    // Font Sizes
    public static final int FONT_SIZE_LARGE = 16;
    public static final int FONT_SIZE_MEDIUM = 14;
    public static final int FONT_SIZE_SMALL = 12;
    public static final int FONT_SIZE_TINY = 11;
    public static final int FONT_SIZE_EMOJI = 20;
    
    // Spacing
    public static final int SPACING_SMALL = 2;
    public static final int SPACING_MEDIUM = 5;
    public static final int SPACING_LARGE = 10;
    public static final int SPACING_XLARGE = 12;
    public static final int SPACING_XXLARGE = 15;
    
    public static final int PADDING_SMALL = 5;
    public static final int PADDING_MEDIUM = 8;
    public static final int PADDING_LARGE = 10;
    public static final int PADDING_XLARGE = 12;
    
    // Labels
    public static final String LABEL_ANNOUNCEMENT = "Thông báo chung";
    public static final String LABEL_EMOJI_ANNOUNCEMENT = "\uD83D\uDCE2";
    public static final String LABEL_NEW_MESSAGE = "Có tin nhắn mới";
    public static final String LABEL_NO_CONVERSATIONS = "No conversations";
    public static final String LABEL_NO_ONE_ONLINE = "No one online";
    
    // Image-related
    public static final int IMAGE_DISPLAY_WIDTH = 250;
}
