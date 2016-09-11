package dkvs.messages.acceptorAdressed;

import dkvs.Ballot;
import dkvs.messages.Message;

public abstract class AcceptorMessage extends Message {

    protected Ballot ballotNum;

    public AcceptorMessage(int fromId, Ballot ballotNum) {
        this.ballotNum = ballotNum;
        this.fromId = fromId;
    }

    public Ballot getBallotNum() {
        return ballotNum;
    }
}
