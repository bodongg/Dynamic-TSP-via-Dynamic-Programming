package tpsalgorithm;

import java.util.*;

public class V4TSPAlgo {

    static int N;
    static int[] packages;  // Packages available to pick up at each location
    static int[] capacity;  // Delivery demand/capacity at each location (unload here)
    static int[][] distance;
    static int MAX_CARRY_CAPACITY;
    static int TIME_LIMIT;

    // Limits
    static final int MAX_NODES = 15;
    static final int MAX_CAPACITY = 50;
    static final int MAX_TIME_LIMIT = 500;

    // DP table: dp[mask][node][currentPackages] = max packages delivered from this state
    static int[][][] dp;
    static int[] remainingPackages;
    static int globalMax = 0;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        try {
            System.out.println("=== Traveling Salesman Package Delivery Optimizer ===\n");
            
            // Input validation for N
            System.out.print("Enter number of locations including depot (2-" + MAX_NODES + "): ");
            N = scanner.nextInt();
            if (N < 2 || N > MAX_NODES) {
                System.out.println("ERROR: N must be between 2 and " + MAX_NODES);
                return;
            }

            // Input validation for MAX_CARRY_CAPACITY
            System.out.print("Enter maximum vehicle capacity (1-" + MAX_CAPACITY + "): ");
            MAX_CARRY_CAPACITY = scanner.nextInt();
            if (MAX_CARRY_CAPACITY < 1 || MAX_CARRY_CAPACITY > MAX_CAPACITY) {
                System.out.println("ERROR: Vehicle capacity must be between 1 and " + MAX_CAPACITY);
                return;
            }

            // Input validation for TIME_LIMIT
            System.out.print("Enter time limit (1-" + MAX_TIME_LIMIT + "): ");
            TIME_LIMIT = scanner.nextInt();
            if (TIME_LIMIT < 1 || TIME_LIMIT > MAX_TIME_LIMIT) {
                System.out.println("ERROR: Time limit must be between 1 and " + MAX_TIME_LIMIT);
                return;
            }

            // Memory estimation
            long estimatedStates = (1L << N) * N * (MAX_CARRY_CAPACITY + 1);
            long estimatedMemoryMB = (estimatedStates * 4) / (1024 * 1024);  // int is 4 bytes
            System.out.printf("Estimated memory usage: ~%d MB%n", estimatedMemoryMB);
            
            if (estimatedMemoryMB > 500) {
                System.out.print("WARNING: High memory usage. Continue? (y/n): ");
                String response = scanner.next();
                if (!response.equalsIgnoreCase("y")) {
                    System.out.println("Operation cancelled.");
                    return;
                }
            }

            // Input packages
            packages = new int[N];
            System.out.println("\nEnter packages available at each location (" + N + " values, space-separated):");
            System.out.print("Packages [0=" + "depot" + ", 1-" + (N-1) + "=clients]: ");
            for (int i = 0; i < N; i++) {
                packages[i] = scanner.nextInt();
                if (packages[i] < 0) {
                    System.out.println("ERROR: Packages cannot be negative");
                    return;
                }
            }

            // Input capacities (delivery demands at each location)
            capacity = new int[N];
            System.out.println("\nEnter delivery capacity/demand at each location (" + N + " values, space-separated):");
            System.out.print("Capacities [0=" + "depot" + ", 1-" + (N-1) + "=clients]: ");
            for (int i = 0; i < N; i++) {
                capacity[i] = scanner.nextInt();
                if (capacity[i] < 0) {
                    System.out.println("ERROR: Capacities cannot be negative");
                    return;
                }
            }

            // Input distance matrix
            distance = new int[N][N];
            System.out.println("\nEnter travel time matrix (" + N + "x" + N + " matrix, row by row):");
            for (int i = 0; i < N; i++) {
                System.out.print("Row " + i + ": ");
                for (int j = 0; j < N; j++) {
                    distance[i][j] = scanner.nextInt();
                    if (distance[i][j] < 0) {
                        System.out.println("ERROR: Travel times cannot be negative");
                        return;
                    }
                }
            }

            // Print input summary
            System.out.println("\n=== INPUT SUMMARY ===");
            System.out.println("Locations: " + N);
            System.out.println("Vehicle capacity: " + MAX_CARRY_CAPACITY);
            System.out.println("Time limit: " + TIME_LIMIT);
            System.out.println("Packages: " + Arrays.toString(packages));
            System.out.println("Capacities: " + Arrays.toString(capacity));
            System.out.println("Travel time matrix:");
            for (int i = 0; i < N; i++) {
                System.out.println("  " + Arrays.toString(distance[i]));
            }
            System.out.println();

            // Precompute remaining packages
            remainingPackages = new int[1 << N];
            for (int mask = 0; mask < (1 << N); mask++) {
                int sum = 0;
                for (int i = 1; i < N; i++) {
                    if ((mask & (1 << i)) == 0) {
                        sum += packages[i];
                    }
                }
                remainingPackages[mask] = sum;
            }

            // Initialize DP table with -1 (uncomputed)
            dp = new int[1 << N][N][MAX_CARRY_CAPACITY + 1];
            for (int[][] layer : dp) {
                for (int[] row : layer) {
                    Arrays.fill(row, -1);
                }
            }

            // Start timing
            System.out.println("Computing optimal solution...");
            long startTime = System.nanoTime();

            int result = solve(1 << 0, 0, 0, 0);  // Start from depot, mask=1 (depot visited), packages=0, time=0

            // End timing
            long endTime = System.nanoTime();
            long durationNano = endTime - startTime;
            double durationMillis = durationNano / 1_000_000.0;
            double durationSeconds = durationNano / 1_000_000_000.0;

            System.out.println("\n=== RESULTS ===");
            System.out.println("Maximum packages delivered: " + result);
            System.out.println("\n=== PERFORMANCE ===");
            System.out.println("Execution time: " + durationNano + " nanoseconds");
            System.out.println("Execution time: " + String.format("%.3f", durationMillis) + " milliseconds");
            System.out.println("Execution time: " + String.format("%.6f", durationSeconds) + " seconds");

        } catch (InputMismatchException e) {
            System.out.println("ERROR: Invalid input format. Please enter valid integers.");
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    static int solve(int mask, int node, int currentPackages, int time) {
        // Pruning: Exceed time or already computed
        if (time > TIME_LIMIT) return -999999;
        if (dp[mask][node][currentPackages] != -1) return dp[mask][node][currentPackages];

        // Upper-bound pruning
        int upperBound = currentPackages + remainingPackages[mask];
        if (upperBound <= globalMax) return -999999;

        // Base case: All clients visited, return to depot
        if (mask == (1 << N) - 1) {
            int timeToDepot = distance[node][0];
            if (time + timeToDepot <= TIME_LIMIT) {
                int finalDelivered = currentPackages;  // Packages delivered so far
                globalMax = Math.max(globalMax, finalDelivered);
                return dp[mask][node][currentPackages] = finalDelivered;
            } else {
                return dp[mask][node][currentPackages] = -999999;
            }
        }

        int best = -999999;

        // Try all unvisited clients
        for (int next = 1; next < N; next++) {  // Skip depot (0) and current node
            if ((mask & (1 << next)) != 0) continue;  // Already visited

            int travelTime = distance[node][next];
            int newTime = time + travelTime;

            // Pruning: Can't reach next or return from there
            if (newTime > TIME_LIMIT || newTime + distance[next][0] > TIME_LIMIT) continue;

            // Update packages: Deliver at next (unload up to capacity[next]), then pick up packages[next]
            int newLoad = currentPackages;
            if (capacity[next] > 0) {
                newLoad = Math.max(0, newLoad - capacity[next]);  // Deliver (unload) up to demand
            }
            newLoad = Math.min(MAX_CARRY_CAPACITY, newLoad + packages[next]);  // Pick up new packages

            // Recursive call
            int newMask = mask | (1 << next);
            int futureBest = solve(newMask, next, newLoad, newTime);
            best = Math.max(best, futureBest);
        }

        // Memoize and return
        globalMax = Math.max(globalMax, best);
        return dp[mask][node][currentPackages] = best;
    }
}
