package Hermesv4.nav;

import battlecode.common.*;
import Hermesv4.*;

public class AStarPathfinder {
    
    private static final int MAX_SEARCH_DEPTH = 35;
    private static final int MAX_NODES = 150;
    private static final int BYTECODE_BUDGET = 3000;
    private static final int BYTECODE_MIN = 500;
    
    
    private static int[] nodeX = new int[MAX_NODES];
    private static int[] nodeY = new int[MAX_NODES];
    private static int[] nodeGCost = new int[MAX_NODES];
    private static int[] nodeHCost = new int[MAX_NODES];
    private static int[] nodeParentIdx = new int[MAX_NODES];
    private static int nodeCount = 0;
    
    
    private static int[] heap = new int[MAX_NODES];
    private static int heapSize = 0;
    
    
    private static int[] openSet = new int[3600]; 
    
    
    private static boolean[] closedSet = new boolean[3600];
    
    public static Direction findBestDirection(MapLocation target) throws GameActionException {
        if (target == null || !Globals.rc.isMovementReady()) return null;
        if (Globals.myLoc.equals(target)) return Direction.CENTER;
        
        int distSq = Globals.myLoc.distanceSquaredTo(target);
        if (distSq <= 2) {
            return Globals.myLoc.directionTo(target);
        }
        
        int maxDistSq = MAX_SEARCH_DEPTH * MAX_SEARCH_DEPTH;
        if (distSq > maxDistSq) {
            return findGreedyDirection(target);
        }
        
        if (Clock.getBytecodesLeft() < BYTECODE_BUDGET) {
            return findGreedyDirection(target);
        }
        
        
        nodeCount = 0;
        heapSize = 0;
        int mapSize = Globals.mapWidth * Globals.mapHeight;
        for (int i = 0; i < mapSize && i < openSet.length; i++) {
            openSet[i] = -1;
            closedSet[i] = false;
        }
        
        
        int startX = Globals.myLoc.x;
        int startY = Globals.myLoc.y;
        int startIdx = startX * Globals.mapHeight + startY;
        int hCost = heuristic(startX, startY, target.x, target.y);
        
        nodeX[0] = startX;
        nodeY[0] = startY;
        nodeGCost[0] = 0;
        nodeHCost[0] = hCost;
        nodeParentIdx[0] = -1;
        nodeCount = 1;
        
        heap[0] = 0;
        heapSize = 1;
        openSet[startIdx] = 0;
        
        int bestNodeIdx = 0;
        int bestHCost = hCost;
        
        while (heapSize > 0 && Clock.getBytecodesLeft() > BYTECODE_MIN) {
            int currentIdx = heapPop();
            int currentX = nodeX[currentIdx];
            int currentY = nodeY[currentIdx];
            int currentGCost = nodeGCost[currentIdx];
            int currentHCost = nodeHCost[currentIdx];
            
            int locIdx = currentX * Globals.mapHeight + currentY;
            if (closedSet[locIdx]) continue;
            closedSet[locIdx] = true;
            
            
            if (currentHCost == 0) {
                return getFirstDirection(currentIdx);
            }
            
            
            if (currentHCost < bestHCost) {
                bestHCost = currentHCost;
                bestNodeIdx = currentIdx;
            }
            
            
            if (currentGCost >= MAX_SEARCH_DEPTH) continue;
            
            
            Direction[] dirs = Globals.ALL_DIRECTIONS;
            for (int i = 0; i < dirs.length; i++) {
                Direction dir = dirs[i];
                if (dir == Direction.CENTER) continue;
                
                int neighborX = currentX + dir.dx;
                int neighborY = currentY + dir.dy;
                
                
                if (neighborX < 0 || neighborX >= Globals.mapWidth || 
                    neighborY < 0 || neighborY >= Globals.mapHeight) continue;
                
                int neighborLocIdx = neighborX * Globals.mapHeight + neighborY;
                
                
                if (closedSet[neighborLocIdx]) continue;
                
                
                MapLocation neighborLoc = new MapLocation(neighborX, neighborY);
                if (!Globals.rc.canSenseLocation(neighborLoc)) continue;
                if (!Globals.rc.sensePassability(neighborLoc)) continue;
                
                int newGCost = currentGCost + 1;
                int newHCost = heuristic(neighborX, neighborY, target.x, target.y);
                
                
                int openHeapIdx = openSet[neighborLocIdx];
                if (openHeapIdx >= 0) {
                    
                    int existingNodeIdx = heap[openHeapIdx];
                    if (newGCost < nodeGCost[existingNodeIdx]) {
                        
                        nodeGCost[existingNodeIdx] = newGCost;
                        nodeHCost[existingNodeIdx] = newHCost;
                        nodeParentIdx[existingNodeIdx] = currentIdx;
                        heapUp(openHeapIdx);
                    }
                } else {
                    
                    if (nodeCount >= MAX_NODES) continue;
                    
                    int newNodeIdx = nodeCount++;
                    nodeX[newNodeIdx] = neighborX;
                    nodeY[newNodeIdx] = neighborY;
                    nodeGCost[newNodeIdx] = newGCost;
                    nodeHCost[newNodeIdx] = newHCost;
                    nodeParentIdx[newNodeIdx] = currentIdx;
                    
                    
                    heap[heapSize] = newNodeIdx;
                    int initialPos = heapSize++;
                    openSet[neighborLocIdx] = initialPos;
                    heapUp(initialPos);
                }
            }
        }
        
        
        Direction result = getFirstDirection(bestNodeIdx);
        if (result == null) {
            return findGreedyDirection(target);
        }
        return result;
    }
    
    
    private static int heapPop() {
        int result = heap[0];
        heap[0] = heap[--heapSize];
        if (heapSize > 0) {
            heapDown(0);
        }
        int nodeIdx = result;
        int locIdx = nodeX[nodeIdx] * Globals.mapHeight + nodeY[nodeIdx];
        openSet[locIdx] = -1;
        return result;
    }
    
    private static void heapUp(int pos) {
        while (pos > 0) {
            int parent = (pos - 1) / 2;
            int posFCost = getFCost(heap[pos]);
            int parentFCost = getFCost(heap[parent]);
            
            if (posFCost >= parentFCost) break;
            
            
            int temp = heap[pos];
            heap[pos] = heap[parent];
            heap[parent] = temp;
            
            
            int posNodeIdx = heap[pos];
            int parentNodeIdx = heap[parent];
            int posLocIdx = nodeX[posNodeIdx] * Globals.mapHeight + nodeY[posNodeIdx];
            int parentLocIdx = nodeX[parentNodeIdx] * Globals.mapHeight + nodeY[parentNodeIdx];
            openSet[posLocIdx] = pos;
            openSet[parentLocIdx] = parent;
            
            pos = parent;
        }
    }
    
    private static void heapDown(int pos) {
        while (true) {
            int left = 2 * pos + 1;
            int right = 2 * pos + 2;
            int smallest = pos;
            
            if (left < heapSize) {
                int leftFCost = getFCost(heap[left]);
                int smallestFCost = getFCost(heap[smallest]);
                if (leftFCost < smallestFCost || 
                    (leftFCost == smallestFCost && nodeHCost[heap[left]] < nodeHCost[heap[smallest]])) {
                    smallest = left;
                }
            }
            
            if (right < heapSize) {
                int rightFCost = getFCost(heap[right]);
                int smallestFCost = getFCost(heap[smallest]);
                if (rightFCost < smallestFCost || 
                    (rightFCost == smallestFCost && nodeHCost[heap[right]] < nodeHCost[heap[smallest]])) {
                    smallest = right;
                }
            }
            
            if (smallest == pos) break;
            
            
            int temp = heap[pos];
            heap[pos] = heap[smallest];
            heap[smallest] = temp;
            
            
            int posNodeIdx = heap[pos];
            int smallestNodeIdx = heap[smallest];
            int posLocIdx = nodeX[posNodeIdx] * Globals.mapHeight + nodeY[posNodeIdx];
            int smallestLocIdx = nodeX[smallestNodeIdx] * Globals.mapHeight + nodeY[smallestNodeIdx];
            openSet[posLocIdx] = pos;
            openSet[smallestLocIdx] = smallest;
            
            pos = smallest;
        }
    }
    
    private static int getFCost(int nodeIdx) {
        return nodeGCost[nodeIdx] + nodeHCost[nodeIdx];
    }
    
    private static Direction findGreedyDirection(MapLocation target) throws GameActionException {
        Direction direct = Globals.myLoc.directionTo(target);
        Direction bestDir = null;
        int bestDist = Globals.myLoc.distanceSquaredTo(target);
        
        MapLocation[] catLocs = (Globals.nearbyCats != null) ? Globals.getLocations(Globals.nearbyCats) : new MapLocation[0];
        
        Direction[] dirs = {direct, direct.rotateLeft(), direct.rotateRight(), 
                           direct.rotateLeft().rotateLeft(), direct.rotateRight().rotateRight()};
        
        for (Direction dir : dirs) {
            if (dir == Direction.CENTER) continue;
            if (!Mover.canMoveInDirection(dir)) continue;
            
            MapLocation newLoc = Globals.myLoc.add(dir);
            int newDist = newLoc.distanceSquaredTo(target);
            
            int penalty = 0;
            for (MapLocation catLoc : catLocs) {
                if (catLoc != null) {
                    int distToCat = newLoc.distanceSquaredTo(catLoc);
                    if (distToCat <= 16) {
                        penalty += (17 - distToCat) * 50;
                    }
                }
            }
            
            int score = newDist + penalty;
            if (score < bestDist) {
                bestDist = score;
                bestDir = dir;
            }
        }
        
        if (bestDir != null) {
            return bestDir;
        }
        
        for (Direction dir : Globals.ALL_DIRECTIONS) {
            if (dir == Direction.CENTER) continue;
            if (!Mover.canMoveInDirection(dir)) continue;
            
            MapLocation newLoc = Globals.myLoc.add(dir);
            int newDist = newLoc.distanceSquaredTo(target);
            
            int penalty = 0;
            for (MapLocation catLoc : catLocs) {
                if (catLoc != null) {
                    int distToCat = newLoc.distanceSquaredTo(catLoc);
                    if (distToCat <= 16) {
                        penalty += (17 - distToCat) * 50;
                    }
                }
            }
            
            int score = newDist + penalty;
            if (score < bestDist) {
                bestDist = score;
                bestDir = dir;
            }
        }
        
        return bestDir;
    }
    
    
    private static int heuristic(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2;
        int dy = y1 - y2;
        return dx * dx + dy * dy;
    }
    
    private static Direction getFirstDirection(int nodeIdx) {
        if (nodeIdx < 0 || nodeParentIdx[nodeIdx] < 0) return null;
        
        int currentIdx = nodeIdx;
        while (nodeParentIdx[currentIdx] >= 0 && nodeParentIdx[nodeParentIdx[currentIdx]] >= 0) {
            currentIdx = nodeParentIdx[currentIdx];
        }
        
        if (nodeParentIdx[currentIdx] < 0) return null;
        
        int parentIdx = nodeParentIdx[currentIdx];
        MapLocation currentLoc = new MapLocation(nodeX[currentIdx], nodeY[currentIdx]);
        MapLocation parentLoc = new MapLocation(nodeX[parentIdx], nodeY[parentIdx]);
        return parentLoc.directionTo(currentLoc);
    }
}
