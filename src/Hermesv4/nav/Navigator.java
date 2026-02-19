package Hermesv4.nav;

import battlecode.common.*;
import Hermesv4.*;

public class Navigator {

    public static boolean navigateTo(MapLocation target) throws GameActionException {
        if (target == null || !Globals.rc.isMovementReady()) return false;
        if (Globals.myLoc.equals(target)) return true;
        
        
        if (Clock.getBytecodesLeft() > 2000) {
            if (MinosNav.navigateTo(target)) {
                return true;
            }
        }
        
        
        if (Clock.getBytecodesLeft() >= 3000) {
            Direction dir = AStarPathfinder.findBestDirection(target);
            if (dir != null && dir != Direction.CENTER) {
                if (Mover.tryMove(dir)) {
                    return true;
                }
            }
        }
        
        
        Direction direct = Globals.myLoc.directionTo(target);
        if (Mover.canMoveInDirection(direct)) {
            return Mover.tryMove(direct);
        }
        
        
        Direction bugResult = BugNav.walkTowards(target);
        return bugResult != null;
    }
}
