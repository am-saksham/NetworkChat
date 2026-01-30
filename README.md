# NetworkChat

This project has been separated into `backend` (Server) and `frontend` (Client).

## How to Run

### Using Scripts (Mac/Linux)

1.  **Run Server**:
    ```bash
    ./run_server.sh
    ```
    This will start the server on port `8192`.

2.  **Run Client**:
    ```bash
    ./run_client.sh
    ```
    This will launch the client login window.

### Manual Steps

#### Server
1.  Navigate to the project root.
2.  Compile the server code:
    ```bash
    mkdir -p backend/bin
    javac -d backend/bin backend/src/com/saksham/networkchat/server/*.java
    ```
3.  Run the server (replace `8192` with your desired port):
    ```bash
    java -cp backend/bin com.saksham.networkchat.server.ServerMain 8192
    ```

#### Client
1.  Navigate to the project root.
2.  Compile the client code:
    ```bash
    mkdir -p frontend/bin
    javac -d frontend/bin frontend/src/com/saksham/networkchat/*.java
    ```
3.  Run the client:
    ```bash
    java -cp frontend/bin com.saksham.networkchat.Login
    ```

## Eclipse Setup
You can import the `backend` and `frontend` folders as existing Java projects into Eclipse.
