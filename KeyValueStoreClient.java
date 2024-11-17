import java.rmi.*;
import java.rmi.registry.*;
import java.util.Scanner;

public class KeyValueStoreClient {
    private final KeyValueStoreInterface server;

    public KeyValueStoreClient(String host, int port) throws Exception {
        Registry registry = LocateRegistry.getRegistry(host, port);
        this.server = (KeyValueStoreInterface) registry.lookup("KeyValueStore");
    }

    public void testOperations() throws RemoteException {
        // Real-world key-value pairs for testing
        String[][] data = {
                { "Name", "Diya" },
                { "Age", "20" },
                { "Course", "Distributed Systems" },
                { "School", "Northeastern University" },
                { "City", "California" },
        };

        System.out.println("Performing 5 PUT operations...");
        for (String[] entry : data) {
            String key = entry[0];
            String value = entry[1];
            boolean putResult = server.put(key, value);
            System.out.println("PUT " + key + " -> " + value + ": " + (putResult ? "Succeeded" : "Failed"));
        }

        System.out.println("\nPerforming 5 GET operations...");
        for (String[] entry : data) {
            String key = entry[0];
            String value = server.get(key);
            System.out.println("GET " + key + ": " + (value != null ? value : "Key not found"));
        }

        System.out.println("\nPerforming 5 DELETE operations...");
        for (String[] entry : data) {
            String key = entry[0];
            boolean deleteResult = server.delete(key);
            System.out.println("DELETE " + key + ": " + (deleteResult ? "Succeeded" : "Failed"));
        }

        System.out.println("\nAll operations completed.");
    }

    public static void main(String[] args) {
        try {
            String host = System.getenv("SERVER_HOST");
            int port = Integer.parseInt(System.getenv("SERVER_PORT"));

            KeyValueStoreClient client = new KeyValueStoreClient(host, port);
            System.out.println("Connected to server at " + host + ":" + port);
            client.testOperations();
        } catch (Exception e) {
            System.err.println("Client encountered an error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
