package dkvs;

import dkvs.actors.ActorSystem;
import dkvs.messages.Message;
import dkvs.web.ConnectionHandler;
import dkvs.web.SocketHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;


public class Node implements Runnable, AutoCloseable {
    private ConnectionHandler connectionHandler;
    private ActorSystem actorSystem;
    private Logger logger;
    private LinkedBlockingDeque<Message> incomingMessages;
    private List<LinkedBlockingDeque<Message>> outcomingMessages;
    private HashMap<Integer, SocketHandler> clients;
    private int id;
    private Boolean started;

    public Node(int nodeId) {
        started = false;
        this.id = nodeId;
        incomingMessages = new LinkedBlockingDeque<>();
        outcomingMessages = new ArrayList<>();
        for (int i = 0; i < Config.getNodesCount(); i++) {
            if (i != id) {
                outcomingMessages.add(new LinkedBlockingDeque<Message>());
            } else {
                outcomingMessages.add(incomingMessages);
            }
        }
        clients = new HashMap<>();
        logger = new Logger(id);
        connectionHandler = new ConnectionHandler(id, incomingMessages, outcomingMessages, clients);
        actorSystem = new ActorSystem(id, incomingMessages, outcomingMessages, clients);
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            for (int i = 0; i < Config.getNodesCount(); i++) {
                final int nodeId = i;
                new Thread(() -> {
                    new Node(nodeId).run();
                }).start();
            }
        } else {
            int nodeId = Integer.parseInt(args[0]);
            new Node(nodeId).run();
        }
    }

    @Override
    public void run() {
        if (started)
            throw new IllegalStateException("Cannot start a node twice");
        started = true;
        logger.connection("run()", "starting node");
        connectionHandler.run();
    }

    @Override
    public void close() throws Exception {
        connectionHandler.close();
    }
}