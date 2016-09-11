package dkvs.messages.leaderAdressed;

import dkvs.Ballot;
import dkvs.messages.Proposal;

public class PhaseTwoResponse extends LeaderMessage {
    private Ballot ballot;
    private Proposal proposal;

    public PhaseTwoResponse(int fromId, Ballot ballot, Proposal proposal) {
        super(fromId);
        this.ballot = ballot;
        this.proposal = proposal;
    }

    public Ballot getBallot() {
        return ballot;
    }

    public Proposal getProposal() {
        return proposal;
    }

    @Override
    public String toString() {
        return String.format("p2b %d %s %s", fromId, ballot, proposal);
    }
}
