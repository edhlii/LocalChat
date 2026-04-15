package org.proptit.localchat.client.controller;

import org.proptit.localchat.common.models.User;

public interface ChatCallView {
    void showCallWindow(User peer, String statusText);

    void showInfo(String message);

    void showError(String message);

    void closeCallWindow();

    boolean confirmIncomingCall(User caller);

    User resolveUser(String username, String nickname);

    String resolveLocalAddress();
}
