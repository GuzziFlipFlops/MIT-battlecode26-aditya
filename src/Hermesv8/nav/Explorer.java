package Hermesv8.nav;
import battlecode.common.*;
import Hermesv8.*;
import Hermesv8.comms.*;
public class Explorer {
    public static boolean explore() throws GameActionException {
        if (!Globals.rc.isMovementReady()) return false;
        if (Globals.nearbyMapInfos != null && Clock.getBytecodesLeft() > 300) {
            for (int i = 0; i < Globals.nearbyMapInfos.length && Clock.getBytecodesLeft() > 200; i++) {
                MapInfo info = Globals.nearbyMapInfos[i];
                if (info != null && info.hasCheeseMine()) {
                    try {
                        CommArray.reportCheeseMine(info.getMapLocation());
                        Navigator.navigateTo(info.getMapLocation());
                        return true;
                    } catch (GameActionException e) {
                    }
                }
            }
        }
        MapLocation nearestMine = CommArray.getNearestCheeseMine();
        if (nearestMine != null) {
            return Navigator.navigateTo(nearestMine);
        }
        MapLocation myLoc = Globals.myLoc;
        MapLocation center = Globals.mapCenter;
        int angle = (Globals.myID * 37) % 360;
        double rad = Math.toRadians(angle);
        int radius = 15 + (Globals.roundNum / 50);
        int targetX = center.x + (int)(Math.cos(rad) * radius);
        int targetY = center.y + (int)(Math.sin(rad) * radius);
        targetX = Math.max(0, Math.min(Globals.mapWidth - 1, targetX));
        targetY = Math.max(0, Math.min(Globals.mapHeight - 1, targetY));
        MapLocation exploreTarget = new MapLocation(targetX, targetY);
        return Navigator.navigateTo(exploreTarget);
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