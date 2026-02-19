package Hermesv4.nav;

import battlecode.common.*;
import Hermesv4.*;

public class FleeNav {

    public static boolean fleeFrom(MapLocation danger) throws GameActionException {
        if (danger == null) return false;
        
        Direction away = danger.directionTo(Globals.myLoc);
        
        Direction[] tryDirs = {
            away,
            away.rotateLeft(),
            away.rotateRight(),
            away.rotateLeft().rotateLeft(),
            away.rotateRight().rotateRight()
        };
        
        for (Direction dir : tryDirs) {
            if (Mover.tryMove(dir)) {
                return true;
            }
        }
        
        return false;
    }

    public static boolean fleeFromMultiple(MapLocation[] dangers) throws GameActionException {
        if (dangers == null || dangers.length == 0) return false;
        
        int dx = 0, dy = 0;
        for (MapLocation danger : dangers) {
            if (danger != null) {
                dx += danger.x - Globals.myLoc.x;
                dy += danger.y - Globals.myLoc.y;
            }
        }
        
        Direction away = Direction.CENTER;
        if (dx != 0 || dy != 0) {
            MapLocation avgDanger = new MapLocation(
                Globals.myLoc.x + dx,
                Globals.myLoc.y + dy
            );
            away = avgDanger.directionTo(Globals.myLoc);
        }
        
        if (away == Direction.CENTER) {
            return RandomMover.tryMoveRandom();
        }
        
        return fleeFrom(new MapLocation(
            Globals.myLoc.x - (away.getDeltaX() * 10),
            Globals.myLoc.y - (away.getDeltaY() * 10)
        ));
    }
}
