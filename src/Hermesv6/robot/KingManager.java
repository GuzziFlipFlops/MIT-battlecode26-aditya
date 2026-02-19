package Hermesv6.robot;
import battlecode.common.*;
import Hermesv6.*;
import Hermesv6.comms.*;
import Hermesv6.nav.*;
import Hermesv6.economy.*;
import Hermesv6.economy.AdaptiveCheeseEconomy;
import Hermesv6.strategy.*;
public class KingManager {
    public static void manage() throws GameActionException {
        int availableCheese = Globals.rc.getAllCheese();
        int roundsLeft = Constants.MAX_ROUNDS - Globals.roundNum;
        int phase = GamePhase.calculate();
        int minBuffer;
        if (roundsLeft > 1500) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * 200;
        } else if (roundsLeft > 1000) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * 300;
        } else if (roundsLeft > 500) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * 400;
        } else if (roundsLeft > 100) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * 500;
        } else {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * (roundsLeft + 30);
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
            spawnThreshold = -100;
            trapThreshold = 10;
            expensiveThreshold = 999999;
        } else if (phase == GamePhase.PHASE_MID) {
            spawnThreshold = -50;
            trapThreshold = 10;
            expensiveThreshold = 150;
        } else {
            spawnThreshold = -30;
            trapThreshold = 10;
            expensiveThreshold = 100;
        }
        boolean canSpawn = excessCheese >= spawnThreshold;
        boolean canPlaceTraps = excessCheese >= trapThreshold;
        boolean canPlaceExpensive = excessCheese >= expensiveThreshold;
        int maxSpawns = (phase == GamePhase.PHASE_EARLY && excessCheese > 200) ? 2 : 1;
        int spawnCount = 0;
        while (Globals.rc.isActionReady() && Clock.getBytecodesLeft() > 1000 && spawnCount < maxSpawns) {
            boolean actionTaken = false;
            if (canSpawn) {
                if (trySpawnRats()) {
                    spawnCount++;
                    actionTaken = true;
                    availableCheese = Globals.rc.getAllCheese();
                    excessCheese = availableCheese - minBuffer;
                    canSpawn = excessCheese >= spawnThreshold;
                    canPlaceTraps = excessCheese >= trapThreshold;
                    canPlaceExpensive = excessCheese >= expensiveThreshold;
                    continue;
                }
            }
            int totalSpawned = CommArray.getSpawnCount();
            int currentRatKingCount = CommArray.getRatKingCount();
            if (totalSpawned >= 8 && currentRatKingCount < CommArray.MAX_RAT_KINGS) {
                try {
                    CommArray.setFlag(CommArray.FLAG_FORM_RAT_KING, 1);
                    if (Globals.roundNum % 20 == 0) {
                        System.out.println("[DEBUG KingManager] Signaling formation: totalSpawned=" + totalSpawned + ", currentKings=" + currentRatKingCount);
                    }
                } catch (GameActionException e) {}
            } else {
                try {
                    CommArray.setFlag(CommArray.FLAG_FORM_RAT_KING, 0);
                } catch (GameActionException e) {}
            }
            boolean hasThreat = (Globals.nearbyEnemies != null && Globals.nearbyEnemies.length > 0) ||
                               (Globals.nearbyCats != null && Globals.nearbyCats.length > 0) ||
                               Globals.myHealth < 300;
            if (canPlaceTraps && Globals.roundNum >= 10) {
                if (TrapPlacer.tryPlaceCatTrap()) {
                    actionTaken = true;
                    excessCheese = Globals.rc.getAllCheese() - minBuffer;
                    canPlaceTraps = excessCheese >= trapThreshold;
                    canPlaceExpensive = excessCheese >= expensiveThreshold;
                    continue;
                }
            }
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
        
        int currentRatKingCount = CommArray.getRatKingCount();
        if (currentRatKingCount >= CommArray.MAX_RAT_KINGS) {
            return false;
        }
        
        if (Globals.globalCheese < Constants.RAT_KING_UPGRADE_COST) return false;
        
        int totalSpawned = CommArray.getSpawnCount();
        if (totalSpawned < 8) {
            return false;
        }
        
        int allyCount = 0;
        MapLocation[] allyLocs = new MapLocation[9];
        int locIndex = 0;
        
        if (Globals.nearbyAllies != null) {
            for (RobotInfo ally : Globals.nearbyAllies) {
                if (ally != null && ally.getType() == UnitType.BABY_RAT) {
                    int dist = Globals.myLoc.distanceSquaredTo(ally.getLocation());
                    if (dist <= 2) {
                        allyCount++;
                        if (locIndex < 9) {
                            allyLocs[locIndex++] = ally.getLocation();
                        }
                    }
                }
            }
        }
        
        boolean has3x3Grid = check3x3Grid(allyLocs, allyCount);
        boolean shouldForm = false;
        if (has3x3Grid && allyCount >= 8) {
            shouldForm = true;
        } else if (allyCount >= 3 && totalSpawned >= 8) {
            shouldForm = true;
        }
        
        if (shouldForm) {
            int teamCheese = Globals.rc.getAllCheese();
            System.out.println("[DEBUG KingManager.tryRatKingFormation] Forming rat king! Round=" + Globals.roundNum + ", Allies=" + allyCount + ", 3x3Grid=" + has3x3Grid + ", TotalSpawned=" + totalSpawned + ", CurrentKings=" + currentRatKingCount + ", Cheese=" + teamCheese);
            try {
                CommArray.incrementRatKingCount();
                Globals.rc.becomeRatKing();
                System.out.println("[DEBUG KingManager.tryRatKingFormation] ✓ Successfully became rat king!");
                return true;
            } catch (GameActionException e) {
                System.out.println("[DEBUG KingManager.tryRatKingFormation] ERROR: " + e.getMessage());
                try {
                    CommArray.decrementRatKingCount();
                } catch (GameActionException e2) {}
                return false;
            }
        }
        return false;
    }
    private static boolean check3x3Grid(MapLocation[] allyLocs, int allyCount) throws GameActionException {
        if (allyCount < 8) return false;
        
        MapLocation center = Globals.myLoc;
        int[][] grid = new int[3][3];
        
        grid[1][1] = 1;
        
        for (int i = 0; i < allyCount && i < 9; i++) {
            if (allyLocs[i] == null) continue;
            int dx = allyLocs[i].x - center.x;
            int dy = allyLocs[i].y - center.y;
            if (dx >= -1 && dx <= 1 && dy >= -1 && dy <= 1) {
                int gridX = dx + 1;
                int gridY = dy + 1;
                if (gridX >= 0 && gridX < 3 && gridY >= 0 && gridY < 3) {
                    grid[gridX][gridY] = 1;
                }
            }
        }
        
        int filled = 0;
        int emptyEdges = 0;
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                if (grid[x][y] == 1) {
                    filled++;
                } else {
                    boolean isEdge = (x == 0 || x == 2 || y == 0 || y == 2);
                    if (isEdge) {
                        emptyEdges++;
                    }
                }
            }
        }
        
        boolean result = filled >= 8 && emptyEdges <= 1;
        if (Globals.roundNum % 50 == 0 && allyCount >= 7) {
            System.out.println("[DEBUG KingManager.check3x3Grid] grid check: filled=" + filled + ", emptyEdges=" + emptyEdges + ", result=" + result);
        }
        return result;
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
        int minBuffer;
        if (roundsLeft > 1500) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * 200;
        } else if (roundsLeft > 1000) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * 300;
        } else if (roundsLeft > 500) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * 400;
        } else if (roundsLeft > 100) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * 500;
        } else {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * (roundsLeft + 30);
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
                CommArray.incrementSpawnCount();
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