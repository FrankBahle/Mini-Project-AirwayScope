package DataStructure;

public class HeapNode<T> {
    private T value;
    private double cost;
    HeapNode<T> leftHeap;
    HeapNode<T> rightHeap;

    public HeapNode(T value, double cost) {
        this.value = value;
        this.cost = cost;
        this.leftHeap = null;
        this.rightHeap = null;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }
}