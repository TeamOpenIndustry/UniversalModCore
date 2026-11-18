package cam72cam.mod.net;

public enum PacketDirection {
    ClientToServer,
    ServerToClient,
    Bidirectional
    ;

    public boolean canSendToClient() {
        return this == ServerToClient || this == Bidirectional;
    }
    public boolean canSendToServer() {
        return this == ClientToServer || this == Bidirectional;
    }
}
