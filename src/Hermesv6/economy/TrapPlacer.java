package Hermesv6.economy;
import battlecode.common.*;
import Hermesv6.*;
import Hermesv6.comms.*;
public class TrapPlacer {
    private static int estimatedRatTraps = 0;
    private static int estimatedCatTraps = 0;
    public static boolean tryPlaceRatTrap() throws GameActionException {
        return false;
    }
    public static boolean tryPlaceCatTrap() throws GameActionException {
        if (!Globals.rc.isActionReady()) return false;
        int availableCheese = Globals.rc.getAllCheese();
        int roundsLeft = Constants.MAX_ROUNDS - Globals.roundNum;
        int minBuffer;
        if (roundsLeft > 1500) {
            minBuffer = Math.max(Constants.RATKING_CHEESE_CONSUMPTION * 30,
                                 Constants.RATKING_CHEESE_CONSUMPTION * (roundsLeft / 10 + 20));
        } else if (roundsLeft > 1000) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * (roundsLeft / 6 + 25);
        } else if (roundsLeft > 500) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * (roundsLeft / 4 + 30);
        } else if (roundsLeft > 100) {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * (roundsLeft / 3 + 35);
        } else {
            minBuffer = Constants.RATKING_CHEESE_CONSUMPTION * (roundsLeft + 30);
        }
        if (availableCheese < Constants.CAT_TRAP_COST + minBuffer) return false;
        if (estimatedCatTraps >= 10) return false;
        MapLocation bestLoc = findBestCatTrapLocation();
        if (bestLoc == null) return false;
        if (Globals.rc.canPlaceCatTrap(bestLoc)) {
            Globals.rc.placeCatTrap(bestLoc);
            estimatedCatTraps++;
            return true;
        }
        return false;
    }
    private static MapLocation findBestRatTrapLocation() throws GameActionException {
        MapLocation best = null;
        int bestScore = Integer.MIN_VALUE;
        MapLocation myLoc = Globals.myLoc;
        MapLocation nearestKing = myLoc;
        for (int i = 0; i < Globals.numKnownAlliedKings; i++) {
            MapLocation kingLoc = Globals.knownAlliedKings[i];
            if (kingLoc != null) {
                int dist = myLoc.distanceSquaredTo(kingLoc);
                if (dist < myLoc.distanceSquaredTo(nearestKing)) {
                    nearestKing = kingLoc;
                }
            }
        }
        RobotInfo[] enemies = Globals.nearbyEnemies;
        Direction[] dirs = Globals.ALL_DIRECTIONS;
        for (int i = 0; i < dirs.length && Clock.getBytecodesLeft() > 500; i++) {
            Direction dir = dirs[i];
            if (dir == Direction.CENTER) continue;
            MapLocation loc = myLoc.add(dir);
            if (!Globals.rc.canPlaceRatTrap(loc)) continue;
            int score = 0;
            int distToKing = loc.distanceSquaredTo(nearestKing);
            if (distToKing <= 4) {
                score += 200;
            } else if (distToKing <= 9) {
                score += 150;
            } else if (distToKing <= 16) {
                score += 100;
            } else if (distToKing <= 25) {
                score += 50;
            }
            for (int j = 0; j < enemies.length; j++) {
                int dist = loc.distanceSquaredTo(enemies[j].getLocation());
                if (dist <= 2) {
                    score += 50;
                } else if (dist <= 4) {
                    score += 30;
                } else if (dist <= 9) {
                    score += 15;
                }
            }
            if (Clock.getBytecodesLeft() > 1000) {
                int adjacentWalls = countAdjacentWalls(loc);
                int adjacentDirt = countAdjacentDirt(loc);
                if (adjacentWalls >= 2) {
                    score += 30;
                }
                if (adjacentDirt >= 1) {
                    score += 20;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = loc;
            }
        }
        return best;
    }
    private static int countAdjacentWalls(MapLocation loc) throws GameActionException {
        int count = 0;
        for (Direction dir : Globals.ALL_DIRECTIONS) {
            if (dir == Direction.CENTER) continue;
            MapLocation adj = loc.add(dir);
            if (Globals.rc.canSenseLocation(adj)) {
                MapInfo info = Globals.rc.senseMapInfo(adj);
                if (info != null && info.isWall()) {
                    count++;
                }
            }
        }
        return count;
    }
    private static MapLocation findBestCatTrapLocation() throws GameActionException {
        MapLocation best = null;
        int bestScore = Integer.MIN_VALUE;
        MapLocation myLoc = Globals.myLoc;
        MapLocation catSpawn = Globals.catSpawnLocation;
        RobotInfo[] cats = Globals.nearbyCats;
        MapLocation king = CommArray.getNearestAlliedKing();
        Direction[] dirs = Globals.ALL_DIRECTIONS;
        for (int i = 0; i < dirs.length && Clock.getBytecodesLeft() > 500; i++) {
            Direction dir = dirs[i];
            if (dir == Direction.CENTER) continue;
            MapLocation loc = myLoc.add(dir);
            if (!Globals.rc.canPlaceCatTrap(loc)) continue;
            int score = 0;
            if (king != null) {
                int distKing = loc.distanceSquaredTo(king);
                if (distKing <= 2) score += 200;
                else if (distKing <= 4) score += 150;
                else if (distKing <= 9) score += 80;
            }
            int distToCatSpawn = loc.distanceSquaredTo(catSpawn);
            if (distToCatSpawn <= 81) {
                score += 50;
            }
            for (int j = 0; j < cats.length; j++) {
                int dist = loc.distanceSquaredTo(cats[j].getLocation());
                if (dist <= 25) {
                    score += 60;
                } else if (dist <= 36) {
                    score += 30;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = loc;
            }
        }
        return best;
    }
    public static boolean tryPlaceDirt(MapLocation loc) throws GameActionException {
        if (Globals.globalCheese < Constants.DIRT_COST) return false;
        if (!Globals.rc.canPlaceDirt(loc)) return false;
        Globals.rc.placeDirt(loc);
        return true;
    }
    public static boolean tryPlaceDefensiveWall() throws GameActionException {
        if (Globals.globalCheese < Constants.DIRT_COST) return false;
        MapLocation kingLoc = Globals.myLoc;
        int bestScore = 0;
        MapLocation bestWallLoc = null;
        for (Direction dir : Globals.ALL_DIRECTIONS) {
            if (dir == Direction.CENTER) continue;
            MapLocation loc = kingLoc.add(dir);
            if (!Globals.rc.canPlaceDirt(loc)) continue;
            int score = 0;
            if (Globals.nearbyCats != null) {
                for (RobotInfo cat : Globals.nearbyCats) {
                    int dist = loc.distanceSquaredTo(cat.getLocation());
                    if (dist <= 4) score += 80;
                    else if (dist <= 9) score += 50;
                    else if (dist <= 16) score += 30;
                }
            }
            if (!Globals.isCooperation && Globals.nearbyEnemies != null) {
                for (RobotInfo enemy : Globals.nearbyEnemies) {
                    int dist = loc.distanceSquaredTo(enemy.getLocation());
                    if (enemy.getType() == UnitType.RAT_KING) {
                        if (dist <= 4) score += 100;
                        else if (dist <= 9) score += 60;
                    } else {
                        if (dist <= 4) score += 40;
                        else if (dist <= 9) score += 25;
                    }
                }
            }
            if (Clock.getBytecodesLeft() > 500) {
                int adjacentWalls = countAdjacentWalls(loc);
                int adjacentDirt = countAdjacentDirt(loc);
                score += (adjacentWalls + adjacentDirt) * 15;
            }
            if (score > bestScore) {
                bestScore = score;
                bestWallLoc = loc;
            }
        }
        if (bestWallLoc != null && bestScore > 15) {
            return tryPlaceDirt(bestWallLoc);
        }
        return false;
    }
    private static int countAdjacentDirt(MapLocation loc) throws GameActionException {
        int count = 0;
        for (Direction dir : Globals.ALL_DIRECTIONS) {
            if (dir == Direction.CENTER) continue;
            MapLocation adj = loc.add(dir);
            if (Globals.rc.canSenseLocation(adj)) {
                MapInfo info = Globals.rc.senseMapInfo(adj);
                if (info != null && info.isDirt()) {
                    count++;
                }
            }
        }
        return count;
    }
    public static boolean tryCreateChokepoint(MapLocation kingLoc, MapLocation threatLoc) throws GameActionException {
        if (Globals.globalCheese < Constants.DIRT_COST * 2) return false;
        if (threatLoc == null) return false;
        Direction toThreat = kingLoc.directionTo(threatLoc);
        Direction[] sideDirs = {toThreat.rotateLeft(), toThreat.rotateRight()};
        for (Direction sideDir : sideDirs) {
            MapLocation chokepoint = kingLoc.add(sideDir);
            if (Globals.rc.canPlaceDirt(chokepoint)) {
                if (tryPlaceDirt(chokepoint)) {
                    return true;
                }
            }
        }
        return false;
    }
    public static void resetEstimates() {
        estimatedRatTraps = 0;
        estimatedCatTraps = 0;
    }
}