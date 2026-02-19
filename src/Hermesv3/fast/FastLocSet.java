package Hermesv3.fast;

import battlecode.common.MapLocation;

public class FastLocSet {

    private int[] xCoords;
    private int[] yCoords;
    private int size;
    private int capacity;

    public FastLocSet(int capacity) {
        this.capacity = capacity;
        this.xCoords = new int[capacity];
        this.yCoords = new int[capacity];
        this.size = 0;
    }

    public FastLocSet() {
        this(64);
    }

    public boolean add(MapLocation loc) {
        if (loc == null) return false;
        if (size >= capacity) return false;
        
        int x = loc.x;
        int y = loc.y;
        
        for (int i = 0; i < size; i++) {
            if (xCoords[i] == x && yCoords[i] == y) {
                return false;
            }
        }
        
        xCoords[size] = x;
        yCoords[size] = y;
        size++;
        return true;
    }

    public boolean contains(MapLocation loc) {
        if (loc == null) return false;
        return contains(loc.x, loc.y);
    }

    public boolean contains(int x, int y) {
        for (int i = 0; i < size; i++) {
            if (xCoords[i] == x && yCoords[i] == y) {
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
