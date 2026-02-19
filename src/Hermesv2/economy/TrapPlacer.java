package Hermesv2.economy;

import battlecode.common.*;
import Hermesv2.*;
import Hermesv2.comms.*;

public class TrapPlacer {

    private static int estimatedRatTraps = 0;
    private static int estimatedCatTraps = 0;

    public static boolean tryPlaceRatTrap() throws GameActionException {
        if (!Globals.rc.isActionReady()) return false;
        if (Globals.globalCheese < Constants.RAT_TRAP_COST) return false;
        if (estimatedRatTraps >= 25) return false;
        
        MapLocation bestLoc = findBestRatTrapLocation();
        if (bestLoc == null) return false;
        
        if (Globals.rc.canPlaceRatTrap(bestLoc)) {
            Globals.rc.placeRatTrap(bestLoc);
            estimatedRatTraps++;
            return true;
        }
        
        return false;
    }

    public static boolean tryPlaceCatTrap() throws GameActionException {
        if (!Globals.rc.isActionReady()) return false;
        if (!Globals.isCooperation) return false;
        if (Globals.globalCheese < Constants.CAT_TRAP_COST) return false;
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
            if (distToKing <= 16) {
                score += 150 - distToKing;
            } else if (distToKing <= 25) {
                score += 50;
            }
            
            for (int j = 0; j < enemies.length; j++) {
                int dist = loc.distanceSquaredTo(enemies[j].getLocation());
                if (dist <= 4) {
                    score += 30;
                }
            }
            
            if (Clock.getBytecodesLeft() > 1000) {
                int adjacentWalls = countAdjacentWalls(loc);
                if (adjacentWalls >= 2) {
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
        Direction[] dirs = Globals.ALL_DIRECTIONS;
        
        for (int i = 0; i < dirs.length && Clock.getBytecodesLeft() > 500; i++) {
            Direction dir = dirs[i];
            if (dir == Direction.CENTER) continue;
            
            MapLocation loc = myLoc.add(dir);
            
            if (!Globals.rc.canPlaceCatTrap(loc)) continue;
            
            int score = 0;
            
            int distToCatSpawn = loc.distanceSquaredTo(catSpawn);
            if (distToCatSpawn <= 100) {
                score += 120 - distToCatSpawn / 2;
            }
            
            for (int j = 0; j < cats.length; j++) {
                int dist = loc.distanceSquaredTo(cats[j].getLocation());
                if (dist <= 25) {
                    score += 100;
                } else if (dist <= 36) {
                    score += 50;
                }
            }
            
            if (Globals.numKnownCheeseMines > 0 && Clock.getBytecodesLeft() > 300) {
                MapLocation nearestMine = CommArray.getNearestCheeseMine();
                if (nearestMine != null) {
                    int distToMine = loc.distanceSquaredTo(nearestMine);
                    int distSpawnToMine = catSpawn.distanceSquaredTo(nearestMine);
                    if (distToMine <= distSpawnToMine / 2) {
                        score += 40;
                    }
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
        if (Globals.globalCheese < Constants.DIRT_COST * 3) return false;
        
        MapLocation kingLoc = Globals.myLoc;
        int bestScore = 0;
        MapLocation bestWallLoc = null;
        
        for (Direction dir : Globals.ALL_DIRECTIONS) {
            if (dir == Direction.CENTER) continue;
            
            MapLocation loc = kingLoc.add(dir);
            if (!Globals.rc.canPlaceDirt(loc)) continue;
            
            int score = 0;
            for (RobotInfo cat : Globals.nearbyCats) {
                int dist = loc.distanceSquaredTo(cat.getLocation());
                if (dist <= 9) {
                    score += 50;
                }
            }
            
            if (!Globals.isCooperation) {
                for (RobotInfo enemy : Globals.nearbyEnemies) {
                    int dist = loc.distanceSquaredTo(enemy.getLocation());
                    if (dist <= 9) {
                        score += 30;
                    }
                }
            }
            
            if (score > bestScore) {
                bestScore = score;
                bestWallLoc = loc;
            }
        }
        
        if (bestWallLoc != null && bestScore > 20) {
            return tryPlaceDirt(bestWallLoc);
        }
        
        return false;
    }

    public static void resetEstimates() {
        estimatedRatTraps = 0;
        estimatedCatTraps = 0;
    }
}
