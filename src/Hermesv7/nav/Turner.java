package Hermesv7.nav;
import battlecode.common.*;
import Hermesv7.*;
public class Turner {
    public static boolean turnTo(Direction dir) throws GameActionException {
        if (dir == null || dir == Direction.CENTER) return false;
        RobotController rc = Globals.rc;
        if (rc.getDirection() == dir) return true;
        if (rc.canTurn()) {
            rc.turn(dir);
            return true;
        }
        return false;
    }
    public static boolean turnToward(MapLocation target) throws GameActionException {
        if (target == null) return false;
        Direction dir = Globals.myLoc.directionTo(target);
        return turnTo(dir);
    }
}