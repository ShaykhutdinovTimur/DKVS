package dkvs.web;

import dkvs.Config;
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

            for (int i = 0; i < Config.getNodesCount(); ++i) {
                if (i != id) {
                    nodes[i] = new SocketHandler(id, i, null, incomingMessages, outcomingMessages.get(i));
                } else {
                    nodes[i] = new SocketHandler(id, i, null, incomingMessages, incomingMessages);
                }
            }
        } catch (IOException e) {
            log(e.getMessage());
        }
    }


    private void log(String message) {
        System.out.println("connection handler " + id + " " + message);
    }

    public void run() {
        if (started)
            throw new IllegalStateException("Cannot start a node twice");
        started = true;
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

            log("GOT request [" + msg + "]");

            switch (parts[0]) {
                case "node":
                    int nodeId = Integer.parseInt(parts[1]);
                    nodes[nodeId].resetInput(client);
                    nodes[nodeId].listen(bufferedReader);
                    log("Started listening to node" + nodeId);
                    break;
                case "get":
                case "set":
                case "delete":
                    final int newClientId = clientID++;
                    SocketHandler entry = new SocketHandler(id, newClientId, client, incomingMessages, new LinkedBlockingDeque<>());
                    clients.put(newClientId, entry);
                    Message firstMessage = ClientRequest.parse(newClientId, parts);
                    incomingMessages.add(firstMessage);
                    log("Client " + newClientId + " connected");
                    entry.listen(bufferedReader);
                    break;
                default:
                    log("something wrong: " + parts[0] + " received");
                    break;
            }

        } catch (IOException e) {
            log(e.getMessage());
        }
    }

    public void close() throws Exception {
        stopping = true;
        inSocket.close();
        for (SocketHandler n : nodes) {
            n.close();
        }

        for (SocketHandler n : clients.values()) {
            n.close();
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
                            log("Breaking connection with " + i);
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
