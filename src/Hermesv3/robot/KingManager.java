package Hermesv3.robot;

import battlecode.common.*;
import Hermesv3.*;
import Hermesv3.comms.*;
import Hermesv3.nav.*;
import Hermesv3.economy.*;
import Hermesv3.strategy.*;
import Hermesv3.robot.*;

public class KingManager {

    public static void manage() throws GameActionException {
        int availableCheese = Globals.rc.getAllCheese();
        int roundsLeft = Constants.MAX_ROUNDS - Globals.roundNum;
        int phase = GamePhase.calculate();
        
        
        
        
        
        int constantFeedingBuffer = Constants.RATKING_CHEESE_CONSUMPTION * Math.max(40, (roundsLeft / 2) + 20);
        int minBuffer = constantFeedingBuffer;
        if (roundsLeft <= 20) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * (roundsLeft + 10); 
        }
        boolean criticalCheese = availableCheese < minBuffer;
        
        
        if (criticalCheese) {
            
            try {
                CommArray.setFlag(CommArray.FLAG_DEFEND_KING, 2);
            } catch (GameActionException e) {
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
        
        
        
        
        
        int spawnThreshold, trapThreshold, expensiveThreshold;
        if (phase == GamePhase.PHASE_EARLY) {
            spawnThreshold = 0; 
            trapThreshold = 20; 
            expensiveThreshold = 999999; 
        } else if (phase == GamePhase.PHASE_MID) {
            spawnThreshold = -10; 
            trapThreshold = 20; 
            expensiveThreshold = 200; 
        } else { 
            spawnThreshold = -20; 
            trapThreshold = 20; 
            expensiveThreshold = 150; 
        }
        
        boolean canSpawn = excessCheese >= spawnThreshold;
        boolean canPlaceTraps = excessCheese >= trapThreshold;
        boolean canPlaceExpensive = excessCheese >= expensiveThreshold;
        
        
        while (Globals.rc.isActionReady() && Clock.getBytecodesLeft() > 1000) {
            boolean actionTaken = false;
            
            
            if (canSpawn) {
                if (trySpawnRats()) {
                    actionTaken = true;
                    
                    availableCheese = Globals.rc.getAllCheese();
                    excessCheese = availableCheese - minBuffer;
                    canSpawn = excessCheese >= spawnThreshold;
                    canPlaceTraps = excessCheese >= trapThreshold;
                    canPlaceExpensive = excessCheese >= expensiveThreshold;
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
                    canPlaceTraps = excessCheese >= trapThreshold;
                    canPlaceExpensive = excessCheese >= expensiveThreshold;
                    continue;
                }
            }
            
            
            if (canPlaceTraps) {
                if (tryPlaceTraps()) {
                    actionTaken = true;
                    excessCheese = Globals.rc.getAllCheese() - minBuffer;
                    canPlaceTraps = excessCheese >= trapThreshold;
                    canPlaceExpensive = excessCheese >= expensiveThreshold;
                    continue;
                }
            }
            
            
            if (excessCheese >= spawnThreshold) {
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
            moveTowardBetterPosition();
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
        
        
        
        if (Globals.isCooperation && TrapPlacer.tryPlaceCatTrap()) {
            return true;
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
        
        
        
        int constantFeedingBuffer = Constants.RATKING_CHEESE_CONSUMPTION * Math.max(40, (roundsLeft / 2) + 20);
        int minBuffer = constantFeedingBuffer;
        if (roundsLeft <= 20) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * (roundsLeft + 10);
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
        
        if (!Globals.rc.isMovementReady()) return;
        
        
        boolean catNearby = false;
        
        if (Globals.nearbyCats != null) {
            for (RobotInfo cat : Globals.nearbyCats) {
                if (cat != null) {
                    int dist = Globals.myLoc.distanceSquaredTo(cat.getLocation());
                    if (dist <= 16) { 
                        catNearby = true;
                        break;
                    }
                }
            }
        }
        
        
        if (!catNearby) {
            return; 
        }
        
        
        Direction[] allDirs = Globals.ALL_DIRECTIONS;
        Direction bestDir = Direction.CENTER;
        int maxMinDist = 0;
        
        for (Direction dir : allDirs) {
            if (dir == Direction.CENTER) continue;
            if (!Globals.rc.canMove(dir)) continue;
            
            MapLocation newLoc = Globals.myLoc.add(dir);
            int minDist = Integer.MAX_VALUE;
            
            
            if (Globals.nearbyCats != null) {
                for (RobotInfo cat : Globals.nearbyCats) {
                    if (cat != null) {
                        int dist = newLoc.distanceSquaredTo(cat.getLocation());
                        minDist = Math.min(minDist, dist);
                    }
                }
            }
            
            if (minDist > maxMinDist) {
                maxMinDist = minDist;
                bestDir = dir;
            }
        }
        
        if (bestDir != Direction.CENTER) {
            Mover.tryMove(bestDir);
        }
    }
}


