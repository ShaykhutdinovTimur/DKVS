package dkvs.messages;

public class DisconnectMessage extends Message {
    public DisconnectMessage(int i) {
        this.fromId = i;
    }

    @Override
    public String toString() {
        return "disconnect " + fromId;
    }
}
