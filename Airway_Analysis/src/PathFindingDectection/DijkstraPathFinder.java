package PathFindingDectection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import DataStructure.HeapNode;
import DataStructure.MinHeap;

public class DijkstraPathFinder<T> {
    private final WeightedGraph<T> graph;

    public DijkstraPathFinder(WeightedGraph<T> graph) {
        this.graph = graph;
    }

    public List<T> findShortestPath(T start, T target) {
        List<T> path = new ArrayList<>();

        if (start == null || target == null) {
            return path;
        }

        Map<T, Double> costs = new HashMap<>();
        Map<T, T> previous = new HashMap<>();

        for (T node : graph.getNodes()) {
            costs.put(node, Double.MAX_VALUE);
            previous.put(node, null);
        }

        costs.put(start, 0.0);

        Set<T> visited = new HashSet<>();
        MinHeap<T> heap = new MinHeap<>();
        heap.insert(start, 0.0);

        while (!heap.isEmpty()) {
            HeapNode<T> entry = heap.extractMin();
            if (entry == null) {
                break;
            }

            T current = entry.getValue();
            double currentCost = entry.getCost();

            if (currentCost > costs.get(current)) {
                continue;
            }

            if (visited.contains(current)) {
                continue;
            }

            visited.add(current);

            if (current.equals(target)) {
                break;
            }

            for (T neighbour : graph.getNeighbours(current)) {
                if (visited.contains(neighbour)) {
                    continue;
                }

                double newCost = costs.get(current) + graph.getCost(current, neighbour);

                if (newCost < costs.get(neighbour)) {
                    costs.put(neighbour, newCost);
                    previous.put(neighbour, current);
                    heap.insert(neighbour, newCost);
                }
            }
        }

        T step = target;
        if (!start.equals(target) && previous.get(target) == null) {
            return path;
        }

        while (step != null) {
            path.add(0, step);
            step = previous.get(step);
        }

        return path;
    }
}