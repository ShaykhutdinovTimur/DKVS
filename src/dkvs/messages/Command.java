package dkvs.messages;

import dkvs.Config;
import dkvs.messages.replicaAdressed.ClientRequest;

import java.util.Arrays;
import java.util.stream.Stream;

public class Command {
    private static volatile int nextId = 0;
    private ClientRequest request;
    private int operationId;

    public Command(int nodeId, ClientRequest request) {
        int curId = get();
        this.operationId = curId * Config.getNodesCount() + nodeId;
        this.request = request;
    }

    private Command(ClientRequest request, int operationId) {
        this.operationId = operationId;
        this.request = request;
    }

    private static synchronized int get() {
        return nextId++;
    }

    public static Command parse(String[] parts) {
        String[] tail = Arrays.copyOfRange(parts, 3, parts.length);
        String[] head = new String[1];
        head[0] = parts[1];
        String[] both = Stream.concat(Arrays.stream(head), Arrays.stream(tail))
                .toArray(String[]::new);
        return new Command(ClientRequest.parse(Integer.parseInt(parts[2]), both),
                Integer.parseInt(parts[0].substring(1, parts[0].length() - 1)));

    }

    public ClientRequest getRequest() {
        return request;
    }

    @Override
    public String toString() {
        return "<" + operationId + "> " + request.toString();
    }

    @Override
    public boolean equals(Object other) {
        return this.toString().equals(other.toString());
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
}
