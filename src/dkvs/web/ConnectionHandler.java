package dkvs.web;

import dkvs.Config;
import dkvs.Logger;
import dkvs.messages.DisconnectMessage;
import dkvs.messages.Message;
import dkvs.messages.Ping;
import dkvs.messages.replicaAdressed.ClientRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingDeque;


public class ConnectionHandler {

    private static final String CHARSET = "UTF-8";
    private Logger logger;
    private LinkedBlockingDeque<Message> incomingMessages;
    private List<LinkedBlockingDeque<Message>> outcomingMessages;
    private SocketHandler[] nodes;
    private HashMap<Integer, SocketHandler> clients;
    private int id;
    private ServerSocket inSocket = null;
    private volatile boolean started = false;
    private volatile boolean stopping = false;
    private int clientID = Config.getNodesCount();
    private Timer timer;


    public ConnectionHandler(int nodeId, LinkedBlockingDeque<Message> incoming,
                             List<LinkedBlockingDeque<Message>> outcoming,
                             HashMap<Integer, SocketHandler> clients) {
        this.id = nodeId;
        this.incomingMessages = incoming;
        this.outcomingMessages = outcoming;
        this.clients = clients;
        try {
            inSocket = new ServerSocket(Config.getPort(id));
            nodes = new SocketHandler[Config.getNodesCount()];
            logger = new Logger(id);

            for (int i = 0; i < Config.getNodesCount(); ++i) {
                nodes[i] = new SocketHandler(id, i, null, incomingMessages);
                nodes[i].outputMessages = outcomingMessages.get(i);
            }
            nodes[id].outputMessages = incomingMessages;
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
        startTimer();
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
                    nodes[nodeId].resetInput(client);
                    nodes[nodeId].listen(bufferedReader);
                    logger.connection("handleRequest(node:" + nodeId + ")",
                            String.format("#%d: Started listening to node.%d from %s", id, nodeId, client.getInetAddress()));

                    break;
                case "get":
                case "set":
                case "delete":
                    final int newClientId = clientID++;
                    SocketHandler entry = new SocketHandler(id, newClientId, client, incomingMessages);
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

    private void startTimer() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                for (int i = 0; i < Config.getNodesCount(); i++) {
                    if (nodes[i] != null && i != id) {
                        if (System.currentTimeMillis() - nodes[i].getLastResponse() > Config.getTimeout()) {
                            nodes[i].send(new Ping(id));
                        }
                        if (System.currentTimeMillis() - nodes[i].getLastResponse() > 2 * Config.getTimeout()) {
                            logger.connection(id + "", "Breaking connection with " + i);
                            incomingMessages.add(new DisconnectMessage(i));
                        }
                    }
                }
                for (int i = 0; i < Config.getNodesCount(); i++) {
                    if (i != id && System.currentTimeMillis() - nodes[i].getLastResponse() > 5 * Config.getTimeout()) {
                        nodes[i].close();
                    }
                }
            }
        }, 0, Config.getTimeout());
    }
}
