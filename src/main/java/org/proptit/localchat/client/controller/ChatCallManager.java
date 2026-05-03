package org.proptit.localchat.client.controller;

import javafx.application.Platform;
import org.proptit.localchat.client.networks.ScreenShareSession;
import org.proptit.localchat.client.networks.SocketClient;
import org.proptit.localchat.client.networks.VideoCallSession;
import org.proptit.localchat.client.networks.VoiceCallSession;
import org.proptit.localchat.common.enums.TypeDataPacket;
import org.proptit.localchat.common.models.ChatGroup;
import org.proptit.localchat.common.models.DataPacket;
import org.proptit.localchat.common.models.User;
import org.proptit.localchat.common.models.call.CallAction;
import org.proptit.localchat.common.models.call.CallSignal;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatCallManager {
    private static final String GROUP_TARGET_PREFIX = "__group__:";

    private final SocketClient client;
    private final User me;
    private final ChatCallView view;

    private VoiceCallSession voiceCallSession;
    private ScreenShareSession screenShareSession;
    private VideoCallSession videoCallSession;

    private final Map<String, User> remoteParticipants = new ConcurrentHashMap<>();
    private final Map<String, RemoteEndpoint> endpointsByUsername = new ConcurrentHashMap<>();

    private String activeCallId;
    private String outgoingCallId;
    private User activeCallPeer;
    private User outgoingCallPeer;

    private boolean activeGroupCall;
    private Integer activeGroupId;
    private String activeGroupName;
    private List<String> activeGroupRoster = new ArrayList<>();

    private int localVoicePort;
    private int localScreenPort;
    private int localVideoPort;

    private boolean localScreenSharing;
    private boolean localVideoSending;
    private boolean videoCallEnabled;
    private boolean autoStartVideoWhenConnected;
    private boolean isEndingCall;

    public ChatCallManager(SocketClient client, User me, ChatCallView view) {
        this.client = client;
        this.me = me;
        this.view = view;
    }

    public void startOutgoingCall(User selectedConversationUser) {
        startOutgoingDirectCall(selectedConversationUser, false);
    }

    public void startOutgoingVideoCall(User selectedConversationUser) {
        startOutgoingDirectCall(selectedConversationUser, true);
    }

    public void startOutgoingGroupCall(ChatGroup selectedGroup) {
        startOutgoingGroupCall(selectedGroup, false);
    }

    public void startOutgoingGroupVideoCall(ChatGroup selectedGroup) {
        startOutgoingGroupCall(selectedGroup, true);
    }

    private void startOutgoingDirectCall(User selectedConversationUser, boolean asVideoCall) {
        if (selectedConversationUser == null) {
            view.showInfo("Please select a user to start a call.");
            return;
        }

        if (activeCallId != null || outgoingCallId != null) {
            view.showInfo("A call is already in progress.");
            return;
        }

        String callId = UUID.randomUUID().toString();
        activeCallId = callId;
        outgoingCallId = callId;
        outgoingCallPeer = selectedConversationUser;
        activeCallPeer = selectedConversationUser;

        activeGroupCall = false;
        activeGroupId = null;
        activeGroupName = null;
        activeGroupRoster = new ArrayList<>();
        autoStartVideoWhenConnected = asVideoCall;

        view.showCallWindow(selectedConversationUser, "Preparing call...");
        refreshCallParticipants();
        startOutgoingCallInitialization(callId, () -> sendCallSignal(new CallSignal(
                callId,
                CallAction.INVITE,
                me.getUsername(),
                me.getNickname(),
                selectedConversationUser.getUsername(),
                view.resolveLocalAddress(),
                localVoicePort,
                localScreenPort,
                localVideoPort
        )));
    }

    private void startOutgoingGroupCall(ChatGroup selectedGroup, boolean asVideoCall) {
        if (selectedGroup == null) {
            view.showInfo("Please select a group to start a call.");
            return;
        }

        if (activeCallId != null || outgoingCallId != null) {
            view.showInfo("A call is already in progress.");
            return;
        }

        String callId = UUID.randomUUID().toString();
        activeCallId = callId;
        outgoingCallId = callId;
        outgoingCallPeer = null;

        activeGroupCall = true;
        activeGroupId = selectedGroup.getId();
        activeGroupName = selectedGroup.getName();
        activeGroupRoster = buildRosterFromGroup(selectedGroup);
        activeCallPeer = buildGroupDisplayUser(activeGroupId, activeGroupName);
        autoStartVideoWhenConnected = asVideoCall;

        view.showCallWindow(activeCallPeer, "Preparing group call...");
        refreshCallParticipants();
        startOutgoingCallInitialization(callId, () -> sendCallSignal(new CallSignal(
                callId,
                CallAction.INVITE,
                me.getUsername(),
                me.getNickname(),
                groupTarget(activeGroupId),
                view.resolveLocalAddress(),
                localVoicePort,
                localScreenPort,
                localVideoPort
        )));
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
            case VIDEO_START:
                handleRemoteVideoStarted(signal);
                break;
            case VIDEO_STOP:
                handleRemoteVideoStopped(signal);
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
        if (activeCallId == null) {
            view.updateScreenShareButton(false);
            return;
        }

        if (sharing) {
            List<InetSocketAddress> screenTargets = buildRemoteTargets(false, true, false);
            if (screenShareSession == null || screenTargets.isEmpty()) {
                view.showInfo("Screen sharing is not ready yet.");
                view.updateScreenShareButton(false);
                return;
            }

            screenShareSession.setRemoteTargets(screenTargets);
            screenShareSession.startSending();
            localScreenSharing = true;
            broadcastInCall(CallAction.SHARE_START, localVideoPort);
            view.updateCallStatus(activeGroupCall
                    ? "Connected (" + remoteParticipants.size() + ") - Sharing screen"
                    : "Connected - Sharing screen");
            return;
        }

        stopLocalScreenShare(true);
    }

    public void setVideoStreaming(boolean active) {
        if (activeCallId == null || !videoCallEnabled) {
            view.setVideoCallActive(false);
            return;
        }

        if (active) {
            startLocalVideoStreaming(true);
            return;
        }

        stopLocalVideoStreaming(true);
    }

    private void handleIncomingInvite(CallSignal signal) {
        boolean alreadyInAnotherCall = (activeCallId != null || outgoingCallId != null)
                && (activeCallId == null || !activeCallId.equals(signal.getCallId()));
        if (alreadyInAnotherCall) {
            sendCallSignal(new CallSignal(
                    signal.getCallId(),
                    CallAction.REJECT,
                    me.getUsername(),
                    me.getNickname(),
                    signal.getFromUsername(),
                    null,
                    0,
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
                    0,
                    0
            ));
            return;
        }

        Integer groupId = parseGroupId(signal.getToUsername());
        boolean isGroupInvite = groupId != null;

        try {
            prepareLocalMedia();
            activeCallId = signal.getCallId();
            outgoingCallId = null;
            outgoingCallPeer = null;
            autoStartVideoWhenConnected = signal.getVideoUdpPort() > 0;

            activeGroupCall = isGroupInvite;
            activeGroupId = groupId;
            activeGroupName = isGroupInvite ? "Group #" + groupId : null;

            if (isGroupInvite) {
                activeCallPeer = buildGroupDisplayUser(groupId, activeGroupName);
                view.showCallWindow(activeCallPeer, "Joining group call...");
            } else {
                activeCallPeer = caller;
                view.showCallWindow(caller, "Connecting...");
            }

            addOrUpdateRemoteEndpoint(caller.getUsername(), signal.getHost(), signal.getUdpPort(), signal.getScreenUdpPort(), signal.getVideoUdpPort());
            remoteParticipants.put(caller.getUsername(), caller);
            refreshCallParticipants();
            refreshMediaTargets();
            updateConnectedStatus();

            String acceptTarget = isGroupInvite ? groupTarget(groupId) : signal.getFromUsername();
            sendCallSignal(new CallSignal(
                    signal.getCallId(),
                    CallAction.ACCEPT,
                    me.getUsername(),
                    me.getNickname(),
                    acceptTarget,
                    view.resolveLocalAddress(),
                    localVoicePort,
                    localScreenPort,
                    localVideoPort
            ));

            if (isGroupInvite) {
                sendCallSignal(new CallSignal(
                        signal.getCallId(),
                        CallAction.READY,
                        me.getUsername(),
                        me.getNickname(),
                        signal.getFromUsername(),
                        view.resolveLocalAddress(),
                        localVoicePort,
                        localScreenPort,
                        localVideoPort
                ));
            }

            if (autoStartVideoWhenConnected) {
                startLocalVideoStreaming(false);
            }
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
                    0,
                    0
            ));
        }
    }

    private void handleCallAccepted(CallSignal signal) {
        if (!isSameCall(signal.getCallId())) {
            return;
        }

        User peer = view.resolveUser(signal.getFromUsername(), signal.getFromNickname());
        if (peer == null || peer.getUsername() == null || peer.getUsername().equalsIgnoreCase(me.getUsername())) {
            return;
        }

        remoteParticipants.put(peer.getUsername(), peer);
        addOrUpdateRemoteEndpoint(peer.getUsername(), signal.getHost(), signal.getUdpPort(), signal.getScreenUdpPort(), signal.getVideoUdpPort());
        refreshMediaTargets();

        if (!activeGroupCall) {
            outgoingCallId = null;
            activeCallPeer = peer;
            view.showCallWindow(peer, "Connecting...");
        } else if (outgoingCallId != null) {
            outgoingCallId = null;
        }

        refreshCallParticipants();

        sendCallSignal(new CallSignal(
                signal.getCallId(),
                CallAction.READY,
                me.getUsername(),
                me.getNickname(),
                peer.getUsername(),
                view.resolveLocalAddress(),
                localVoicePort,
                localScreenPort,
                localVideoPort
        ));

        if (autoStartVideoWhenConnected && !localVideoSending) {
            startLocalVideoStreaming(false);
        }

        updateConnectedStatus();
    }

    private void handleCallReady(CallSignal signal) {
        if (!isSameCall(signal.getCallId())) {
            return;
        }

        User peer = view.resolveUser(signal.getFromUsername(), signal.getFromNickname());
        if (peer == null || peer.getUsername() == null || peer.getUsername().equalsIgnoreCase(me.getUsername())) {
            return;
        }

        remoteParticipants.put(peer.getUsername(), peer);
        addOrUpdateRemoteEndpoint(peer.getUsername(), signal.getHost(), signal.getUdpPort(), signal.getScreenUdpPort(), signal.getVideoUdpPort());
        refreshMediaTargets();

        if (!activeGroupCall) {
            outgoingCallId = null;
            activeCallPeer = peer;
            view.showCallWindow(peer, "Connected");
        }

        refreshCallParticipants();

        if (autoStartVideoWhenConnected && !localVideoSending) {
            startLocalVideoStreaming(false);
        }

        updateConnectedStatus();
    }

    private void handleCallRejected(CallSignal signal) {
        if (outgoingCallId == null || !outgoingCallId.equals(signal.getCallId())) {
            return;
        }

        if (activeGroupCall) {
            String nickname = signal.getFromNickname() != null && !signal.getFromNickname().isBlank()
                    ? signal.getFromNickname()
                    : signal.getFromUsername();
            if (nickname != null && !nickname.isBlank()) {
                view.showInfo(nickname + " declined the group call.");
            }
            return;
        }

        cleanupCallState(false);
        view.showInfo("Call was rejected.");
        view.closeCallWindow();
    }

    private void handleRemoteHangup(CallSignal signal) {
        if (!isSameCall(signal.getCallId())) {
            return;
        }

        if (!activeGroupCall) {
            cleanupCallState(false);
            view.showInfo("Call ended by remote user.");
            view.closeCallWindow();
            return;
        }

        String username = signal.getFromUsername();
        if (username != null) {
            remoteParticipants.remove(username);
            endpointsByUsername.remove(username);
            refreshMediaTargets();
        }

        refreshCallParticipants();

        updateConnectedStatus();

        if (remoteParticipants.isEmpty() && outgoingCallId == null) {
            view.updateCallStatus("Waiting for participants...");
            stopLocalScreenShare(false);
            if (!localVideoSending) {
                view.clearRemoteScreenFrame();
            }
        }
    }

    private void handleRemoteShareStarted(CallSignal signal) {
        if (!isSameCall(signal.getCallId())) {
            return;
        }

        if (activeGroupCall) {
            view.updateCallStatus("Connected (" + remoteParticipants.size() + ") - Remote is sharing");
            return;
        }
        view.updateCallStatus("Connected - Remote is sharing");
    }

    private void handleRemoteShareStopped(CallSignal signal) {
        if (!isSameCall(signal.getCallId())) {
            return;
        }

        view.clearRemoteScreenFrame();
        updateConnectedStatus();
    }

    private void handleRemoteVideoStarted(CallSignal signal) {
        if (!isSameCall(signal.getCallId())) {
            return;
        }

        if (signal.getFromUsername() != null && !signal.getFromUsername().equalsIgnoreCase(me.getUsername())) {
            addOrUpdateRemoteEndpoint(
                    signal.getFromUsername(),
                    signal.getHost(),
                    0,
                    0,
                    signal.getVideoUdpPort());
            refreshMediaTargets();
        }

        if (activeGroupCall) {
            view.updateCallStatus("Connected (" + remoteParticipants.size() + ") - Video");
            return;
        }
        view.updateCallStatus("Connected - Video");
    }

    private void handleRemoteVideoStopped(CallSignal signal) {
        if (!isSameCall(signal.getCallId())) {
            return;
        }

        if (signal.getFromUsername() != null) {
            RemoteEndpoint endpoint = endpointsByUsername.get(signal.getFromUsername());
            if (endpoint != null) {
                endpoint.videoPort = 0;
                refreshMediaTargets();
            }
        }

        if (!localVideoSending) {
            view.clearRemoteScreenFrame();
        }
        updateConnectedStatus();
    }

    private void prepareLocalMedia() throws Exception {
        localVoicePort = ensureVoiceSessionOpened();
        localScreenPort = ensureScreenSessionOpened();
        localVideoPort = ensureVideoSessionOpened();
        videoCallEnabled = true;

        voiceCallSession.start();
        view.setVideoCallAvailable(true);
        view.setVideoCallActive(localVideoSending);
    }

    private void startOutgoingCallInitialization(String callId, Runnable onReady) {
        Thread initThread = new Thread(() -> {
            try {
                prepareLocalMedia();
                Platform.runLater(() -> {
                    if (!isSameCall(callId)) {
                        return;
                    }
                    refreshCallParticipants();
                    updateConnectedStatus();
                    if (onReady != null) {
                        onReady.run();
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    if (!isSameCall(callId)) {
                        return;
                    }
                    cleanupCallState(false);
                    view.showError("Unable to access call devices.");
                });
            }
        }, "call-init");
        initThread.setDaemon(true);
        initThread.start();
    }

    private int ensureVoiceSessionOpened() throws Exception {
        if (voiceCallSession == null) {
            voiceCallSession = new VoiceCallSession();
        }
        return voiceCallSession.open();
    }

    private int ensureVideoSessionOpened() throws Exception {
        if (videoCallSession == null) {
            videoCallSession = new VideoCallSession();
        }
        return videoCallSession.open(view::showRemoteVideoFrame);
    }

    private int ensureScreenSessionOpened() throws Exception {
        if (screenShareSession == null) {
            screenShareSession = new ScreenShareSession();
        }
        return screenShareSession.open(view::showRemoteScreenFrame);
    }

    private void startLocalVideoStreaming(boolean notifyUserOnFailure) {
        if (!videoCallEnabled || videoCallSession == null) {
            view.setVideoCallActive(false);
            return;
        }

        try {
            videoCallSession.start(view::showLocalVideoFrame);
            localVideoSending = true;
            view.setVideoCallActive(true);
            refreshMediaTargets();
        } catch (Exception ex) {
            ex.printStackTrace();
            if (notifyUserOnFailure) {
                view.showError("Unable to start video stream.");
            }
            view.setVideoCallActive(false);
            return;
        }

        broadcastInCall(CallAction.VIDEO_START, localVideoPort);
        if (activeGroupCall) {
            view.updateCallStatus("Connected (" + remoteParticipants.size() + ") - Video");
        } else {
            view.updateCallStatus("Connected - Video");
        }
    }

    private void stopLocalVideoStreaming(boolean notifyPeer) {
        if (!localVideoSending) {
            view.setVideoCallActive(false);
            return;
        }

        if (videoCallSession != null) {
            videoCallSession.stopSending();
        }
        localVideoSending = false;
        view.setVideoCallActive(false);
        view.clearLocalVideoFrame();

        if (notifyPeer) {
            broadcastInCall(CallAction.VIDEO_STOP, localVideoPort);
        }
        updateConnectedStatus();
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

        if (notifyPeer) {
            broadcastInCall(CallAction.SHARE_STOP, localVideoPort);
        }
        updateConnectedStatus();
    }

    private void refreshMediaTargets() {
        if (voiceCallSession != null) {
            voiceCallSession.setRemoteTargets(buildRemoteTargets(true, false, false));
        }
        if (videoCallSession != null) {
            videoCallSession.setRemoteTargets(buildRemoteTargets(false, false, true));
        }
        if (screenShareSession != null && localScreenSharing) {
            screenShareSession.setRemoteTargets(buildRemoteTargets(false, true, false));
        }
    }

    private List<InetSocketAddress> buildRemoteTargets(boolean requireVoice, boolean requireScreen, boolean requireVideo) {
        List<InetSocketAddress> targets = new ArrayList<>();
        for (RemoteEndpoint endpoint : endpointsByUsername.values()) {
            if (endpoint == null || endpoint.host == null || endpoint.host.isBlank()) {
                continue;
            }

            int port = endpoint.voicePort;
            if (requireScreen) {
                port = endpoint.screenPort;
            } else if (requireVideo) {
                port = endpoint.videoPort;
            }

            if ((requireVoice || requireScreen || requireVideo) && port <= 0) {
                continue;
            }

            try {
                InetAddress address = InetAddress.getByName(endpoint.host);
                targets.add(new InetSocketAddress(address, port));
            } catch (Exception ignore) {
                // ignore invalid participant endpoint
            }
        }
        return targets;
    }

    private void addOrUpdateRemoteEndpoint(String username, String host, int voicePort, int screenPort, int videoPort) {
        if (username == null || username.isBlank()) {
            return;
        }

        RemoteEndpoint endpoint = endpointsByUsername.computeIfAbsent(username, ignored -> new RemoteEndpoint());
        if (host != null && !host.isBlank()) {
            endpoint.host = host;
        }
        if (voicePort > 0) {
            endpoint.voicePort = voicePort;
        }
        if (screenPort > 0) {
            endpoint.screenPort = screenPort;
        }
        if (videoPort > 0) {
            endpoint.videoPort = videoPort;
        }
    }

    private boolean isSameCall(String callId) {
        if (callId == null) {
            return false;
        }
        if (activeCallId != null && activeCallId.equals(callId)) {
            return true;
        }
        return outgoingCallId != null && outgoingCallId.equals(callId);
    }

    private void updateConnectedStatus() {
        if (activeGroupCall) {
            int participantCount = remoteParticipants.size();
            if (outgoingCallId != null && participantCount == 0) {
                view.updateCallStatus("Calling group...");
                return;
            }

            if (localScreenSharing) {
                view.updateCallStatus("Connected (" + participantCount + ") - Sharing screen");
                return;
            }

            if (localVideoSending) {
                view.updateCallStatus("Connected (" + participantCount + ") - Video");
                return;
            }

            view.updateCallStatus("Connected (" + participantCount + ")");
            return;
        }

        if (localScreenSharing) {
            view.updateCallStatus("Connected - Sharing screen");
            return;
        }
        if (localVideoSending) {
            view.updateCallStatus("Connected - Video");
            return;
        }
        view.updateCallStatus("Connected");
    }

    private void refreshCallParticipants() {
        view.updateCallParticipants(buildParticipantLabels());
    }

    private List<String> buildParticipantLabels() {
        List<String> labels = new ArrayList<>();
        labels.add("You");

        if (activeGroupCall) {
            if (remoteParticipants.isEmpty() && !activeGroupRoster.isEmpty()) {
                for (String label : activeGroupRoster) {
                    if (label != null && !label.isBlank() && !labels.contains(label)) {
                        labels.add(label);
                    }
                }
                return labels;
            }

            Set<String> participantNames = new LinkedHashSet<>();
            for (User participant : remoteParticipants.values()) {
                String displayName = participantDisplayName(participant);
                if (displayName != null && !displayName.isBlank()) {
                    participantNames.add(displayName);
                }
            }
            labels.addAll(participantNames);
            return labels;
        }

        String peerDisplayName = participantDisplayName(activeCallPeer != null ? activeCallPeer : outgoingCallPeer);
        if (peerDisplayName != null && !peerDisplayName.isBlank()) {
            labels.add(peerDisplayName);
        }
        return labels;
    }

    private List<String> buildRosterFromGroup(ChatGroup selectedGroup) {
        List<String> roster = new ArrayList<>();
        if (selectedGroup == null || selectedGroup.getMembers() == null) {
            return roster;
        }

        for (User member : selectedGroup.getMembers()) {
            String displayName = participantDisplayName(member);
            if (displayName != null && !displayName.isBlank() && !roster.contains(displayName)) {
                roster.add(displayName);
            }
        }
        return roster;
    }

    private String participantDisplayName(User participant) {
        if (participant == null) {
            return null;
        }
        if (participant.getNickname() != null && !participant.getNickname().isBlank()) {
            return participant.getNickname();
        }
        return participant.getUsername();
    }

    private void broadcastInCall(CallAction action, int videoPort) {
        if (activeCallId == null || action == null) {
            return;
        }

        String target;
        if (activeGroupCall && activeGroupId != null) {
            target = groupTarget(activeGroupId);
        } else {
            target = activeCallPeer != null
                    ? activeCallPeer.getUsername()
                    : (outgoingCallPeer != null ? outgoingCallPeer.getUsername() : null);
        }

        if (target == null || target.isBlank()) {
            return;
        }

        sendCallSignal(new CallSignal(
                activeCallId,
                action,
                me.getUsername(),
                me.getNickname(),
                target,
                view.resolveLocalAddress(),
                localVoicePort,
                localScreenPort,
                videoPort
        ));
    }

    private void cleanupCallState(boolean notifyPeer) {
        if (isEndingCall) {
            return;
        }
        isEndingCall = true;

        try {
            if (notifyPeer && activeCallId != null) {
                if (activeGroupCall && activeGroupId != null) {
                    sendCallSignal(new CallSignal(
                            activeCallId,
                            CallAction.HANGUP,
                            me.getUsername(),
                            me.getNickname(),
                            groupTarget(activeGroupId),
                            null,
                            0,
                            0,
                            0
                    ));
                } else {
                    String target = activeCallPeer != null
                            ? activeCallPeer.getUsername()
                            : (outgoingCallPeer != null ? outgoingCallPeer.getUsername() : null);
                    if (target != null) {
                        sendCallSignal(new CallSignal(
                                activeCallId,
                                CallAction.HANGUP,
                                me.getUsername(),
                                me.getNickname(),
                                target,
                                null,
                                0,
                                0,
                                0
                        ));
                    }
                }
            }

            stopLocalScreenShare(false);

            if (screenShareSession != null) {
                screenShareSession.stop();
                screenShareSession = null;
            }

            if (videoCallSession != null) {
                videoCallSession.stop();
                videoCallSession = null;
            }

            if (voiceCallSession != null) {
                voiceCallSession.stop();
                voiceCallSession = null;
            }

            remoteParticipants.clear();
            endpointsByUsername.clear();

            localVoicePort = 0;
            localScreenPort = 0;
            localVideoPort = 0;

            localScreenSharing = false;
            localVideoSending = false;
            videoCallEnabled = false;
            autoStartVideoWhenConnected = false;

            activeCallId = null;
            outgoingCallId = null;
            activeCallPeer = null;
            outgoingCallPeer = null;

            activeGroupCall = false;
            activeGroupId = null;
            activeGroupName = null;

            view.clearRemoteScreenFrame();
            view.clearLocalVideoFrame();
            view.updateScreenShareButton(false);
            view.setVideoCallAvailable(false);
            view.setVideoCallActive(false);
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

    private String groupTarget(int groupId) {
        return GROUP_TARGET_PREFIX + groupId;
    }

    private Integer parseGroupId(String target) {
        if (target == null || !target.startsWith(GROUP_TARGET_PREFIX)) {
            return null;
        }
        try {
            return Integer.parseInt(target.substring(GROUP_TARGET_PREFIX.length()));
        } catch (Exception ex) {
            return null;
        }
    }

    private User buildGroupDisplayUser(Integer groupId, String groupName) {
        String displayName = (groupName != null && !groupName.isBlank()) ? groupName : ("Group #" + groupId);
        User groupUser = new User("group-" + groupId);
        groupUser.setNickname(displayName);
        return groupUser;
    }

    private static class RemoteEndpoint {
        private String host;
        private int voicePort;
        private int screenPort;
        private int videoPort;
    }
}
