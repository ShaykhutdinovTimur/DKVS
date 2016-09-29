package dkvs.messages.replicaAdressed;

public class SetRequest extends ClientRequest {
    private String key;
    private String value;

    public SetRequest(int fromId, String key, String value) {
        this.fromId = fromId;
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "set " + fromId + " " + key + " " + value;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof SetRequest) {
            if (this.toString().equals(other.toString()))
                return true;
        }
        return false;
    }
}
