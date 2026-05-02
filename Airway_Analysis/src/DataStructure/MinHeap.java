package DataStructure;

import java.util.ArrayList;

public class MinHeap<T> {
    private HeapNode<T> rootHeap;
    private int size;

    public MinHeap() {
        rootHeap = null;
        size = 0;
    }

    public void insert(T value, double cost) {
        HeapNode<T> newNode = new HeapNode<>(value, cost);

        if (rootHeap == null) {
            rootHeap = newNode;
            size++;
            return;
        }

        size++;

        ArrayList<Integer> aDirections = obtainRightDirection();

        HeapNode<T> current = rootHeap;
        for (int i = 0; i < aDirections.size() - 1; i++) {
            if (aDirections.get(i) == 0) {
                current = current.leftHeap;
            } else {
                current = current.rightHeap;
            }
        }

        if (aDirections.get(aDirections.size() - 1) == 0) {
            current.leftHeap = newNode;
        } else {
            current.rightHeap = newNode;
        }

        swapUpWithParent();
    }

    private ArrayList<Integer> obtainRightDirection() {
        int path = size;
        ArrayList<Integer> rDirections = new ArrayList<>();

        while (path > 1) {
            rDirections.add(path % 2);
            path = path / 2;
        }

        ArrayList<Integer> aDirections = new ArrayList<>();
        for (int i = rDirections.size() - 1; i >= 0; i--) {
            aDirections.add(rDirections.get(i));
        }

        return aDirections;
    }

    private void swapUpWithParent() {
        boolean isSwapped = true;

        while (isSwapped) {
            isSwapped = false;
            ArrayList<Integer> aDirections = obtainRightDirection();
            HeapNode<T> current = rootHeap;

            for (int i = 0; i < aDirections.size(); i++) {
                HeapNode<T> child;

                if (aDirections.get(i) == 0) {
                    child = current.leftHeap;
                } else {
                    child = current.rightHeap;
                }

                if (child != null && child.getCost() < current.getCost()) {
                    double tempCost = current.getCost();
                    current.setCost(child.getCost());
                    child.setCost(tempCost);

                    T tempValue = current.getValue();
                    current.setValue(child.getValue());
                    child.setValue(tempValue);

                    isSwapped = true;
                }

                if (aDirections.get(i) == 0) {
                    current = current.leftHeap;
                } else {
                    current = current.rightHeap;
                }
            }
        }
    }

    public HeapNode<T> extractMin() {
        if (rootHeap == null) {
            return null;
        }

        HeapNode<T> recRoot = new HeapNode<>(rootHeap.getValue(), rootHeap.getCost());

        if (size == 1) {
            rootHeap = null;
            size--;
            return recRoot;
        }

        ArrayList<Integer> aDirections = obtainRightDirection();

        HeapNode<T> current = rootHeap;
        for (int i = 0; i < aDirections.size() - 1; i++) {
            if (aDirections.get(i) == 0) {
                current = current.leftHeap;
            } else {
                current = current.rightHeap;
            }
        }

        HeapNode<T> lastHeapNode;
        if (aDirections.get(aDirections.size() - 1) == 0) {
            lastHeapNode = current.leftHeap;
            current.leftHeap = null;
        } else {
            lastHeapNode = current.rightHeap;
            current.rightHeap = null;
        }

        size--;

        rootHeap.setCost(lastHeapNode.getCost());
        rootHeap.setValue(lastHeapNode.getValue());
        swapDownWithChild();

        return recRoot;
    }

    private void swapDownWithChild() {
        boolean isSwapped = true;
        HeapNode<T> current = rootHeap;

        while (isSwapped) {
            isSwapped = false;
            HeapNode<T> cheapestChild = null;

            if (current.leftHeap != null && current.rightHeap != null) {
                if (current.leftHeap.getCost() <= current.rightHeap.getCost()) {
                    cheapestChild = current.leftHeap;
                } else {
                    cheapestChild = current.rightHeap;
                }
            } else if (current.leftHeap != null) {
                cheapestChild = current.leftHeap;
            } else if (current.rightHeap != null) {
                cheapestChild = current.rightHeap;
            }

            if (cheapestChild != null && cheapestChild.getCost() < current.getCost()) {
                double tempCost = current.getCost();
                current.setCost(cheapestChild.getCost());
                cheapestChild.setCost(tempCost);

                T tempValue = current.getValue();
                current.setValue(cheapestChild.getValue());
                cheapestChild.setValue(tempValue);

                current = cheapestChild;
                isSwapped = true;
            }
        }
    }

    public boolean isEmpty() {
        return rootHeap == null;
    }

    public HeapNode<T> getRootHeap() {
        return rootHeap;
    }

    public int getSize() {
        return size;
    }
}