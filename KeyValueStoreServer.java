import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.*;
import java.net.InetAddress;

public class KeyValueStoreServer extends UnicastRemoteObject implements KeyValueStoreInterface {
    private final Map<String, String> store = new HashMap<>();
    private final List<KeyValueStoreInterface> replicas = new ArrayList<>();
    private final List<String> replicaServers;

    public KeyValueStoreServer(List<String> replicaServers) throws RemoteException {
        this.replicaServers = replicaServers;
    }

    @Override
    public String get(String key) throws RemoteException {
        return store.getOrDefault(key, null);
    }

    @Override
    public boolean put(String key, String value) throws RemoteException {
        store.put(key, value);
        replicateToReplicas("put", key, value);
        return true;
    }

    @Override
    public boolean delete(String key) throws RemoteException {
        store.remove(key);
        replicateToReplicas("delete", key, null);
        return true;
    }

    @Override
    public boolean prepare(String operation, String key, String value) throws RemoteException {
        // In a real-world implementation, this would involve locking the resource
        // and checking for potential conflicts or readiness.
        System.out.println("Preparing operation: " + operation + " on key: " + key + " with value: " + value);
        return true; // Indicate readiness to commit
    }

    @Override
    public boolean commit(String operation, String key, String value) throws RemoteException {
        switch (operation.toLowerCase()) {
            case "put":
                store.put(key, value);
                return true;
            case "delete":
                store.remove(key);
                return true;
            default:
                return false;
        }
    }

    private void replicateToReplicas(String operation, String key, String value) {
        for (KeyValueStoreInterface replica : replicas) {
            try {
                // Prepare phase
                boolean ready = replica.prepare(operation, key, value);
                if (ready) {
                    // Commit phase
                    replica.commit(operation, key, value);
                } else {
                    System.err.println("Replica not ready to commit operation: " + operation);
                }
            } catch (RemoteException e) {
                System.err.println("Failed to replicate operation to a replica: " + e.getMessage());
            }
        }
    }

    public void connectToReplicas() {
        for (String replica : replicaServers) {
            int retries = 5;
            while (retries > 0) {
                try {
                    String[] parts = replica.split(":");
                    String host = parts[0];
                    int port = Integer.parseInt(parts[1]);

                    Registry registry = LocateRegistry.getRegistry(host, port);
                    KeyValueStoreInterface stub = (KeyValueStoreInterface) registry.lookup("KeyValueStore");

                    System.out.println("Connected to replica at " + replica);
                    replicas.add(stub);
                    break; // Exit retry loop on success
                } catch (Exception e) {
                    System.err.println("Failed to connect to replica at " + replica + ". Retrying... (" + retries
                            + " retries left)");
                    retries--;
                    try {
                        Thread.sleep(2000); // Wait before retrying
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            }

            if (retries == 0) {
                System.err.println("Could not connect to replica at " + replica);
            }
        }
    }

    public static void main(String[] args) {
        try {
            String host = InetAddress.getLocalHost().getHostName();
            int port = Integer.parseInt(System.getenv("PORT"));
            String replicas = System.getenv("REPLICAS");

            KeyValueStoreServer server = new KeyValueStoreServer(parseReplicas(replicas));
            LocateRegistry.createRegistry(port);
            Naming.rebind("//" + host + ":" + port + "/KeyValueStore", server);

            System.out.println("KeyValueStoreServer started on " + host + ":" + port);
            System.out.println("Replicas: " + replicas);

            // Add a delay before connecting to replicas
            Thread.sleep(5000); // Wait 5 seconds for other servers to initialize
            server.connectToReplicas();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<String> parseReplicas(String replicas) {
        return replicas == null || replicas.isEmpty() ? new ArrayList<>() : Arrays.asList(replicas.split(","));
    }
}
