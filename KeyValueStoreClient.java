import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.util.logging.*;
import java.io.IOException;

public class KeyValueStoreClient {

    private static final Logger logger = Logger.getLogger(KeyValueStoreClient.class.getName());

    public static void main(String[] args) {
        setupLogger();

        if (args.length != 2) {
            logger.info("Usage: java KeyValueStoreClient <hostname> <port>");
            return;
        }

        String hostname = args[0];
        int port = Integer.parseInt(args[1]);

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

            // Operations counters
            int putCount = 0, getCount = 0, deleteCount = 0;

            Scanner scanner = new Scanner(System.in);
            while (true) {
                logger.info(
                        "\nWhat would you like to do? \n1. Add a key-value pair\n2. Get a value by key\n3. Delete a key-value pair\n4. Exit\nEnter your choice:\n");
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume the newline character

                String response = "";

                switch (choice) {
                    case 1:
                        logger.info("\nEnter the key (in lowercase):");
                        String key = scanner.nextLine().toLowerCase();
                        logger.info("\nEnter the value (use underscore between multi-word values):");
                        String value = scanner.nextLine();
                        response = keyValueStore.put(key, value);
                        putCount++;
                        break;

                    case 2:
                        logger.info("\nEnter the key (in lowercase):");
                        key = scanner.nextLine().toLowerCase();
                        response = keyValueStore.get(key);
                        getCount++;
                        break;

                    case 3:
                        logger.info("\nEnter the key (in lowercase):");
                        key = scanner.nextLine().toLowerCase();
                        response = keyValueStore.delete(key);
                        deleteCount++;
                        break;

                    case 4:
                        if (putCount > 4 && getCount > 4 && deleteCount > 4) {
                            logger.info("Exiting...");
                            response = "EXIT"; // Send exit command to the server
                        } else {
                            logger.info("You need to perform " + (5 - putCount) + " more PUT, " + (5 - getCount)
                                    + " GET, and " + (5 - deleteCount) + " DELETE operations before exiting");
                            continue;
                        }
                        break;

                    default:
                        logger.info("Invalid choice. Please try again.");
                        continue; // Skip sending for invalid choices
                }

                logger.info(response);

                if (response.equalsIgnoreCase("EXIT")) {
                    break; // Exit the loop for exit command
                }
            }
            scanner.close();
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
