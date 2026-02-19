package Hermesv7.nav;
import battlecode.common.*;
import Hermesv7.*;
public class Navigator {
    private static MapLocation lastTarget = null;
    private static MapLocation lastLocation = null;
    private static int stuckCounter = 0;
    private static Direction unstuckDirection = null;

    public static boolean navigateTo(MapLocation target) throws GameActionException {
        if (target == null || !Globals.rc.isMovementReady()) return false;
        if (Globals.myLoc.equals(target)) return true;

        // Detect if stuck
        if (lastLocation != null && lastLocation.equals(Globals.myLoc) &&
            lastTarget != null && lastTarget.equals(target)) {
            stuckCounter++;
        } else {
            stuckCounter = 0;
            unstuckDirection = null;
        }

        lastLocation = Globals.myLoc;
        lastTarget = target;

        // If stuck, try unstuck maneuver
        if (stuckCounter >= 3) {
            if (tryUnstuck(target)) {
                stuckCounter = 0;
                return true;
            }
        }

        // Try direct path first
        Direction direct = Globals.myLoc.directionTo(target);
        if (direct != Direction.CENTER && Mover.canMoveInDirection(direct)) {
            if (Mover.tryMove(direct)) {
                stuckCounter = 0;
                return true;
            }
        }

        // Try greedy approach - find best direction that moves closer
        if (Clock.getBytecodesLeft() > 500) {
            Direction bestDir = findBestDirection(target);
            if (bestDir != null && Mover.tryMove(bestDir)) {
                stuckCounter = 0;
                return true;
            }
        }

        // Try MinosNav for medium-range pathfinding
        if (Clock.getBytecodesLeft() > 1500) {
            if (MinosNav.navigateTo(target)) {
                stuckCounter = 0;
                return true;
            }
        }

        // Try A* for complex pathfinding
        if (Clock.getBytecodesLeft() >= 3000) {
            Direction dir = AStarPathfinder.findBestDirection(target);
            if (dir != null && dir != Direction.CENTER) {
                if (Mover.tryMove(dir)) {
                    stuckCounter = 0;
                    return true;
                }
            }
        }

        // Fallback to BugNav
        Direction bugResult = BugNav.walkTowards(target);
        if (bugResult != null) {
            stuckCounter = 0;
        }
        return bugResult != null;
    }

    private static Direction findBestDirection(MapLocation target) throws GameActionException {
        int currentDist = Globals.myLoc.distanceSquaredTo(target);
        Direction bestDir = null;
        int bestDist = currentDist;
        int bestScore = Integer.MIN_VALUE;

        // Try all 8 directions
        for (Direction dir : Direction.allDirections()) {
            if (dir == Direction.CENTER) continue;

            MapLocation newLoc = Globals.myLoc.add(dir);
            if (!Globals.rc.canSenseLocation(newLoc)) continue;
            if (!Globals.rc.sensePassability(newLoc)) continue;
            if (!Mover.canMoveInDirection(dir)) continue;

            int newDist = newLoc.distanceSquaredTo(target);
            int score = currentDist - newDist;

            // Bonus for moving towards target
            if (newDist < bestDist) {
                score += 100;
            }

            // Penalty for moving near allies (collision avoidance)
            if (Globals.nearbyAllies != null) {
                for (RobotInfo ally : Globals.nearbyAllies) {
                    if (ally != null && ally.getType() == UnitType.BABY_RAT) {
                        int allyDist = newLoc.distanceSquaredTo(ally.getLocation());
                        if (allyDist <= 2) {
                            score -= 50;
                        }
                    }
                }
            }

            // Prefer forward movement
            if (dir == Globals.myDir) {
                score += 10;
            }

            if (score > bestScore) {
                bestScore = score;
                bestDir = dir;
                bestDist = newDist;
            }
        }

        return bestDir;
    }

    private static boolean tryUnstuck(MapLocation target) throws GameActionException {
        // Try to move perpendicular to stuck direction
        if (unstuckDirection == null) {
            Direction toTarget = Globals.myLoc.directionTo(target);
            // Try rotating 90 degrees
            unstuckDirection = toTarget.rotateRight().rotateRight();
        }

        // Try the unstuck direction and its neighbors
        for (int i = 0; i < 5; i++) {
            Direction tryDir = unstuckDirection;
            if (i == 1) tryDir = unstuckDirection.rotateLeft();
            if (i == 2) tryDir = unstuckDirection.rotateRight();
            if (i == 3) tryDir = unstuckDirection.rotateLeft().rotateLeft();
            if (i == 4) tryDir = unstuckDirection.rotateRight().rotateRight();

            if (Mover.canMoveInDirection(tryDir)) {
                if (Mover.tryMove(tryDir)) {
                    System.out.println("[Hermesv7] Rat #" + Globals.myID + " unstuck using " + tryDir);
                    unstuckDirection = null;
                    return true;
                }
            }
        }

        // Try any available direction
        for (Direction dir : Direction.allDirections()) {
            if (dir != Direction.CENTER && Mover.canMoveInDirection(dir)) {
                if (Mover.tryMove(dir)) {
                    System.out.println("[Hermesv7] Rat #" + Globals.myID + " unstuck using random " + dir);
                    unstuckDirection = null;
                    return true;
                }
            }
        }

        unstuckDirection = null;
        return false;
    }
}