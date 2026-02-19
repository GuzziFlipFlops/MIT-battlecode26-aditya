package Hermesv5.nav;
import battlecode.common.*;
import Hermesv5.*;
public class BugNav extends Navigation {
    private static Direction bugDirection = null;
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
        if (rc.canMove(d) && passable(rc, d)) {
            rc.move(d);
            bugDirection = null;
        } else {
            if (bugDirection == null) {
                bugDirection = d;
            }
            for (int i = 0; i < 8; i++) {
                if (rc.canMove(bugDirection) && passable(rc, bugDirection)) {
                    rc.move(bugDirection);
                    result = bugDirection;
                    bugDirection = bugDirection.rotateLeft();
                    break;
                } else {
                    bugDirection = bugDirection.rotateRight();
                }
            }
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
        if (safe(rc, d) && rc.canMove(d)) {
            rc.move(d);
            bugDirection = null;
        } else {
            if (bugDirection == null) {
                bugDirection = d;
            }
            for (int i = 0; i < 8; i++) {
                if (rc.canMove(bugDirection) && safe(rc, bugDirection)) {
                    rc.move(bugDirection);
                    result = bugDirection;
                    bugDirection = bugDirection.rotateLeft();
                    break;
                } else {
                    bugDirection = bugDirection.rotateRight();
                }
            }
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
        if (rc.canMove(d) && passable(rc, d)) {
            rc.move(d);
            bugDirection = null;
        } else {
            if (bugDirection == null) {
                bugDirection = d;
            }
            for (int i = 0; i < 8; i++) {
                if (rc.canMove(bugDirection) && passable(rc, bugDirection)) {
                    rc.move(bugDirection);
                    result = bugDirection;
                    bugDirection = bugDirection.rotateLeft();
                    break;
                } else {
                    bugDirection = bugDirection.rotateRight();
                }
            }
        }
        updateMovement();
        return result;
    }
}