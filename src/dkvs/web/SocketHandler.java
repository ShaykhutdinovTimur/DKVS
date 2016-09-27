package dkvs.web;

import dkvs.Config;
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
    private Socket input = null;
    private Socket output = null;
    private LinkedBlockingDeque<Message> outputMessages;
    private LinkedBlockingDeque<Message> inputMessages;
    private long lastResponse;
    private volatile boolean close = false;
    private int senderId, addressId;

    public SocketHandler(int from, int to, Socket inputSocket,
                         LinkedBlockingDeque<Message> inputMessages,
                         LinkedBlockingDeque<Message> outputMessages) {
        senderId = from;
        addressId = to;
        input = inputSocket;
        this.inputMessages = inputMessages;
        this.outputMessages = outputMessages;
        lastResponse = System.currentTimeMillis();
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
                    log("GOT message [" + m + "]");
                    inputMessages.add(m);
                }
            } catch (IllegalArgumentException e) {
                log(e.getMessage());
            } catch (IOException e) {
                log(e.getMessage());
                break;
            }
        }
    }

    private void speakToWriter(OutputStreamWriter writer) {
        while (!close) {
            try {
                Message m = outputMessages.take();
                try {
                    writer.write(m + "\n");
                    writer.flush();
                    if (!(m instanceof Ping) && !(m instanceof Pong))
                        log("SENT " + m);
                } catch (IOException ioe) {
                    log("Couldn't send a message. Retrying.");
                    outputMessages.put(m);
                    break;
                }
            } catch (InterruptedException e) {
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
            while (!close) {
                try {
                    resetOutput(new Socket());
                    Socket clientSocket = output;
                    clientSocket.connect(new InetSocketAddress(address, port));
                    log("CONNECTED");
                    outputMessages.addFirst(new NodeMessage(senderId));
                    speakToWriter(new OutputStreamWriter(clientSocket.getOutputStream(), CHARSET));
                } catch (IOException e) {
                    log("Connection lost " + e.getMessage());
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
        close = true;
        resetInput(null);
        resetOutput(null);
    }


    private void log(String message) {
        System.out.println("Socket handler " + senderId + " to " + addressId + " " + message);
    }

    public void send(Message m) {
        outputMessages.add(m);
    }

}
