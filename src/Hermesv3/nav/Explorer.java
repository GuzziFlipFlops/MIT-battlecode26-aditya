package Hermesv3.nav;

import battlecode.common.*;
import Hermesv3.*;
import Hermesv3.comms.*;

public class Explorer {

    public static boolean explore() throws GameActionException {
        
        MapLocation enemyKing = CommArray.getNearestEnemyKing();
        if (enemyKing != null) {
            return Navigator.navigateTo(enemyKing);
        }

        MapLocation myLoc = Globals.myLoc;
        MapLocation center = Globals.mapCenter;
        
        
        int distToCenter = myLoc.distanceSquaredTo(center);
        
        
        if (distToCenter > 100) {
            
            if (Navigator.navigateTo(center)) {
                return true;
            }
        }
        
        
        if (Globals.isNearCatSpawn(myLoc, 36)) {
            Direction away = center.directionTo(myLoc);
            if (away != Direction.CENTER && Mover.tryMove(away)) {
                return true;
            }
        }
        
        
        if (distToCenter <= 100) {
            
            int enemyX = center.x + (center.x - myLoc.x);
            int enemyY = center.y + (center.y - myLoc.y);
            enemyX = Math.max(0, Math.min(Globals.mapWidth - 1, enemyX));
            enemyY = Math.max(0, Math.min(Globals.mapHeight - 1, enemyY));
            MapLocation enemyArea = new MapLocation(enemyX, enemyY);
            
            
            if (Navigator.navigateTo(enemyArea)) {
                return true;
            }
        }
        
        
        
        if (isNearWall(myLoc)) {
            
            Direction toCenter = myLoc.directionTo(center);
            if (toCenter != Direction.CENTER) {
                
                if (Mover.tryMove(toCenter)) {
                    return true;
                }
                
                if (Mover.tryMove(toCenter.rotateLeft())) {
                    return true;
                }
                if (Mover.tryMove(toCenter.rotateRight())) {
                    return true;
                }
            }
        }
        
        
        Direction toCenter = myLoc.directionTo(center);
        if (toCenter != Direction.CENTER) {
            
            if (Mover.tryMove(toCenter)) {
                return true;
            }
            
            Direction[] offsets = {toCenter.rotateLeft(), toCenter.rotateRight()};
            for (Direction offset : offsets) {
                if (Mover.tryMove(offset)) {
                    return true;
                }
            }
        }
        
        
        return RandomMover.tryMoveRandom();
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
