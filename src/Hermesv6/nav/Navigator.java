package Hermesv6.nav;
import battlecode.common.*;
import Hermesv6.*;
public class Navigator {
    public static boolean navigateTo(MapLocation target) throws GameActionException {
        if (target == null || !Globals.rc.isMovementReady()) return false;
        if (Globals.myLoc.equals(target)) return true;
        
        Direction direct = Globals.myLoc.directionTo(target);
        if (direct != Direction.CENTER && Mover.canMoveInDirection(direct)) {
            if (Mover.tryMove(direct)) {
                return true;
            }
        }
        
        if (Clock.getBytecodesLeft() > 500) {
            int bestDist = Globals.myLoc.distanceSquaredTo(target);
            Direction bestDir = null;
            int bestScore = Integer.MIN_VALUE;
            boolean avoidingCat = false;
            for (Direction dir : Direction.allDirections()) {
                if (dir == Direction.CENTER) continue;
                MapLocation newLoc = Globals.myLoc.add(dir);
                if (Globals.rc.canSenseLocation(newLoc) && Globals.rc.sensePassability(newLoc)) {
                    RobotInfo robot = Globals.rc.senseRobotAtLocation(newLoc);
                    if (robot != null && robot.getTeam() == Globals.myTeam) continue;
                    int newDist = newLoc.distanceSquaredTo(target);
                    int score = -newDist;
                    if (Globals.nearbyCats != null) {
                        for (RobotInfo cat : Globals.nearbyCats) {
                            if (cat != null) {
                                int distToCat = newLoc.distanceSquaredTo(cat.getLocation());
                                if (distToCat <= 16) {
                                    score -= (17 - distToCat) * 100;
                                    avoidingCat = true;
                                }
                            }
                        }
                    }
                    if (score > bestScore && Mover.canMoveInDirection(dir)) {
                        bestScore = score;
                        bestDist = newDist;
                        bestDir = dir;
                    }
                }
            }
            if (bestDir != null && Mover.tryMove(bestDir)) {
                if (avoidingCat && Globals.roundNum % 30 == 0) {
                    System.out.println("[DEBUG Navigator] Rat #" + Globals.myID + " avoiding cat while navigating to " + target);
                }
                return true;
            }
        }
        
        if (Clock.getBytecodesLeft() > 1500) {
            if (MinosNav.navigateTo(target)) {
                return true;
            }
        }
        
        Direction bugResult = BugNav.walkTowards(target);
        if (bugResult != null) {
            return true;
        }
        
        if (Clock.getBytecodesLeft() >= 3000) {
            Direction dir = AStarPathfinder.findBestDirection(target);
            if (dir != null && dir != Direction.CENTER) {
                if (Mover.tryMove(dir)) {
                    return true;
                }
            }
        }
        
        return false;
    }
}