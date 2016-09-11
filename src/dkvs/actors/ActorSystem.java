package dkvs.actors;

import dkvs.Config;
import dkvs.Logger;
import dkvs.messages.DisconnectMessage;
import dkvs.messages.Message;
import dkvs.messages.acceptorAdressed.AcceptorMessage;
import dkvs.messages.leaderAdressed.LeaderMessage;
import dkvs.messages.replicaAdressed.ReplicaMessage;
import dkvs.web.SocketHandler;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

public class ActorSystem {

    private Replica replica;
    private Acceptor acceptor;
    private Leader leader;

    private Logger logger;
    private int id;

    private LinkedBlockingDeque<Message> incomingMessages;
    private List<LinkedBlockingDeque<Message>> outcomingMessages;

    private HashMap<Integer, SocketHandler> clients;

    public ActorSystem(int id, Logger logger, LinkedBlockingDeque<Message> incomingMessages,
                       List<LinkedBlockingDeque<Message>> outcomingMessages,
                       HashMap<Integer, SocketHandler> clients) {
        this.logger = logger;
        this.id = id;
        this.incomingMessages = incomingMessages;
        this.outcomingMessages = outcomingMessages;
        this.clients = clients;
        replica = new Replica(id, logger, this);
        leader = new Leader(id, logger, this);
        acceptor = new Acceptor(id, logger, this);
        leader.startLeader();
        new Thread(this::handleMessages).start();
    }

    private void handleMessages() {
        while (true) {
            try {
                Message m = incomingMessages.take();

                logger.messageIn("handleMessages()", String.format("Handling message: %s", m));
                if (m instanceof DisconnectMessage) {
                    leader.notifyFault(m.getSource());
                } else if (m instanceof ReplicaMessage) {
                    replica.receiveMessage((ReplicaMessage) m);
                } else if (m instanceof LeaderMessage) {
                    leader.receiveMessage((LeaderMessage) m);
                } else if (m instanceof AcceptorMessage) {
                    acceptor.receiveMessage((AcceptorMessage) m);
                } else {
                    logger.messageIn("handleMessages()", String.format("Unknown message: %s", m));
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public void sendToNode(int to, Message message) {
        boolean sent = false;
        while (!sent) {
            try {
                if (to == id) {
                    incomingMessages.put(message);
                } else {
                    outcomingMessages.get(to).put(message);
                }
                sent = true;
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public void sendToAll(Message message) {
        for (int i = 0; i < Config.getNodesCount(); i++) {
            sendToNode(i, message);
        }
    }

    public void sendToClient(int to, Message message) {
        clients.get(to).send(message);
    }
}
