package org.proptit.localchat.common.models.call;

import java.io.Serializable;

public enum CallAction implements Serializable {
    INVITE,
    ACCEPT,
    REJECT,
    READY,
    HANGUP,
    SHARE_START,
    SHARE_STOP
}
