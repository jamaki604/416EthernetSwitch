# Network Simulation Project

This project simulates a network with hosts, switches, and routers using Java. Routers implement Distance Vector (DV) routing to forward packets between subnets. The simulation runs on localhost using UDP sockets.

## Prerequisites
- **Java Development Kit (JDK)**: JDK 17 or later.
- **IntelliJ IDEA**: Community or Ultimate edition.
- **Project Files**:
  - Java classes: `ConfigLoader.java`, `DistanceVector.java`, `Host.java`, `Router.java`, `Switch.java`.
  - Configuration file: `config.properties` (must be in the `src/` directory).

## Setup in IntelliJ IDEA
1. **Open the Project**:
   - In IntelliJ, go to **File > Open** and select the project directory (e.g., `EthernetSwitch`).
   - Ensure the `src/` directory contains all Java files and `config.properties`.

2. **Verify Project Structure**:
   - Go to **File > Project Structure**.
   - Under **Project**, confirm the Project SDK is set to your JDK.
   - Under **Modules**, ensure the `src` folder is marked as the source root (highlighted in blue).
   - Click **Apply** and **OK**.

3. **Create Run Configurations**:
   - Go to **Run > Edit Configurations**.
   - For each device (3 hosts, 3 switches, 6 routers), create an **Application** configuration:
     - **Host A**: Main class `Host`, Program arguments `A`.
     - **Host B**: Main class `Host`, Program arguments `B`.
     - **Host C**: Main class `Host`, Program arguments `C`.
     - **Switch S1**: Main class `Switch`, Program arguments `S1`.
     - **Switch S2**: Main class `Switch`, Program arguments `S2`.
     - **Switch S3**: Main class `Switch`, Program arguments `S3`.
     - **Router R1**: Main class `Router`, Program arguments `R1`.
     - **Router R2**: Main class `Router`, Program arguments `R2`.
     - **Router R3**: Main class `Router`, Program arguments `R3`.
     - **Router R4**: Main class `Router`, Program arguments `R4`.
     - **Router R5**: Main class `Router`, Program arguments `R5`.
     - **Router R6**: Main class `Router`, Program arguments `R6`.
   - Enable parallel execution:
     - In **Run > Edit Configurations**, click the gear icon, select **Application**, check **Allow parallel run**, and click **Apply**.

## Starting the Project
Devices must be started in a specific order to ensure proper initialization and communication. Each device runs as a separate process in IntelliJ.

1. **Start the Switches**:
   - Run the following configurations:
     - `Switch S1`
     - `Switch S2`
     - `Switch S3`
   - Output example: `Switch S1 listening on port 6001`.

2. **Start the Routers**:
   - After all switches are running, start the routers:
     - `Router R1`
     - `Router R2`
     - `Router R3`
     - `Router R4`
     - `Router R5`
     - `Router R6`
   - Wait for the routers to converge (5–10 seconds). Look for DV updates in the logs, e.g., `Router R6 updated its Distance Vector`, until they stop.

3. **Start the Hosts**:
   - Once routers have converged, start the hosts:
     - `Host A`
     - `Host B`
     - `Host C`
   - Output example: `Host A listening for messages...`.

## Running the Test Cases
With all devices running, run the following test cases to verify packet routing. Enter the commands in the **Run** tab of each host in IntelliJ.

- **Test Case 1: Host A to Host C**
  - In `Host A`:
    ```
    Enter Destination IP (format: [subnet].[HostID])
    net3.C
    Enter Message:
    Hello from A to C
    ```
  - Expected path: A → S1 → R1 → R2 → R4 → R6 → S3 → C

- **Test Case 2: Host B to Host A**
  - In `Host B`:
    ```
    Enter Destination IP (format: [subnet].[HostID])
    net1.A
    Enter Message:
    Hello from B to A
    ```
  - Expected path: B → S2 → R3 → R1 → S1 → A

- **Test Case 3: Host C to Host B**
  - In `Host C`:
    ```
    Enter Destination IP (format: [subnet].[HostID])
    net2.B
    Enter Message:
    Hello from C to B
    ```
  - Expected path: C → S3 → R6 → R4 → R5 → R3 → S2 → B

## Verifying Results
- Check the logs in the **Run** tab for each device to trace the packet paths.
- Ensure the `Switch` forwarding tables (e.g., S3) only contain directly connected devices (e.g., `C` and `R6` for S3).
- Confirm each packet reaches its destination with the expected message (e.g., `Message received from A: Hello from A to C` in Host C’s log).

## Stopping the Simulation
- Stop all devices by clicking the red "Stop" button in the **Run** tab for each configuration.
