package dkvs;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class Config {
    private static Map<Integer, String> hosts;
    private static Map<Integer, Integer> ports;
    private static int nodesCount;
    private static int timeout;
    private static List<Integer> ids;

    static {
        try (FileInputStream inputStream = new FileInputStream("src/resources/dkvs.properties")) {
            Properties properties = new Properties();
            ids = new ArrayList<>();
            properties.load(inputStream);
            hosts = new HashMap<>();
            ports = new HashMap<>();
            for (nodesCount = 0; properties.getProperty("node." + nodesCount) != null; nodesCount++) {
                String address = properties.getProperty("node." + nodesCount);
                hosts.put(nodesCount, address.split(":")[0]);
                ports.put(nodesCount, Integer.parseInt(address.split(":")[1]));
                ids.add(nodesCount);
            }
            timeout = Integer.parseInt(properties.getProperty("timeout"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Config() {
    }

    public static int getPort(int id) {
        return ports.get(id);
    }

    public static String getHost(int id) {
        return hosts.get(id);
    }

    public static int getNodesCount() {
        return nodesCount;
    }

    public static int getTimeout() {
        return timeout;
    }

    public static List<Integer> getIds() {
        return ids;
    }
}

