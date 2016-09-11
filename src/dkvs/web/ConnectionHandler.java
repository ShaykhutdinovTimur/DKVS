package dkvs.web;

import dkvs.Config;
import dkvs.Logger;
import dkvs.messages.Message;
import dkvs.messages.replicaAdressed.ClientRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

public class ConnectionHandler {

    private static final String CHARSET = "UTF-8";
    private Logger logger;
    private LinkedBlockingDeque<Message> incomingMessages;
    private List<LinkedBlockingDeque<Message>> outcomingMessages;
    private List<SocketHandler> nodes;
    private HashMap<Integer, SocketHandler> clients;
    private int id;
    private ServerSocket inSocket = null;
    private volatile boolean started = false;
    private volatile boolean stopping = false;
    private int clientID = Config.getNodesCount();
    //private Timer timer;


    public ConnectionHandler(int nodeId, Logger logger, LinkedBlockingDeque<Message> incoming,
                             List<LinkedBlockingDeque<Message>> outcoming,
                             HashMap<Integer, SocketHandler> clients) {
        this.id = nodeId;
        this.incomingMessages = incoming;
        this.outcomingMessages = outcoming;
        this.clients = clients;
        this.logger = logger;
        try {
            inSocket = new ServerSocket(Config.getPort(id));
            nodes = new ArrayList<>(Config.getNodesCount());
            logger = new Logger(id);

            for (int i = 0; i < Config.getNodesCount(); ++i) {
                nodes.add(new SocketHandler(id, i, null, logger, incomingMessages));
                nodes.get(i).outputMessages = outcomingMessages.get(i);
            }
            nodes.get(id).outputMessages = incomingMessages;
        } catch (IOException e) {
            logger.error("Node()", e.getMessage());
        }
    }


    public void run() {
        if (started)
            throw new IllegalStateException("Cannot start a node twice");
        started = true;

        logger.connection("run()", "starting node");


        new Thread(() -> {
            while (!stopping)
                try {
                    Socket client = inSocket.accept();
                    new Thread(() -> {
                        handleRequest(client);
                    }).start();
                } catch (IOException e) {
                }
        }).start();
        /*
        TimerTask pingTask = new TimerTask() {
            @Override
            public void run() {
                pingIfIdle();
            }
        };

        TimerTask monitorFaultsTask = new TimerTask() {
            @Override
            public void run() {
                monitorFaults();
            }
        };

        timer.scheduleAtFixedRate(pingTask, Config.getTimeout(), Config.getTimeout());
        timer.scheduleAtFixedRate(monitorFaultsTask, 4 * Config.getTimeout(), 4 * Config.getTimeout());
        */
    }

    private void handleRequest(Socket client) {
        try (InputStreamReader reader = new InputStreamReader(client.getInputStream(), CHARSET);
             BufferedReader bufferedReader = new BufferedReader(reader)) {

            String msg = bufferedReader.readLine();
            String[] parts = msg.split(" ");

            logger.messageIn("handleRequest():", "GOT request [" + msg + "]");

            switch (parts[0]) {
                case "node":
                    int nodeId = Integer.parseInt(parts[1]);
                    nodes.get(nodeId).resetInput(client);
                    nodes.get(nodeId).listen(bufferedReader);
                    logger.connection("handleRequest(node:" + nodeId + ")",
                            String.format("#%d: Started listening to node.%d from %s", id, nodeId, client.getInetAddress()));

                    break;
                case "get":
                case "set":
                case "delete":
                    final int newClientId = clientID++;
                    SocketHandler entry = new SocketHandler(id, newClientId, client, logger, incomingMessages);
                    clients.put(newClientId, entry);
                    Message firstMessage = ClientRequest.parse(newClientId, parts);
                    incomingMessages.add(firstMessage);
                    logger.connection("handleRequest(client:" + newClientId + ")",
                            String.format("Client %d connected to %d.", newClientId, id));
                    entry.listen(bufferedReader);
                    break;
                default:
                    logger.messageIn("handleRequest( ... )",
                            "something goes wrong: \"" + parts[0] + "\" received");
                    break;
            }

        } catch (IOException e) {
            logger.error("handleRequest()", e.getMessage());
        }
    }

    public void close() throws Exception {
        stopping = true;
        inSocket.close();
        for (SocketHandler n : nodes) {
            if (n.input != null) n.input.close();
            if (n.output != null) n.output.close();
        }

        for (SocketHandler n : clients.values()) {
            if (n.input != null) n.input.close();
            if (n.output != null) n.output.close();
        }
    }


    /*private void pingIfIdle() {

        nodes.stream()
                .filter(it -> (it. != id))
                .filter(it -> it.getValue().ready)
                .forEach(it -> {
                    if (!it.getValue().outputAlive)
                        sendToNode(it.getKey(), new Ping(id));
                    it.getValue().outputAlive = false;
                });
    }

    private void monitorFaults() {
        HashSet<Integer> faultyNodes = new HashSet<>();

        nodes.entrySet().stream()
                .filter(it -> it.getKey() != id)
                .forEach(it -> {
                    if (!it.getValue().inputAlive) {
                        if (it.getValue().input != null)
                            try {
                                it.getValue().input.close();
                            } catch (IOException e) {
                            }
                        faultyNodes.add(it.getKey());
                        logger.connection("monitorFaults()", "Node " + it.getKey() + " is faulty, closing its connection.");
                    }
                    it.getValue().inputAlive = false;
                });

        if (faultyNodes.size() > 0) {
            for (int i : faultyNodes) {
                incomingMessages.add(new DisconnectMessage(i));
            }
        }
    }*/

}
