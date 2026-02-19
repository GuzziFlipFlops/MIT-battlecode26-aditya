package Hermesv3.fast;

import battlecode.common.MapLocation;

public class FastQueue {

    private int[] xCoords;
    private int[] yCoords;
    private int head;
    private int tail;
    private int size;
    private int capacity;

    public FastQueue(int capacity) {
        this.capacity = capacity;
        this.xCoords = new int[capacity];
        this.yCoords = new int[capacity];
        this.head = 0;
        this.tail = 0;
        this.size = 0;
    }

    public FastQueue() {
        this(256);
    }

    public boolean push(MapLocation loc) {
        if (loc == null || size >= capacity) return false;
        
        xCoords[tail] = loc.x;
        yCoords[tail] = loc.y;
        tail = (tail + 1) % capacity;
        size++;
        return true;
    }

    public MapLocation pop() {
        if (size == 0) return null;
        
        int x = xCoords[head];
        int y = yCoords[head];
        head = (head + 1) % capacity;
        size--;
        return new MapLocation(x, y);
    }

    public void clear() {
        head = 0;
        tail = 0;
        size = 0;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }
}
