package dkvs.messages.acceptorAdressed;

import dkvs.Ballot;

public class PhaseOneRequest extends AcceptorMessage {
    public PhaseOneRequest(int fromId, Ballot ballotNum) {
        super(fromId, ballotNum);
    }

    @Override
    public String toString() {
        return String.format("p1a %d %s", fromId, ballotNum);
    }
}
