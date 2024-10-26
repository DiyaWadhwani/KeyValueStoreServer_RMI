import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.*;
import java.io.IOException;

public class KeyValueStoreClient {

    private static final Logger logger = Logger.getLogger(KeyValueStoreClient.class.getName());

    public static void main(String[] args) {
        setupLogger();

        String hostname = "kv_store_server";
        int port = 1099;

        // Create multiple client threads
        for (int i = 1; i <= 5; i++) {
            Thread clientThread = new Thread(new ClientRunnable(hostname, port, i));
            clientThread.start();
        }
    }

    private static class ClientRunnable implements Runnable {
        private final String hostname;
        private final int port;
        private final int clientId;

        public ClientRunnable(String hostname, int port, int clientId) {
            this.hostname = hostname;
            this.port = port;
            this.clientId = clientId;
        }

        @Override
        public void run() {
            try {
                Registry registry = LocateRegistry.getRegistry(hostname, port);
                KeyValueStore keyValueStore = (KeyValueStore) registry.lookup("KeyValueStore");

                // Log the client ID
                KeyValueStoreClient.logger.info("Client " + clientId + " starting.");

                // Pre-populate key-value pairs
                keyValueStore.put("client" + clientId + "_name", "Diya" + clientId);
                keyValueStore.put("client" + clientId + "_age", "20");
                keyValueStore.put("client" + clientId + "_city", "Manama");
                keyValueStore.put("client" + clientId + "_country", "Bahrain");
                keyValueStore.put("client" + clientId + "_profession", "Student");

                KeyValueStoreClient.logger.info("Client " + clientId + " added its key-value pairs.");

                // Retrieve and log the values
                KeyValueStoreClient.logger.info("Client " + clientId + " retrieved name: "
                        + keyValueStore.get("client" + clientId + "_name"));
                KeyValueStoreClient.logger.info("Client " + clientId + " retrieved age: "
                        + keyValueStore.get("client" + clientId + "_age"));
                KeyValueStoreClient.logger.info("Client " + clientId + " retrieved city: "
                        + keyValueStore.get("client" + clientId + "_city"));
                KeyValueStoreClient.logger.info("Client " + clientId + " retrieved country: "
                        + keyValueStore.get("client" + clientId + "_country"));
                KeyValueStoreClient.logger.info("Client " + clientId + " retrieved profession: "
                        + keyValueStore.get("client" + clientId + "_profession"));

                // Delete the key-value pairs
                keyValueStore.delete("client" + clientId + "_name");
                keyValueStore.delete("client" + clientId + "_age");
                keyValueStore.delete("client" + clientId + "_city");
                keyValueStore.delete("client" + clientId + "_country");
                keyValueStore.delete("client" + clientId + "_profession");

                // Log the deletion
                KeyValueStoreClient.logger.info("Client " + clientId + " deleted its key-value pairs.");

            } catch (Exception e) {
                KeyValueStoreClient.logger.severe("Client " + clientId + " error: " + e.getMessage());
            }
        }
    }

    private static void setupLogger() {
        try {
            Logger rootLogger = Logger.getLogger("");
            Handler[] handlers = rootLogger.getHandlers();
            for (Handler handler : handlers) {
                rootLogger.removeHandler(handler);
            }

            FileHandler fileHandler = new FileHandler("logs/RMIClient.log");
            fileHandler.setFormatter(new CustomFormatter());
            rootLogger.addHandler(fileHandler);

            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new CustomFormatter());
            rootLogger.addHandler(consoleHandler);

            rootLogger.setLevel(Level.INFO);
        } catch (IOException e) {
            System.err.println("Error setting up logger: " + e.getMessage());
        }
    }
}
