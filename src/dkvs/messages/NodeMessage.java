package dkvs.messages;

public class NodeMessage extends Message {

    public NodeMessage(int fromId) {
        this.fromId = fromId;
    }

    @Override
    public String toString() {
        return String.format("node %d", fromId);
    }
}
