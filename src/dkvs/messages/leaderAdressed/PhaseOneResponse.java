package dkvs.messages.leaderAdressed;

import dkvs.Ballot;
import dkvs.messages.Proposal;

import java.util.*;
import java.util.stream.Collectors;

public class PhaseOneResponse extends LeaderMessage {
    private Ballot originalBallot;
    private Ballot ballotNum;
    private Collection<Proposal> pvalues;

    public PhaseOneResponse(int fromId, Ballot originalBallot,
                            Ballot ballotNum, Collection<Proposal> pvalues) {
        super(fromId);
        this.originalBallot = originalBallot;
        this.ballotNum = ballotNum;
        this.pvalues = pvalues;
    }

    public static PhaseOneResponse parse(String[] parts) {
        if (!"p1b".equals(parts[0]))
            throw new IllegalArgumentException("PhaseOneResponse should start by \"p1b\"");

        int fromId = Integer.parseInt(parts[1]);
        Ballot originalBallot = Ballot.parse(parts[2]);
        Ballot ballotNum = Ballot.parse(parts[3]);
        String[] ss = String.join(" ", Arrays.asList(Arrays.copyOfRange(parts, 4, parts.length))).split("#");
        List<Proposal> pvalues = new ArrayList<>(Arrays.asList(ss)).stream()
                .filter(s -> s.length() > 0).map(s -> Proposal.parse(s.split(" ")))
                .collect(Collectors.toList());
        return new PhaseOneResponse(fromId, originalBallot, ballotNum, new LinkedHashSet<>(pvalues));
    }

    public Ballot getOriginalBallot() {
        return originalBallot;
    }

    public Ballot getBallotNum() {
        return ballotNum;
    }

    public Collection<Proposal> getPvalues() {
        return pvalues;
    }

    @Override
    public String toString() {
        return String.format("p1b %d %s %s %s", fromId, originalBallot, ballotNum,
                pvalues.stream().map(Proposal::toString).collect(Collectors.joining("#"))
        );
    }


}
