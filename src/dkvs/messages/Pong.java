package dkvs.messages;

public class Pong extends Message {
    public Pong(int fromId) {
        this.fromId = fromId;
    }

    @Override
    public String toString() {
        return "pong";
    }
}
