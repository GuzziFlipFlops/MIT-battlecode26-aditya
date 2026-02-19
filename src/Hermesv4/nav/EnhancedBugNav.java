package Hermesv4.nav;

import battlecode.common.*;
import Hermesv4.*;

public class EnhancedBugNav {

    private static Direction bugDirection = null;

    public static Direction walkTowards(MapLocation target) throws GameActionException {
        bugDirection = null;
        if (!Globals.rc.isMovementReady()) {
            return null;
        }

        if (Globals.myLoc.equals(target)) {
            return null;
        }

        Direction d = Globals.myLoc.directionTo(target);
        Direction result = null;
        
        
        if (Globals.rc.canMove(d) && isPassable(d)) {
            Globals.rc.move(d);
            bugDirection = null;
            return d;
        } else {
            
            if (bugDirection == null) {
                bugDirection = d;
            }
            
            
            for (int i = 0; i < 8; i++) {
                if (Globals.rc.canMove(bugDirection) && isPassable(bugDirection)) {
                    Globals.rc.move(bugDirection);
                    result = bugDirection;
                    bugDirection = bugDirection.rotateLeft();
                    break;
                } else {
                    bugDirection = bugDirection.rotateRight();
                }
            }
        }
        return result;
    }

    public static Direction walkAway(MapLocation target) throws GameActionException {
        bugDirection = null;
        if (!Globals.rc.isMovementReady()) {
            return null;
        }

        if (Globals.myLoc.equals(target)) {
            return null;
        }

        Direction d = target.directionTo(Globals.myLoc);
        Direction result = null;
        
        if (Globals.rc.canMove(d) && isPassable(d)) {
            Globals.rc.move(d);
            bugDirection = null;
            return d;
        } else {
            if (bugDirection == null) {
                bugDirection = d;
            }
            for (int i = 0; i < 8; i++) {
                if (Globals.rc.canMove(bugDirection) && isPassable(bugDirection)) {
                    Globals.rc.move(bugDirection);
                    result = bugDirection;
                    bugDirection = bugDirection.rotateLeft();
                    break;
                } else {
                    bugDirection = bugDirection.rotateRight();
                }
            }
        }
        return result;
    }

    private static boolean isPassable(Direction d) throws GameActionException {
        MapLocation adjacentLocation = Globals.myLoc.add(d);
        if (!Globals.rc.canSenseLocation(adjacentLocation)) {
            return false;
        }
        return Globals.rc.sensePassability(adjacentLocation) && 
               !Globals.rc.canSenseRobotAtLocation(adjacentLocation);
    }
}



