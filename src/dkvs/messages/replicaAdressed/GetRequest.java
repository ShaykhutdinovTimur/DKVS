package dkvs.messages.replicaAdressed;

public class GetRequest extends ClientRequest {
    private String key;

    public GetRequest(int fromId, String key) {
        this.fromId = fromId;
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        return "get " + fromId + " " + key;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof GetRequest) {
            if (this.toString().equals(other.toString()))
                return true;
        }
        return false;
    }
}
