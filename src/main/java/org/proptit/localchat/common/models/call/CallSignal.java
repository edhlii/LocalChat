package org.proptit.localchat.common.models.call;

import java.io.Serializable;

public class CallSignal implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String callId;
    private final CallAction action;
    private final String fromUsername;
    private final String fromNickname;
    private final String toUsername;
    private final String host;
    private final int udpPort;
    private final int screenUdpPort;

    public CallSignal(String callId, CallAction action, String fromUsername, String fromNickname, String toUsername, String host, int udpPort, int screenUdpPort) {
        this.callId = callId;
        this.action = action;
        this.fromUsername = fromUsername;
        this.fromNickname = fromNickname;
        this.toUsername = toUsername;
        this.host = host;
        this.udpPort = udpPort;
        this.screenUdpPort = screenUdpPort;
    }

    public String getCallId() {
        return callId;
    }

    public CallAction getAction() {
        return action;
    }

    public String getFromUsername() {
        return fromUsername;
    }

    public String getFromNickname() {
        return fromNickname;
    }

    public String getToUsername() {
        return toUsername;
    }

    public String getHost() {
        return host;
    }

    public int getUdpPort() {
        return udpPort;
    }

    public int getScreenUdpPort() {
        return screenUdpPort;
    }
}
