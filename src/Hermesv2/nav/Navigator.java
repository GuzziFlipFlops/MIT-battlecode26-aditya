package Hermesv2.nav;

import battlecode.common.*;
import Hermesv2.*;

public class Navigator {

    public static boolean navigateTo(MapLocation target) throws GameActionException {
        if (target == null) return false;
        if (Globals.myLoc.equals(target)) return true;
        
        if (BugNav.getTarget() == null || !BugNav.getTarget().equals(target)) {
            BugNav.reset();
            BugNav.setTarget(target);
        }
        
        Direction direct = Globals.myLoc.directionTo(target);
        
        if (!BugNav.isBugging()) {
            if (Mover.canMoveInDirection(direct)) {
                return Mover.tryMove(direct);
            }
            BugNav.startBugging(direct, target);
        }
        
        return BugNav.executeBugNav(target);
    }
}
