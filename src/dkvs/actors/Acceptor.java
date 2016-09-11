package dkvs.actors;

import dkvs.Ballot;
import dkvs.Logger;
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
    private Logger logger;

    public Acceptor(int id, Logger logger, ActorSystem actorSystem) {
        this.id = id;
        this.logger = logger;
        this.actorSystem = actorSystem;
        this.ballotNumber = new Ballot();
        this.accepted = new HashMap<>();
    }

    public void receiveMessage(AcceptorMessage message) {
        if (message instanceof PhaseOneRequest) {
            if (ballotNumber.less(message.getBallotNum())) {
                ballotNumber = message.getBallotNum();
                logger.paxos("receiveMessage() in acceptor " + id, "ACCEPTOR ADOPTED " + ballotNumber);
            }
            actorSystem.sendToNode(message.getSource(),
                    new PhaseOneResponse(id, message.getBallotNum(), ballotNumber, accepted.values()));
        } else if (message instanceof PhaseTwoRequest) {
            if (((PhaseTwoRequest) message).getPayload().getBallotNum().equals(ballotNumber)) {
                accepted.put(((PhaseTwoRequest) message).getPayload().getSlot(), ((PhaseTwoRequest) message).getPayload());
                logger.paxos("receiveMessage() in acceptor " + id, "ACCEPTOR ACCEPTED " + ballotNumber);
            }
            actorSystem.sendToNode(message.getSource(),
                    new PhaseTwoResponse(id, ballotNumber, ((PhaseTwoRequest) message).getPayload()));
        } else {
            throw new IllegalStateException("Incorrect message");
        }
    }
}


