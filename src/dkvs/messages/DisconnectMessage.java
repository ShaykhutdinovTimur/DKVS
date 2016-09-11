package dkvs.messages;

public class DisconnectMessage extends Message {
    public DisconnectMessage(int i) {
        this.fromId = i;
    }
}
