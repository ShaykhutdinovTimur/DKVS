package dkvs.messages;

public class Ping extends Message {
    public Ping(int fromId) {
        this.fromId = fromId;
    }


    @Override
    public String toString() {
        return "ping from " + fromId;
    }
}
