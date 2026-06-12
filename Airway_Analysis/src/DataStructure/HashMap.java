package DataStructure;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class HashMap<K, V> extends AbstractMap<K, V> {

    private static class Entry<K, V> implements Map.Entry<K, V> {
        K key;
        V value;
        Entry<K, V> next;

        Entry(K key, V value) {
            this.key = key;
            this.value = value;
            this.next = null;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }
    }

    private Entry<K, V>[] table;
    private int size;

    public HashMap() {
        table = new Entry[16];
        size = 0;
    }

    private int getIndex(Object key) {
        if (key == null) {
            return 0;
        }

        return Math.abs(key.hashCode()) % table.length;
    }

    private boolean keysEqual(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        }

        if (a == null || b == null) {
            return false;
        }

        return a.equals(b);
    }

    @Override
    public V put(K key, V value) {
        int index = getIndex(key);
        Entry<K, V> current = table[index];

        while (current != null) {
            if (keysEqual(current.key, key)) {
                V oldValue = current.value;
                current.value = value;
                return oldValue;
            }

            current = current.next;
        }

        Entry<K, V> newEntry = new Entry<>(key, value);
        newEntry.next = table[index];
        table[index] = newEntry;
        size++;

        return null;
    }

    @Override
    public V get(Object key) {
        int index = getIndex(key);
        Entry<K, V> current = table[index];

        while (current != null) {
            if (keysEqual(current.key, key)) {
                return current.value;
            }

            current = current.next;
        }

        return null;
    }

    @Override
    public boolean containsKey(Object key) {
        int index = getIndex(key);
        Entry<K, V> current = table[index];

        while (current != null) {
            if (keysEqual(current.key, key)) {
                return true;
            }

            current = current.next;
        }

        return false;
    }

    @Override
    public V remove(Object key) {
        int index = getIndex(key);
        Entry<K, V> current = table[index];
        Entry<K, V> previous = null;

        while (current != null) {
            if (keysEqual(current.key, key)) {
                if (previous == null) {
                    table[index] = current.next;
                } else {
                    previous.next = current.next;
                }

                size--;
                return current.value;
            }

            previous = current;
            current = current.next;
        }

        return null;
    }

    @Override
    public void clear() {
        table = new Entry[16];
        size = 0;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new AbstractSet<Map.Entry<K, V>>() {

            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                return new Iterator<Map.Entry<K, V>>() {
                    private int bucketIndex = 0;
                    private Entry<K, V> current = findNext();

                    private Entry<K, V> findNext() {
                        while (bucketIndex < table.length) {
                            if (table[bucketIndex] != null) {
                                Entry<K, V> found = table[bucketIndex];
                                bucketIndex++;
                                return found;
                            }

                            bucketIndex++;
                        }

                        return null;
                    }

                    @Override
                    public boolean hasNext() {
                        return current != null;
                    }

                    @Override
                    public Map.Entry<K, V> next() {
                        if (current == null) {
                            throw new NoSuchElementException();
                        }

                        Entry<K, V> result = current;

                        if (current.next != null) {
                            current = current.next;
                        } else {
                            current = findNext();
                        }

                        return result;
                    }
                };
            }

            @Override
            public int size() {
                return HashMap.this.size;
            }
        };
    }
}