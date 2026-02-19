package Hermesv8.nav;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

public class BugNav extends Navigation {

    private static Direction bugDirection = null;

    private static boolean isWall(MapLocation loc) {
        if (loc.x < 0 || loc.x >= MAP_WIDTH || loc.y < 0 || loc.y >= MAP_HEIGHT) {
            return true;
        }
        if (map == null) {
            return false;
        }
        int tileType = map[loc.x][loc.y];
        return tileType == WALL;
    }

    public static Direction walkTowards(MapLocation target) throws GameActionException {
        bugDirection = null;
        if (!rc.isMovementReady()) {
            return null;
        }

        if (rc.getLocation().equals(target)) {
            return null;
        }

        Direction d = rc.getLocation().directionTo(target);
        Direction result = null;
        MapLocation nextLoc = rc.getLocation().add(d);

        if (d != Direction.CENTER && !isWall(nextLoc) && passable(rc, d)) {
            if (rc.getDirection() != d && rc.canTurn()) {
                rc.turn(d);
            }
            if (rc.canMove(d)) {
                rc.move(d);
                bugDirection = null;
                updateMovement();
                return d;
            }
        }

        if (bugDirection == null) {
            bugDirection = d;
        }
        for (int i = 0; i < 8; i++) {
            nextLoc = rc.getLocation().add(bugDirection);
            if (!isWall(nextLoc) && passable(rc, bugDirection)) {
                if (rc.getDirection() != bugDirection && rc.canTurn()) {
                    rc.turn(bugDirection);
                }
                if (rc.canMove(bugDirection)) {
                    rc.move(bugDirection);
                    result = bugDirection;
                    bugDirection = bugDirection.rotateLeft();
                    updateMovement();
                    return result;
                }
            }
            bugDirection = bugDirection.rotateRight();
        }

        updateMovement();
        return result;
    }

    public static Direction walkTowardsSafe(MapLocation target) throws GameActionException {
        bugDirection = null;
        if (!rc.isMovementReady()) {
            return null;
        }

        if (rc.getLocation().equals(target)) {
            return null;
        }

        Direction d = rc.getLocation().directionTo(target);
        Direction result = null;
        MapLocation nextLoc = rc.getLocation().add(d);

        if (d != Direction.CENTER && !isWall(nextLoc) && safe(rc, d)) {
            if (rc.getDirection() != d && rc.canTurn()) {
                rc.turn(d);
            }
            if (rc.canMove(d)) {
                rc.move(d);
                bugDirection = null;
                updateMovement();
                return d;
            }
        }

        if (bugDirection == null) {
            bugDirection = d;
        }
        for (int i = 0; i < 8; i++) {
            nextLoc = rc.getLocation().add(bugDirection);
            if (!isWall(nextLoc) && safe(rc, bugDirection)) {
                if (rc.getDirection() != bugDirection && rc.canTurn()) {
                    rc.turn(bugDirection);
                }
                if (rc.canMove(bugDirection)) {
                    rc.move(bugDirection);
                    result = bugDirection;
                    bugDirection = bugDirection.rotateLeft();
                    updateMovement();
                    return result;
                }
            }
            bugDirection = bugDirection.rotateRight();
        }

        updateMovement();
        return result;
    }

    public static Direction walkAway(MapLocation target) throws GameActionException {
        bugDirection = null;
        if (!rc.isMovementReady()) {
            return null;
        }

        if (rc.getLocation().equals(target)) {
            return null;
        }

        Direction d = target.directionTo(rc.getLocation());
        Direction result = null;
        MapLocation nextLoc = rc.getLocation().add(d);

        if (d != Direction.CENTER && !isWall(nextLoc) && passable(rc, d)) {
            if (rc.getDirection() != d && rc.canTurn()) {
                rc.turn(d);
            }
            if (rc.canMove(d)) {
                rc.move(d);
                bugDirection = null;
                updateMovement();
                return d;
            }
        }

        if (bugDirection == null) {
            bugDirection = d;
        }
        for (int i = 0; i < 8; i++) {
            nextLoc = rc.getLocation().add(bugDirection);
            if (!isWall(nextLoc) && passable(rc, bugDirection)) {
                if (rc.getDirection() != bugDirection && rc.canTurn()) {
                    rc.turn(bugDirection);
                }
                if (rc.canMove(bugDirection)) {
                    rc.move(bugDirection);
                    result = bugDirection;
                    bugDirection = bugDirection.rotateLeft();
                    updateMovement();
                    return result;
                }
            }
            bugDirection = bugDirection.rotateRight();
        }

        updateMovement();
        return result;
    }
}
