package dkvs.kvs;

import dkvs.messages.replicaAdressed.ClientRequest;
import dkvs.messages.replicaAdressed.DeleteRequest;
import dkvs.messages.replicaAdressed.GetRequest;
import dkvs.messages.replicaAdressed.SetRequest;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

public class StateMachine {
    private static final String FILE_NAME = "logs/node_%d.log";
    public volatile int slotOut = -1;
    private String fileName;
    private volatile KVS keyValueStorage;
    private BufferedWriter writer = null;

    public StateMachine(int nodeId) {
        fileName = String.format(FILE_NAME, nodeId);
        try {
            writer = new BufferedWriter(new FileWriter(fileName, true));
            keyValueStorage = new KVS(parseLog(fileName));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("no such file in this directory:" + fileName);
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private HashMap<String, String> parseLog(String fileName) throws FileNotFoundException {
        File file = new File(fileName);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        ArrayList<String> lines = new ArrayList<>(reader.lines().collect(Collectors.toList()));
        HashMap<String, String> hashMap = new HashMap<>();
        for (String l : lines) {
            String[] parts = l.split(" ");
            String key = parts[4];
            switch (parts[3]) {
                case "set":
                    hashMap.put(key, parts[5]);
                    break;
                case "delete":
                    hashMap.remove(key);
                    break;
            }
            slotOut = Math.max(slotOut, Integer.parseInt(parts[1]));
            break;
        }
        return hashMap;
    }

    public String apply(ClientRequest request) {
        String result = keyValueStorage.apply(request);
        if (!(request instanceof GetRequest)) {
            String s = String.format("slot %d %s", slotOut, request);
            try {
                writer.write(s);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("unable to write to file " + fileName);
                System.exit(1);
            }
        }
        return result;
    }

    private class KVS {
        private volatile HashMap<String, String> storage;

        KVS(HashMap<String, String> storage) {
            this.storage = storage;
        }

        String apply(ClientRequest request) {
            if (request instanceof GetRequest) {
                String key = ((GetRequest) request).getKey();
                String value = storage.get(key);
                return (value == null) ? "NOT_FOUND" : "VALUE ";
            } else if (request instanceof SetRequest) {
                storage.put(((SetRequest) request).getKey(), ((SetRequest) request).getValue());
                return "STORED";
            } else if (request instanceof DeleteRequest) {
                String result = storage.get(((DeleteRequest) request).getKey());
                storage.remove(((DeleteRequest) request).getKey());
                return (result != null) ? "DELETED" : "NOT_FOUND";
            }
            return null;
        }
    }
}
