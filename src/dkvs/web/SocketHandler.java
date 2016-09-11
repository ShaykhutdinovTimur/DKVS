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
import java.util.concurrent.LinkedBlockingQueue;

public class SocketHandler implements AutoCloseable {

    private static final String CHARSET = "UTF-8";
    Socket input = null;
    Socket output = null;
    LinkedBlockingDeque<Message> outputMessages = new LinkedBlockingDeque<>();
    LinkedBlockingQueue<Message> inputMessages = new LinkedBlockingQueue<>();
    Logger logger;
    private int senderId, addressId;

    /**
     * if you want connection to client to == -1
     *
     * @param from
     * @param to
     */
    public SocketHandler(int from, int to, Socket inputSocket, Logger logger) {
        senderId = from;
        addressId = to;
        input = inputSocket;
        this.logger = logger;
        new Thread(() -> {
            if (addressId == -1) {
                //client connection
                try (OutputStreamWriter writer = new OutputStreamWriter(input.getOutputStream(), CHARSET)) {
                    speakToWriter(writer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                speak();
            }
        }).start();
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
                String data = breader.readLine();
                Message m = Message.parse(addressId, data.split(" "));
                if (m instanceof Ping) {
                    outputMessages.add(new Pong(senderId));
                } else if (!(m instanceof Pong)) {
                    logger.messageIn("listenToNode(nodeId:" + addressId + ")",
                            "GOT message [" + m + "] from " + addressId);
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
        while (true) {
            try {
                Message m = outputMessages.take();
                try {
                    writer.write(m + "\n");
                    writer.flush();
                    if (!(m instanceof Ping) && !(m instanceof Pong))
                        logger.messageOut("speakToNode(nodeId: " + addressId + ")",
                                String.format("SENT to %d: %s", addressId, m));
                } catch (IOException ioe) {
                    logger.error("speakToNode(nodeId: " + addressId + ")",
                            String.format("Couldn't send a message from %d to %d. Retrying.",
                                    this.addressId, addressId));
                    outputMessages.put(m);
                    break;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void speak() {
        String address = Config.getHost(addressId);
        int port = Config.getPort(addressId);
        while (true) {
            try {
                resetOutput(new Socket());
                Socket clientSocket = output;
                clientSocket.connect(new InetSocketAddress(address, port));
                logger.connection("speakToNode(nodeId: " + addressId + ")",
                        String.format("#%d: CONNECTED to node.%d", this.addressId, addressId));
                outputMessages.addFirst(new NodeMessage(senderId));
                speakToWriter(new OutputStreamWriter(clientSocket.getOutputStream(), CHARSET));
            } catch (IOException e) {
                logger.error("speakToNode(nodeId: " + addressId + ")",
                        String.format("Connection from %d to node.%d lost: %s",
                                addressId, addressId, e.getMessage()));
            }
        }
    }

    @Override
    public void close() throws Exception {
        resetInput(null);
        resetOutput(null);
    }

    public void send(Message m) {
        outputMessages.add(m);
    }
}
