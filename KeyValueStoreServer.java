import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.*;
import java.io.IOException;

public class KeyValueStoreServer extends UnicastRemoteObject implements KeyValueStore {

    private static final Logger logger = Logger.getLogger(KeyValueStoreServer.class.getName());
    private static final int PORT = 1099;
    private static final int THREAD_POOL_SIZE = 10; // Configure as needed
    private static final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private final Map<String, String> keyValueStore = new ConcurrentHashMap<>();

    public KeyValueStoreServer() throws RemoteException {
        super();
    }

    public void start() {
        try {
            Registry registry = LocateRegistry.createRegistry(PORT);
            registry.rebind("KeyValueStore", this);
            logger.info("RMI Key-Value Store Server is running...");
        } catch (RemoteException e) {
            logger.severe("Error starting RMI registry: " + e.getMessage());
        }
    }

    public String put(String key, String value) throws RemoteException {
        Future<String> futureResponse = executorService.submit(() -> {
            String response;
            if (keyValueStore.containsKey(key)) {
                logger.info("Key already exists. Updating value for key: " + key + " with value: " + value);
                keyValueStore.put(key, value);
                response = "Key already exists. Updating value for key: " + key + " with value: " + value;
            } else {
                logger.info("Added key-value pair: " + key + " = " + value);
                keyValueStore.put(key, value);
                response = "PUT successful: " + key + " = " + value;
            }
            return response;
        });

        try {
            return futureResponse.get(); // Wait for the task to complete and return the result
        } catch (InterruptedException | ExecutionException e) {
            logger.severe("Error executing put operation: " + e.getMessage());
            return "Error occurred while putting the key-value pair.";
        }
    }

    public String get(String key) throws RemoteException {
        Future<String> futureResponse = executorService.submit(() -> {
            logger.info("Retrieved value for key: " + key);
            return keyValueStore.containsKey(key) ? keyValueStore.get(key) : "Key not found: " + key;
        });

        try {
            return futureResponse.get(); // Wait for the task to complete and return the result
        } catch (InterruptedException | ExecutionException e) {
            logger.severe("Error executing get operation: " + e.getMessage());
            return "Error occurred while retrieving the value.";
        }
    }

    public String delete(String key) throws RemoteException {
        Future<String> futureResponse = executorService.submit(() -> {
            logger.info("Deleted key: " + key);
            return keyValueStore.remove(key) != null ? "DELETE successful: " + key
                    : "Key not found for deletion: " + key;
        });

        try {
            return futureResponse.get(); // Wait for the task to complete and return the result
        } catch (InterruptedException | ExecutionException e) {
            logger.severe("Error executing delete operation: " + e.getMessage());
            return "Error occurred while deleting the key.";
        }
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
            server.start();
            logger.info("RMI Key-Value Store Server is running...");
        } catch (Exception e) {
            logger.severe("Server error: " + e.getMessage());
        }
    }
}
