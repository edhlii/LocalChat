package org.proptit.localchat.common.models;

import org.proptit.localchat.common.enums.TypeDataPacket;

import java.io.Serializable;

public class DataPacket implements Serializable {
    private static final long serialVersionUID = 3L;
    private TypeDataPacket typeDataPacket;
    private Object data;

    public DataPacket(TypeDataPacket typeDataPacket, Object data) {
        this.typeDataPacket = typeDataPacket;
        this.data = data;
    }

    public void setTypeDataPacket(TypeDataPacket typeDataPacket) {
        this.typeDataPacket = typeDataPacket;
    }

    public void setData(Object data) {
        this.data = data;
    }
    
    public TypeDataPacket getTypeDataPacket() { return typeDataPacket; }
    public Object getData() { return data; }


}
