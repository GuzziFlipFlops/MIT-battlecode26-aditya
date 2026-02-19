package Hermesv4.nav;

import battlecode.common.*;
import Hermesv4.*;
import Hermesv4.comms.*;

public class Explorer {

    public static boolean explore() throws GameActionException {
        if (!Globals.rc.isMovementReady()) return false;
        
        MapLocation enemyKing = CommArray.getNearestEnemyKing();
        if (enemyKing != null) {
            try {
                Squeaker.squeakAttack(enemyKing);
            } catch (GameActionException e) {}
            return Navigator.navigateTo(enemyKing);
        }

        MapLocation myLoc = Globals.myLoc;
        int oppositeX = Globals.mapWidth - 1 - myLoc.x;
        int oppositeY = Globals.mapHeight - 1 - myLoc.y;
        MapLocation oppositeSide = new MapLocation(oppositeX, oppositeY);
        
        if (Globals.nearbyCats != null && Globals.nearbyCats.length > 0) {
            MapLocation[] catLocs = Globals.getLocations(Globals.nearbyCats);
            Direction fleeDir = findDirectionAwayFromCats(catLocs, oppositeSide);
            if (fleeDir != null && fleeDir != Direction.CENTER) {
                if (Mover.tryMove(fleeDir)) {
                    return true;
                }
            }
        }
        
        return Navigator.navigateTo(oppositeSide);
    }
    
    private static Direction findDirectionAwayFromCats(MapLocation[] catLocs, MapLocation target) throws GameActionException {
        if (catLocs == null || catLocs.length == 0) return null;
        
        Direction direct = Globals.myLoc.directionTo(target);
        Direction bestDir = null;
        int bestScore = Integer.MIN_VALUE;
        
        for (Direction dir : Globals.ALL_DIRECTIONS) {
            if (dir == Direction.CENTER) continue;
            if (!Mover.canMoveInDirection(dir)) continue;
            
            MapLocation newLoc = Globals.myLoc.add(dir);
            int score = newLoc.distanceSquaredTo(target);
            
            for (MapLocation catLoc : catLocs) {
                if (catLoc != null) {
                    int distToCat = newLoc.distanceSquaredTo(catLoc);
                    if (distToCat <= 16) {
                        score -= (17 - distToCat) * 100;
                    }
                }
            }
            
            if (score > bestScore) {
                bestScore = score;
                bestDir = dir;
            }
        }
        
        return bestDir;
    }
    
    private static boolean isNearWall(MapLocation loc) throws GameActionException {
        
        for (Direction dir : Globals.ALL_DIRECTIONS) {
            if (dir == Direction.CENTER) continue;
            MapLocation adj = loc.add(dir);
            if (Globals.rc.canSenseLocation(adj)) {
                MapInfo info = Globals.rc.senseMapInfo(adj);
                if (info != null && info.isWall()) {
                    return true;
                }
            }
        }
        return false;
    }
}
