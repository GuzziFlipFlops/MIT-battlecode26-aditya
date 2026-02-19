package Hermesv4.nav;

import battlecode.common.*;
import Hermesv4.*;

public class Navigation {
    
    protected static RobotController rc = Globals.rc;
    protected static MapLocation currentLocation = Globals.myLoc;
    
    protected static boolean passable(RobotController rc, Direction d) throws GameActionException {
        MapLocation adjacentLocation = rc.getLocation().add(d);
        if (!rc.canSenseLocation(adjacentLocation)) {
            return false;
        }
        return rc.sensePassability(adjacentLocation) && !rc.canSenseRobotAtLocation(adjacentLocation);
    }
    
    protected static boolean safe(RobotController rc, Direction d) throws GameActionException {
        return passable(rc, d);
    }
    
    protected static void updateMovement() {
        currentLocation = Globals.myLoc;
    }
}



