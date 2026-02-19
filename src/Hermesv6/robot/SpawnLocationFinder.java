package Hermesv6.robot;
import battlecode.common.*;
import Hermesv6.*;
import Hermesv6.comms.*;
public class SpawnLocationFinder {
    public static MapLocation findBest() throws GameActionException {
        MapLocation[] adjLocs = Globals.rc.getAllLocationsWithinRadiusSquared(Globals.myLoc, Constants.BUILD_RADIUS_SQ);
        MapLocation best = null;
        int bestScore = Integer.MIN_VALUE;
        for (MapLocation loc : adjLocs) {
            if (!Globals.rc.canBuildRat(loc)) continue;
            int score = 0;
            if (Globals.numKnownCheeseMines > 0) {
                MapLocation nearestMine = CommArray.getNearestCheeseMine();
                if (nearestMine != null) {
                    score -= loc.distanceSquaredTo(nearestMine);
                }
            }
            for (RobotInfo cat : Globals.nearbyCats) {
                score += loc.distanceSquaredTo(cat.getLocation());
            }
            int distToCatSpawn = loc.distanceSquaredTo(Globals.catSpawnLocation);
            score += distToCatSpawn / 2;
            for (RobotInfo ally : Globals.nearbyAllies) {
                if (loc.distanceSquaredTo(ally.getLocation()) < 2) {
                    score -= 100;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                best = loc;
            }
        }
        return best;
    }
}