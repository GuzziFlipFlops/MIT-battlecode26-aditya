package Hermesv3.nav;

import battlecode.common.*;
import Hermesv3.*;

public class BugNav {

    private static boolean bugging = false;
    private static MapLocation bugTarget = null;
    private static Direction bugDir = null;
    private static int bugTurnDir = 1;
    private static MapLocation bugStart = null;
    private static int bugStartDist = Integer.MAX_VALUE;

    public static void reset() {
        bugging = false;
        bugTarget = null;
        bugDir = null;
        bugStart = null;
        bugStartDist = Integer.MAX_VALUE;
    }

    public static MapLocation getTarget() {
        return bugTarget;
    }

    public static void setTarget(MapLocation target) {
        bugTarget = target;
    }

    public static boolean isBugging() {
        return bugging;
    }

    public static void startBugging(Direction direct, MapLocation target) throws GameActionException {
        bugging = true;
        bugDir = direct;
        bugStart = Globals.myLoc;
        bugStartDist = Globals.myLoc.distanceSquaredTo(target);
        
        int leftDist = simulateBugDistance(direct.rotateLeft(), target, -1);
        int rightDist = simulateBugDistance(direct.rotateRight(), target, 1);
        bugTurnDir = (leftDist <= rightDist) ? -1 : 1;
    }

    public static boolean executeBugNav(MapLocation target) throws GameActionException {
        int currentDist = Globals.myLoc.distanceSquaredTo(target);
        if (currentDist < bugStartDist) {
            Direction direct = Globals.myLoc.directionTo(target);
            if (Mover.canMoveInDirection(direct)) {
                reset();
                return Mover.tryMove(direct);
            }
        }
        
        Direction moveDir = bugDir;
        
        for (int i = 0; i < 8; i++) {
            if (Mover.canMoveInDirection(moveDir)) {
                if (bugTurnDir == 1) {
                    bugDir = moveDir.rotateLeft().rotateLeft();
                } else {
                    bugDir = moveDir.rotateRight().rotateRight();
                }
                return Mover.tryMove(moveDir);
            }
            
            if (bugTurnDir == 1) {
                moveDir = moveDir.rotateRight();
            } else {
                moveDir = moveDir.rotateLeft();
            }
        }
        
        return false;
    }

    private static int simulateBugDistance(Direction startDir, MapLocation target, int turnDir) throws GameActionException {
        Direction dir = startDir;
        for (int i = 0; i < 8; i++) {
            MapLocation newLoc = Globals.myLoc.add(dir);
            if (Globals.rc.canSenseLocation(newLoc) && Globals.rc.sensePassability(newLoc)) {
                return newLoc.distanceSquaredTo(target);
            }
            if (turnDir == 1) {
                dir = dir.rotateRight();
            } else {
                dir = dir.rotateLeft();
            }
        }
        return Integer.MAX_VALUE;
    }
}
