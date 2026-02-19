package Hermesv8.robot;
import battlecode.common.*;
import Hermesv8.*;
import Hermesv8.comms.*;
import Hermesv8.nav.*;
import Hermesv8.nav.GradientBFS;
import Hermesv8.economy.*;
import Hermesv8.strategy.*;
import Hermesv8.util.DebugLogger;
public class KingManager {
    public static void manage() throws GameActionException {
        try {
            DebugLogger.logKingStatus(Globals.rc.getAllCheese(), Globals.myHealth, Constants.MAX_ROUNDS - Globals.roundNum);

            // Check for king expansion opportunity
            if (KingExpansion.shouldExpand()) {
                KingExpansion.findExpansionDirection();
                if (KingExpansion.executeExpansion()) {
                    return; // Expansion takes priority
                }
            }

            updateModeAndGradients();

        int availableCheese = Globals.rc.getAllCheese();
        int roundsLeft = Constants.MAX_ROUNDS - Globals.roundNum;
        int phase = GamePhase.calculate();
        int minBuffer;
        if (roundsLeft > 1800) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * 30;
        } else if (roundsLeft > 1500) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * 50;
        } else if (roundsLeft > 1000) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * 80;
        } else if (roundsLeft > 500) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * 120;
        } else if (roundsLeft > 100) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * 150;
        } else {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * (roundsLeft + 20);
        }
        boolean criticalCheese = availableCheese < minBuffer;
        if (criticalCheese) {
            try {
                CommArray.setFlag(CommArray.FLAG_DEFEND_KING, 2);
            } catch (GameActionException e) {
            }
            int emergencyThreshold = Constants.RATKING_CHEESE_CONSUMPTION * 50;
            if (availableCheese < emergencyThreshold) {
                return;
            }
        }
        try {
            if (availableCheese > minBuffer + 20) {
                CommArray.setFlag(CommArray.FLAG_DEFEND_KING, 0);
            }
        } catch (GameActionException e) {
        }
        int excessCheese = availableCheese - minBuffer;
        int spawnThreshold, trapThreshold, expensiveThreshold;

        if (criticalCheese) {
            spawnThreshold = 50;
            trapThreshold = 100;
            expensiveThreshold = 999999;
        } else if (phase == GamePhase.PHASE_EARLY) {
            spawnThreshold = 0;
            trapThreshold = 30;
            expensiveThreshold = 999999;
        } else if (phase == GamePhase.PHASE_MID) {
            spawnThreshold = 0;
            trapThreshold = 25;
            expensiveThreshold = 200;
        } else {
            spawnThreshold = 0;
            trapThreshold = 20;
            expensiveThreshold = 150;
        }
        boolean canSpawn = excessCheese >= spawnThreshold;
        boolean canPlaceTraps = excessCheese >= trapThreshold;
        @SuppressWarnings("unused")
        boolean canPlaceExpensive = excessCheese >= expensiveThreshold;
        int maxSpawns = (phase == GamePhase.PHASE_EARLY && excessCheese > 500) ? 2 : 1;
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
            boolean catVeryClose = false;
            if (Globals.nearbyCats != null) {
                for (RobotInfo cat : Globals.nearbyCats) {
                    if (cat != null) {
                        int dist = Globals.myLoc.distanceSquaredTo(cat.getLocation());
                        if (dist <= 4) {
                            catVeryClose = true;
                            break;
                        }
                    }
                }
            }
            if (catVeryClose) {
                moveTowardBetterPosition();
            }
        }
        } catch (GameActionException e) {
            DebugLogger.logException("KingManager.manage", e, "main_loop");
            throw e;
        } catch (Exception e) {
            DebugLogger.logException("KingManager.manage", e, "unexpected_error");
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
        @SuppressWarnings("unused")
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
            if (Globals.roundNum % 50 == 0) {
                System.out.println("[KingManager] Can't spawn: action not ready");
            }
            return false;
        }
        int availableCheese = Globals.rc.getAllCheese();
        int spawnCost = Globals.getSpawnCost();
        int roundsLeft = Constants.MAX_ROUNDS - Globals.roundNum;
        int phase = GamePhase.calculate();

        int minBuffer;
        if (roundsLeft > 1800) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * 30;
        } else if (roundsLeft > 1500) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * 50;
        } else if (roundsLeft > 1000) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * 80;
        } else if (roundsLeft > 500) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * 120;
        } else if (roundsLeft > 100) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * 150;
        } else {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * (roundsLeft + 20);
        }
        if (availableCheese < spawnCost + minBuffer) {
            if (Globals.roundNum % 50 == 0) {
                System.out.println("[KingManager] Can't spawn: insufficient cheese (" + availableCheese + " < " + (spawnCost + minBuffer) + ")");
            }
            return false;
        }
        int excessCheese = availableCheese - minBuffer - spawnCost;
        boolean shouldSpawn = false;

        // Count nearby allies to cap rat spawning
        int allyRatCount = 0;
        if (Globals.nearbyAllies != null) {
            for (RobotInfo ally : Globals.nearbyAllies) {
                if (ally != null && ally.getType() == UnitType.BABY_RAT) {
                    allyRatCount++;
                }
            }
        }
        int maxRatsNearby = 10; // Only spawn if fewer than this many rats nearby

        if (availableCheese < minBuffer) {
            shouldSpawn = false;
        } else if (allyRatCount >= maxRatsNearby) {
            shouldSpawn = false; // Stop spawning if too many rats nearby
        } else if (phase == GamePhase.PHASE_EARLY) {
            shouldSpawn = excessCheese >= 500;
        } else if (phase == GamePhase.PHASE_MID) {
            shouldSpawn = excessCheese >= 400;
        } else {
            shouldSpawn = excessCheese >= 300;
        }
        if (!shouldSpawn) {
            if (Globals.roundNum % 50 == 0) {
                System.out.println("[KingManager] Can't spawn: excessCheese check failed (" + excessCheese + ")");
            }
            return false;
        }
        MapLocation[] adjLocs = Globals.rc.getAllLocationsWithinRadiusSquared(Globals.myLoc, Constants.BUILD_RADIUS_SQ);
        if (adjLocs == null || adjLocs.length == 0) {
            if (Globals.roundNum % 50 == 0) {
                System.out.println("[KingManager] Can't spawn: no adjacent locations");
            }
            return false;
        }
        for (MapLocation loc : adjLocs) {
            if (loc != null && Globals.rc.canBuildRat(loc)) {
                DebugLogger.logSpawn("buildRat", loc, Globals.getSpawnCost(), false, "attempting");
                Globals.rc.buildRat(loc);
                System.out.println("[KingManager] ✓ Spawned rat at " + loc + " on round " + Globals.roundNum);
                DebugLogger.logSpawn("buildRat", loc, Globals.getSpawnCost(), true, "success");
                return true;
            }
        }
        if (Globals.roundNum % 50 == 0) {
            System.out.println("[KingManager] Can't spawn: no valid build locations (checked " + adjLocs.length + " locations)");
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

    private static void updateModeAndGradients() throws GameActionException {
        int teamCheese = Globals.rc.getAllCheese();
        boolean enemiesNearKing = (Globals.nearbyEnemies != null && Globals.nearbyEnemies.length > 0) ||
                                  (Globals.nearbyCats != null && Globals.nearbyCats.length > 0);

        int mode = Constants.MODE_ECONOMY;
        if (enemiesNearKing || teamCheese < Constants.CHEESE_LOW_THRESHOLD) {
            if (teamCheese < Constants.CHEESE_LOW_THRESHOLD / 2) {
                mode = Constants.MODE_ECONOMY;
            } else {
                mode = Constants.MODE_DEFEND;
            }
        } else if (teamCheese > Constants.CHEESE_HIGH_THRESHOLD) {
            RobotInfo[] nearbyAllies = Globals.nearbyAllies;
            int allyCount = (nearbyAllies != null) ? nearbyAllies.length : 0;
            if (allyCount >= 5) {
                mode = Constants.MODE_ATTACK;
            }
        }

        try {
            CommArray.setFlag(CommArray.MODE_INDEX, mode);
        } catch (GameActionException e) {
        }

        MapLocation attackTarget = determineAttackTarget();
        if (attackTarget != null) {
            try {
                CommArray.setFlag(CommArray.ATTACK_TARGET_X, attackTarget.x);
                CommArray.setFlag(CommArray.ATTACK_TARGET_Y, attackTarget.y);
            } catch (GameActionException e) {
            }
        }

        if (Globals.roundNum % Constants.GRADIENT_UPDATE_INTERVAL == 0 || 
            Globals.roundNum == 1) {
            if (Clock.getBytecodesLeft() > 2000) {
                int bytecodesBefore = Clock.getBytecodeNum();
                GradientBFS.computeDistHome(Globals.myLoc);
                int bytecodesAfter = Clock.getBytecodeNum();
                DebugLogger.logGradient("computeDistHome", Globals.myLoc, true, bytecodesAfter - bytecodesBefore);
            }
            if (mode == Constants.MODE_ATTACK && attackTarget != null && Clock.getBytecodesLeft() > 2000) {
                int bytecodesBefore = Clock.getBytecodeNum();
                GradientBFS.computeDistTarget(attackTarget);
                int bytecodesAfter = Clock.getBytecodeNum();
                DebugLogger.logGradient("computeDistTarget", attackTarget, true, bytecodesAfter - bytecodesBefore);
            }
        }
    }

    private static MapLocation determineAttackTarget() {
        MapLocation enemyKing = CommArray.getNearestEnemyKing();
        if (enemyKing != null) return enemyKing;

        if (Globals.numKnownCheeseMines > 0) {
            int totalX = 0, totalY = 0, count = 0;
            for (int i = 0; i < Globals.numKnownCheeseMines && i < 5; i++) {
                MapLocation mine = Globals.knownCheeseMines[i];
                if (mine != null) {
                    totalX += mine.x;
                    totalY += mine.y;
                    count++;
                }
            }
            if (count > 0) {
                return new MapLocation(totalX / count, totalY / count);
            }
        }

        int oppositeX = Globals.mapWidth - 1 - Globals.myLoc.x;
        int oppositeY = Globals.mapHeight - 1 - Globals.myLoc.y;
        return new MapLocation(oppositeX, oppositeY);
    }
}