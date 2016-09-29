package dkvs.messages.acceptorAdressed;

import dkvs.messages.Proposal;

public class PhaseTwoRequest extends AcceptorMessage {
    private Proposal payload;

    public PhaseTwoRequest(int fromId, Proposal payload) {
        super(fromId, payload.getBallotNum());
        this.payload = payload;
    }

    public Proposal getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "p2a " + fromId + " " + payload;
    }
}
