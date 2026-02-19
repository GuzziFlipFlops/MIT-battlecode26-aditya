package Hermesv2.robot;

import battlecode.common.*;
import Hermesv2.*;
import Hermesv2.comms.*;
import Hermesv2.nav.*;
import Hermesv2.economy.*;

public class KingDefender {

    public static void defend() throws GameActionException {
        CommArray.setFlag(CommArray.FLAG_DEFEND_KING, 1);
        
        if (Globals.rc.isActionReady()) {
            int threatLevel = calculateThreatLevel();
            
            while (Globals.rc.isActionReady() && Clock.getBytecodesLeft() > 1000) {
                boolean actionTaken = false;
                
                if (threatLevel > 30) {
                    if (KingManager.tryPlaceDefensiveTraps()) {
                        actionTaken = true;
                        continue;
                    }
                }
                
                if (threatLevel > 50) {
                    if (TrapPlacer.tryPlaceRatTrap()) {
                        actionTaken = true;
                        continue;
                    }
                }
                
                if (threatLevel > 80 && Globals.globalCheese >= Constants.DIRT_COST * 3) {
                    if (TrapPlacer.tryPlaceDefensiveWall()) {
                        actionTaken = true;
                        continue;
                    }
                }
                
                if (KingManager.trySpawnRats()) {
                    actionTaken = true;
                    continue;
                }
                
                if (!actionTaken) break;
            }
            
            if (Globals.nearbyCats != null) {
                for (RobotInfo cat : Globals.nearbyCats) {
                    if (cat != null && Globals.myLoc.isAdjacentTo(cat.getLocation())) {
                        if (Globals.rc.canAttack(cat.getLocation())) {
                            Globals.rc.attack(cat.getLocation());
                            return;
                        }
                    }
                }
            }
        }
        
        if (Globals.rc.isMovementReady()) {
            MapLocation safestDir = findSafestDirection();
            if (safestDir != null && !safestDir.equals(Globals.myLoc)) {
                Direction dir = Globals.myLoc.directionTo(safestDir);
                if (Mover.tryMove(dir)) {
                    return;
                }
            }
            KingManager.moveTowardBetterPosition();
        }
    }

    private static int calculateThreatLevel() {
        int threat = 0;
        
        if (Globals.nearbyCats != null) {
            for (RobotInfo cat : Globals.nearbyCats) {
                if (cat != null) {
                    int dist = Globals.myLoc.distanceSquaredTo(cat.getLocation());
                    if (dist <= 4) threat += 80;
                    else if (dist <= 9) threat += 50;
                    else if (dist <= 16) threat += 30;
                    else if (dist <= 25) threat += 15;
                }
            }
        }
        
        if (!Globals.isCooperation && Globals.nearbyEnemies != null) {
            for (RobotInfo enemy : Globals.nearbyEnemies) {
                if (enemy != null) {
                    int dist = Globals.myLoc.distanceSquaredTo(enemy.getLocation());
                    if (enemy.getType() == UnitType.RAT_KING) {
                        if (dist <= 4) threat += 100;
                        else if (dist <= 9) threat += 60;
                        else if (dist <= 16) threat += 30;
                    } else {
                        if (dist <= 4) threat += 40;
                        else if (dist <= 9) threat += 25;
                        else if (dist <= 16) threat += 10;
                    }
                }
            }
        }
        
        if (Globals.myHealth < 200) {
            threat += 50;
        }
        if (Globals.myHealth < 100) {
            threat += 100;
        }
        
        return threat;
    }

    public static MapLocation findSafestDirection() throws GameActionException {
        Direction[] dirs = Globals.ALL_DIRECTIONS;
        MapLocation safest = Globals.myLoc;
        int maxMinDist = 0;
        
        for (Direction dir : dirs) {
            if (dir == Direction.CENTER) continue;
            
            MapLocation newLoc = Globals.myLoc.add(dir);
            if (!Globals.rc.canMove(dir)) continue;
            
            int minDist = Integer.MAX_VALUE;
            
            if (Globals.nearbyCats != null) {
                for (RobotInfo cat : Globals.nearbyCats) {
                    if (cat != null) {
                        int dist = newLoc.distanceSquaredTo(cat.getLocation());
                        minDist = Math.min(minDist, dist);
                    }
                }
            }
            
            if (!Globals.isCooperation && Globals.nearbyEnemies != null) {
                for (RobotInfo enemy : Globals.nearbyEnemies) {
                    if (enemy != null && enemy.getType() == UnitType.RAT_KING) {
                        int dist = newLoc.distanceSquaredTo(enemy.getLocation());
                        minDist = Math.min(minDist, dist);
                    }
                }
            }
            
            if (minDist > maxMinDist) {
                maxMinDist = minDist;
                safest = newLoc;
            }
        }
        
        return safest;
    }
}
