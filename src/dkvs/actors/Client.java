package dkvs.actors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Client {
    public static void main(String[] args) {

        int id = 0;
        if (args.length != 0)
            id = Integer.parseInt(args[0]);

        String listOfPorts = "4000 4001 4002";
        System.out.println(listOfPorts + ": picked first");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        try {
            int port = Integer.parseInt(listOfPorts.split(" ")[id]);
            Socket socket = new Socket();
            InetSocketAddress address = new InetSocketAddress("localhost", port);
            socket.connect(address);
            System.out.println("connected: " + port);

            try (OutputStreamWriter socketWriter = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
                 InputStreamReader socketReader = new InputStreamReader(socket.getInputStream(), "UTF-8");
                 BufferedReader bufferedReader = new BufferedReader(socketReader)) {
                while (true) {
                    String command = reader.readLine();
                    System.out.println("request: " + command);
                    if (command == null || command == "") {
                        socketWriter.close();
                        return;
                    }
                    socketWriter.write(command + "\n");
                    socketWriter.flush();

                    String response = bufferedReader.readLine();
                    System.out.println("response: " + response);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
