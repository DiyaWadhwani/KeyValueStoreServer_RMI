import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.*;
import java.io.IOException;

public class KeyValueStoreClient {

    private static final Logger logger = Logger.getLogger(KeyValueStoreClient.class.getName());

    public static void main(String[] args) {
        setupLogger();

        String hostname = "kv_store_server"; // You can replace this with args[0] if needed
        int port = 1099; // You can replace this with Integer.parseInt(args[1]) if needed

        try {
            Registry registry = LocateRegistry.getRegistry(hostname, port);
            KeyValueStore keyValueStore = (KeyValueStore) registry.lookup("KeyValueStore");

            logger.info("\nWelcome to the RMI Key-Value Store Client");
            logger.info("\nPre-population of 5 key-value pairs completed:\n");

            // Insert key-value pairs
            keyValueStore.put("name", "Diya");
            keyValueStore.put("age", "20");
            keyValueStore.put("city", "Manama");
            keyValueStore.put("country", "Bahrain");
            keyValueStore.put("profession", "Student");

            // Retrieve and log the values
            logger.info("Retrieved name: " + keyValueStore.get("name"));
            logger.info("Retrieved age: " + keyValueStore.get("age"));
            logger.info("Retrieved city: " + keyValueStore.get("city"));
            logger.info("Retrieved country: " + keyValueStore.get("country"));
            logger.info("Retrieved profession: " + keyValueStore.get("profession"));

            // Delete the key-value pairs
            keyValueStore.delete("name");
            keyValueStore.delete("age");
            keyValueStore.delete("city");
            keyValueStore.delete("country");
            keyValueStore.delete("profession");

            // Log the deletion
            logger.info("Deleted the 5 key-value pairs.");

        } catch (Exception e) {
            logger.severe("Client error: " + e.getMessage());
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
