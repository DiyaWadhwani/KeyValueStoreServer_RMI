import java.rmi.Remote;
import java.rmi.RemoteException;

public interface KeyValueStoreInterface extends Remote {
    /**
     * Retrieves the value associated with the specified key.
     *
     * @param key The key to look up.
     * @return The value associated with the key, or null if not found.
     * @throws RemoteException If an RMI error occurs.
     */
    String get(String key) throws RemoteException;

    /**
     * Stores the specified key-value pair in the store.
     *
     * @param key   The key to store.
     * @param value The value to associate with the key.
     * @return True if the operation succeeded, false otherwise.
     * @throws RemoteException If an RMI error occurs.
     */
    boolean put(String key, String value) throws RemoteException;

    /**
     * Deletes the specified key from the store.
     *
     * @param key The key to delete.
     * @return True if the operation succeeded, false otherwise.
     * @throws RemoteException If an RMI error occurs.
     */
    boolean delete(String key) throws RemoteException;

    /**
     * Prepares for a two-phase commit operation.
     *
     * @param operation The operation to perform (e.g., "PUT", "DELETE").
     * @param key       The key involved in the operation.
     * @param value     The value involved in the operation (only for PUT).
     * @return True if the server is ready to commit, false otherwise.
     * @throws RemoteException If an RMI error occurs.
     */
    boolean prepare(String operation, String key, String value) throws RemoteException;

    /**
     * Commits the specified operation.
     *
     * @param operation The operation to perform (e.g., "PUT", "DELETE").
     * @param key       The key involved in the operation.
     * @param value     The value involved in the operation (only for PUT).
     * @throws RemoteException If an RMI error occurs.
     */
    boolean commit(String operation, String key, String value) throws RemoteException;
}
