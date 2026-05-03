package org.proptit.localchat.common.models.call;

import java.io.Serializable;

public class RoomCallSignal implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int roomId;
    private final CallSignal signal;

    public RoomCallSignal(int roomId, CallSignal signal) {
        this.roomId = roomId;
        this.signal = signal;
    }

    public int getRoomId() {
        return roomId;
    }

    public CallSignal getSignal() {
        return signal;
    }
}
