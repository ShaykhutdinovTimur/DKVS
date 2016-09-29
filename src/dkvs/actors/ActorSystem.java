package dkvs.actors;

import dkvs.Config;
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
    private int id;

    private LinkedBlockingDeque<Message> incomingMessages;
    private List<LinkedBlockingDeque<Message>> outcomingMessages;

    private HashMap<Integer, SocketHandler> clients;

    public ActorSystem(int id, LinkedBlockingDeque<Message> incomingMessages,
                       List<LinkedBlockingDeque<Message>> outcomingMessages,
                       HashMap<Integer, SocketHandler> clients) {
        this.id = id;
        this.incomingMessages = incomingMessages;
        this.outcomingMessages = outcomingMessages;
        this.clients = clients;
        replica = new Replica(id, this);
        leader = new Leader(id, this);
        acceptor = new Acceptor(id, this);
        leader.startLeader();
        new Thread(this::handleMessages).start();
    }

    private void handleMessages() {
        while (true) {
            try {
                Message m = incomingMessages.take();
                if (m instanceof DisconnectMessage) {
                    leader.notifyFault(m.getSource());
                } else if (m instanceof ReplicaMessage) {
                    replica.receiveMessage((ReplicaMessage) m);
                } else if (m instanceof LeaderMessage) {
                    leader.receiveMessage((LeaderMessage) m);
                } else if (m instanceof AcceptorMessage) {
                    acceptor.receiveMessage((AcceptorMessage) m);
                } else {
                    System.out.println("Unknown message handled on " + id + " : " + m);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public void sendToNode(int to, Message message) {
        outcomingMessages.get(to).add(message);
    }

    public void sendToAll(Message message) {
        for (int i = 0; i < Config.getNodesCount(); i++) {
            sendToNode(i, message);
        }
    }

    public void sendToClient(int to, Message message) {
        try {
            clients.get(to).send(message);
        } catch (NullPointerException e) {
            System.out.println("npe client id: " + to);
        }
    }
}
