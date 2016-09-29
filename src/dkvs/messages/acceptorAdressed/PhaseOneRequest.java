package dkvs.messages.acceptorAdressed;

import dkvs.Ballot;

public class PhaseOneRequest extends AcceptorMessage {
    public PhaseOneRequest(int fromId, Ballot ballotNum) {
        super(fromId, ballotNum);
    }

    @Override
    public String toString() {
        return "p1a " + fromId + " " + ballotNum;
    }
}
