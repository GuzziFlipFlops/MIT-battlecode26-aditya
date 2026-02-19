package Hermesv7.nav;
import battlecode.common.*;
import Hermesv7.*;
public class RandomMover {
    private static final Direction[] MOVE_DIRECTIONS = {
        Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
        Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST
    };
    public static boolean tryMoveRandom() throws GameActionException {
        Direction[] dirs = MOVE_DIRECTIONS.clone();
        for (int i = dirs.length - 1; i > 0; i--) {
            int j = (int) (Math.random() * (i + 1));
            Direction temp = dirs[i];
            dirs[i] = dirs[j];
            dirs[j] = temp;
        }
        for (Direction dir : dirs) {
            MapLocation next = Globals.myLoc.add(dir);
            if (Globals.rc.canSenseLocation(next)) {
                MapInfo info = Globals.rc.senseMapInfo(next);
                if (info != null && info.isWall()) {
                    continue;
                }
            }
            if (Mover.tryMove(dir)) {
                return true;
            }
        }
        return false;
    }
}