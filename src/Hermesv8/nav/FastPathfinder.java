package Hermesv8.nav;
import battlecode.common.*;
import Hermesv8.*;
public class FastPathfinder {
    public static boolean navigateTo(MapLocation target) throws GameActionException {
        if (target == null || !Globals.rc.isMovementReady()) return false;
        if (Globals.myLoc.equals(target)) return true;
        Direction direct = Globals.myLoc.directionTo(target);
        if (direct != Direction.CENTER && Mover.canMoveInDirection(direct)) {
            if (Mover.tryMove(direct)) {
                return true;
            }
        }
        Direction bestDir = null;
        int bestDist = Globals.myLoc.distanceSquaredTo(target);
        Direction[] dirs = {direct, direct.rotateLeft(), direct.rotateRight(),
                           direct.rotateLeft().rotateLeft(), direct.rotateRight().rotateRight(),
                           direct.rotateLeft().rotateLeft().rotateLeft(), direct.rotateRight().rotateRight().rotateRight(),
                           direct.opposite().rotateLeft(), direct.opposite().rotateRight()};
        for (Direction dir : dirs) {
            if (dir == Direction.CENTER) continue;
            if (!Mover.canMoveInDirection(dir)) continue;
            MapLocation newLoc = Globals.myLoc.add(dir);
            int newDist = newLoc.distanceSquaredTo(target);
            if (newDist < bestDist) {
                bestDist = newDist;
                bestDir = dir;
            }
        }
        if (bestDir != null) {
            return Mover.tryMove(bestDir);
        }
        Direction[] allDirs = Globals.ALL_DIRECTIONS;
        for (Direction dir : allDirs) {
            if (dir == Direction.CENTER) continue;
            if (!Mover.canMoveInDirection(dir)) continue;
            MapLocation newLoc = Globals.myLoc.add(dir);
            int newDist = newLoc.distanceSquaredTo(target);
            if (newDist < bestDist) {
                bestDist = newDist;
                bestDir = dir;
            }
        }
        if (bestDir != null) {
            return Mover.tryMove(bestDir);
        }
        Direction bugResult = BugNav.walkTowards(target);
        return bugResult != null;
    }
}