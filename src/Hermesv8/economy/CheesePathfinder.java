package Hermesv8.economy;
import battlecode.common.*;
import Hermesv8.*;
import Hermesv8.nav.*;

/**
 * Efficient pathfinding specifically for cheese collection
 * Uses GradientBFS when available, falls back to smart greedy navigation
 */
public class CheesePathfinder {
    private static MapLocation currentTarget = null;
    private static int lastComputedRound = -1;

    /**
     * Navigate to best cheese location using gradient field
     */
    public static boolean navigateToCheese() throws GameActionException {
        if (!Globals.rc.isMovementReady()) return false;

        MapLocation target = CheeseTracker.getBestCheeseTarget();
        if (target == null) {
            // No cheese known, explore
            return exploreForCheese();
        }

        // Update gradient field if target changed or stale
        if (!target.equals(currentTarget) || Globals.roundNum > lastComputedRound + 5) {
            if (Clock.getBytecodesLeft() > 3000) {
                GradientBFS.computeDistTarget(target);
                currentTarget = target;
                lastComputedRound = Globals.roundNum;
            }
        }

        // Try gradient-based movement first
        if (currentTarget != null && currentTarget.equals(target) && Clock.getBytecodesLeft() > 500) {
            Direction gradientDir = GradientBFS.pickGradientMoveTarget(true);
            if (gradientDir != null && Globals.rc.canMove(gradientDir)) {
                Globals.rc.move(gradientDir);
                return true;
            }
        }

        // Fallback to direct greedy movement
        return greedyMoveTo(target);
    }

    /**
     * Greedy movement towards target with obstacle avoidance
     */
    private static boolean greedyMoveTo(MapLocation target) throws GameActionException {
        if (target == null || !Globals.rc.isMovementReady()) return false;

        Direction direct = Globals.myLoc.directionTo(target);
        int currentDist = Globals.myLoc.distanceSquaredTo(target);

        // Try direct direction first
        if (direct != Direction.CENTER && Globals.rc.canMove(direct)) {
            Globals.rc.move(direct);
            return true;
        }

        // Try directions that get closer
        Direction[] candidates = {
            direct.rotateLeft(),
            direct.rotateRight(),
            direct.rotateLeft().rotateLeft(),
            direct.rotateRight().rotateRight()
        };

        for (Direction dir : candidates) {
            if (dir == Direction.CENTER) continue;
            MapLocation nextLoc = Globals.myLoc.add(dir);
            if (nextLoc.distanceSquaredTo(target) < currentDist && Globals.rc.canMove(dir)) {
                Globals.rc.move(dir);
                return true;
            }
        }

        // Last resort: any valid move
        for (Direction dir : Globals.ALL_DIRECTIONS) {
            if (dir == Direction.CENTER) continue;
            if (Globals.rc.canMove(dir)) {
                Globals.rc.move(dir);
                return true;
            }
        }

        return false;
    }

    /**
     * Explore map systematically to find cheese
     */
    private static boolean exploreForCheese() throws GameActionException {
        if (!Globals.rc.isMovementReady()) return false;

        // Use spiral exploration pattern based on rat ID
        MapLocation center = Globals.mapCenter;
        int angle = (Globals.myID * 73 + Globals.roundNum / 10) % 360;
        double rad = Math.toRadians(angle);
        int radius = 10 + (Globals.roundNum / 30);

        int targetX = center.x + (int)(Math.cos(rad) * radius);
        int targetY = center.y + (int)(Math.sin(rad) * radius);

        targetX = Math.max(0, Math.min(Globals.mapWidth - 1, targetX));
        targetY = Math.max(0, Math.min(Globals.mapHeight - 1, targetY));

        MapLocation exploreTarget = new MapLocation(targetX, targetY);
        return greedyMoveTo(exploreTarget);
    }

    /**
     * Quick check if there's cheese immediately adjacent
     */
    public static MapLocation getAdjacentCheese() throws GameActionException {
        if (Globals.nearbyMapInfos == null) return null;

        for (MapInfo info : Globals.nearbyMapInfos) {
            if (info == null) continue;
            MapLocation loc = info.getMapLocation();
            if (loc == null) continue;

            if (info.getCheeseAmount() > 0 && Globals.myLoc.isAdjacentTo(loc)) {
                if (Globals.rc.canPickUpCheese(loc)) {
                    return loc;
                }
            }
        }

        return null;
    }

    /**
     * Reset pathfinder state
     */
    public static void reset() {
        currentTarget = null;
        lastComputedRound = -1;
    }
}
