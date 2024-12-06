import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.InetAddress;

public class KeyValueStoreServer extends UnicastRemoteObject implements KeyValueStoreInterface {
    private final Map<String, String> store = new HashMap<>();
    private final int nodeId;
    private final List<String> peerAddresses;
    private final AtomicInteger activeOperations = new AtomicInteger(0); // Track active operations
    private final Timer shutdownTimer = new Timer(); // Timer for graceful shutdown

    private int proposalCounter = 0;
    private int highestPromised = 0;
    private int highestAcceptedProposal = 0;
    private String acceptedValue = null;
    private final Random random = new Random();

    // ANSI color codes for terminal output
    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";

    public KeyValueStoreServer(int nodeId, List<String> peerAddresses) throws RemoteException {
        this.nodeId = nodeId;
        this.peerAddresses = peerAddresses;
        startShutdownWatcher();
    }

    private void startShutdownWatcher() {
        shutdownTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (activeOperations.get() == 0) {
                    System.out.println(
                            PURPLE + "Node " + nodeId + " - No active operations. Shutting down gracefully." + RESET);
                    shutdown();
                }
            }
        }, 5000, 5000); // Check every 5 seconds
    }

    private void shutdown() {
        try {
            UnicastRemoteObject.unexportObject(this, true);
            shutdownTimer.cancel();
            System.out.println(GREEN + "Node " + nodeId + " - Server stopped successfully." + RESET);
            System.exit(0);
        } catch (Exception e) {
            System.out.println(RED + "Node " + nodeId + " - Error during shutdown: " + e.getMessage() + RESET);
        }
    }

    @Override
    public String get(String key) throws RemoteException {
        activeOperations.incrementAndGet();
        System.out.println(BLUE + "Node " + nodeId + " - GET operation: Key = " + key + RESET);
        String result = store.getOrDefault(key, null);
        activeOperations.decrementAndGet();
        return result;
    }

    @Override
    public boolean put(String key, String value) throws RemoteException {
        activeOperations.incrementAndGet();
        System.out.println(BLUE + "Node " + nodeId + " - PUT operation: Key = " + key + ", Value = " + value + RESET);
        boolean result = executeWithRetries("PUT:" + key + ":" + value);
        activeOperations.decrementAndGet();
        return result;
    }

    @Override
    public boolean delete(String key) throws RemoteException {
        activeOperations.incrementAndGet();
        System.out.println(BLUE + "Node " + nodeId + " - DELETE operation: Key = " + key + RESET);
        boolean result = executeWithRetries("DELETE:" + key);
        activeOperations.decrementAndGet();
        return result;
    }

    private boolean executeWithRetries(String operation) {
        int retries = 5; // Retry up to 5 times
        while (retries > 0) {
            int proposalNumber = generateProposalNumber(); // Incrementing proposal number
            System.out.println(BLUE + "Node " + nodeId + " - Starting Paxos for operation: " + operation
                    + " with proposal: " + proposalNumber + RESET);

            if (runPaxos(proposalNumber, operation)) {
                System.out.println(GREEN + "Node " + nodeId + " - Operation succeeded: " + operation + RESET);
                return true;
            }

            System.out
                    .println(YELLOW + "Node " + nodeId + " - Operation failed: " + operation + ". Retrying..." + RESET);
            retries--;
        }

        System.out.println(
                PURPLE + "Node " + nodeId + " - Operation permanently failed after retries: " + operation + RESET);
        return false;
    }

    private int generateProposalNumber() {
        proposalCounter++;
        return proposalCounter * 1000 + nodeId; // Ensures unique and incrementing proposal numbers
    }

    private boolean runPaxos(int proposalNumber, String operation) {
        // Phase 1: Prepare
        int prepareAckCount = 0;
        for (String peer : peerAddresses) {
            try {
                String[] parts = peer.split(":");
                Registry registry = LocateRegistry.getRegistry(parts[0], Integer.parseInt(parts[1]));
                KeyValueStoreInterface peerStub = (KeyValueStoreInterface) registry.lookup("KeyValueStore");

                if (peerStub.prepare(proposalNumber, operation)) {
                    prepareAckCount++;
                }
            } catch (Exception e) {
                System.out.println(
                        RED + "Node " + nodeId + " - Prepare failed for peer " + peer + ": " + e.getMessage() + RESET);
            }
        }

        System.out.println(BLUE + "Node " + nodeId + " - Prepare phase completed. Acks: " + prepareAckCount + RESET);

        if (prepareAckCount <= peerAddresses.size() / 2) {
            System.out.println(RED + "Node " + nodeId + " - Prepare phase failed." + RESET);
            return false;
        }

        // Phase 2: Accept
        int acceptAckCount = 0;
        for (String peer : peerAddresses) {
            try {
                String[] parts = peer.split(":");
                Registry registry = LocateRegistry.getRegistry(parts[0], Integer.parseInt(parts[1]));
                KeyValueStoreInterface peerStub = (KeyValueStoreInterface) registry.lookup("KeyValueStore");

                if (peerStub.accept(proposalNumber, operation)) {
                    acceptAckCount++;
                }
            } catch (Exception e) {
                System.out.println(
                        RED + "Node " + nodeId + " - Accept failed for peer " + peer + ": " + e.getMessage() + RESET);
            }
        }

        System.out.println(BLUE + "Node " + nodeId + " - Accept phase completed. Acks: " + acceptAckCount + RESET);

        if (acceptAckCount <= peerAddresses.size() / 2) {
            System.out.println(RED + "Node " + nodeId + " - Accept phase failed." + RESET);
            return false;
        }

        // Phase 3: Learn
        for (String peer : peerAddresses) {
            try {
                String[] parts = peer.split(":");
                Registry registry = LocateRegistry.getRegistry(parts[0], Integer.parseInt(parts[1]));
                KeyValueStoreInterface peerStub = (KeyValueStoreInterface) registry.lookup("KeyValueStore");

                peerStub.learn(operation);
            } catch (Exception e) {
                System.out.println(
                        RED + "Node " + nodeId + " - Learn failed for peer " + peer + ": " + e.getMessage() + RESET);
            }
        }

        System.out.println(GREEN + "Node " + nodeId + " - Learn phase completed." + RESET);
        applyOperation(operation);
        return true;
    }

    @Override
    public boolean prepare(int proposalNumber, String operation) throws RemoteException {
        synchronized (this) {
            // Simulate random failure
            if (random.nextDouble() < 0.2) { // 20% chance to fail
                System.out.println(RED + "Node " + nodeId + " - Simulating failure during Prepare phase for proposal: "
                        + proposalNumber + RESET);
                return false;
            }

            if (proposalNumber > highestPromised) {
                highestPromised = proposalNumber;
                System.out
                        .println(GREEN + "Node " + nodeId + " - Promise made for proposal: " + proposalNumber + RESET);
                return true;
            }

            System.out.println(RED + "Node " + nodeId + " - Rejected proposal: " + proposalNumber
                    + " (Highest Promised: " + highestPromised + ")" + RESET);
            return false;
        }
    }

    @Override
    public boolean accept(int proposalNumber, String operation) throws RemoteException {
        synchronized (this) {
            // Simulate random failure
            if (random.nextDouble() < 0.4) { // 40% chance to fail
                System.out.println(RED + "Node " + nodeId + " - Simulating failure during Accept phase for proposal: "
                        + proposalNumber + RESET);
                return false;
            }

            if (proposalNumber >= highestPromised) {
                highestPromised = proposalNumber;
                highestAcceptedProposal = proposalNumber;
                acceptedValue = operation;
                System.out.println(GREEN + "Node " + nodeId + " - Accepted proposal: " + proposalNumber + RESET);
                return true;
            }

            System.out.println(RED + "Node " + nodeId + " - Rejected proposal: " + proposalNumber
                    + " (Highest Promised: " + highestPromised + ")" + RESET);
            return false;
        }
    }

    @Override
    public void learn(String operation) throws RemoteException {
        applyOperation(operation);
    }

    private void applyOperation(String operation) {
        String[] parts = operation.split(":");
        String type = parts[0];
        String key = parts[1];
        String value = parts.length > 2 ? parts[2] : null;

        synchronized (this) {
            switch (type) {
                case "PUT":
                    store.put(key, value);
                    System.out.println(GREEN + "Node " + nodeId + " - Key added: " + key + " = " + value + RESET);
                    break;
                case "DELETE":
                    store.remove(key);
                    System.out.println(GREEN + "Node " + nodeId + " - Key deleted: " + key + RESET);
                    break;
            }
        }
    }

    public static void main(String[] args) {
        try {
            String host = InetAddress.getLocalHost().getHostName();
            int port = Integer.parseInt(System.getenv("PORT"));
            int nodeId = Integer.parseInt(System.getenv("NODE_ID"));
            String peers = System.getenv("PEERS");

            List<String> peerAddresses = parsePeers(peers);

            KeyValueStoreServer server = new KeyValueStoreServer(nodeId, peerAddresses);
            LocateRegistry.createRegistry(port);
            Naming.rebind("//" + host + ":" + port + "/KeyValueStore", server);

            System.out.println(GREEN + "KeyValueStoreServer started on " + host + ":" + port + RESET);
        } catch (Exception e) {
            System.out.println(RED + "Error starting server: " + e.getMessage() + RESET);
            e.printStackTrace();
        }
    }

    private static List<String> parsePeers(String peers) {
        return peers == null || peers.isEmpty() ? new ArrayList<>() : Arrays.asList(peers.split(","));
    }
}
