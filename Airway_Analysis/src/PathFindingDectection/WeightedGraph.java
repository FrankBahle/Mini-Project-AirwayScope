package PathFindingDectection;

import java.util.List;

public interface WeightedGraph<T> {
    List<T> getNodes();
    List<T> getNeighbours(T node);
    double getCost(T from, T to);
}