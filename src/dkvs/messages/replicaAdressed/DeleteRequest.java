package dkvs.messages.replicaAdressed;

public class DeleteRequest extends ClientRequest {
    private String key;

    public DeleteRequest(int fromId, String key) {
        this.fromId = fromId;
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return "delete " + fromId + " " + key;
    }

}
