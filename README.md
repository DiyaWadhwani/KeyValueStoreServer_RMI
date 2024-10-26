# Java RMI Key-Value Store

This project implements a simple Key-Value Store using Java Remote Method Invocation (RMI). It includes a server that handles key-value storage and a client that interacts with the server to perform operations like `put`, `get`, and `delete`.

## Features

- Multi-threaded client support
- Concurrent key-value storage using `ConcurrentHashMap`
- Logging of operations and errors to both console and file
- Custom logging format for better readability
- RMI-based architecture for remote method invocation

## Requirements

- Java Development Kit (JDK) 17 or higher
- RMI Registry (automatically started by the server)
- Docker

## How to Run

### Using Docker

1. **Create the network:**

   ```bash
   docker network create kv_store_network
   ```

2. **Build the Docker server and client images:**

   ```bash
   docker build -t kv_store_server -f Dockerfile.server .
   docker build -t kv_store_client -f Dockerfile.client .
   ```

3. **Run the server and client:**

   ```bash
   docker run -d --name kv_store_server --network kv_store_network kv_store_server
   docker run --rm --name kv_store_client --network kv_store_network kv_store_client
   ```

# Client Operations

The client performs the following operations:

- **Pre-populate key-value pairs:**  
  `client1_name`, `client1_age`, etc. for each client.

- **Retrieve values:**  
  Log the retrieved values to the console.

- **Delete key-value pairs:**  
  Remove the entries after retrieval.

# Logging

**You can check the server or client logs using the following commands:**
`bash
    docker logs kv_store_server
    docker logs kv_store_client
    `

# Custom Logger Format

The `CustomFormatter` class formats the log messages to include timestamps and log levels for improved readability.

# License

This project is licensed under the MIT License - see the LICENSE file for details.
