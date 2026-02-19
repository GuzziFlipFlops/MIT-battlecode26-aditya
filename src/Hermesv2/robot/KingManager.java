package Hermesv2.robot;

import battlecode.common.*;
import Hermesv2.*;
import Hermesv2.comms.*;
import Hermesv2.nav.*;
import Hermesv2.economy.*;
import Hermesv2.strategy.*;
import Hermesv2.robot.*;

public class KingManager {

    public static void manage() throws GameActionException {
        int availableCheese = Globals.rc.getAllCheese();
        int roundsLeft = Constants.MAX_ROUNDS - Globals.roundNum;
        int phase = GamePhase.calculate();
        
        
        
        int minBuffer = Math.max(Constants.RATKING_CHEESE_CONSUMPTION * 15, Constants.RATKING_CHEESE_CONSUMPTION * (roundsLeft / 5 + 10));
        if (roundsLeft <= 20) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * (roundsLeft + 5); 
        }
        boolean criticalCheese = availableCheese < minBuffer;
        
        
        if (criticalCheese) {
            
            try {
                CommArray.setFlag(CommArray.FLAG_DEFEND_KING, 2);
            } catch (GameActionException e) {
            }
            
            
            if (Globals.rc.isMovementReady()) {
                
                MapInfo[] nearbyInfos = Globals.nearbyMapInfos;
                MapLocation nearestCheese = null;
                int minDist = Integer.MAX_VALUE;
                if (nearbyInfos != null) {
                    for (MapInfo info : nearbyInfos) {
                        if (info != null && info.getCheeseAmount() > 0) {
                            MapLocation loc = info.getMapLocation();
                            if (loc != null) {
                                int dist = Globals.myLoc.distanceSquaredTo(loc);
                                if (dist < minDist) {
                                    minDist = dist;
                                    nearestCheese = loc;
                                }
                            }
                        }
                    }
                }
                
                if (nearestCheese != null) {
                    Navigator.navigateTo(nearestCheese);
                    return;
                }
                
                
                MapLocation nearestMine = CommArray.getNearestCheeseMine();
                if (nearestMine != null) {
                    Navigator.navigateTo(nearestMine);
                    return;
                }
                Navigator.navigateTo(Globals.mapCenter);
                return;
            }
            
            return;
        }
        
        
        try {
            if (availableCheese > minBuffer + 20) {
                CommArray.setFlag(CommArray.FLAG_DEFEND_KING, 0);
            }
        } catch (GameActionException e) {
        }
        
        
        int excessCheese = availableCheese - minBuffer;
        boolean canSpawnAggressively = excessCheese > 50;
        boolean canPlaceTraps = excessCheese > 20;
        
        
        while (Globals.rc.isActionReady() && Clock.getBytecodesLeft() > 1000) {
            boolean actionTaken = false;
            
            
            
            if (canSpawnAggressively || phase == GamePhase.PHASE_EARLY || excessCheese >= 0) {
                if (trySpawnRats()) {
                    actionTaken = true;
                    
                    availableCheese = Globals.rc.getAllCheese();
                    excessCheese = availableCheese - minBuffer;
                    canSpawnAggressively = excessCheese > 50;
                    canPlaceTraps = excessCheese > 20;
                    continue;
                }
            }
            
            
            if (tryRatKingFormation()) {
                actionTaken = true;
                continue;
            }
            
            
            boolean hasThreat = (Globals.nearbyEnemies != null && Globals.nearbyEnemies.length > 0) || 
                               (Globals.nearbyCats != null && Globals.nearbyCats.length > 0) ||
                               Globals.myHealth < 300;
            
            if (hasThreat && canPlaceTraps && Globals.roundNum >= 20) {
                if (tryPlaceDefensiveTraps()) {
                    actionTaken = true;
                    excessCheese = Globals.rc.getAllCheese() - minBuffer;
                    canPlaceTraps = excessCheese > 20;
                    continue;
                }
            }
            
            
            if (canPlaceTraps) {
                if (tryPlaceTraps()) {
                    actionTaken = true;
                    excessCheese = Globals.rc.getAllCheese() - minBuffer;
                    canPlaceTraps = excessCheese > 20;
                    continue;
                }
            }
            
            
            
            if (excessCheese >= -10) { 
                if (trySpawnRats()) {
                    actionTaken = true;
                    availableCheese = Globals.rc.getAllCheese();
                    excessCheese = availableCheese - minBuffer;
                    continue;
                }
            }
            
            if (!actionTaken) break;
        }
        
        reportCheeseMines();
        reportEnemies();
        
        if (Globals.rc.isMovementReady()) {
            MapLocation target = Globals.mapCenter;
            if (Globals.numKnownCheeseMines > 0) {
                target = CommArray.getNearestCheeseMine();
            }
            if (target != null && Globals.myLoc.distanceSquaredTo(target) > 100) {
                Navigator.navigateTo(target);
            } else {
                moveTowardBetterPosition();
            }
        }
    }

    private static boolean tryRatKingFormation() throws GameActionException {
        if (!Globals.rc.canBecomeRatKing()) return false;
        if (Globals.globalCheese < Constants.RAT_KING_UPGRADE_COST) return false;
        
        int allyCount = 0;
        boolean hasEnemyInZone = false;
        
        if (Globals.nearbyAllies != null) {
            for (RobotInfo ally : Globals.nearbyAllies) {
                if (ally != null && ally.getType() == UnitType.BABY_RAT) {
                    int dist = Globals.myLoc.distanceSquaredTo(ally.getLocation());
                    if (dist <= 2) {
                        allyCount++;
                    }
                }
            }
        }
        
        if (!Globals.isCooperation && Globals.nearbyEnemies != null) {
            for (RobotInfo enemy : Globals.nearbyEnemies) {
                if (enemy != null) {
                    int dist = Globals.myLoc.distanceSquaredTo(enemy.getLocation());
                    if (dist <= 2) {
                        hasEnemyInZone = true;
                        break;
                    }
                }
            }
        }
        
        if (allyCount >= 7 || (hasEnemyInZone && allyCount >= 5)) {
            Globals.rc.becomeRatKing();
            return true;
        }
        return false;
    }

    public static boolean tryPlaceDefensiveTraps() throws GameActionException {
        if (!Globals.rc.isActionReady()) return false;
        
        int availableCheese = Globals.rc.getAllCheese();
        int roundsLeft = Constants.MAX_ROUNDS - Globals.roundNum;
        
        int minBuffer = Math.max(Constants.RATKING_CHEESE_CONSUMPTION * 8, Constants.RATKING_CHEESE_CONSUMPTION * (roundsLeft / 15 + 3));
        if (roundsLeft <= 10) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * roundsLeft;
        }
        if (availableCheese < minBuffer + Constants.RAT_TRAP_COST) {
            return false;
        }
        
        MapLocation myLoc = Globals.myLoc;
        Direction[] dirs = Globals.ALL_DIRECTIONS;
        MapLocation bestLoc = null;
        int bestScore = Integer.MIN_VALUE;
        
        for (int i = 0; i < dirs.length; i++) {
            Direction dir = dirs[i];
            if (dir == Direction.CENTER) continue;
            
            MapLocation loc = myLoc.add(dir);
            if (!Globals.rc.canPlaceRatTrap(loc)) continue;
            
            int score = 0;
            int distToKing = loc.distanceSquaredTo(myLoc);
            if (distToKing <= 2) {
                score += 300;
            } else if (distToKing <= 4) {
                score += 200;
            } else if (distToKing <= 9) {
                score += 100;
            } else if (distToKing <= 16) {
                score += 50;
            }
            
            if (Globals.nearbyEnemies != null) {
                for (int j = 0; j < Globals.nearbyEnemies.length && j < 10; j++) {
                    RobotInfo enemy = Globals.nearbyEnemies[j];
                    if (enemy != null) {
                        int dist = loc.distanceSquaredTo(enemy.getLocation());
                        if (dist <= 2) {
                            score += 100;
                        } else if (dist <= 4) {
                            score += 50;
                        } else if (dist <= 9) {
                            score += 20;
                        }
                    }
                }
            }
            
            if (Globals.nearbyCats != null) {
                for (int j = 0; j < Globals.nearbyCats.length && j < 5; j++) {
                    RobotInfo cat = Globals.nearbyCats[j];
                    if (cat != null) {
                        int dist = loc.distanceSquaredTo(cat.getLocation());
                        if (dist <= 4) {
                            score += 40;
                        }
                    }
                }
            }
            
            
            if (score > bestScore) {
                bestScore = score;
                bestLoc = loc;
            }
        }
        
        if (bestLoc != null && bestScore > 20 && Globals.rc.canPlaceRatTrap(bestLoc)) {
            Globals.rc.placeRatTrap(bestLoc);
            return true;
        }
        return false;
    }

    private static boolean tryPlaceTraps() throws GameActionException {
        if (!Globals.rc.isActionReady()) return false;
        
        int availableCheese = Globals.rc.getAllCheese();
        int roundsLeft = Constants.MAX_ROUNDS - Globals.roundNum;
        int phase = GamePhase.calculate();
        
        
        int minBuffer = Math.max(Constants.RATKING_CHEESE_CONSUMPTION * 8, Constants.RATKING_CHEESE_CONSUMPTION * (roundsLeft / 15 + 3));
        if (roundsLeft <= 10) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * roundsLeft;
        }
        
        
        int trapCost = Math.max(Constants.CAT_TRAP_COST, Constants.RAT_TRAP_COST);
        if (availableCheese < minBuffer + trapCost) {
            return false;
        }
        
        
        
        if (phase == GamePhase.PHASE_EARLY && Globals.isCooperation && Globals.roundNum >= 50) {
            if (TrapPlacer.tryPlaceCatTrap()) {
                return true;
            }
        }
        
        
        if (phase == GamePhase.PHASE_MID) {
            
            if (TrapPlacer.tryPlaceRatTrap()) {
                return true;
            }
            
            if (Globals.isCooperation && TrapPlacer.tryPlaceCatTrap()) {
                return true;
            }
        }
        
        
        if (phase == GamePhase.PHASE_LATE) {
            
            if (!Globals.isCooperation) {
                if (TrapPlacer.tryPlaceRatTrap()) {
                    return true;
                }
            } else {
                
                if (TrapPlacer.tryPlaceCatTrap()) {
                    return true;
                }
                if (TrapPlacer.tryPlaceRatTrap()) {
                    return true;
                }
            }
        }
        
        
        if (RatKingState.getState() == RatKingStateType.DEFENDING || RatKingState.getState() == RatKingStateType.RETREATING) {
            if (TrapPlacer.tryPlaceRatTrap()) {
                return true;
            }
        }
        
        
        if (!Globals.isCooperation || Globals.roundNum > 200) {
            if (TrapPlacer.tryPlaceRatTrap()) {
                return true;
            }
        }
        
        return false;
    }

    public static boolean trySpawnRats() throws GameActionException {
        if (!Globals.rc.isActionReady()) {
            return false;
        }
        
        int availableCheese = Globals.rc.getAllCheese();
        int spawnCost = Globals.getSpawnCost();
        
        int roundsLeft = Constants.MAX_ROUNDS - Globals.roundNum;
        int phase = GamePhase.calculate();
        
        
        int minBuffer = Math.max(Constants.RATKING_CHEESE_CONSUMPTION * 15, Constants.RATKING_CHEESE_CONSUMPTION * (roundsLeft / 5 + 10));
        if (roundsLeft <= 20) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * (roundsLeft + 5);
        }
        
        
        if (availableCheese < spawnCost + minBuffer) {
            return false;
        }
        
        
        int excessCheese = availableCheese - minBuffer - spawnCost;
        
        
        
        boolean shouldSpawn = false;
        if (phase == GamePhase.PHASE_EARLY) {
            
            shouldSpawn = true;
        } else if (phase == GamePhase.PHASE_MID) {
            
            shouldSpawn = excessCheese >= 0; 
        } else {
            
            shouldSpawn = excessCheese >= -20; 
        }
        
        if (!shouldSpawn) {
            return false;
        }
        
        MapLocation[] adjLocs = Globals.rc.getAllLocationsWithinRadiusSquared(Globals.myLoc, Constants.BUILD_RADIUS_SQ);
        
        if (adjLocs == null || adjLocs.length == 0) {
            return false;
        }
        
        for (MapLocation loc : adjLocs) {
            if (loc != null && Globals.rc.canBuildRat(loc)) {
                Globals.rc.buildRat(loc);
                return true;
            }
        }
        return false;
    }

    private static void reportCheeseMines() throws GameActionException {
        if (Globals.nearbyMapInfos != null) {
            for (MapInfo info : Globals.nearbyMapInfos) {
                if (info != null && info.hasCheeseMine()) {
                    CommArray.reportCheeseMine(info.getMapLocation());
                }
            }
        }
    }

    private static void reportEnemies() throws GameActionException {
        if (Globals.nearbyCats != null) {
            for (RobotInfo cat : Globals.nearbyCats) {
                if (cat != null) {
                    CommArray.reportCat(cat.getLocation());
                }
            }
        }
        
        if (Globals.nearbyEnemies != null) {
            for (RobotInfo enemy : Globals.nearbyEnemies) {
                if (enemy != null && enemy.getType() == UnitType.RAT_KING) {
                    CommArray.reportEnemyKing(enemy.getLocation());
                }
            }
        }
    }

    public static void moveTowardBetterPosition() throws GameActionException {
        if (Globals.isNearCatSpawn(Globals.myLoc, 100)) {
            Direction awayFromCenter = Globals.mapCenter.directionTo(Globals.myLoc);
            if (awayFromCenter != Direction.CENTER) {
                MapLocation saferSpot = Globals.myLoc.add(awayFromCenter).add(awayFromCenter);
                if (Navigator.navigateTo(saferSpot)) {
                    return;
                }
            }
        }
        
        MapLocation target = Globals.mapCenter;
        if (Globals.numKnownCheeseMines > 0) {
            target = CommArray.getNearestCheeseMine();
        }
        
        if (target != null) {
            if (!Globals.isNearCatSpawn(target, 64)) {
                if (Navigator.navigateTo(target)) {
                    return;
                }
            }
        }
        
        Direction[] dirs = Globals.ALL_DIRECTIONS;
        for (Direction dir : dirs) {
            if (dir != Direction.CENTER && Globals.rc.canMove(dir)) {
                Globals.rc.move(dir);
                return;
            }
        }
    }
}


