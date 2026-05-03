package org.proptit.localchat.client.controller;

import org.proptit.localchat.common.models.User;

import java.util.List;

public interface ChatCallView {
    void showCallWindow(User peer, String statusText);

    void updateCallParticipants(List<String> participantLabels);

    void updateCallStatus(String statusText);

    void setVideoCallAvailable(boolean available);

    void setVideoCallActive(boolean active);

    void updateScreenShareButton(boolean sharing);

    void showRemoteScreenFrame(byte[] frameBytes);

    void showRemoteVideoFrame(byte[] frameBytes);

    void showLocalVideoFrame(byte[] frameBytes);

    void clearRemoteScreenFrame();

    void clearLocalVideoFrame();

    void showInfo(String message);

    void showError(String message);

    void closeCallWindow();

    boolean confirmIncomingCall(User caller);

    User resolveUser(String username, String nickname);

    String resolveLocalAddress();
}
