package dkvs.actors;

import dkvs.Ballot;
import dkvs.Config;
import dkvs.messages.Command;
import dkvs.messages.Proposal;
import dkvs.messages.acceptorAdressed.PhaseOneRequest;
import dkvs.messages.acceptorAdressed.PhaseTwoRequest;
import dkvs.messages.leaderAdressed.LeaderMessage;
import dkvs.messages.leaderAdressed.PhaseOneResponse;
import dkvs.messages.leaderAdressed.PhaseTwoResponse;
import dkvs.messages.leaderAdressed.ProposeMessage;
import dkvs.messages.replicaAdressed.Decision;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class Leader {
    private Ballot currentBallot;
    private int id;
    private ActorSystem actorSystem;
    private List<Integer> acceptorIds;
    private HashMap<Integer, Command> proposals;

    private HashMap<Proposal, Commander> commanders;
    private HashMap<Ballot, Scout> scouts;

    private int currentLeader = -1;

    public Leader(int id, ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
        this.id = id;
        this.acceptorIds = Config.getIds();
        proposals = new HashMap<>();
        currentBallot = new Ballot(0, id);
        commanders = new HashMap<>();
        scouts = new HashMap<>();
    }


    private void log(String message) {
        System.out.println("Leader " + id + " " + message);
    }

    public void startLeader() {
        startScouting(currentBallot);
    }

    public void receiveMessage(LeaderMessage message) {
        log("pushed message [" + message + "]");

        if (message instanceof ProposeMessage) {
            if (!proposals.containsKey(((ProposeMessage) message).getSlot())) {
                proposals.put(((ProposeMessage) message).getSlot(), ((ProposeMessage) message).getRequest());
                if (currentLeader == id) {
                    command(new Proposal(currentBallot, ((ProposeMessage) message).getSlot(),
                            ((ProposeMessage) message).getRequest()));
                } else {
                    log("IS NOT active.");
                }
            } else {
                log("slot " + ((ProposeMessage) message).getSlot() + " already used!");
            }
        } else if (message instanceof PhaseOneResponse) {
            Ballot ballot = ((PhaseOneResponse) message).getOriginalBallot();
            Scout scout = scouts.get(ballot);
            scout.receiveResponse((PhaseOneResponse) message);
        } else if (message instanceof PhaseTwoResponse) {
            Proposal proposal = ((PhaseTwoResponse) message).getProposal();
            Commander commander = commanders.get(proposal);
            commander.receiveResponse((PhaseTwoResponse) message);
        }
    }

    private void preempted(Ballot b) {
        log("PREEMPTED: there's ballot " + b);
        if (currentBallot.less(b)) {
            log("WAITING for " + b.getLeaderId() + " to fail");
            currentLeader = b.getLeaderId();
            currentBallot = new Ballot(b.getBallotNum(), id);
        }
    }

    private void adopted(Ballot ballot, Map<Integer, Proposal> pvalues) {
        log("ADOPTED with ballot " + ballot);

        for (Map.Entry<Integer, Proposal> entry : pvalues.entrySet()) {
            Integer key = entry.getKey();
            Proposal value = entry.getValue();
            proposals.put(key, value.getCommand());
        }
        currentLeader = id;
        for (Map.Entry<Integer, Command> entry : proposals.entrySet()) {
            Integer key = entry.getKey();
            Command value = entry.getValue();
            command(new Proposal(ballot, key, value));
        }
    }

    private void startScouting(Ballot ballot) {
        scouts.put(ballot, new Scout(currentBallot));
        actorSystem.sendToAll(new PhaseOneRequest(id, ballot));
    }

    private void command(Proposal proposal) {
        log("COMMANDER started for " + proposal);
        commanders.put(proposal, new Commander(proposal));
        actorSystem.sendToAll(new PhaseTwoRequest(id, proposal));
    }

    public void notifyFault(int fault) {
        if (currentLeader != id && fault == currentLeader) {
            startScouting(currentBallot);
        }
    }

    private class Scout {
        HashSet<Integer> waitFor;
        HashMap<Integer, Proposal> proposals;
        Ballot b;

        Scout(Ballot b) {
            this.b = b;
            waitFor = new HashSet<>(acceptorIds);
            proposals = new HashMap<>();
        }

        void receiveResponse(PhaseOneResponse response) {
            if (response.getBallotNum().equals(b)) {
                response.getPvalues().forEach(r ->
                        {
                            if ((!proposals.containsKey(r.getSlot())) ||
                                    proposals.get(r.getSlot()).getBallotNum().less(r.getBallotNum())
                                    )
                                proposals.put(r.getSlot(), r);
                        }
                );
                waitFor.remove(response.getSource());

                if (waitFor.size() < (acceptorIds.size() + 1) / 2) {
                    adopted(b, proposals);
                }
            } else {
                preempted(response.getBallotNum());
            }
        }
    }

    private class Commander {
        Proposal proposal;
        HashSet<Integer> waitFor;

        Commander(Proposal proposal) {
            this.proposal = proposal;
            this.waitFor = new HashSet<>(acceptorIds);
        }

        void receiveResponse(PhaseTwoResponse response) {
            if (response.getBallot().equals(currentBallot)) {
                waitFor.remove(response.getSource());
                if (waitFor.size() < (acceptorIds.size() + 1) / 2) {
                    actorSystem.sendToAll(new Decision(response.getProposal().getSlot(), response.getProposal().getCommand()));
                }
            } else {
                preempted(response.getBallot());
            }
        }
    }

}



