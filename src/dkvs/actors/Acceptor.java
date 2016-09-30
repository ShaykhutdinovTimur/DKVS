package dkvs.actors;

import dkvs.Ballot;
import dkvs.messages.Proposal;
import dkvs.messages.acceptorAdressed.AcceptorMessage;
import dkvs.messages.acceptorAdressed.PhaseOneRequest;
import dkvs.messages.acceptorAdressed.PhaseTwoRequest;
import dkvs.messages.leaderAdressed.PhaseOneResponse;
import dkvs.messages.leaderAdressed.PhaseTwoResponse;

import java.util.HashMap;

public class Acceptor {
    private int id;
    private Ballot ballotNumber;
    private HashMap<Integer, Proposal> accepted;
    private ActorSystem actorSystem;

    public Acceptor(int id, ActorSystem actorSystem) {
        this.id = id;
        this.actorSystem = actorSystem;
        this.ballotNumber = new Ballot();
        this.accepted = new HashMap<>();
    }

    private void log(String message) {
        System.out.println("Acceptor " + id + " " + message);
    }

    public void receiveMessage(AcceptorMessage message) {
        if (message instanceof PhaseOneRequest) {
            if (ballotNumber.less(message.getBallotNum())) {
                ballotNumber = message.getBallotNum();
                log("adopted " + ballotNumber);
            }
            actorSystem.sendToNode(message.getSource(),
                    new PhaseOneResponse(id, message.getBallotNum(), ballotNumber, accepted.values()));
        } else if (message instanceof PhaseTwoRequest) {
            if (((PhaseTwoRequest) message).getPayload().getBallotNum().equals(ballotNumber)) {
                accepted.put(((PhaseTwoRequest) message).getPayload().getSlot(), ((PhaseTwoRequest) message).getPayload());
                log("accepted " + ballotNumber);
            }
            actorSystem.sendToNode(message.getSource(),
                    new PhaseTwoResponse(id, ballotNumber, ((PhaseTwoRequest) message).getPayload()));
        } else {
            throw new IllegalStateException("Incorrect message");
        }
    }
}


