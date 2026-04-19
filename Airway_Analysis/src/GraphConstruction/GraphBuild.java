package GraphConstruction;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

public class GraphBuild {
    private final int[][] binaryMask;
    private int[][] skeletonMask;
    private Graph graph;
    private final Set<String> visitedSteps;
    private final Map<String, AirwayNode> nodePixelOwner = new HashMap<>();
    
    
    public GraphBuild(int[][] binaryMask) {
        validateMask(binaryMask);
        this.binaryMask = copyMask(binaryMask);
        this.graph = new Graph();
        this.visitedSteps = new HashSet<>();
    }

    public Graph buildGraph() {
        graph = new Graph();
        visitedSteps.clear();
        nodePixelOwner.clear();

        skeletonMask = skeletonise(binaryMask);
        findNodes();
        findEdges();
        //this.Width = measureEdgeWidths(pathPixels);

        return graph;
    }

    public Graph getGraph() {
        return buildGraph();
    }
    
    
   
    public int[][] skeletonise(int[][] inputMask) {
        int rows = inputMask.length;
        int cols = inputMask[0].length;

        int[][] skeleton = copyMask(inputMask);
        boolean changed;

        ///////////////// Zhang-Suen thinning //////////////////////
        do {
            changed = false;
            boolean[][] toDelete = new boolean[rows][cols];

            for (int row = 1; row < rows - 1; row++) {
                for (int col = 1; col < cols - 1; col++) {

                    if (skeleton[row][col] != 1) {
                        continue;
                    }

                    int p2 = skeleton[row - 1][col];
                    int p3 = skeleton[row - 1][col + 1];
                    int p4 = skeleton[row][col + 1];
                    int p5 = skeleton[row + 1][col + 1];
                    int p6 = skeleton[row + 1][col];
                    int p7 = skeleton[row + 1][col - 1];
                    int p8 = skeleton[row][col - 1];
                    int p9 = skeleton[row - 1][col - 1];

                    int neighbours = p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9;

                    int transitions = 0;
                    int[] sequence = {p2, p3, p4, p5, p6, p7, p8, p9, p2};
                    for (int i = 0; i < 8; i++) {
                        if (sequence[i] == 0 && sequence[i + 1] == 1) {
                            transitions++;
                        }
                    }

                    if (neighbours >= 2 && neighbours <= 6 &&
                        transitions == 1 &&
                        (p2 * p4 * p6 == 0) &&
                        (p4 * p6 * p8 == 0)) {
                        toDelete[row][col] = true;
                        changed = true;
                    }
                }
            }

            for (int row = 1; row < rows - 1; row++) {
                for (int col = 1; col < cols - 1; col++) {
                    if (toDelete[row][col]) {
                        skeleton[row][col] = 0;
                    }
                }
            }

            toDelete = new boolean[rows][cols];

            for (int row = 1; row < rows - 1; row++) {
                for (int col = 1; col < cols - 1; col++) {

                    if (skeleton[row][col] != 1) {
                        continue;
                    }

                    int p2 = skeleton[row - 1][col];
                    int p3 = skeleton[row - 1][col + 1];
                    int p4 = skeleton[row][col + 1];
                    int p5 = skeleton[row + 1][col + 1];
                    int p6 = skeleton[row + 1][col];
                    int p7 = skeleton[row + 1][col - 1];
                    int p8 = skeleton[row][col - 1];
                    int p9 = skeleton[row - 1][col - 1];

                    int neighbours = p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9;

                    int transitions = 0;
                    int[] sequence = {p2, p3, p4, p5, p6, p7, p8, p9, p2};
                    for (int i = 0; i < 8; i++) {
                        if (sequence[i] == 0 && sequence[i + 1] == 1) {
                            transitions++;
                        }
                    }

                    if (neighbours >= 2 && neighbours <= 6 &&
                        transitions == 1 &&
                        (p2 * p4 * p8 == 0) &&
                        (p2 * p6 * p8 == 0)) {
                        toDelete[row][col] = true;
                        changed = true;
                    }
                }
            }

            for (int row = 1; row < rows - 1; row++) {
                for (int col = 1; col < cols - 1; col++) {
                    if (toDelete[row][col]) {
                        skeleton[row][col] = 0;
                    }
                }
            }

        } while (changed);

        return skeleton;
    }

    private void findNodes() {
        int rows = skeletonMask.length;
        int cols = skeletonMask[0].length;

        boolean[][] visitedBranch = new boolean[rows][cols];

        for (int r = 1; r < rows - 1; r++) {
            for (int c = 1; c < cols - 1; c++) {

                if (skeletonMask[r][c] != 1) {
                    continue;
                }

                int neighbours = countWhiteNeighbours(r, c);

                if (neighbours == 1) {
                    AirwayNode endNode = new AirwayNode(new PixelPoint(r, c), NodeType.END);
                    graph.addNode(endNode);
                    nodePixelOwner.put(pixelKey(r, c), endNode);
                }

                else if (neighbours >= 3 && !visitedBranch[r][c]) {
                    List<PixelPoint> branchCluster = collectBranchCluster(r, c, visitedBranch);
                    PixelPoint representative = chooseRepresentative(branchCluster);

                    AirwayNode branchNode = new AirwayNode(representative, NodeType.BRANCH);
                    graph.addNode(branchNode);

                    for (PixelPoint p : branchCluster) {
                        nodePixelOwner.put(pixelKey(p.getRow(), p.getCol()), branchNode);
                    }
                }
            }
        }
    }
    
    
    ////////////////HELPER METHOD FOR NODE //////////////////////////
    private PixelPoint chooseRepresentative(List<PixelPoint> cluster) {
        double avgRow = 0.0;
        double avgCol = 0.0;

        for (PixelPoint p : cluster) {
            avgRow += p.getRow();
            avgCol += p.getCol();
        }

        avgRow /= cluster.size();
        avgCol /= cluster.size();

        PixelPoint best = cluster.get(0);
        double bestDistance = Double.MAX_VALUE;

        for (PixelPoint p : cluster) {
            double dr = p.getRow() - avgRow;
            double dc = p.getCol() - avgCol;
            double dist = dr * dr + dc * dc;

            if (dist < bestDistance) {
                bestDistance = dist;
                best = p;
            }
        }

        return best;
    }

    private List<PixelPoint> collectBranchCluster(int startRow, int startCol, boolean[][] visitedBranch) {
        List<PixelPoint> cluster = new ArrayList<>();
        java.util.ArrayDeque<PixelPoint> queue = new java.util.ArrayDeque<>();

        queue.add(new PixelPoint(startRow, startCol));
        visitedBranch[startRow][startCol] = true;

        while (!queue.isEmpty()) {
            PixelPoint current = queue.poll();
            cluster.add(current);

            for (int r = current.getRow() - 1; r <= current.getRow() + 1; r++) {
                for (int c = current.getCol() - 1; c <= current.getCol() + 1; c++) {

                    if (!isInside(r, c) || (r == current.getRow() && c == current.getCol())) {
                        continue;
                    }

                    if (skeletonMask[r][c] != 1 || visitedBranch[r][c]) {
                        continue;
                    }

                    int neighbours = countWhiteNeighbours(r, c);

                    if (neighbours >= 3) {
                        visitedBranch[r][c] = true;
                        queue.add(new PixelPoint(r, c));
                    }
                }
            }
        }

        return cluster;
    }
    
    //////////////////////////////////////////////////////////////////////////

    
    
    private void findEdges() {
        for (AirwayNode startNode : graph.getNodes()) {
            PixelPoint startPoint = startNode.getPixelPoint();
            List<PixelPoint> neighbours = getWhiteNeighbours(startPoint.getRow(), startPoint.getCol());

            for (PixelPoint nextPixel : neighbours) {
                if (!isStepVisited(startPoint, nextPixel)) {
                    traceEdge(startNode, nextPixel);
                }
            }
        }
    }

    private List<Double> measureEdgeWidths(List<PixelPoint> pathPixels) {
        List<Double> widths = new ArrayList<>();

        for (PixelPoint p : pathPixels) {
            widths.add(estimateWidthAtPixel(p));
        }

        return widths;
    }
    
    private double estimateWidthAtPixel(PixelPoint p) {
        int row = p.getRow();
        int col = p.getCol();

        if (binaryMask[row][col] == 0) {
            return 0.0;
        }

        double nearestBackgroundDistance = findNearestBackgroundDistance(row, col);

     
        double width = 2.0 * nearestBackgroundDistance - 1.0;

        return Math.max(1.0, width);
    }
    
    private double findNearestBackgroundDistance(int row, int col) {
        int rows = binaryMask.length;
        int cols = binaryMask[0].length;

        int maxRadius = Math.max(rows, cols);
        double minDistance = Double.MAX_VALUE;
        boolean found = false;

        for (int radius = 1; radius < maxRadius; radius++) {
            int rStart = Math.max(0, row - radius);
            int rEnd   = Math.min(rows - 1, row + radius);
            int cStart = Math.max(0, col - radius);
            int cEnd   = Math.min(cols - 1, col + radius);

            for (int r = rStart; r <= rEnd; r++) {
                for (int c = cStart; c <= cEnd; c++) {

                    boolean onRing = (r == rStart || r == rEnd || c == cStart || c == cEnd);

                    if (onRing && binaryMask[r][c] == 0) {
                        double dr = r - row;
                        double dc = c - col;
                        double distance = Math.sqrt(dr * dr + dc * dc);

                        if (distance < minDistance) {
                            minDistance = distance;
                        }
                        found = true;
                    }
                }
            }

            if (found) {
                break;
            }
        }

        if (!found) {
            return 1.0;
        }

        return minDistance;
    }
    
    private void traceEdge(AirwayNode startNode, PixelPoint currentPixel) {
        List<PixelPoint> pathPixels = new ArrayList<>();
        Set<String> localVisited = new HashSet<>();

        PixelPoint previous = startNode.getPixelPoint();
        PixelPoint current = currentPixel;

        pathPixels.add(current);

        localVisited.add(pixelKey(previous.getRow(), previous.getCol()));
        localVisited.add(pixelKey(current.getRow(), current.getCol()));

        markStepVisited(previous, current);

        while (true) {
            AirwayNode foundNode = getNodeAt(current.getRow(), current.getCol());

            if (foundNode != null && !foundNode.equals(startNode)) {
                List<Double> widths = measureEdgeWidths(pathPixels);
                AirwayEdge edge = new AirwayEdge(startNode, foundNode, new ArrayList<>(pathPixels), widths);
                graph.addEdge(edge);
                return;
            }

            List<PixelPoint> neighbours = getWhiteNeighbours(current.getRow(), current.getCol());
            PixelPoint nextStep = null;

            for (PixelPoint neighbour : neighbours) {
                String key = pixelKey(neighbour.getRow(), neighbour.getCol());

                if (neighbour.equals(previous)) {
                    continue;
                }

                if (localVisited.contains(key)) {
                    continue;
                }

                nextStep = neighbour;
                break;
            }

            if (nextStep == null) {
                return;
            }

            markStepVisited(current, nextStep);

            previous = current;
            current = nextStep;

            localVisited.add(pixelKey(current.getRow(), current.getCol()));
            pathPixels.add(current);
        }
    }

    private AirwayNode getNodeAt(int row, int col) {
        return nodePixelOwner.get(pixelKey(row, col));
    }
    
    private String pixelKey(int row, int col) {
        return row + "," + col;
    }

    private int countWhiteNeighbours(int row, int col) {
        int count = 0;

        for (int r = row - 1; r <= row + 1; r++) {
            for (int c = col - 1; c <= col + 1; c++) {
                if (isInside(r, c) &&
                    !(r == row && c == col) &&
                    skeletonMask[r][c] == 1) {
                    count++;
                }
            }
        }

        return count;
    }

    private List<PixelPoint> getWhiteNeighbours(int row, int col) {
        List<PixelPoint> neighbours = new ArrayList<>();

        for (int r = row - 1; r <= row + 1; r++) {
            for (int c = col - 1; c <= col + 1; c++) {
                if (isInside(r, c) &&
                    !(r == row && c == col) &&
                    skeletonMask[r][c] == 1) {
                    neighbours.add(new PixelPoint(r, c));
                }
            }
        }

        return neighbours;
    }
    
    
    
    /////////////////VALIDATING//////////////////////////////

    private boolean isInside(int row, int col) {
        return row >= 0 && row < skeletonMask.length &&
               col >= 0 && col < skeletonMask[0].length;
    }

    private void markStepVisited(PixelPoint a, PixelPoint b) {
        visitedSteps.add(buildStepKey(a, b));
    }

    private boolean isStepVisited(PixelPoint a, PixelPoint b) {
        return visitedSteps.contains(buildStepKey(a, b));
    }

    private String buildStepKey(PixelPoint a, PixelPoint b) {
        String first = a.getRow() + "," + a.getCol();
        String second = b.getRow() + "," + b.getCol();

        return (first.compareTo(second) <= 0)
                ? first + "->" + second
                : second + "->" + first;
    }

    private static int[][] copyMask(int[][] source) {
        int[][] copy = new int[source.length][source[0].length];
        for (int i = 0; i < source.length; i++) {
            System.arraycopy(source[i], 0, copy[i], 0, source[i].length);
        }
        return copy;
    }

    private static void validateMask(int[][] mask) {
        if (mask == null || mask.length == 0 || mask[0].length == 0) {
            throw new IllegalArgumentException("binaryMask cannot be null or empty");
        }

        int width = mask[0].length;
        for (int i = 1; i < mask.length; i++) {
            if (mask[i].length != width) {
                throw new IllegalArgumentException("binaryMask must be rectangular");
            }
        }
    }
    
    //////////////////Print ////////////////////////////
    public void printNodesAndEdges(String fileName) {
        if (graph == null) {
            System.out.println("Graph is null.");
            return;
        }

        if (graph.getNodes().isEmpty() && graph.getEdges().isEmpty()) {
            System.out.println("Graph is empty. Build the graph first.");
            return;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {

            System.out.println("========== NODES ==========");
            writer.println("========== NODES ==========");

            for (AirwayNode node : graph.getNodes()) {
                String nodeLine =
                    "Node -> row: " + node.getRow() +
                    ", col: " + node.getCol() +
                    ", type: " + node.getType();

                System.out.println(nodeLine);
                writer.println(nodeLine);
            }

            System.out.println("========== EDGES ==========");
            writer.println("========== EDGES ==========");

            for (AirwayEdge edge : graph.getEdges()) {
                String edgeLine =
                    "Edge -> from: (" + edge.getFrom().getRow() + ", " + edge.getFrom().getCol() + ")" +
                    " to: (" + edge.getTo().getRow() + ", " + edge.getTo().getCol() + ")";

                System.out.println(edgeLine);
                writer.println(edgeLine);
            }

            System.out.println("Graph saved to: " + fileName);

        } catch (IOException e) {
            System.out.println("Error writing graph to file: " + e.getMessage());
        }
    }
}