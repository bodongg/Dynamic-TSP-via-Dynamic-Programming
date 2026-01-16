package tpsalgorithm;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Arrays;
import java.util.InputMismatchException;

public class TSP_GUI extends JFrame {
    // Algorithm parameters (shared with algorithm methods)
    static int N;
    static int[] packagesArr;
    static int[] capacityArr;
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

    // UI components
    private final JTextField nField = new JTextField("5", 4);
    private final JTextField maxLoadField = new JTextField("10", 4);
    private final JTextField timeLimitField = new JTextField("100", 6);
    private JTable packagesTable;
    private JTable capacityTable;
    private JTable distanceTable;
    private final JTextArea outputArea = new JTextArea(12, 50);
    private final JButton runButton = new JButton("Run Algorithm");
    private final JButton resizeTablesButton = new JButton("Initialize Tables");
    private final JLabel estimateLabel = new JLabel("Estimated memory: 0 MB");

    public TSP_GUI() {
        setTitle("V4 TSP Algorithm - GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        topPanel.add(new JLabel("Number of nodes (including depot):"));
        topPanel.add(nField);
        topPanel.add(new JLabel("MAX_LOAD:"));
        topPanel.add(maxLoadField);
        topPanel.add(new JLabel("TIME_LIMIT:"));
        topPanel.add(timeLimitField);
        topPanel.add(resizeTablesButton);
        add(topPanel, BorderLayout.NORTH);

        // Center: tables in tabs
        JTabbedPane tabs = new JTabbedPane();

        packagesTable = new JTable(1, 1);
        packagesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);  // Allow horizontal scrolling
        capacityTable = new JTable(1, 1);
        capacityTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);  // Allow horizontal scrolling
        distanceTable = new JTable(1, 1);
        distanceTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);  // Allow horizontal scrolling

        tabs.addTab("Packages", new JScrollPane(packagesTable));
        tabs.addTab("Capacities", new JScrollPane(capacityTable));
        tabs.addTab("Distance Matrix", new JScrollPane(distanceTable));

        add(tabs, BorderLayout.CENTER);

        // Bottom: controls and output
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(estimateLabel);
        controlPanel.add(runButton);
        bottomPanel.add(controlPanel, BorderLayout.NORTH);

        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        bottomPanel.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);

        // Wire actions
        resizeTablesButton.addActionListener(e -> resizeTables());
        runButton.addActionListener(e -> onRun());

        pack();
        setLocationRelativeTo(null);
        resizeTables();
    }

    private void resizeTables() {
        try {
            int newN = Integer.parseInt(nField.getText().trim());
            if (newN < 2 || newN > MAX_NODES) {
                JOptionPane.showMessageDialog(this, "N must be between 2 and " + MAX_NODES);
                return;
            }
            
            // Store existing values if tables already have data
            int oldN = packagesTable.getColumnCount();
            int[] oldPackages = new int[Math.min(oldN, newN)];
            int[] oldCapacities = new int[Math.min(oldN, newN)];
            int[][] oldDistance = new int[Math.min(oldN, newN)][Math.min(oldN, newN)];
            
            // Save existing data
            for (int i = 0; i < Math.min(oldN, newN); i++) {
                Object pVal = packagesTable.getValueAt(0, i);
                oldPackages[i] = pVal != null ? Integer.parseInt(pVal.toString().trim()) : 0;
                
                Object cVal = capacityTable.getValueAt(0, i);
                oldCapacities[i] = cVal != null ? Integer.parseInt(cVal.toString().trim()) : 0;
                
                for (int j = 0; j < Math.min(oldN, newN); j++) {
                    Object dVal = distanceTable.getValueAt(i, j);
                    oldDistance[i][j] = dVal != null ? Integer.parseInt(dVal.toString().trim()) : 0;
                }
            }
            
            // Packages table: single row of length N
            DefaultTableModel pm = new DefaultTableModel(1, newN);
            packagesTable.setModel(pm);
            packagesTable.setRowHeight(22);
            packagesTable.setBackground(Color.WHITE);
            packagesTable.setFillsViewportHeight(true);
            // Set wider column width for better visibility
            for (int i = 0; i < newN; i++) {
                packagesTable.getColumnModel().getColumn(i).setPreferredWidth(60);
                packagesTable.getColumnModel().getColumn(i).setMinWidth(50);
            }

            // Capacities table: single row
            DefaultTableModel cm = new DefaultTableModel(1, newN);
            capacityTable.setModel(cm);
            capacityTable.setRowHeight(22);
            capacityTable.setBackground(Color.WHITE);
            // Set wider column width for better visibility
            for (int i = 0; i < newN; i++) {
                capacityTable.getColumnModel().getColumn(i).setPreferredWidth(60);
                capacityTable.getColumnModel().getColumn(i).setMinWidth(50);
            }

            // Distance matrix: NxN
            DefaultTableModel dm = new DefaultTableModel(newN, newN);
            distanceTable.setModel(dm);
            distanceTable.setRowHeight(22);
            distanceTable.setBackground(Color.WHITE);
            // Set wider column width for better visibility
            for (int i = 0; i < newN; i++) {
                distanceTable.getColumnModel().getColumn(i).setPreferredWidth(50);
                distanceTable.getColumnModel().getColumn(i).setMinWidth(45);
            }

            // Restore or fill defaults
            for (int i = 0; i < newN; i++) {
                if (i < Math.min(oldN, newN) && oldN > 1) {
                    // Restore existing values
                    packagesTable.setValueAt(oldPackages[i], 0, i);
                    capacityTable.setValueAt(oldCapacities[i], 0, i);
                    for (int j = 0; j < newN; j++) {
                        if (j < Math.min(oldN, newN)) {
                            distanceTable.setValueAt(oldDistance[i][j], i, j);
                        } else {
                            // New columns get defaults
                            distanceTable.setValueAt(i == j ? 0 : 1, i, j);
                        }
                    }
                } else {
                    // New rows/columns get defaults
                    packagesTable.setValueAt(i == 0 ? 0 : 1, 0, i);
                    capacityTable.setValueAt(i == 0 ? 0 : 5, 0, i);
                    for (int j = 0; j < newN; j++) {
                        if (i == j) distanceTable.setValueAt(0, i, j);
                        else distanceTable.setValueAt(1 + Math.abs(i - j), i, j);
                    }
                }
            }

            // Update estimate
            long estimatedStates = (1L << newN) * newN * (Long.parseLong(maxLoadField.getText().isEmpty() ? "1" : maxLoadField.getText().trim()) + 1);
            long estimatedMemoryMB = (estimatedStates * 4) / (1024 * 1024);  // int is 4 bytes
            estimateLabel.setText("Estimated memory: ~" + estimatedMemoryMB + " MB");

            pack();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid integer in N or MAX_LOAD");
        }
    }

    private void onRun() {
        // Validate and read inputs from UI. Then run algorithm in SwingWorker.
        try {
        	
        	if (packagesTable.isEditing()) packagesTable.getCellEditor().stopCellEditing();
            if (capacityTable.isEditing()) capacityTable.getCellEditor().stopCellEditing();
            if (distanceTable.isEditing()) distanceTable.getCellEditor().stopCellEditing();
        	
            N = Integer.parseInt(nField.getText().trim());
            if (N < 2 || N > MAX_NODES) {
                JOptionPane.showMessageDialog(this, "N must be between 2 and " + MAX_NODES);
                return;
            }
            MAX_CARRY_CAPACITY = Integer.parseInt(maxLoadField.getText().trim());
            if (MAX_CARRY_CAPACITY < 1 || MAX_CARRY_CAPACITY > MAX_CAPACITY) {
                JOptionPane.showMessageDialog(this, "MAX_LOAD must be between 1 and " + MAX_CAPACITY);
                return;
            }
            TIME_LIMIT = Integer.parseInt(timeLimitField.getText().trim());
            if (TIME_LIMIT < 1 || TIME_LIMIT > MAX_TIME_LIMIT) {
                JOptionPane.showMessageDialog(this, "TIME_LIMIT must be between 1 and " + MAX_TIME_LIMIT);
                return;
            }

            // Read packages
            packagesArr = new int[N];
            for (int i = 0; i < N; i++) {
                Object val = packagesTable.getValueAt(0, i);
                if (val == null) val = "0";
                int p = Integer.parseInt(val.toString().trim());
                if (p < 0) throw new InputMismatchException("Packages cannot be negative");
                packagesArr[i] = p;
            }

            // Read capacities
            capacityArr = new int[N];
            for (int i = 0; i < N; i++) {
                Object val = capacityTable.getValueAt(0, i);
                if (val == null) val = "0";
                int c = Integer.parseInt(val.toString().trim());
                if (c < 0) throw new InputMismatchException("Capacities cannot be negative");
                capacityArr[i] = c;
            }

            // Read distance matrix
            distance = new int[N][N];
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    Object val = distanceTable.getValueAt(i, j);
                    if (val == null) val = "0";
                    int d = Integer.parseInt(val.toString().trim());
                    if (d < 0) throw new InputMismatchException("Distances cannot be negative");
                    distance[i][j] = d;
                }
            }

            // Print input summary to verify
            StringBuilder inputSummary = new StringBuilder();
            inputSummary.append("=== INPUT SUMMARY ===\n");
            inputSummary.append("Locations: ").append(N).append('\n');
            inputSummary.append("Vehicle capacity: ").append(MAX_CARRY_CAPACITY).append('\n');
            inputSummary.append("Time limit: ").append(TIME_LIMIT).append('\n');
            inputSummary.append("Packages: ").append(Arrays.toString(packagesArr)).append('\n');
            inputSummary.append("Capacities: ").append(Arrays.toString(capacityArr)).append('\n');
            inputSummary.append("Travel time matrix:\n");
            for (int i = 0; i < N; i++) {
                inputSummary.append("  ").append(Arrays.toString(distance[i])).append('\n');
            }
            inputSummary.append('\n');

            // Start worker
            runButton.setEnabled(false);
            outputArea.setText(inputSummary.toString());
            outputArea.append("Computing optimal solution...\n");

            SwingWorker<Integer, Void> worker = new SwingWorker<>() {
                long startTimeNano;
                long initStartTime;

                @Override
                protected Integer doInBackground() {
                    // Start timing from the beginning of all algorithm work
                    initStartTime = System.nanoTime();
                    
                    // Precompute remaining packages
                    remainingPackages = new int[1 << N];
                    for (int mask = 0; mask < (1 << N); mask++) {
                        int sum = 0;
                        for (int i = 1; i < N; i++) {
                            if ((mask & (1 << i)) == 0) {
                                sum += packagesArr[i];
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
                    globalMax = 0;
                    
                    // Start timing just the solve() call
                    startTimeNano = System.nanoTime();
                    int res = solve(1 << 0, 0, 0, 0);
                    return res;
                }

                @Override
                protected void done() {
                    runButton.setEnabled(true);
                    try {
                        int result = get();
                        long endTimeNano = System.nanoTime();
                        
                        long totalDurationNano = endTimeNano - initStartTime;
                        
                        double totalMillis = totalDurationNano / 1_000_000.0;
                        double totalSeconds = totalDurationNano / 1_000_000_000.0;

                        StringBuilder sb = new StringBuilder();
                        sb.append("=== RESULTS ===\n");
                        sb.append("Maximum packages delivered: ").append(result).append('\n');
                        sb.append("\n=== PERFORMANCE ===\n");
                        sb.append("Total execution time (including initialization):\n");
                        sb.append(String.format("  %,d nanoseconds\n", totalDurationNano));
                        sb.append(String.format("  %.3f milliseconds\n", totalMillis));
                        sb.append(String.format("  %.6f seconds\n", totalSeconds));

                        outputArea.append(sb.toString());
                    } catch (Exception ex) {
                        outputArea.append("ERROR: " + ex.getMessage() + "\n");
                        ex.printStackTrace();
                    }
                }
            };

            worker.execute();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid integer value: " + ex.getMessage());
        } catch (InputMismatchException ex) {
            JOptionPane.showMessageDialog(this, "Input error: " + ex.getMessage());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unexpected error: " + ex.getMessage());
            ex.printStackTrace();
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

            // Update packages: Deliver at next (unload up to capacityArr[next]), then pick up packagesArr[next]
            int newLoad = currentPackages;
            if (capacityArr[next] > 0) {
                newLoad = Math.max(0, newLoad - capacityArr[next]);  // Deliver (unload) up to demand
            }
            newLoad = Math.min(MAX_CARRY_CAPACITY, newLoad + packagesArr[next]);  // Pick up new packages

            // Recursive call
            int newMask = mask | (1 << next);
            int futureBest = solve(newMask, next, newLoad, newTime);
            best = Math.max(best, futureBest);
        }

        // Memoize and return
        globalMax = Math.max(globalMax, best);
        return dp[mask][node][currentPackages] = best;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TSP_GUI gui = new TSP_GUI();
            gui.setVisible(true);
        });
    }
}
