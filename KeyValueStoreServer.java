import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.*;
import java.io.IOException;

public class KeyValueStoreServer extends UnicastRemoteObject implements KeyValueStore {

    private static final Map<String, String> keyValueStore = new HashMap<>();
    private static final Logger logger = Logger.getLogger(KeyValueStoreServer.class.getName());

    public KeyValueStoreServer() throws RemoteException {
        super();
    }

    public synchronized String put(String key, String value) throws RemoteException {
        String response;
        if (keyValueStore.containsKey(key)) {
            logger.info("Key already exists. Updating value for key: " + key + "with value: " + value);
            keyValueStore.put(key, value);
            response = "Key already exists. Updating value for key: " + key + "with value: " + value;
        } else {
            logger.info("Added key-value pair: " + key + " = " + value);
            keyValueStore.put(key, value);
            response = "PUT successful: " + key + " = " + value;
        }
        return response;
    }

    public synchronized String get(String key) throws RemoteException {
        logger.info("Retrieved value for key: " + key);
        return keyValueStore.containsKey(key) ? keyValueStore.get(key) : "Key not found: " + key;
    }

    public synchronized String delete(String key) throws RemoteException {
        logger.info("Deleted key: " + key);
        return keyValueStore.remove(key) != null ? "DELETE successful: " + key : "Key not found for deletion: " + key;
    }

    private static void setupLogger() {
        try {
            Logger rootLogger = Logger.getLogger("");
            Handler[] handlers = rootLogger.getHandlers();
            for (Handler handler : handlers) {
                rootLogger.removeHandler(handler);
            }

            FileHandler fileHandler = new FileHandler("logs/RMIServer.log");
            fileHandler.setFormatter(new CustomFormatter());
            logger.addHandler(fileHandler);

            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new CustomFormatter());
            logger.addHandler(consoleHandler);

            logger.setLevel(Level.INFO);
        } catch (IOException e) {
            System.err.println("Error setting up logger: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        setupLogger();
        try {
            KeyValueStoreServer server = new KeyValueStoreServer();
            java.rmi.registry.LocateRegistry.createRegistry(1099).rebind("KeyValueStore", server);
            logger.info("RMI Key-Value Store Server is running...");
        } catch (Exception e) {
            logger.severe("Server error: " + e.getMessage());
        }
    }
}
