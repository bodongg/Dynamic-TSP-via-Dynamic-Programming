# Dynamic TSP Package Delivery Optimizer

A Java project that explores a time-limited package delivery route with a vehicle capacity constraint. It includes a command-line version and a desktop GUI. Both use recursive dynamic programming with a bitmask to track which client locations have been visited.

## Problem overview

Location `0` is the depot. The remaining locations are clients. For each location, the program takes:

- the number of packages available to pick up;
- a delivery demand or unload capacity;
- travel times to and from every other location.

The vehicle starts at the depot with no packages. At each client, it unloads up to that location's demand from its current load, then picks up the available packages without exceeding the vehicle capacity. A route must visit every client once and return to the depot before the time limit. The program reports the largest load it can bring back to the depot.

## Algorithm

The solver represents the visited locations with a bitmask. For example, a set bit means that location has already been visited. It recursively tries each unvisited client, updates the vehicle load and elapsed travel time, and uses a memoization table to reuse subproblem results. It also prunes routes that exceed the time limit or cannot return to the depot in time.

The exact search is exponential, so the project limits the input to 15 locations. Vehicle capacity is limited to 50 and the time limit to 500.

## Requirements

- Java Development Kit (JDK) 9 or later
- No external libraries are required

## Compile

Open a terminal in the directory containing `V4TSPAlgo.java` and `TSP_GUI.java`:

```bash
javac -d out V4TSPAlgo.java TSP_GUI.java
```

The `-d out` option places compiled classes in an `out` directory and creates the package folders automatically.

## Run the console version

```bash
java -cp out tpsalgorithm.V4TSPAlgo
```

Enter the number of locations, vehicle capacity, and time limit when prompted. Then enter:

1. One package count for each location, including the depot.
2. One delivery demand or unload capacity for each location.
3. The complete square travel-time matrix, one row at a time.

All values must be non-negative integers. The number of locations must be between 2 and 15, vehicle capacity between 1 and 50, and the time limit between 1 and 500.

## Run the GUI version

```bash
java -cp out tpsalgorithm.TSP_GUI
```

Set the number of locations, vehicle capacity, and time limit at the top of the window. Select **Initialize Tables**, enter package counts, unload demands, and travel times in the tabs, then select **Run Algorithm**. The result and execution time appear in the output area.

The GUI starts with sample table values. Edit them to try a different scenario. The distance matrix may be asymmetric; the program uses the travel times exactly as entered.

## Important implementation note

This is an educational implementation and its current solver does not guarantee an optimal result for every input. The memoization table does not include elapsed time in its state key, even though route feasibility depends on elapsed time. Its global-best pruning can also affect values cached for later calls. Treat results as demonstrations of the approach until those issues are corrected. If no complete route fits the time limit, the current program may display `-999999` instead of a friendly “no feasible route” message.

## Files

| File | Purpose |
|---|---|
| `V4TSPAlgo.java` | Console input and dynamic programming solver |
| `TSP_GUI.java` | Swing interface and dynamic programming solver |

Both source files use the Java package `tpsalgorithm`.
