package Hermesv5.fast;
public class FastIntSet {
    private int[] values;
    private int size;
    private int capacity;
    public FastIntSet(int capacity) {
        this.capacity = capacity;
        this.values = new int[capacity];
        this.size = 0;
    }
    public FastIntSet() {
        this(64);
    }
    public boolean add(int value) {
        if (size >= capacity) return false;
        for (int i = 0; i < size; i++) {
            if (values[i] == value) {
                return false;
            }
        }
        values[size++] = value;
        return true;
    }
    public boolean contains(int value) {
        for (int i = 0; i < size; i++) {
            if (values[i] == value) {
                return true;
            }
        }
        return false;
    }
    public void clear() {
        size = 0;
    }
    public int size() {
        return size;
    }
    public boolean isEmpty() {
        return size == 0;
    }
}