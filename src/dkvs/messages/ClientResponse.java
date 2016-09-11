package dkvs.messages;

public class ClientResponse extends Message {
    private String data;

    public ClientResponse(int fromId, String data) {
        this.fromId = fromId;
        this.data = data;
    }

    @Override
    public String toString() {
        return data;
    }
}

