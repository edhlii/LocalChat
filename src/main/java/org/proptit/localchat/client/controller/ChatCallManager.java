package org.proptit.localchat.client.controller;

import org.proptit.localchat.client.networks.ScreenShareSession;
import org.proptit.localchat.client.networks.SocketClient;
import org.proptit.localchat.client.networks.VoiceCallSession;
import org.proptit.localchat.common.enums.TypeDataPacket;
import org.proptit.localchat.common.models.DataPacket;
import org.proptit.localchat.common.models.User;
import org.proptit.localchat.common.models.call.CallAction;
import org.proptit.localchat.common.models.call.CallSignal;

import java.util.UUID;

public class ChatCallManager {
    private final SocketClient client;
    private final User me;
    private final ChatCallView view;

    private VoiceCallSession voiceCallSession;
    private ScreenShareSession screenShareSession;
    private String activeCallId;
    private User activeCallPeer;
    private String outgoingCallId;
    private User outgoingCallPeer;
    private boolean isEndingCall;
    private String remoteMediaHost;
    private int remoteScreenPort;
    private boolean localScreenSharing;

    public ChatCallManager(SocketClient client, User me, ChatCallView view) {
        this.client = client;
        this.me = me;
        this.view = view;
    }

    public void startOutgoingCall(User selectedConversationUser) {
        if (selectedConversationUser == null) {
            view.showInfo("Please select a user to start a call.");
            return;
        }

        if (activeCallId != null || outgoingCallId != null) {
            view.showInfo("A call is already in progress.");
            return;
        }

        String callId = UUID.randomUUID().toString();
        outgoingCallId = callId;
        outgoingCallPeer = selectedConversationUser;

        view.showCallWindow(selectedConversationUser, "Calling...");
        sendCallSignal(new CallSignal(
                callId,
                CallAction.INVITE,
                me.getUsername(),
                me.getNickname(),
                selectedConversationUser.getUsername(),
                null,
            0,
            0
        ));
    }

    public void receiveCallSignal(CallSignal signal) {
        if (signal == null || signal.getAction() == null) {
            return;
        }

        switch (signal.getAction()) {
            case INVITE:
                handleIncomingInvite(signal);
                break;
            case ACCEPT:
                handleCallAccepted(signal);
                break;
            case READY:
                handleCallReady(signal);
                break;
            case REJECT:
                handleCallRejected(signal);
                break;
            case HANGUP:
                handleRemoteHangup(signal);
                break;
            case SHARE_START:
                handleRemoteShareStarted(signal);
                break;
            case SHARE_STOP:
                handleRemoteShareStopped(signal);
                break;
            default:
                break;
        }
    }

    public void setMuted(boolean muted) {
        if (voiceCallSession != null) {
            voiceCallSession.setMuted(muted);
        }
    }

    public void endCall(boolean notifyPeer) {
        cleanupCallState(notifyPeer);
    }

    public void setScreenSharing(boolean sharing) {
        if (activeCallId == null || activeCallPeer == null) {
            view.updateScreenShareButton(false);
            return;
        }

        if (sharing) {
            if (screenShareSession == null || remoteMediaHost == null || remoteMediaHost.isBlank() || remoteScreenPort <= 0) {
                view.showInfo("Screen sharing is not ready yet.");
                view.updateScreenShareButton(false);
                return;
            }

            screenShareSession.startSending(remoteMediaHost, remoteScreenPort);
            localScreenSharing = true;
            sendCallSignal(new CallSignal(
                    activeCallId,
                    CallAction.SHARE_START,
                    me.getUsername(),
                    me.getNickname(),
                    activeCallPeer.getUsername(),
                    null,
                    0,
                    0
            ));
            view.updateCallStatus("Connected - Sharing screen");
            return;
        }

        stopLocalScreenShare(true);
    }

    private void handleIncomingInvite(CallSignal signal) {
        if (activeCallId != null || outgoingCallId != null) {
            sendCallSignal(new CallSignal(
                    signal.getCallId(),
                    CallAction.REJECT,
                    me.getUsername(),
                    me.getNickname(),
                    signal.getFromUsername(),
                    null,
                    0,
                    0
            ));
            return;
        }

        User caller = view.resolveUser(signal.getFromUsername(), signal.getFromNickname());
        boolean accepted = view.confirmIncomingCall(caller);
        if (!accepted) {
            sendCallSignal(new CallSignal(
                    signal.getCallId(),
                    CallAction.REJECT,
                    me.getUsername(),
                    me.getNickname(),
                    signal.getFromUsername(),
                    null,
                    0,
                    0
            ));
            return;
        }

        try {
            int udpPort = ensureVoiceSessionOpened();
                int screenPort = ensureScreenSessionOpened();
            activeCallId = signal.getCallId();
            activeCallPeer = caller;
            view.showCallWindow(caller, "Connecting...");
            sendCallSignal(new CallSignal(
                    signal.getCallId(),
                    CallAction.ACCEPT,
                    me.getUsername(),
                    me.getNickname(),
                    signal.getFromUsername(),
                    view.resolveLocalAddress(),
                        udpPort,
                        screenPort
            ));
        } catch (Exception ex) {
            ex.printStackTrace();
            cleanupCallState(false);
                    view.showError("Unable to access call devices.");
            sendCallSignal(new CallSignal(
                    signal.getCallId(),
                    CallAction.REJECT,
                    me.getUsername(),
                    me.getNickname(),
                    signal.getFromUsername(),
                    null,
                        0,
                        0
            ));
        }
    }

    private void handleCallAccepted(CallSignal signal) {
        if (outgoingCallId == null || !outgoingCallId.equals(signal.getCallId())) {
            return;
        }

        User peer = outgoingCallPeer != null
                ? outgoingCallPeer
                : view.resolveUser(signal.getFromUsername(), signal.getFromNickname());

        try {
            int udpPort = ensureVoiceSessionOpened();
            int screenPort = ensureScreenSessionOpened();
            activeCallId = outgoingCallId;
            activeCallPeer = peer;
            outgoingCallId = null;
            outgoingCallPeer = null;
            remoteMediaHost = signal.getHost();
            remoteScreenPort = signal.getScreenUdpPort();

            view.showCallWindow(peer, "Connecting...");
            sendCallSignal(new CallSignal(
                    activeCallId,
                    CallAction.READY,
                    me.getUsername(),
                    me.getNickname(),
                    signal.getFromUsername(),
                    view.resolveLocalAddress(),
                    udpPort,
                    screenPort
            ));

            startVoiceStreaming(signal.getHost(), signal.getUdpPort());
        } catch (Exception ex) {
            ex.printStackTrace();
            cleanupCallState(false);
            view.showError("Unable to start voice call.");
        }
    }

    private void handleCallReady(CallSignal signal) {
        if (activeCallId == null || !activeCallId.equals(signal.getCallId())) {
            return;
        }

        remoteMediaHost = signal.getHost();
        remoteScreenPort = signal.getScreenUdpPort();
        startVoiceStreaming(signal.getHost(), signal.getUdpPort());
    }

    private void handleCallRejected(CallSignal signal) {
        if (outgoingCallId == null || !outgoingCallId.equals(signal.getCallId())) {
            return;
        }

        cleanupCallState(false);
        view.showInfo("Call was rejected.");
        view.closeCallWindow();
    }

    private void handleRemoteHangup(CallSignal signal) {
        boolean isActive = activeCallId != null && activeCallId.equals(signal.getCallId());
        boolean isOutgoing = outgoingCallId != null && outgoingCallId.equals(signal.getCallId());
        if (!isActive && !isOutgoing) {
            return;
        }

        cleanupCallState(false);
        view.showInfo("Call ended by remote user.");
        view.closeCallWindow();
    }

    private void handleRemoteShareStarted(CallSignal signal) {
        if (activeCallId == null || !activeCallId.equals(signal.getCallId())) {
            return;
        }

        view.updateCallStatus("Connected - Remote is sharing");
    }

    private void handleRemoteShareStopped(CallSignal signal) {
        if (activeCallId == null || !activeCallId.equals(signal.getCallId())) {
            return;
        }

        view.clearRemoteScreenFrame();
        view.updateCallStatus("Connected");
    }

    private void startVoiceStreaming(String host, int port) {
        if (voiceCallSession == null || host == null || host.isBlank() || port <= 0) {
            return;
        }

        try {
            voiceCallSession.start(host, port);
            view.showCallWindow(activeCallPeer != null ? activeCallPeer : outgoingCallPeer, "Connected");
            view.updateCallStatus("Connected");
        } catch (Exception ex) {
            ex.printStackTrace();
            view.showError("Unable to start audio stream.");
            cleanupCallState(true);
        }
    }

    private int ensureVoiceSessionOpened() throws Exception {
        if (voiceCallSession == null) {
            voiceCallSession = new VoiceCallSession();
        }
        return voiceCallSession.open();
    }

    private int ensureScreenSessionOpened() throws Exception {
        if (screenShareSession == null) {
            screenShareSession = new ScreenShareSession();
        }
        return screenShareSession.open(view::showRemoteScreenFrame);
    }

    private void stopLocalScreenShare(boolean notifyPeer) {
        if (!localScreenSharing) {
            view.updateScreenShareButton(false);
            return;
        }

        if (screenShareSession != null) {
            screenShareSession.stopSending();
        }
        localScreenSharing = false;
        view.updateScreenShareButton(false);
        view.updateCallStatus("Connected");

        if (notifyPeer && activeCallId != null && activeCallPeer != null) {
            sendCallSignal(new CallSignal(
                    activeCallId,
                    CallAction.SHARE_STOP,
                    me.getUsername(),
                    me.getNickname(),
                    activeCallPeer.getUsername(),
                    null,
                    0,
                    0
            ));
        }
    }

    private void cleanupCallState(boolean notifyPeer) {
        if (isEndingCall) {
            return;
        }
        isEndingCall = true;

        try {
            if (notifyPeer) {
                String target = activeCallPeer != null
                        ? activeCallPeer.getUsername()
                        : (outgoingCallPeer != null ? outgoingCallPeer.getUsername() : null);
                String callId = activeCallId != null ? activeCallId : outgoingCallId;
                if (target != null && callId != null) {
                    sendCallSignal(new CallSignal(
                            callId,
                            CallAction.HANGUP,
                            me.getUsername(),
                            me.getNickname(),
                            target,
                            null,
                            0,
                            0
                    ));
                }
            }

            stopLocalScreenShare(false);

            if (screenShareSession != null) {
                screenShareSession.stop();
                screenShareSession = null;
            }

            if (voiceCallSession != null) {
                voiceCallSession.stop();
                voiceCallSession = null;
            }

            remoteMediaHost = null;
            remoteScreenPort = 0;
            localScreenSharing = false;
            activeCallId = null;
            activeCallPeer = null;
            outgoingCallId = null;
            outgoingCallPeer = null;
            view.clearRemoteScreenFrame();
            view.updateScreenShareButton(false);
            view.closeCallWindow();
        } finally {
            isEndingCall = false;
        }
    }

    private void sendCallSignal(CallSignal signal) {
        if (client == null || signal == null) {
            return;
        }
        client.sendData(new DataPacket(TypeDataPacket.CALL_SIGNAL, signal));
    }
}
