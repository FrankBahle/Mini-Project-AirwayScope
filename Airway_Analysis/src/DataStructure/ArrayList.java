package DataStructure;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public class ArrayList<T> implements List<T> {

    private Object[] data;
    private int size;

    private static final int DEFAULT_CAPACITY = 10;

    public ArrayList() {
        data = new Object[DEFAULT_CAPACITY];
        size = 0;
    }

    public ArrayList(Collection<? extends T> collection) {
        if (collection == null) {
            throw new NullPointerException("Collection cannot be null");
        }

        data = new Object[Math.max(DEFAULT_CAPACITY, collection.size())];
        size = 0;

        for (T item : collection) {
            add(item);
        }
    }

    private void ensureCapacity(int neededCapacity) {
        if (neededCapacity <= data.length) {
            return;
        }

        int newCapacity = data.length * 2;

        if (newCapacity < neededCapacity) {
            newCapacity = neededCapacity;
        }

        Object[] newData = new Object[newCapacity];

        for (int i = 0; i < size; i++) {
            newData[i] = data[i];
        }

        data = newData;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    private void checkIndexForAdd(int index) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    @Override
    public boolean add(T element) {
        ensureCapacity(size + 1);
        data[size] = element;
        size++;
        return true;
    }

    @Override
    public void add(int index, T element) {
        checkIndexForAdd(index);
        ensureCapacity(size + 1);

        for (int i = size; i > index; i--) {
            data[i] = data[i - 1];
        }

        data[index] = element;
        size++;
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        if (collection == null) {
            throw new NullPointerException("Collection cannot be null");
        }

        boolean changed = false;

        for (T item : collection) {
            add(item);
            changed = true;
        }

        return changed;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> collection) {
        checkIndexForAdd(index);

        if (collection == null) {
            throw new NullPointerException("Collection cannot be null");
        }

        boolean changed = false;

        for (T item : collection) {
            add(index, item);
            index++;
            changed = true;
        }

        return changed;
    }

    @Override
    public T get(int index) {
        checkIndex(index);
        return (T) data[index];
    }

    @Override
    public T set(int index, T element) {
        checkIndex(index);

        T oldValue = (T) data[index];
        data[index] = element;

        return oldValue;
    }

    @Override
    public T remove(int index) {
        checkIndex(index);

        T removedValue = (T) data[index];

        for (int i = index; i < size - 1; i++) {
            data[i] = data[i + 1];
        }

        data[size - 1] = null;
        size--;

        return removedValue;
    }

    @Override
    public boolean remove(Object object) {
        int index = indexOf(object);

        if (index == -1) {
            return false;
        }

        remove(index);
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        if (collection == null) {
            throw new NullPointerException("Collection cannot be null");
        }

        boolean changed = false;

        for (int i = size - 1; i >= 0; i--) {
            if (collection.contains(data[i])) {
                remove(i);
                changed = true;
            }
        }

        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        if (collection == null) {
            throw new NullPointerException("Collection cannot be null");
        }

        boolean changed = false;

        for (int i = size - 1; i >= 0; i--) {
            if (!collection.contains(data[i])) {
                remove(i);
                changed = true;
            }
        }

        return changed;
    }

    @Override
    public void clear() {
        for (int i = 0; i < size; i++) {
            data[i] = null;
        }

        size = 0;
    }

    @Override
    public boolean contains(Object object) {
        return indexOf(object) != -1;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        if (collection == null) {
            throw new NullPointerException("Collection cannot be null");
        }

        for (Object item : collection) {
            if (!contains(item)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int indexOf(Object object) {
        for (int i = 0; i < size; i++) {
            if (object == null) {
                if (data[i] == null) {
                    return i;
                }
            } else {
                if (object.equals(data[i])) {
                    return i;
                }
            }
        }

        return -1;
    }

    @Override
    public int lastIndexOf(Object object) {
        for (int i = size - 1; i >= 0; i--) {
            if (object == null) {
                if (data[i] == null) {
                    return i;
                }
            } else {
                if (object.equals(data[i])) {
                    return i;
                }
            }
        }

        return -1;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public Object[] toArray() {
        Object[] copy = new Object[size];

        for (int i = 0; i < size; i++) {
            copy[i] = data[i];
        }

        return copy;
    }

    @Override
    public <E> E[] toArray(E[] array) {
        if (array.length < size) {
            array = (E[]) java.lang.reflect.Array.newInstance(
                    array.getClass().getComponentType(),
                    size
            );
        }

        for (int i = 0; i < size; i++) {
            array[i] = (E) data[i];
        }

        if (array.length > size) {
            array[size] = null;
        }

        return array;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {

            private int currentIndex = 0;
            private int lastReturnedIndex = -1;

            @Override
            public boolean hasNext() {
                return currentIndex < size;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                lastReturnedIndex = currentIndex;
                T value = (T) data[currentIndex];
                currentIndex++;

                return value;
            }

            @Override
            public void remove() {
                if (lastReturnedIndex < 0) {
                    throw new IllegalStateException();
                }

                ArrayList.this.remove(lastReturnedIndex);
                currentIndex = lastReturnedIndex;
                lastReturnedIndex = -1;
            }
        };
    }

    @Override
    public ListIterator<T> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        checkIndexForAdd(index);

        return new ListIterator<T>() {

            private int currentIndex = index;
            private int lastReturnedIndex = -1;

            @Override
            public boolean hasNext() {
                return currentIndex < size;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                lastReturnedIndex = currentIndex;
                T value = (T) data[currentIndex];
                currentIndex++;

                return value;
            }

            @Override
            public boolean hasPrevious() {
                return currentIndex > 0;
            }

            @Override
            public T previous() {
                if (!hasPrevious()) {
                    throw new NoSuchElementException();
                }

                currentIndex--;
                lastReturnedIndex = currentIndex;

                return (T) data[currentIndex];
            }

            @Override
            public int nextIndex() {
                return currentIndex;
            }

            @Override
            public int previousIndex() {
                return currentIndex - 1;
            }

            @Override
            public void remove() {
                if (lastReturnedIndex < 0) {
                    throw new IllegalStateException();
                }

                ArrayList.this.remove(lastReturnedIndex);

                if (lastReturnedIndex < currentIndex) {
                    currentIndex--;
                }

                lastReturnedIndex = -1;
            }

            @Override
            public void set(T element) {
                if (lastReturnedIndex < 0) {
                    throw new IllegalStateException();
                }

                ArrayList.this.set(lastReturnedIndex, element);
            }

            @Override
            public void add(T element) {
                ArrayList.this.add(currentIndex, element);
                currentIndex++;
                lastReturnedIndex = -1;
            }
        };
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        if (fromIndex < 0 || toIndex > size || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException();
        }

        ArrayList<T> subList = new ArrayList<>();

        for (int i = fromIndex; i < toIndex; i++) {
            subList.add(get(i));
        }

        return subList;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");

        for (int i = 0; i < size; i++) {
            builder.append(data[i]);

            if (i < size - 1) {
                builder.append(", ");
            }
        }

        builder.append("]");
        return builder.toString();
    }
}