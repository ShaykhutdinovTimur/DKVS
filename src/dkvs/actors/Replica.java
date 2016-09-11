package dkvs.actors;

import dkvs.Logger;
import dkvs.kvs.StateMachine;
import dkvs.messages.ClientResponse;
import dkvs.messages.Command;
import dkvs.messages.leaderAdressed.ProposeMessage;
import dkvs.messages.replicaAdressed.ClientRequest;
import dkvs.messages.replicaAdressed.Decision;
import dkvs.messages.replicaAdressed.GetRequest;
import dkvs.messages.replicaAdressed.ReplicaMessage;

import java.util.HashMap;
import java.util.HashSet;

public class Replica {
    private volatile int slotIn;
    private int id;
    private ActorSystem actorSystem;
    private Logger logger;
    private StateMachine stateMachine;
    private HashSet<Command> requests = new HashSet<>();
    private HashMap<Integer, Command> proposals = new HashMap<>();
    private HashMap<Integer, Command> decisions = new HashMap<>();
    private HashMap<Command, Integer> awaitingClients = new HashMap<>();
    private HashSet<Command> performed = new HashSet<>();

    public Replica(int id, Logger logger, ActorSystem actorSystem) {
        this.id = id;
        this.logger = logger;
        this.actorSystem = actorSystem;
        this.stateMachine = new StateMachine(id);
        stateMachine.slotOut++;
        slotIn = stateMachine.slotOut;
    }

    public void receiveMessage(ReplicaMessage message) {
        if (message instanceof GetRequest) {
            String result = stateMachine.apply((GetRequest) message);
            actorSystem.sendToClient(message.getSource(), new ClientResponse(message.getSource(), result));
            return;
        } else if (message instanceof ClientRequest) {
            Command command = new Command(id, (ClientRequest) message);
            requests.add(command);
            awaitingClients.put(command, message.getSource());
        } else if (message instanceof Decision) {
            Command actualRequestCommand = ((Decision) message).getRequest();
            int actualSlot = ((Decision) message).getSlot();

            logger.paxos("receiveMessage(message)", String.format("DECISION %s", message));
            decisions.put(actualSlot, actualRequestCommand);

            while (decisions.containsKey(stateMachine.slotOut)) {
                Command command = decisions.get(stateMachine.slotOut);
                if (proposals.containsKey(stateMachine.slotOut)) {
                    Command proposalCommand = proposals.get(stateMachine.slotOut);
                    proposals.remove(stateMachine.slotOut);

                    if (!command.equals(proposalCommand)) {
                        requests.add(proposalCommand);
                    }
                }
                perform(command);
                ++stateMachine.slotOut;
            }
        }
        propose();
    }

    private void propose() {
        while (!requests.isEmpty()) {
            Command command = requests.iterator().next();
            logger.paxos("propose()", String.format("PROPOSING %s to slot %d", command, slotIn));
            if (!decisions.containsKey(slotIn)) {
                requests.remove(command);
                proposals.put(slotIn, command);
                actorSystem.sendToAll(new ProposeMessage(id, slotIn, command));
            }
            ++slotIn;
        }
    }

    private void perform(Command command) {
        logger.paxos("perform()", String.format("PERFORMING %s at %d", command, stateMachine.slotOut));
        if (performed.contains(command))
            return;
        if (!(command.getRequest() instanceof GetRequest)) {
            ClientResponse response = new ClientResponse(command.getRequest().getSource(),
                    stateMachine.apply(command.getRequest()));
            Integer awaitingClient = awaitingClients.get(command);
            if (awaitingClient != null) {
                actorSystem.sendToClient(awaitingClient, response);
                awaitingClients.remove(command);
            }
        }
        performed.add(command);
    }
}
