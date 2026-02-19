package Hermesv4.robot;

import battlecode.common.*;
import Hermesv4.*;
import Hermesv4.comms.*;
import Hermesv4.nav.*;
import Hermesv4.economy.*;
import Hermesv4.strategy.*;

public class KingDefender {

    public static void defend() throws GameActionException {
        CommArray.setFlag(CommArray.FLAG_DEFEND_KING, 1);
        
        if (Globals.rc.isActionReady()) {
            int threatLevel = calculateThreatLevel();
            
            while (Globals.rc.isActionReady() && Clock.getBytecodesLeft() > 1000) {
                boolean actionTaken = false;
                
                
                if (threatLevel > 20) {
                    if (KingManager.tryPlaceDefensiveTraps()) {
                        actionTaken = true;
                        continue;
                    }
                }
                
                if (threatLevel > 30) {
                    if (TrapPlacer.tryPlaceRatTrap()) {
                        actionTaken = true;
                        continue;
                    }
                }
                
                
                
                int phase = GamePhase.calculate();
                int availableCheese = Globals.rc.getAllCheese();
                int minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * 15;
                int excessCheese = availableCheese - minBuffer;
                
                
                if (phase >= GamePhase.PHASE_MID && threatLevel > 60 && excessCheese >= 150) {
                    if (TrapPlacer.tryPlaceDefensiveWall()) {
                        actionTaken = true;
                        continue;
                    }
                }
                
                
                if (threatLevel > 50 && Globals.nearbyEnemies != null && Globals.nearbyEnemies.length > 0) {
                    boolean canAffordChokepoint = (phase == GamePhase.PHASE_LATE && excessCheese >= 100) ||
                                                  (phase == GamePhase.PHASE_MID && excessCheese >= 200);
                    if (canAffordChokepoint) {
                        MapLocation nearestThreat = null;
                        int minThreatDist = Integer.MAX_VALUE;
                        for (RobotInfo enemy : Globals.nearbyEnemies) {
                            if (enemy != null) {
                                int dist = Globals.myLoc.distanceSquaredTo(enemy.getLocation());
                                if (dist < minThreatDist) {
                                    minThreatDist = dist;
                                    nearestThreat = enemy.getLocation();
                                }
                            }
                        }
                        if (nearestThreat != null && minThreatDist <= 16) {
                            if (TrapPlacer.tryCreateChokepoint(Globals.myLoc, nearestThreat)) {
                                actionTaken = true;
                                continue;
                            }
                        }
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
            boolean catThreat = false;
            if (Globals.nearbyCats != null) {
                for (RobotInfo cat : Globals.nearbyCats) {
                    if (cat != null && Globals.myLoc.distanceSquaredTo(cat.getLocation()) <= 16) {
                        catThreat = true;
                        break;
                    }
                }
            }
            
            if (catThreat) {
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
