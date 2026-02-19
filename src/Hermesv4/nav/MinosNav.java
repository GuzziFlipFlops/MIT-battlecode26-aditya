package Hermesv4.nav;

import Hermesv4.fast.FastIntSet;
import Hermesv4.fast.FastQueue;
import battlecode.common.*;
import Hermesv4.*;

public class MinosNav extends BugNav {

    static MapLocation target;
    private static MapLocation prevTarget = null;
    static int minDistToEnemy = 99999;
    static FastIntSet visited = new FastIntSet(1000);
    static MapLocation lastObstacleFound = null; 
    static boolean rotateRight = true; 
    static boolean shouldGuessRotation = true; 
    static FastQueue defenseTowers = new FastQueue(10); 

    static boolean isCautious = false;

    static boolean canMove(Direction dir) throws GameActionException {
        if (!rc.canMove(dir)) {
            return false;
        } else if (isCautious && !safe(rc, dir)) {
            return false;
        }
        return !attacked(rc, dir);
    }

    public static int getCode() {
        int x = rc.getLocation().x;
        int y = rc.getLocation().y;
        Direction obstacleDir = rc.getLocation().directionTo(target);
        if (lastObstacleFound != null)
            obstacleDir = rc.getLocation().directionTo(lastObstacleFound);
        int bit = rotateRight ? 1 : 0;
        return (((((x << 6) | y) << 4) | obstacleDir.ordinal()) << 1) | bit;
    }

    public static void resetPathfinding() {
        lastObstacleFound = null;
        minDistToEnemy = 99999;
        visited.clear();
        shouldGuessRotation = true;
    }

    static int distance(MapLocation A, MapLocation B) {
        return Math.max(Math.abs(A.x - B.x), Math.abs(A.y - B.y));
    }

    public static boolean nav() throws GameActionException {
        
        try {
            
            if (prevTarget == null || distance(prevTarget, target) > 0) {
                resetPathfinding();
            }

            
            MapLocation myLoc = rc.getLocation();
            int d = myLoc.distanceSquaredTo(target);
            if (d < minDistToEnemy) {
                resetPathfinding();
                minDistToEnemy = d;
            }

            int code = getCode();

            if (visited.contains(code)) {
                resetPathfinding();
            }
            visited.add(code);

            
            prevTarget = target;

            
            
            Direction dir = myLoc.directionTo(target);
            if (lastObstacleFound != null) {
                dir = myLoc.directionTo(lastObstacleFound);
            }

            if (canMove(dir)) {
                resetPathfinding();
            }

            
            
            for (int i = 8; i-- > 0;) {
                MapLocation newLoc = myLoc.add(dir);
                if (rc.canSenseLocation(newLoc)) {
                    if (canMove(dir)) {
                        rc.move(dir);
                        updateMovement();
                        return true;
                    }
                }
                RobotInfo ri;
                if (!rc.onTheMap(newLoc)) {
                    rotateRight = !rotateRight;
                } else if ((ri = rc.senseRobotAtLocation(newLoc)) != null) {
                    
                } else if (!rc.sensePassability(newLoc) || attacked(rc, dir)) {
                    
                    lastObstacleFound = newLoc;
                    if (shouldGuessRotation) {
                        shouldGuessRotation = false;
                        
                        Direction dirL = dir;
                        for (int j = 8; j-- > 0;) {
                            if (canMove(dirL))
                                break;
                            dirL = dirL.rotateLeft();
                        }

                        Direction dirR = dir;
                        for (int j = 8; j-- > 0;) {
                            if (canMove(dirR))
                                break;
                            dirR = dirR.rotateRight();
                        }

                        
                        MapLocation locL = myLoc.add(dirL);
                        MapLocation locR = myLoc.add(dirR);

                        int lDist = distance(target, locL);
                        int rDist = distance(target, locR);
                        int lDistSq = target.distanceSquaredTo(locL);
                        int rDistSq = target.distanceSquaredTo(locR);

                        if (lDist < rDist) {
                            rotateRight = false;
                        } else if (rDist < lDist) {
                            rotateRight = true;
                        } else {
                            rotateRight = rDistSq < lDistSq;
                        }
                    }
                }

                if (rotateRight) dir = dir.rotateRight();
                else dir = dir.rotateLeft();
            }

            if (canMove(dir)) {
                rc.move(dir);
                updateMovement();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public static Direction walkTowards(MapLocation t) throws GameActionException {
        isCautious = false;
        target = t;
        nav();
        return null;
    }

    public static Direction walkTowardsSafe(MapLocation t) throws GameActionException {
        isCautious = true;
        target = t;
        nav();
        return null;
    }
    
    public static boolean navigateTo(MapLocation t) throws GameActionException {
        if (t == null || !rc.isMovementReady()) {
            return false;
        }
        target = t;
        return nav();
    }
    
    static boolean attacked(RobotController rc, Direction d) {
        
        
        return false;
    }
}
