package dkvs.messages.leaderAdressed;

import dkvs.messages.Command;

public class ProposeMessage extends LeaderMessage {
    private int slot;
    private Command request;

    public ProposeMessage(int fromId, int slot, Command command) {
        super(fromId);
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
        return "propose " + fromId + " " + slot + " " + request;
    }
}
