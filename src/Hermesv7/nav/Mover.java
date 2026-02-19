package Hermesv7.nav;
import battlecode.common.*;
import Hermesv7.*;
public class Mover {
    public static boolean tryMove(Direction dir) throws GameActionException {
        if (dir == null || dir == Direction.CENTER) return false;
        RobotController rc = Globals.rc;
        if (rc.getDirection() != dir && rc.canTurn()) {
            rc.turn(dir);
        }
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }
        return false;
    }
    public static boolean tryMoveForward() throws GameActionException {
        RobotController rc = Globals.rc;
        if (rc.canMoveForward()) {
            rc.moveForward();
            return true;
        }
        return false;
    }
    public static boolean canMoveInDirection(Direction dir) throws GameActionException {
        if (dir == null || dir == Direction.CENTER) return false;
        RobotController rc = Globals.rc;
        if (rc.getDirection() != dir) {
            if (!rc.canTurn()) return false;
        }
        return rc.canMove(dir);
    }
}