package dkvs.messages.replicaAdressed;

import dkvs.messages.Command;

public class Decision extends ReplicaMessage {
    private int slot;
    private Command request;

    public Decision(int slot, Command command) {
        super();
        this.slot = slot;
        this.request = command;
    }

    public int getSlot() {
        return slot;
    }

    public Command getRequest() {
        return request;
    }

    @Override
    public String toString() {
        return String.format("decision %d %s", slot, request);
    }

}
