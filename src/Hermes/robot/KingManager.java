package Hermes.robot;

import battlecode.common.*;
import Hermes.*;
import Hermes.comms.*;
import Hermes.nav.*;
import Hermes.economy.*;
import Hermes.strategy.*;
import Hermes.robot.*;

public class KingManager {

    public static void manage() throws GameActionException {
        int availableCheese = Globals.rc.getAllCheese();
        int roundsLeft = Constants.MAX_ROUNDS - Globals.roundNum;
        int phase = GamePhase.calculate();
        
        // Calculate minimum buffer - ensure we NEVER starve
        // Be very conservative: need enough for consumption + safety margin
        int minBuffer = Math.max(Constants.RATKING_CHEESE_CONSUMPTION * 15, Constants.RATKING_CHEESE_CONSUMPTION * (roundsLeft / 5 + 10));
        if (roundsLeft <= 20) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * (roundsLeft + 5); // Extra safety
        }
        boolean criticalCheese = availableCheese < minBuffer;
        
        // If critical, DO NOTHING except get cheese - no spawning, no traps, nothing
        if (criticalCheese) {
            // Set emergency flag so baby rats prioritize cheese
            try {
                CommArray.setFlag(CommArray.FLAG_DEFEND_KING, 2);
            } catch (GameActionException e) {
            }
            
            // Move toward cheese immediately
            if (Globals.rc.isMovementReady()) {
                // First check for nearby cheese
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
                
                // Otherwise go to nearest mine
                MapLocation nearestMine = CommArray.getNearestCheeseMine();
                if (nearestMine != null) {
                    Navigator.navigateTo(nearestMine);
                    return;
                }
                Navigator.navigateTo(Globals.mapCenter);
                return;
            }
            // Don't do ANYTHING else if critical
            return;
        }
        
        // Clear emergency flag if we're safe
        try {
            if (availableCheese > minBuffer + 20) {
                CommArray.setFlag(CommArray.FLAG_DEFEND_KING, 0);
            }
        } catch (GameActionException e) {
        }
        
        // Calculate excess cheese for aggressive production
        int excessCheese = availableCheese - minBuffer;
        boolean canSpawnAggressively = excessCheese > 50;
        boolean canPlaceTraps = excessCheese > 20;
        
        // Continuous production loop - prioritize spawning, then traps
        while (Globals.rc.isActionReady() && Clock.getBytecodesLeft() > 1000) {
            boolean actionTaken = false;
            
            // PRIORITY 1: Spawn rats continuously (scaled by cheese availability)
            // Always try to spawn if we have enough cheese - be more aggressive
            if (canSpawnAggressively || phase == GamePhase.PHASE_EARLY || excessCheese >= 0) {
                if (trySpawnRats()) {
                    actionTaken = true;
                    // Update excess cheese after spawning
                    availableCheese = Globals.rc.getAllCheese();
                    excessCheese = availableCheese - minBuffer;
                    canSpawnAggressively = excessCheese > 50;
                    canPlaceTraps = excessCheese > 20;
                    continue;
                }
            }
            
            // PRIORITY 2: Formation bomb if opportunity
            if (tryRatKingFormation()) {
                actionTaken = true;
                continue;
            }
            
            // PRIORITY 3: Defensive traps if threatened
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
            
            // PRIORITY 4: Offensive trap placement (following strategy plan)
            if (canPlaceTraps) {
                if (tryPlaceTraps()) {
                    actionTaken = true;
                    excessCheese = Globals.rc.getAllCheese() - minBuffer;
                    canPlaceTraps = excessCheese > 20;
                    continue;
                }
            }
            
            // PRIORITY 5: Always try spawning if we have minimum buffer (continuous production)
            // This ensures we keep producing rats throughout the game
            if (excessCheese >= -10) { // Allow slight deficit to keep production going
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
        // Calculate minimum buffer
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
        
        // Calculate minimum buffer
        int minBuffer = Math.max(Constants.RATKING_CHEESE_CONSUMPTION * 8, Constants.RATKING_CHEESE_CONSUMPTION * (roundsLeft / 15 + 3));
        if (roundsLeft <= 10) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * roundsLeft;
        }
        
        // Need enough cheese for trap + buffer
        int trapCost = Math.max(Constants.CAT_TRAP_COST, Constants.RAT_TRAP_COST);
        if (availableCheese < minBuffer + trapCost) {
            return false;
        }
        
        // Follow strategy plan for trap placement
        // EARLY GAME (1-300): Place 3-5 cat traps
        if (phase == GamePhase.PHASE_EARLY && Globals.isCooperation && Globals.roundNum >= 50) {
            if (TrapPlacer.tryPlaceCatTrap()) {
                return true;
            }
        }
        
        // MID GAME (300-1000): Place defensive rat traps
        if (phase == GamePhase.PHASE_MID) {
            // Place rat traps defensively
            if (TrapPlacer.tryPlaceRatTrap()) {
                return true;
            }
            // Also place cat traps if in cooperation
            if (Globals.isCooperation && TrapPlacer.tryPlaceCatTrap()) {
                return true;
            }
        }
        
        // LATE GAME (1000-2000): Maximize all traps
        if (phase == GamePhase.PHASE_LATE) {
            // Prioritize rat traps in backstabbing mode
            if (!Globals.isCooperation) {
                if (TrapPlacer.tryPlaceRatTrap()) {
                    return true;
                }
            } else {
                // In cooperation, place both types
                if (TrapPlacer.tryPlaceCatTrap()) {
                    return true;
                }
                if (TrapPlacer.tryPlaceRatTrap()) {
                    return true;
                }
            }
        }
        
        // Always place rat traps if defending
        if (RatKingState.getState() == RatKingStateType.DEFENDING || RatKingState.getState() == RatKingStateType.RETREATING) {
            if (TrapPlacer.tryPlaceRatTrap()) {
                return true;
            }
        }
        
        // Default: place rat traps if not in early cooperation
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
        
        // Calculate minimum buffer - ensure we NEVER starve (very conservative)
        int minBuffer = Math.max(Constants.RATKING_CHEESE_CONSUMPTION * 15, Constants.RATKING_CHEESE_CONSUMPTION * (roundsLeft / 5 + 10));
        if (roundsLeft <= 20) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * (roundsLeft + 5);
        }
        
        // Check if we can afford to spawn - must have buffer + spawn cost
        if (availableCheese < spawnCost + minBuffer) {
            return false;
        }
        
        // Calculate excess cheese to determine spawn priority
        int excessCheese = availableCheese - minBuffer - spawnCost;
        
        // Continuous production - spawn whenever we can afford it
        // Scale aggressiveness by phase, but always produce if possible
        boolean shouldSpawn = false;
        if (phase == GamePhase.PHASE_EARLY) {
            // Early game: spawn continuously to build army (15-20 rats)
            shouldSpawn = true;
        } else if (phase == GamePhase.PHASE_MID) {
            // Mid game: spawn more aggressively to maintain/grow army
            shouldSpawn = excessCheese >= 0; // Spawn if we have buffer
        } else {
            // Late game: spawn to replace losses and maintain pressure
            shouldSpawn = excessCheese >= -20; // Allow slight deficit to keep production
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


