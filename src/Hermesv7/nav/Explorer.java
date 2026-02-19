package Hermesv7.nav;
import battlecode.common.*;
import Hermesv7.*;
import Hermesv7.comms.*;
public class Explorer {
    private static MapLocation explorationTarget = null;

    public static boolean explore() throws GameActionException {
        if (!Globals.rc.isMovementReady()) return false;

        System.out.println("[DEBUG Explorer] Rat #" + Globals.myID + " exploring from " + Globals.myLoc);

        // Report cheese mines
        if (Globals.nearbyMapInfos != null && Clock.getBytecodesLeft() > 300) {
            for (int i = 0; i < Globals.nearbyMapInfos.length && Clock.getBytecodesLeft() > 200; i++) {
                MapInfo info = Globals.nearbyMapInfos[i];
                if (info != null && info.hasCheeseMine()) {
                    try {
                        CommArray.reportCheeseMine(info.getMapLocation());
                    } catch (GameActionException e) {
                    }
                }
            }
        }

        // If we haven't set an exploration target yet, pick one based on our ID
        if (explorationTarget == null || Globals.myLoc.distanceSquaredTo(explorationTarget) <= 25) {
            explorationTarget = pickExplorationTarget();
            System.out.println("[DEBUG Explorer] Rat #" + Globals.myID + " picked new exploration target: " + explorationTarget);
        }

        // Check for enemy king
        MapLocation enemyKing = CommArray.getNearestEnemyKing();
        if (enemyKing != null && !Globals.isCooperation) {
            System.out.println("[DEBUG Explorer] Rat #" + Globals.myID + " found enemy king at " + enemyKing + ", attacking!");
            try {
                Squeaker.squeakAttack(enemyKing);
            } catch (GameActionException e) {}
            return Navigator.navigateTo(enemyKing);
        }

        // Navigate towards our unique exploration target
        if (Globals.nearbyCats != null && Globals.nearbyCats.length > 0) {
            MapLocation[] catLocs = Globals.getLocations(Globals.nearbyCats);
            Direction fleeDir = findDirectionAwayFromCats(catLocs, explorationTarget);
            if (fleeDir != null && fleeDir != Direction.CENTER) {
                if (Mover.tryMove(fleeDir)) {
                    System.out.println("[DEBUG Explorer] Rat #" + Globals.myID + " fleeing from cat");
                    return true;
                }
            }
        }

        System.out.println("[DEBUG Explorer] Rat #" + Globals.myID + " moving towards target " + explorationTarget);
        return Navigator.navigateTo(explorationTarget);
    }

    private static MapLocation pickExplorationTarget() {
        // Use robot ID to pick a unique sector of the map to explore
        int sector = Globals.myID % 8;
        int targetX, targetY;

        // Divide map into 8 sectors and assign each rat to a different one
        switch (sector) {
            case 0: // Top-left
                targetX = Globals.mapWidth / 4;
                targetY = 3 * Globals.mapHeight / 4;
                break;
            case 1: // Top
                targetX = Globals.mapWidth / 2;
                targetY = Globals.mapHeight - 3;
                break;
            case 2: // Top-right
                targetX = 3 * Globals.mapWidth / 4;
                targetY = 3 * Globals.mapHeight / 4;
                break;
            case 3: // Right
                targetX = Globals.mapWidth - 3;
                targetY = Globals.mapHeight / 2;
                break;
            case 4: // Bottom-right
                targetX = 3 * Globals.mapWidth / 4;
                targetY = Globals.mapHeight / 4;
                break;
            case 5: // Bottom
                targetX = Globals.mapWidth / 2;
                targetY = 3;
                break;
            case 6: // Bottom-left
                targetX = Globals.mapWidth / 4;
                targetY = Globals.mapHeight / 4;
                break;
            case 7: // Left
                targetX = 3;
                targetY = Globals.mapHeight / 2;
                break;
            default:
                targetX = Globals.mapWidth - 1 - Globals.myLoc.x;
                targetY = Globals.mapHeight - 1 - Globals.myLoc.y;
        }

        // Make sure target is on the map
        targetX = Math.max(0, Math.min(Globals.mapWidth - 1, targetX));
        targetY = Math.max(0, Math.min(Globals.mapHeight - 1, targetY));

        return new MapLocation(targetX, targetY);
    }
    private static Direction findDirectionAwayFromCats(MapLocation[] catLocs, MapLocation target) throws GameActionException {
        if (catLocs == null || catLocs.length == 0) return null;
        @SuppressWarnings("unused")
        Direction direct = Globals.myLoc.directionTo(target);
        Direction bestDir = null;
        int bestScore = Integer.MIN_VALUE;
        for (Direction dir : Globals.ALL_DIRECTIONS) {
            if (dir == Direction.CENTER) continue;
            if (!Mover.canMoveInDirection(dir)) continue;
            MapLocation newLoc = Globals.myLoc.add(dir);
            int score = newLoc.distanceSquaredTo(target);
            for (MapLocation catLoc : catLocs) {
                if (catLoc != null) {
                    int distToCat = newLoc.distanceSquaredTo(catLoc);
                    if (distToCat <= 16) {
                        score -= (17 - distToCat) * 100;
                    }
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestDir = dir;
            }
        }
        return bestDir;
    }
    @SuppressWarnings("unused")
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