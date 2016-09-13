package dkvs.web;

import dkvs.Config;
import dkvs.Logger;
import dkvs.messages.Message;
import dkvs.messages.NodeMessage;
import dkvs.messages.Ping;
import dkvs.messages.Pong;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingDeque;

import static java.lang.Thread.sleep;

public class SocketHandler implements AutoCloseable {

    private static final String CHARSET = "UTF-8";
    Socket input = null;
    Socket output = null;
    LinkedBlockingDeque<Message> outputMessages = new LinkedBlockingDeque<>();
    LinkedBlockingDeque<Message> inputMessages;
    Logger logger;
    long lastResponse;
    boolean alive = false;
    volatile boolean reconnect = false;
    private int senderId, addressId;

    public SocketHandler(int from, int to, Socket inputSocket, Logger logger, LinkedBlockingDeque<Message> inputMessages) {
        senderId = from;
        addressId = to;
        input = inputSocket;
        this.inputMessages = inputMessages;
        lastResponse = System.currentTimeMillis();
        this.logger = logger;
        if (from != to) {
            new Thread(() -> {
                speak();
            }).start();
        }
    }

    public long getLastResponse() {
        return lastResponse;
    }

    public void resetInput(Socket inputSocket) {
        try {
            if (input != null) {
                input.close();
            }
            input = inputSocket;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void resetOutput(Socket outputSocket) {
        try {
            if (output != null) {
                output.close();
            }
            output = outputSocket;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listen(BufferedReader breader) {
        while (true) {
            try {
                lastResponse = System.currentTimeMillis();
                String data = breader.readLine();
                if (data == null) {
                    break;
                }
                Message m = Message.parse(addressId, data.split(" "));
                if (m instanceof Ping) {
                    outputMessages.add(new Pong(senderId));
                } else if (!(m instanceof Pong)) {
                    logger.messageIn("listen on node" + senderId,
                            "GOT message [" + m + "] from " + m.getSource());
                    inputMessages.add(m);
                }
            } catch (IOException e) {
                logger.error("listenToNode(nodeId:" + addressId + ")",
                        addressId + ": " + e.getMessage());
                break;
            }
        }
    }

    private void speakToWriter(OutputStreamWriter writer) {
        while (!reconnect) {
            try {
                alive = true;
                Message m = outputMessages.take();
                try {
                    writer.write(m + "\n");
                    writer.flush();
                    if (!(m instanceof Ping) && !(m instanceof Pong))
                        logger.messageOut("speakToNode(nodeId: " + addressId + ")",
                                String.format("SENT from %d to %d: %s", senderId, addressId, m));
                } catch (IOException ioe) {
                    alive = false;
                    logger.error("speakToNode(nodeId: " + addressId + ")",
                            String.format("Couldn't send a message from %d to %d. Retrying.",
                                    this.addressId, addressId));
                    outputMessages.put(m);
                    break;
                }
            } catch (InterruptedException e) {
                alive = false;
                e.printStackTrace();
            }
        }
    }

    private void speak() {
        String address = Config.getHost(addressId);
        if (address == null) {
            try (OutputStreamWriter writer = new OutputStreamWriter(input.getOutputStream(), CHARSET)) {
                speakToWriter(writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            int port = Config.getPort(addressId);
            while (true) {
                try {
                    alive = false;
                    resetOutput(new Socket());
                    Socket clientSocket = output;
                    clientSocket.connect(new InetSocketAddress(address, port));
                    logger.connection("speakToNode(nodeId: " + addressId + ")",
                            String.format("#%d: CONNECTED to node.%d", this.addressId, addressId));
                    outputMessages.addFirst(new NodeMessage(senderId));
                    reconnect = false;
                    speakToWriter(new OutputStreamWriter(clientSocket.getOutputStream(), CHARSET));
                } catch (IOException e) {
                    logger.error("speakToNode(nodeId: " + addressId + ")",
                            String.format("Connection from %d to node.%d lost: %s",
                                    addressId, addressId, e.getMessage()));
                    try {
                        sleep(Config.getTimeout());
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void close() {
        resetInput(null);
        resetOutput(null);
    }

    public void send(Message m) {
        outputMessages.add(m);
    }

    public void setReconnect() {
        reconnect = true;
    }
}
